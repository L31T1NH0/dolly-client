import java.nio.IntBuffer;

/** Coalesces only adjacent complete quads; transparent draw order is preserved. */
public final class DollyTerrain {
    static int merge(IntBuffer first, IntBuffer counts, int mode) {
        int size = counts.remaining(), a = first.position(), b = counts.position();
        if (mode != 7 || first.remaining() != size || size < 2) return size;
        int out = 0;
        for (int i = 0; i < size; i++) {
            int start = first.get(a + i), count = counts.get(b + i);
            if (out > 0) {
                int previous = counts.get(b + out - 1), base = first.get(a + out - 1);
                if (base >= 0 && start >= 0 && previous > 0 && count > 0 && previous % 4 == 0 && count % 4 == 0
                        && (long) base + previous == start && (long) previous + count <= Integer.MAX_VALUE) {
                    counts.put(b + out - 1, previous + count);
                    continue;
                }
            }
            if (out != i) { first.put(a + out, start); counts.put(b + out, count); }
            out++;
        }
        return out;
    }

    public static void draw(int mode, IntBuffer first, IntBuffer counts) {
        int firstLimit = first.limit(), countLimit = counts.limit();
        int n = DollySettings.config.mergeTerrain && !Config.isShaders() ? merge(first, counts, mode) : counts.remaining();
        try {
            first.limit(first.position() + n); counts.limit(counts.position() + n);
            bfl.glMultiDrawArrays(mode, first, counts);
        } finally { first.limit(firstLimit); counts.limit(countLimit); }
    }
}
