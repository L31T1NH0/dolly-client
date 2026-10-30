import org.lwjgl.opengl.*;

/** Timestamp pairs avoid interfering with OptiFine's TIME_ELAPSED queries. Render thread only. */
public final class DollyProfilerGpu {
    interface Backend {
        boolean supported();
        int create();
        void stamp(int query);
        boolean available(int query);
        long result(int query);
        void delete(int query);
    }
    private static Backend backend;
    private static DollyProfiler.Session owner;
    private static Thread thread;
    private static final int[] begin = new int[16], end = new int[16];
    private static final long[] frames = new long[16];
    private static int current = -1;
    private static boolean unsupported, checked;
    public static void begin() {
        DollyProfiler.Session s = DollyProfiler.session();
        if (s != owner) { stop(); owner = s; thread = Thread.currentThread(); unsupported = false; checked = false; }
        if (s == null || unsupported || DollyProfiler.frameIndex() == 0 || current >= 0) return;
        try {
            if (backend == null) backend = new Lwjgl();
            if (!checked) { checked = true; if (!backend.supported()) { unavailable(1); unsupported = true; return; } }
            poll();
            for (int i = 0; i < frames.length; i++) if (frames[i] == 0) {
                if (begin[i] == 0) begin[i] = backend.create();
                if (end[i] == 0) end[i] = backend.create();
                frames[i] = DollyProfiler.frameIndex(); current = i;
                backend.stamp(begin[i]); s.gpuIssued.increment(); return;
            }
            unavailable(2);
        } catch (RuntimeException | LinkageError e) { unsupported = true; unavailable(3); cleanup(); }
    }
    public static void end() {
        if (current < 0 || thread != Thread.currentThread()) return;
        try { backend.stamp(end[current]); }
        catch (RuntimeException | LinkageError e) { unsupported = true; unavailable(3); cleanup(); }
        finally { current = -1; }
    }
    private static void poll() {
        for (int i = 0; i < frames.length; i++) if (frames[i] != 0 && i != current && backend.available(end[i]) && backend.available(begin[i])) {
            long duration = backend.result(end[i]) - backend.result(begin[i]);
            DollyProfiler.gpu(owner, frames[i], duration >= 0 ? duration : -1, duration >= 0 ? 0 : 3);
            frames[i] = 0;
        }
    }
    private static void unavailable(int reason) { if (owner != null) DollyProfiler.gpu(owner, DollyProfiler.frameIndex(), -1, reason); }
    public static void stop() {
        if (owner == null || thread != Thread.currentThread()) return;
        try {
            if (backend != null && !unsupported) {
                if (current >= 0) { backend.stamp(end[current]); current = -1; }
                poll();
                for (long frame : frames) if (frame != 0) DollyProfiler.gpu(owner, frame, -1, 4);
            }
        } catch (RuntimeException | LinkageError e) { unavailable(3); }
        finally { cleanup(); owner = null; unsupported = false; }
    }
    private static void cleanup() {
        for (int i = 0; i < frames.length; i++) {
            try { if (begin[i] != 0) backend.delete(begin[i]); } catch (RuntimeException | LinkageError ignored) { }
            try { if (end[i] != 0) backend.delete(end[i]); } catch (RuntimeException | LinkageError ignored) { }
            begin[i] = end[i] = 0; frames[i] = 0;
        }
        current = -1;
    }
    static void backendForTests(Backend replacement) { stop(); backend = replacement; }
    private static final class Lwjgl implements Backend {
        private final boolean core = GLContext.getCapabilities().OpenGL33;
        public boolean supported() {
            return GLContext.getCapabilities().OpenGL15 && (core || GLContext.getCapabilities().GL_ARB_timer_query)
                    && GL15.glGetQueryi(GL33.GL_TIMESTAMP, GL15.GL_QUERY_COUNTER_BITS) > 0;
        }
        public int create() { return GL15.glGenQueries(); }
        public void stamp(int query) { if (core) GL33.glQueryCounter(query, GL33.GL_TIMESTAMP); else ARBTimerQuery.glQueryCounter(query, GL33.GL_TIMESTAMP); }
        public boolean available(int query) { return GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT_AVAILABLE) != 0; }
        public long result(int query) { return core ? GL33.glGetQueryObjectui64(query, GL15.GL_QUERY_RESULT) : ARBTimerQuery.glGetQueryObjectui64(query, GL15.GL_QUERY_RESULT); }
        public void delete(int query) { GL15.glDeleteQueries(query); }
    }
}
