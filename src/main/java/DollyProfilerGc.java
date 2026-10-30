import java.lang.management.*;
import java.lang.reflect.*;
import java.util.*;
import javax.management.*;
import javax.management.openmbean.CompositeData;

/** JDK management extensions are optional; no HotSpot classes are linked at startup. */
final class DollyProfilerGc implements AutoCloseable {
    private final List<NotificationEmitter> emitters = new ArrayList<NotificationEmitter>();
    private final NotificationListener listener;
    DollyProfilerGc(DollyProfiler.Session session) {
        listener = (notification, handback) -> {
            if (!"com.sun.management.gc.notification".equals(notification.getType())) return;
            try {
                Class<?> type = Class.forName("com.sun.management.GarbageCollectionNotificationInfo");
                Object info = type.getMethod("from", CompositeData.class).invoke(null, notification.getUserData());
                Object gc = type.getMethod("getGcInfo").invoke(info);
                Class<?> gcType = Class.forName("com.sun.management.GcInfo");
                Event event = new Event((String) type.getMethod("getGcName").invoke(info),
                        (String) type.getMethod("getGcCause").invoke(info), (String) type.getMethod("getGcAction").invoke(info),
                        (Long) gcType.getMethod("getStartTime").invoke(gc), (Long) gcType.getMethod("getEndTime").invoke(gc),
                        used(gcType.getMethod("getMemoryUsageBeforeGc").invoke(gc)), used(gcType.getMethod("getMemoryUsageAfterGc").invoke(gc)));
                submit(session, event);
            } catch (ReflectiveOperationException | RuntimeException e) { session.gcDropped.increment(); }
        };
        try { Class.forName("com.sun.management.GarbageCollectionNotificationInfo"); Class.forName("com.sun.management.GcInfo"); }
        catch (ClassNotFoundException | LinkageError e) { return; }
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) if (gc instanceof NotificationEmitter) {
            NotificationEmitter emitter = (NotificationEmitter) gc;
            try { emitter.addNotificationListener(listener, null, null); emitters.add(emitter); }
            catch (RuntimeException ignored) { }
        }
    }
    private static long used(Object usages) {
        long used = 0;
        for (Object usage : ((Map<?, ?>) usages).values()) used += ((MemoryUsage) usage).getUsed();
        return used;
    }
    static void submit(DollyProfiler.Session s, Event event) {
        synchronized (s) {
            if (!s.closed && s.uptimeOrigin + event.end * 1000000 >= s.started && !s.gcEvents.offer(event)) s.gcDropped.increment();
        }
    }
    synchronized boolean available() { return !emitters.isEmpty(); }
    public synchronized void close() {
        for (NotificationEmitter emitter : emitters) try { emitter.removeNotificationListener(listener); }
        catch (ListenerNotFoundException | RuntimeException ignored) { }
        emitters.clear();
    }
    static final class Event {
        final String name, cause, action;
        final long start, end, before, after;
        Event(String name, String cause, String action, long start, long end, long before, long after) {
            this.name = name; this.cause = cause; this.action = action; this.start = start; this.end = end; this.before = before; this.after = after;
        }
        String json(DollyProfiler.Session s) {
            boolean stw = name.equals("Copy") || name.equals("MarkSweepCompact") || name.equals("PS Scavenge") || name.equals("PS MarkSweep");
            return "{\"type\":\"gc_event\",\"name\":" + DollyProfiler.quote(name) + ",\"cause\":" + DollyProfiler.quote(cause)
                    + ",\"action\":" + DollyProfiler.quote(action) + ",\"start_ns\":" + (s.uptimeOrigin + start * 1000000 - s.started)
                    + ",\"duration_ns\":" + ((end - start) * 1000000) + ",\"pause_ns\":" + (stw ? (end - start) * 1000000 : -1)
                    + ",\"memory_used_before_bytes\":" + before + ",\"memory_used_after_bytes\":" + after + "}";
        }
    }
}
