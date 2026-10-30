package dolly.debug;

/** Fixed ring and monotonic frame intervals, independent of profiler recording. */
public final class FrameHistory {
    private final long[] samples = new long[240];
    private int write, size;
    private Object world;
    private long previous;
    public void frame(Object nextWorld, long now) {
        if (nextWorld == null || nextWorld != world) { world = nextWorld; previous = now; write = size = 0; return; }
        if (previous != 0 && now > previous) add(now - previous);
        previous = now;
    }
    public void add(long duration) { if (duration > 0) { samples[write] = duration; write = (write + 1) % samples.length; size = Math.min(samples.length, size + 1); } }
    public int size() { return size; }
    public long get(int index) { if (index < 0 || index >= size) throw new IndexOutOfBoundsException(); return samples[(write - size + index + samples.length) % samples.length]; }
    public static int height(long nanos, boolean fps) { return (int) Math.max(0, Math.min(60, nanos * (fps ? 1800d / 1_000_000_000d : 1200d / 1_000_000_000d))); }
    public static int color(int height) {
        int value = Math.max(0, Math.min(60, height));
        return 0xFF000000 | (value < 30 ? Math.round(value * 255f / 30) << 16 | 255 << 8 : 255 << 16 | Math.round((60 - value) * 255f / 30) << 8);
    }
}
