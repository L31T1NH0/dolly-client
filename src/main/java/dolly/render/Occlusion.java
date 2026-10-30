package dolly.render;

/** Proves a box is hidden only when a complete opaque voxel plane covers its projected silhouette. */
public final class Occlusion {
    // Enclose first-person near-plane/anaglyph offsets around the interpolated eye position.
    private static final double CAMERA_MARGIN = .25;
    public interface Blocks { boolean opaque(int x, int y, int z); boolean exhausted(); }

    /** Exact coordinates, bounded storage, and no results carried into another frame. */
    public static final class Cache implements Blocks {
        private final Blocks source;
        private final int[] xs = new int[1024], ys = new int[1024], zs = new int[1024], stamps = new int[1024];
        private final boolean[] values = new boolean[1024];
        private int frame;
        public Cache(Blocks source) { this.source = source; }
        public void reset() {
            if (++frame == 0) { java.util.Arrays.fill(stamps, 0); frame = 1; }
        }
        public boolean exhausted() { return source.exhausted(); }
        public boolean opaque(int x, int y, int z) {
            int slot = (x * 73428767 ^ y * 912931 ^ z * 438289) & 1023;
            if (stamps[slot] == frame && frame != 0 && xs[slot] == x && ys[slot] == y && zs[slot] == z) return values[slot];
            boolean value = source.opaque(x, y, z);
            xs[slot] = x; ys[slot] = y; zs[slot] = z; values[slot] = value; stamps[slot] = frame;
            return value;
        }
    }

    public static boolean hidden(double x, double y, double z, double minX, double minY, double minZ,
                                 double maxX, double maxY, double maxZ, Blocks blocks) {
        if (!Double.isFinite(x + y + z + minX + minY + minZ + maxX + maxY + maxZ)
                || minX >= maxX || minY >= maxY || minZ >= maxZ
                || maxX - minX > 16 || maxY - minY > 16 || maxZ - minZ > 16) return false;
        double dx = Math.max(minX - x, x - maxX), dy = Math.max(minY - y, y - maxY), dz = Math.max(minZ - z, z - maxZ);
        if (dx >= dy && dx >= dz) return planes(0, x, y, z, minX, minY, minZ, maxX, maxY, maxZ, blocks);
        if (dy >= dz) return planes(1, y, x, z, minY, minX, minZ, maxY, maxX, maxZ, blocks);
        return planes(2, z, x, y, minZ, minX, minY, maxZ, maxX, maxY, blocks);
    }

    private static boolean planes(int axis, double u, double v, double w, double minU, double minV, double minW,
                                  double maxU, double maxV, double maxW, Blocks blocks) {
        if (u + CAMERA_MARGIN >= minU && u - CAMERA_MARGIN <= maxU) return false;
        int step = u < minU ? -1 : 1;
        int cell = step < 0 ? (int) Math.floor(minU - .500001) : (int) Math.ceil(maxU - .499999);
        for (int i = 0; i < 64; i++, cell += step) {
            double plane = cell + .5;
            if (step < 0 ? plane <= u + CAMERA_MARGIN : plane >= u - CAMERA_MARGIN) break;
            if (blocks.exhausted()) return false;
            double t0 = (plane - u + CAMERA_MARGIN) / (minU - u + CAMERA_MARGIN);
            double t1 = (plane - u + CAMERA_MARGIN) / (maxU - u + CAMERA_MARGIN);
            double t2 = (plane - u - CAMERA_MARGIN) / (minU - u - CAMERA_MARGIN);
            double t3 = (plane - u - CAMERA_MARGIN) / (maxU - u - CAMERA_MARGIN);
            double lo = Math.min(Math.min(t0, t1), Math.min(t2, t3)), hi = Math.max(Math.max(t0, t1), Math.max(t2, t3));
            int a = (int) Math.floor(Math.min(project(v - CAMERA_MARGIN, minV, lo), project(v - CAMERA_MARGIN, minV, hi)) - .000001);
            int b = (int) Math.floor(Math.max(project(v + CAMERA_MARGIN, maxV, lo), project(v + CAMERA_MARGIN, maxV, hi)) + .000001);
            int c = (int) Math.floor(Math.min(project(w - CAMERA_MARGIN, minW, lo), project(w - CAMERA_MARGIN, minW, hi)) - .000001);
            int d = (int) Math.floor(Math.max(project(w + CAMERA_MARGIN, maxW, lo), project(w + CAMERA_MARGIN, maxW, hi)) + .000001);
            if ((long) (b - a + 1) * (d - c + 1) > 256) continue;
            boolean covered = true;
            for (int j = a; covered && j <= b; j++) for (int k = c; k <= d; k++) {
                if (blocks.exhausted()) return false;
                boolean opaque = axis == 0 ? blocks.opaque(cell, j, k)
                        : axis == 1 ? blocks.opaque(j, cell, k) : blocks.opaque(j, k, cell);
                if (!opaque) { covered = false; break; }
            }
            if (covered) return true;
        }
        return false;
    }

    private static double project(double eye, double target, double fraction) { return eye + (target - eye) * fraction; }
}
