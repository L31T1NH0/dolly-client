import java.io.*;
import java.lang.management.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Numeric collection on game threads; only the writer formats JSON. */
public final class DollyProfiler {
    public enum Mode { NORMAL, DEEP }
    private static volatile Session active;
    private static Session draining;
    private static Path directory = Paths.get("dolly", "logs");
    public static volatile String status = "Profiler: off";
    public static volatile boolean recording;
    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();
    private static final List<String> SYMBOLS = new ArrayList<String>();
    private static final Map<String, Integer> IDS = new HashMap<String, Integer>();
    private static final ThreadLocal<State> LOCAL = new ThreadLocal<State>();
    static {
        SYMBOLS.add("unknown"); IDS.put("unknown", 0);
        try (InputStream in = DollyProfiler.class.getResourceAsStream("/dolly-profiler-symbols.txt")) {
            if (in != null) try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String name; while ((name = r.readLine()) != null) id(name);
            }
        } catch (IOException e) { failure(e); }
        id("frame");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Session s;
            synchronized (DollyProfiler.class) { stop(); s = draining; }
            if (s != null) try { s.writer.join(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "Dolly-profiler-shutdown"));
    }
    /** Registration is a cold path; injected hooks use build-time integer constants. */
    public static synchronized int id(String name) {
        Integer found = IDS.get(name);
        if (found != null) return found;
        if (active != null) return 0;
        int id = SYMBOLS.size(); SYMBOLS.add(name); IDS.put(name, id); return id;
    }
    public static synchronized void directory(File gameDirectory) { directory = gameDirectory.toPath().resolve("dolly/logs"); }
    public static synchronized void toggle(boolean deep) {
        if (active != null) { DollyProfilerGpu.stop(); stop(); }
        else start(deep ? Mode.DEEP : Mode.NORMAL);
    }
    public static synchronized boolean start(Mode mode) {
        if (active != null || (draining != null && draining.writer.isAlive())) return false;
        try {
            Session s = new Session(mode, directory);
            active = s; recording = true;
            status = "Profiler: " + mode + " (F8 para salvar)";
            s.gc = new DollyProfilerGc(s);
            s.writer.start();
            return true;
        } catch (IOException | RuntimeException e) {
            Session s = active; active = null; recording = false;
            if (s != null) { if (s.gc != null) s.gc.close(); try { s.output.close(); } catch (IOException ignored) { } }
            failure(e); return false;
        }
    }
    public static synchronized void stop() {
        Session s = active;
        if (s == null) return;
        recording = false; active = null;
        synchronized (s) { s.closed = true; s.pendingAtStop = s.pending.get(); }
        if (s.gc != null) s.gc.close();
        draining = s;
        status = "Profiler: salvando " + s.path.getFileName();
    }
    private static void failure(Exception e) {
        status = "Profiler: erro ao gravar (" + e.getClass().getSimpleName() + ")";
        System.err.println("[Dolly profiler] " + e);
    }
    static Session session() { return active; }
    private static State state(Session s) {
        State t = LOCAL.get();
        if (t != null && t.session == s) return t;
        synchronized (s) {
            if (s.closed) return null;
            if (s.states.size() >= 64) { s.untracked.increment(); return null; }
            t = new State(s); s.states.add(t); LOCAL.set(t); return t;
        }
    }
    public static long begin(int id, boolean deepOnly) {
        Session s = active;
        if (s == null || (deepOnly && (s.mode != Mode.DEEP || !s.traceWindow || id < 0 || id >= s.enabled.length || !s.enabled[id]))) return 0;
        State t = state(s);
        if (t == null) return 0;
        if (t.window != s.frameSequence) { t.window = s.frameSequence; t.budget = 0; }
        if (deepOnly && t.budget >= s.maxScopes) { t.skipped++; return 0; }
        if (t.depth == t.ids.length) { t.overflow++; return 0; }
        int d = t.depth++;
        long now = System.nanoTime(), token = Math.max(now, t.lastToken + 1);
        t.lastToken = token; t.tokens[d] = token; t.starts[d] = now; t.children[d] = 0;
        t.ids[d] = id >= 0 && id < s.names.length ? id : 0;
        t.trace[d] = s.mode == Mode.DEEP && s.traceWindow && s.enabled[t.ids[d]] && t.budget++ < s.maxScopes;
        return token;
    }
    public static void end(long token) {
        if (token == 0) return;
        Session s = active;
        State t = LOCAL.get();
        if (s == null || t == null || t.session != s || token < s.started) return;
        int target = t.depth - 1;
        while (target >= 0 && t.tokens[target] != token) target--;
        if (target < 0) return;
        long now = System.nanoTime();
        synchronized (t) {
            if (t.retired) return;
            // Unwind unfinished GL scopes when their enclosing method exits exceptionally.
            while (t.depth > target) {
                int d = --t.depth, id = t.ids[d];
                long duration = Math.max(0, now - t.starts[d]);
                long exclusive = Math.max(0, duration - t.children[d]);
                if (d > 0) t.children[d - 1] += duration;
                Metric m = t.metrics[id];
                if (m == null) t.metrics[id] = m = new Metric();
                m.count++; m.total += duration; m.exclusive += exclusive; m.max = Math.max(m.max, duration);
                m.exclusiveMax = Math.max(m.exclusiveMax, exclusive);
                t.stage(id, duration);
                if (t.trace[d]) t.offer(1, id, t.starts[d] - s.started, duration, exclusive, d > target ? 1 : 0);
            }
        }
    }
    public static void gl(boolean draw) {
        Session s = active; if (s == null) return;
        State t = state(s); if (t == null) return;
        synchronized (t) { if (!t.retired) { t.gl++; if (draw) t.draws++; } }
    }
    public static long frameBegin() {
        Session s = active; if (s == null) return 0;
        State t = state(s); if (t == null) return 0;
        s.frameSequence++;
        s.traceWindow = s.mode == Mode.DEEP && (s.frameSequence - 1) % s.everyFrame == 0;
        if (s.traceWindow) s.sampledFrames++;
        t.frame = s.frameSequence;
        t.cpu = THREADS.isCurrentThreadCpuTimeSupported() && THREADS.isThreadCpuTimeEnabled() ? THREADS.getCurrentThreadCpuTime() : -1;
        Arrays.fill(t.stages, 0);
        t.frameGl = s.gl(false); t.frameDraws = s.gl(true);
        long token = begin(s.frameId, false);
        t.frameToken = token; t.frameStarted = System.nanoTime();
        return token;
    }
    public static long frameIndex() {
        State t = LOCAL.get(); return t != null && t.session == active && t.frameToken != 0 ? t.frame : 0;
    }
    public static void frameEnd(long token) {
        if (token == 0) return;
        Session s = active; State t = LOCAL.get();
        if (s == null || t == null || t.session != s || t.frameToken != token) return;
        long now = System.nanoTime(), duration = now - t.frameStarted;
        long cpu = t.cpu < 0 ? -1 : THREADS.getCurrentThreadCpuTime() - t.cpu;
        end(token);
        synchronized (t) {
            if (t.retired) return;
            t.frames++; t.frameTotal += duration; t.frameMax = Math.max(t.frameMax, duration);
            t.histogram[(int) Math.min(999, duration / 1000000)]++;
            int row = t.reserve();
            if (row >= 0) {
                long[] b = t.events;
                b[row] = 2; b[row + 1] = t.frame; b[row + 2] = t.frameStarted - s.started; b[row + 3] = duration;
                b[row + 4] = cpu; b[row + 5] = s.gl(false) - t.frameGl; b[row + 6] = s.gl(true) - t.frameDraws;
                for (int i = 0; i < 6; i++) b[row + 7 + i] = t.stages[i];
                for (int i = 0; i < 6; i++) b[row + 13 + i] = t.scene[i];
                b[row + 19] = s.pending.get(); b[row + 20] = s.pendingMax.get();
                t.published++;
            }
            t.frameToken = 0;
        }
    }
    public static void scene(int loadedEntities, int chunks, int pendingChunks, int renderedEntities, int tileEntities, int particles) {
        State t = LOCAL.get(); if (t == null || t.session != active) return;
        t.scene[0] = loadedEntities; t.scene[1] = chunks; t.scene[2] = pendingChunks;
        t.scene[3] = renderedEntities; t.scene[4] = tileEntities; t.scene[5] = particles;
    }
    static void gpu(Session s, long frame, long duration, int reason) {
        if (s != active) return;
        State t = state(s); if (t == null) return;
        synchronized (t) { if (!t.retired) { t.offer(3, 0, frame, duration, reason, 0); if (reason == 0) s.gpuResults.increment(); } }
    }
    static void network(Session s, int id, long queued, long start, long duration) {
        if (s != active) return;
        State t = state(s); if (t == null) return;
        synchronized (t) {
            if (t.retired) return;
            t.networkCalls++; t.queueTotal += start - queued; t.queueMax = Math.max(t.queueMax, start - queued);
            t.handlerTotal += duration; t.handlerMax = Math.max(t.handlerMax, duration);
            if (s.mode == Mode.DEEP && s.traceWindow && t.budget < s.maxScopes) {
                t.budget++; t.offer(4, id, queued - s.started, start - queued, duration, start - s.started);
            }
        }
    }
    public static void traffic(boolean outbound, long bytes) {
        Session s = active; if (s == null) return;
        State t = state(s); if (t == null) return;
        synchronized (t) {
            if (t.retired) return;
            if (outbound) { t.packetsOut++; t.bytesOut += Math.max(0, bytes); }
            else { t.packetsIn++; t.bytesIn += Math.max(0, bytes); }
        }
    }
    private static final class Metric { long count, total, exclusive, max, exclusiveMax; }
    static final class State {
        static final int CAPACITY = 512, STRIDE = 24;
        final Session session;
        final long thread = Thread.currentThread().getId();
        final Metric[] metrics;
        final long[] events = new long[CAPACITY * STRIDE];
        final int[] ids = new int[256];
        final long[] tokens = new long[256], starts = new long[256], children = new long[256];
        final boolean[] trace = new boolean[256];
        final long[] stages = new long[6], scene = new long[6], histogram = new long[1000];
        volatile long published, consumed, gl, draws;
        boolean retired;
        int depth, budget;
        long lastToken, window = -1, dropped;
        volatile long skipped, overflow;
        long cpu, frame, frameToken, frameStarted, frameGl, frameDraws, frames, frameTotal, frameMax;
        long networkCalls, queueTotal, queueMax, handlerTotal, handlerMax, packetsIn, packetsOut, bytesIn, bytesOut;
        State(Session s) { session = s; metrics = new Metric[s.names.length]; }
        int reserve() {
            if (published - consumed >= CAPACITY) { dropped++; return -1; }
            return (int) (published % CAPACITY) * STRIDE;
        }
        void offer(int type, int id, long a, long b, long c, long d) {
            int r = reserve(); if (r < 0) return;
            events[r] = type; events[r + 1] = id; events[r + 2] = a; events[r + 3] = b; events[r + 4] = c; events[r + 5] = d;
            published++;
        }
        void stage(int id, long duration) {
            for (int i = 0; i < session.stageIds.length; i++) if (id == session.stageIds[i]) { stages[i == 6 ? 4 : i] += duration; break; }
        }
    }
    static final class Session {
        final Mode mode;
        final Path path;
        final long started = System.nanoTime(), epoch = System.currentTimeMillis();
        final long uptimeOrigin;
        final String[] names;
        final int frameId, everyFrame, maxScopes;
        final boolean[] enabled;
        final String categories;
        final int[] stageIds = new int[7];
        final ConcurrentLinkedQueue<State> states = new ConcurrentLinkedQueue<State>();
        final ArrayBlockingQueue<DollyProfilerGc.Event> gcEvents = new ArrayBlockingQueue<DollyProfilerGc.Event>(256);
        final LongAdder untracked = new LongAdder(), gcDropped = new LongAdder(), gpuIssued = new LongAdder(), gpuResults = new LongAdder();
        final AtomicInteger pending = new AtomicInteger(), pendingMax = new AtomicInteger();
        final Thread writer;
        final BufferedWriter output;
        volatile boolean closed, traceWindow;
        volatile long frameSequence;
        long sampledFrames;
        int pendingAtStop;
        DollyProfilerGc gc;
        Session(Mode mode, Path dir) throws IOException {
            this.mode = mode; names = SYMBOLS.toArray(new String[0]); frameId = id("frame");
            categories = System.getProperty("dolly.profiler.deepCategories", "all");
            Set<String> selected = new HashSet<String>(Arrays.asList(categories.toLowerCase(Locale.ROOT).split(",")));
            enabled = new boolean[names.length];
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                String category = name.startsWith("org/lwjgl/opengl/") ? "gl" : name.startsWith("Dolly") ? "dolly"
                        : name.startsWith("network.") || name.startsWith("packet.") ? "network"
                        : name.startsWith("ave.") || name.equals("frame") || name.equals("tick") ? "client" : "render";
                enabled[i] = selected.contains("all") || selected.contains(category);
            }
            String[] stages = {"tick", "render.frame", "render.chunk_layer", "render.entities", "render.particles", "render.hud", "render.particles_lit"};
            for (int i = 0; i < stages.length; i++) stageIds[i] = IDS.containsKey(stages[i]) ? IDS.get(stages[i]) : -1;
            everyFrame = integerProperty("dolly.profiler.deepEveryFrame", 10, 1, 10000);
            maxScopes = integerProperty("dolly.profiler.maxScopesPerFrame", 512, 1, 1000000);
            traceWindow = mode == Mode.DEEP;
            long before = System.nanoTime(), uptime = ManagementFactory.getRuntimeMXBean().getUptime();
            uptimeOrigin = before + (System.nanoTime() - before) / 2 - uptime * 1000000;
            Files.createDirectories(dir);
            path = Files.createTempFile(dir, "profile-" + epoch + "-" + mode + "-", ".jsonl");
            output = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
            writer = new Thread(this::write, "Dolly-profiler-writer"); writer.setDaemon(true);
        }
        long gl(boolean draw) { long total = 0; for (State t : states) total += draw ? t.draws : t.gl; return total; }
        void line(String value) throws IOException { output.write(value); output.newLine(); }
        void write() {
            try (BufferedWriter ignored = output) {
                line("{\"type\":\"session\",\"schema\":2,\"mode\":" + quote(mode.name()) + ",\"epoch_ms\":" + epoch
                        + ",\"java\":" + quote(System.getProperty("java.version")) + ",\"os\":" + quote(System.getProperty("os.name"))
                        + ",\"processors\":" + Runtime.getRuntime().availableProcessors() + ",\"max_heap_bytes\":" + Runtime.getRuntime().maxMemory()
                        + ",\"gc_notifications\":" + (gc != null && gc.available()) + ",\"gc_clock_resolution_ns\":1000000,\"deep_categories\":" + quote(categories) + ",\"deep_every_frame\":" + everyFrame + ",\"max_scopes_per_thread_frame\":" + maxScopes + ",\"ring_capacity_per_thread\":512}");
                for (int i = 0; i < names.length; i++) line("{\"type\":\"symbol\",\"id\":" + i + ",\"name\":" + quote(names[i]) + "}");
                long sampleAt = 0;
                while (!closed) {
                    drain(); long now = System.nanoTime();
                    if (now >= sampleAt) { sample(now); output.flush(); sampleAt = now + 1000000000L; }
                    Thread.sleep(5);
                }
                for (State t : states) synchronized (t) { t.retired = true; }
                drain(); sample(System.nanoTime()); summary(); output.flush();
                status = "Profiler: salvo " + path.getFileName();
            } catch (IOException | InterruptedException | RuntimeException e) {
                synchronized (DollyProfiler.class) { if (active == this) { active = null; recording = false; draining = this; } }
                synchronized (this) { closed = true; }
                for (State t : states) synchronized (t) { t.retired = true; }
                failure(e);
            } finally { if (gc != null) gc.close(); }
        }
        void drain() throws IOException {
            for (State t : states) {
                long limit = t.published;
                while (t.consumed < limit) {
                    int r = (int) (t.consumed % State.CAPACITY) * State.STRIDE;
                    event(t, r); t.consumed++;
                }
            }
            DollyProfilerGc.Event event; while ((event = gcEvents.poll()) != null) line(event.json(this));
        }
        void event(State t, int r) throws IOException {
            long[] b = t.events; int type = (int) b[r], id = (int) b[r + 1];
            if (type == 1) line("{\"type\":\"scope\",\"id\":" + id + ",\"start_ns\":" + b[r+2] + ",\"duration_ns\":" + b[r+3]
                    + ",\"exclusive_ns\":" + b[r+4] + ",\"unwound\":" + (b[r+5] != 0) + ",\"thread_id\":" + t.thread + "}");
            else if (type == 2) {
                String[] keys = {"index", "start_ns", "duration_ns", "thread_cpu_ns", "gl_calls", "draw_calls", "tick_ns", "render_ns", "chunks_ns", "entities_ns", "particles_ns", "hud_ns", "loaded_entities", "visible_chunks", "pending_chunks", "rendered_entities", "rendered_tile_entities", "particles", "network_pending", "network_pending_max"};
                StringBuilder row = new StringBuilder("{\"type\":\"frame\",\"thread_id\":" + t.thread);
                for (int i = 0; i < keys.length; i++) row.append(',').append(quote(keys[i])).append(':').append(b[r + 1 + i]);
                row.append(",\"fps\":").append(b[r+3] > 0 ? 1e9 / b[r+3] : 0).append('}'); line(row.toString());
            } else if (type == 3) line("{\"type\":\"gpu\",\"frame_index\":" + b[r+2] + ",\"duration_ns\":" + b[r+3] + ",\"reason\":" + b[r+4] + "}");
            else if (type == 4) line("{\"type\":\"packet\",\"id\":" + id + ",\"queued_ns\":" + b[r+2] + ",\"queue_delay_ns\":" + b[r+3]
                    + ",\"handler_ns\":" + b[r+4] + ",\"start_ns\":" + b[r+5] + ",\"thread_id\":" + t.thread + "}");
        }
        void summary() throws IOException {
            Metric[] totals = new Metric[names.length];
            long frames = 0, frameTotal = 0, frameMax = 0, dropped = gcDropped.sum(), skipped = 0, overflow = 0;
            long netCalls = 0, queueTotal = 0, queueMax = 0, handlerTotal = 0, handlerMax = 0, in = 0, out = 0, bytesIn = 0, bytesOut = 0;
            long[] histogram = new long[1000];
            for (State t : states) {
                frames += t.frames; frameTotal += t.frameTotal; frameMax = Math.max(frameMax, t.frameMax);
                dropped += t.dropped; skipped += t.skipped; overflow += t.overflow;
                netCalls += t.networkCalls; queueTotal += t.queueTotal; queueMax = Math.max(queueMax, t.queueMax);
                handlerTotal += t.handlerTotal; handlerMax = Math.max(handlerMax, t.handlerMax);
                in += t.packetsIn; out += t.packetsOut; bytesIn += t.bytesIn; bytesOut += t.bytesOut;
                for (int i = 0; i < 1000; i++) histogram[i] += t.histogram[i];
                for (int i = 0; i < totals.length; i++) if (t.metrics[i] != null) {
                    Metric m = totals[i]; if (m == null) totals[i] = m = new Metric();
                    Metric x = t.metrics[i]; m.count += x.count; m.total += x.total; m.exclusive += x.exclusive;
                    m.max = Math.max(m.max, x.max); m.exclusiveMax = Math.max(m.exclusiveMax, x.exclusiveMax);
                }
            }
            for (int i = 0; i < totals.length; i++) if (totals[i] != null) {
                Metric m = totals[i];
                line("{\"type\":\"aggregate\",\"id\":" + i + ",\"name\":" + quote(names[i]) + ",\"calls\":" + m.count
                        + ",\"total_ns\":" + m.total + ",\"exclusive_ns\":" + m.exclusive + ",\"max_ns\":" + m.max + ",\"exclusive_max_ns\":" + m.exclusiveMax + "}");
            }
            line("{\"type\":\"network_summary\",\"handled_packets\":" + netCalls + ",\"queue_total_ns\":" + queueTotal + ",\"queue_max_ns\":" + queueMax
                    + ",\"handler_total_ns\":" + handlerTotal + ",\"handler_max_ns\":" + handlerMax + ",\"pending_at_stop\":" + pendingAtStop
                    + ",\"pending_max\":" + pendingMax.get() + ",\"packets_in\":" + in + ",\"packets_out\":" + out + ",\"bytes_in\":" + bytesIn + ",\"bytes_out\":" + bytesOut + "}");
            line("{\"type\":\"summary\",\"frames\":" + frames + ",\"frame_total_ns\":" + frameTotal + ",\"frame_max_ns\":" + frameMax
                    + ",\"frame_p50_ms_bucket\":" + percentile(histogram, frames, .5) + ",\"frame_p95_ms_bucket\":" + percentile(histogram, frames, .95)
                    + ",\"frame_p99_ms_bucket\":" + percentile(histogram, frames, .99) + ",\"gl_calls\":" + gl(false) + ",\"draw_calls\":" + gl(true)
                    + ",\"dropped_records\":" + dropped + ",\"sampled_frames\":" + sampledFrames + ",\"deep_scopes_skipped\":" + skipped + ",\"stack_overflows\":" + overflow
                    + ",\"untracked_calls\":" + untracked.sum() + ",\"gpu_issued\":" + gpuIssued.sum() + ",\"gpu_collected\":" + gpuResults.sum() + "}");
        }
        void sample(long now) throws IOException {
            long calls = 0, queued = 0, handled = 0, in = 0, out = 0, inBytes = 0, outBytes = 0;
            for (State t : states) synchronized (t) {
                calls += t.networkCalls; queued += t.queueTotal; handled += t.handlerTotal;
                in += t.packetsIn; out += t.packetsOut; inBytes += t.bytesIn; outBytes += t.bytesOut;
            }
            line("{\"type\":\"network\",\"at_ns\":" + (now - started) + ",\"handled_packets\":" + calls
                    + ",\"queue_total_ns\":" + queued + ",\"handler_total_ns\":" + handled + ",\"pending\":" + pending.get()
                    + ",\"pending_max\":" + pendingMax.get() + ",\"packets_in\":" + in + ",\"packets_out\":" + out
                    + ",\"bytes_in\":" + inBytes + ",\"bytes_out\":" + outBytes + "}");
            MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            MemoryUsage nonheap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
            line("{\"type\":\"resources\",\"at_ns\":" + (now - started) + ",\"heap_used_bytes\":" + heap.getUsed()
                    + ",\"heap_committed_bytes\":" + heap.getCommitted() + ",\"nonheap_used_bytes\":" + nonheap.getUsed()
                    + ",\"process_cpu_load\":" + os("getProcessCpuLoad") + ",\"system_cpu_load\":" + os("getSystemCpuLoad")
                    + ",\"process_cpu_ns\":" + os("getProcessCpuTime") + ",\"process_committed_virtual_bytes\":" + os("getCommittedVirtualMemorySize") + "}");
            for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans())
                line("{\"type\":\"gc\",\"at_ns\":" + (now - started) + ",\"name\":" + quote(gc.getName())
                        + ",\"collections\":" + gc.getCollectionCount() + ",\"collection_ms\":" + gc.getCollectionTime() + "}");
        }
    }
    private static int integerProperty(String name, int fallback, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(System.getProperty(name, "" + fallback)))); }
        catch (RuntimeException e) { return fallback; }
    }
    private static int percentile(long[] bins, long frames, double p) {
        if (frames == 0) return -1;
        long sum = 0, target = (long) Math.ceil(frames * p);
        for (int i = 0; i < bins.length; i++) if ((sum += bins[i]) >= target) return i;
        return 999;
    }
    private static Number os(String name) {
        try {
            Method method = Class.forName("com.sun.management.OperatingSystemMXBean").getMethod(name);
            return (Number) method.invoke(ManagementFactory.getOperatingSystemMXBean());
        } catch (ReflectiveOperationException | RuntimeException e) { return -1; }
    }
    static String quote(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c < 32) b.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
            else b.append(c);
        }
        return b.append('"').toString();
    }
}
