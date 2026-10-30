import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class DollyProfilerTest {
    private static final JsonParser JSON = new JsonParser();
    private static final int NORMAL = DollyProfiler.id("normal\"\\\n"), TICK = DollyProfiler.id("tick"), CHILD = DollyProfiler.id("test.child"),
            CONCURRENT = DollyProfiler.id("concurrent"), GL = DollyProfiler.id("org/lwjgl/opengl/GL11.glTest()V"), PACKET = DollyProfiler.id("packet.test");
    private static Path root;
    public static void main(String[] args) throws Exception {
        root = Files.createTempDirectory("dolly-profiler-test-");
        DollyProfiler.directory(root.toFile());
        try {
            normal(); concurrency(); exclusive(); sampling(); gpu(); network(); gc(); ring();
            Path blocked = root.resolve("blocked"); Files.createFile(blocked);
            DollyProfiler.directory(blocked.toFile());
            assert !DollyProfiler.start(DollyProfiler.Mode.NORMAL);
            assert !DollyProfiler.recording && DollyProfiler.status.startsWith("Profiler: erro");
        } finally {
            DollyProfilerGpu.stop(); DollyProfiler.stop();
            System.clearProperty("dolly.profiler.deepEveryFrame"); System.clearProperty("dolly.profiler.maxScopesPerFrame");
            System.clearProperty("dolly.profiler.deepCategories");
            try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
                for (Path p : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(p);
            }
        }
        System.out.println("Profiler passed: numeric rings, concurrency, modes/sampling, exclusive recursion/unwind, GPU async/lifecycle/fallback, GC events, network queue/errors/session isolation and I/O failure.");
    }
    private static void start(DollyProfiler.Mode mode) { assert DollyProfiler.start(mode); }
    private static List<JsonObject> finish() throws Exception {
        DollyProfiler.Session session = DollyProfiler.session();
        DollyProfiler.stop(); session.writer.join(5000);
        assert !session.writer.isAlive() && DollyProfiler.status.startsWith("Profiler: salvo") : DollyProfiler.status;
        List<JsonObject> rows = new ArrayList<JsonObject>();
        for (String line : Files.readAllLines(session.path)) rows.add(JSON.parse(line).getAsJsonObject());
        assert rows.get(rows.size() - 1).get("type").getAsString().equals("summary");
        return rows;
    }
    private static void normal() throws Exception {
        assert DollyProfiler.begin(NORMAL, false) == 0;
        start(DollyProfiler.Mode.NORMAL);
        assert !DollyProfiler.start(DollyProfiler.Mode.DEEP) && DollyProfiler.begin(CHILD, true) == 0;
        long frame = DollyProfiler.frameBegin();
        DollyProfiler.scene(10, 20, 3, 4, 2, 30);
        DollyProfiler.end(DollyProfiler.begin(NORMAL, false));
        DollyProfiler.end(DollyProfiler.begin(TICK, false)); DollyProfiler.gl(true); DollyProfiler.frameEnd(frame);
        List<JsonObject> rows = finish();
        assert type(rows, "summary").get("frames").getAsInt() == 1;
        assert type(rows, "summary").get("gl_calls").getAsInt() == 1;
        assert type(rows, "frame").get("particles").getAsInt() == 30;
        assert type(rows, "frame").get("tick_ns").getAsLong() > 0;
        assert aggregate(rows, NORMAL).get("name").getAsString().equals("normal\"\\\n");
        assert count(rows, "scope") == 0 && type(rows, "resources").has("process_cpu_ns");
    }
    private static void concurrency() throws Exception {
        start(DollyProfiler.Mode.DEEP);
        Thread[] workers = new Thread[4];
        for (int t = 0; t < workers.length; t++) {
            workers[t] = new Thread(() -> {
                for (int i = 0; i < 500; i++) {
                    DollyProfiler.end(DollyProfiler.begin(CONCURRENT, true)); DollyProfiler.gl(i % 2 == 0);
                }
            }); workers[t].start();
        }
        for (Thread worker : workers) worker.join();
        List<JsonObject> rows = finish();
        assert aggregate(rows, CONCURRENT).get("calls").getAsInt() == 2000;
        assert type(rows, "summary").get("gl_calls").getAsInt() == 2000;
        assert type(rows, "summary").get("draw_calls").getAsInt() == 1000;
        assert type(rows, "scope").has("thread_id");
    }
    private static void exclusive() throws Exception {
        start(DollyProfiler.Mode.DEEP);
        long parent = DollyProfiler.begin(NORMAL, false), child = DollyProfiler.begin(CHILD, false);
        long recursive = DollyProfiler.begin(CHILD, false);
        DollyProfiler.end(recursive); DollyProfiler.end(child); DollyProfiler.end(parent);
        long unwindParent = DollyProfiler.begin(TICK, false);
        DollyProfiler.begin(GL, true);
        DollyProfiler.end(unwindParent); // same path as a GL exception caught by an enclosing hook
        List<JsonObject> rows = finish();
        JsonObject p = aggregate(rows, NORMAL), c = aggregate(rows, CHILD);
        assert c.get("calls").getAsInt() == 2;
        long childOuter = rows.stream().filter(o -> is(o, "scope") && o.get("id").getAsInt() == CHILD)
                .mapToLong(o -> o.get("duration_ns").getAsLong()).max().getAsLong();
        assert p.get("exclusive_ns").getAsLong() == p.get("total_ns").getAsLong() - childOuter;
        assert c.get("exclusive_ns").getAsLong() == childOuter;
        assert rows.stream().anyMatch(o -> is(o, "scope") && o.get("id").getAsInt() == GL && o.get("unwound").getAsBoolean());
    }
    private static void sampling() throws Exception {
        System.setProperty("dolly.profiler.deepEveryFrame", "2"); System.setProperty("dolly.profiler.maxScopesPerFrame", "4");
        System.setProperty("dolly.profiler.deepCategories", "render,client");
        start(DollyProfiler.Mode.DEEP);
        long first = DollyProfiler.frameBegin();
        assert DollyProfiler.begin(GL, true) == 0;
        for (int i = 0; i < 12; i++) DollyProfiler.end(DollyProfiler.begin(CHILD, true));
        DollyProfiler.frameEnd(first);
        long second = DollyProfiler.frameBegin();
        assert DollyProfiler.begin(CHILD, true) == 0;
        DollyProfiler.end(DollyProfiler.begin(TICK, false)); DollyProfiler.frameEnd(second);
        List<JsonObject> rows = finish();
        assert type(rows, "summary").get("sampled_frames").getAsInt() == 1;
        assert type(rows, "summary").get("deep_scopes_skipped").getAsInt() > 0;
        assert aggregate(rows, TICK).get("calls").getAsInt() == 1;
        assert aggregate(rows, CHILD).get("calls").getAsInt() == 3; // frame consumes one budget slot
        System.clearProperty("dolly.profiler.deepEveryFrame"); System.clearProperty("dolly.profiler.maxScopesPerFrame");
        System.clearProperty("dolly.profiler.deepCategories");
    }
    private static final class FakeGpu implements DollyProfilerGpu.Backend {
        int created, deleted, reads, stamps;
        boolean ready, supported = true;
        final Map<Integer, Long> values = new HashMap<Integer, Long>();
        public boolean supported() { return supported; }
        public int create() { return ++created; }
        public void stamp(int id) { values.put(id, (long) ++stamps * 100); }
        public boolean available(int id) { return ready; }
        public long result(int id) { assert ready; reads++; return values.get(id); }
        public void delete(int id) { deleted++; }
    }
    private static void gpu() throws Exception {
        FakeGpu gpu = new FakeGpu(); DollyProfilerGpu.backendForTests(gpu);
        start(DollyProfiler.Mode.NORMAL);
        long first = DollyProfiler.frameBegin(); DollyProfilerGpu.begin(); DollyProfilerGpu.end(); DollyProfiler.frameEnd(first);
        long second = DollyProfiler.frameBegin(); DollyProfilerGpu.begin(); DollyProfilerGpu.end(); DollyProfiler.frameEnd(second);
        assert gpu.reads == 0;
        gpu.ready = true;
        long third = DollyProfiler.frameBegin(); DollyProfilerGpu.begin(); DollyProfilerGpu.end(); DollyProfiler.frameEnd(third);
        DollyProfilerGpu.stop();
        List<JsonObject> rows = finish();
        assert gpu.created == gpu.deleted && gpu.reads == 6;
        assert type(rows, "gpu").get("frame_index").getAsInt() == 1;
        assert type(rows, "gpu").get("duration_ns").getAsLong() == 100;
        FakeGpu unsupported = new FakeGpu(); unsupported.supported = false; DollyProfilerGpu.backendForTests(unsupported);
        start(DollyProfiler.Mode.NORMAL);
        long frame = DollyProfiler.frameBegin(); DollyProfilerGpu.begin(); DollyProfilerGpu.end(); DollyProfiler.frameEnd(frame);
        DollyProfilerGpu.stop(); rows = finish();
        assert type(rows, "gpu").get("reason").getAsInt() == 1 && unsupported.created == 0;
        FakeGpu full = new FakeGpu(); DollyProfilerGpu.backendForTests(full);
        start(DollyProfiler.Mode.NORMAL);
        for (int i = 0; i < 17; i++) {
            frame = DollyProfiler.frameBegin(); DollyProfilerGpu.begin(); DollyProfilerGpu.end(); DollyProfiler.frameEnd(frame);
        }
        DollyProfilerGpu.stop(); rows = finish();
        assert rows.stream().anyMatch(o -> is(o, "gpu") && o.get("reason").getAsInt() == 2);
        assert rows.stream().anyMatch(o -> is(o, "gpu") && o.get("reason").getAsInt() == 4);
        assert full.created == full.deleted;
        DollyProfilerGpu.backendForTests(null);
    }
    private static void network() throws Exception {
        start(DollyProfiler.Mode.DEEP);
        AtomicInteger executions = new AtomicInteger();
        Runnable queued = DollyProfilerNetwork.track(executions::incrementAndGet, PACKET);
        assert DollyProfiler.session().pending.get() == 1 && executions.get() == 0;
        queued.run();
        RuntimeException original = new RuntimeException("original");
        Runnable failing = DollyProfilerNetwork.track(() -> { throw original; }, PACKET);
        try { failing.run(); throw new AssertionError(); } catch (RuntimeException e) { assert e == original; }
        Runnable oldSession = DollyProfilerNetwork.track(executions::incrementAndGet, PACKET);
        DollyProfiler.traffic(false, 123); DollyProfiler.traffic(true, 456);
        List<JsonObject> rows = finish();
        JsonObject net = type(rows, "network_summary");
        assert net.get("handled_packets").getAsInt() == 2 && net.get("pending_at_stop").getAsInt() == 1;
        assert net.get("bytes_in").getAsInt() == 123 && net.get("bytes_out").getAsInt() == 456;
        assert count(rows, "packet") == 2 && type(rows, "packet").get("queue_delay_ns").getAsLong() >= 0;
        start(DollyProfiler.Mode.NORMAL); oldSession.run(); rows = finish();
        assert executions.get() == 2 && type(rows, "network_summary").get("handled_packets").getAsInt() == 0;
    }
    private static void gc() throws Exception {
        start(DollyProfiler.Mode.NORMAL);
        DollyProfiler.Session s = DollyProfiler.session();
        long uptime = (s.started - s.uptimeOrigin) / 1000000 + 1;
        DollyProfilerGc.submit(s, new DollyProfilerGc.Event("PS Scavenge", "test", "end of minor GC", uptime, uptime + 8, 200, 100));
        DollyProfilerGc.submit(s, new DollyProfilerGc.Event("ConcurrentMarkSweep", "test", "end of major GC", uptime, uptime + 20, 200, 100));
        List<JsonObject> rows = finish();
        JsonObject event = rows.stream().filter(o -> is(o, "gc_event") && o.get("cause").getAsString().equals("test") && o.get("name").getAsString().equals("PS Scavenge")).findFirst().get();
        assert event.get("duration_ns").getAsLong() == 8000000 && event.get("pause_ns").getAsLong() == 8000000;
        assert rows.stream().anyMatch(o -> is(o, "gc_event") && o.get("name").getAsString().equals("ConcurrentMarkSweep") && o.get("pause_ns").getAsLong() == -1);
    }
    private static void ring() throws Exception {
        DollyProfiler.Session s = new DollyProfiler.Session(DollyProfiler.Mode.DEEP, root);
        DollyProfiler.State t = new DollyProfiler.State(s); s.states.add(t);
        for (int i = 0; i < 600; i++) t.offer(1, CHILD, i, i, i, 0);
        assert t.published == 512 && t.dropped == 88;
        s.drain(); assert t.consumed == 512;
        t.offer(1, CHILD, 1000, 1000, 1000, 0); s.drain(); s.output.close();
        List<String> lines = Files.readAllLines(s.path);
        assert lines.size() == 513 && JSON.parse(lines.get(512)).getAsJsonObject().get("start_ns").getAsInt() == 1000;
    }
    private static boolean is(JsonObject row, String type) { return row.get("type").getAsString().equals(type); }
    private static long count(List<JsonObject> rows, String type) { return rows.stream().filter(o -> is(o, type)).count(); }
    private static JsonObject type(List<JsonObject> rows, String type) { return rows.stream().filter(o -> is(o, type)).findFirst().orElseThrow(AssertionError::new); }
    private static JsonObject aggregate(List<JsonObject> rows, int id) { return rows.stream().filter(o -> is(o, "aggregate") && o.get("id").getAsInt() == id).findFirst().orElseThrow(AssertionError::new); }
}
