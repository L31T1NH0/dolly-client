/** Observe scheduling without changing Minecraft's runnable, ordering or exception behavior. */
public final class DollyProfilerNetwork {
    private static final ClassValue<Integer> PACKETS = new ClassValue<Integer>() {
        protected Integer computeValue(Class<?> type) { return DollyProfiler.id("packet." + type.getName()); }
    };
    public static Runnable queue(Runnable task, Object packet, Object target) {
        if (!DollyProfiler.recording || target != ave.A()) return task;
        return track(task, PACKETS.get(packet.getClass()));
    }
    public static void traffic(boolean outbound, fg direction, int bytes) {
        if (!DollyProfiler.recording || bytes <= 0) return;
        if (direction.name().equals(outbound ? "SERVERBOUND" : "CLIENTBOUND")) DollyProfiler.traffic(outbound, bytes);
    }
    static Runnable track(Runnable task, int id) {
        DollyProfiler.Session s = DollyProfiler.session();
        if (s == null) return task;
        long queued = System.nanoTime();
        synchronized (s) {
            if (s.closed) return task;
            int pending = s.pending.incrementAndGet(); s.pendingMax.accumulateAndGet(pending, Math::max);
        }
        return () -> {
            s.pending.decrementAndGet();
            long start = System.nanoTime();
            long token = s == DollyProfiler.session() ? DollyProfiler.begin(id, false) : 0;
            try { task.run(); }
            finally {
                DollyProfiler.end(token);
                DollyProfiler.network(s, id, queued, start, System.nanoTime() - start);
            }
        };
    }
}
