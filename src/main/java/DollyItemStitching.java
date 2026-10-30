import java.util.*;
import org.lwjgl.util.vector.Vector3f;

/** Correct generated-item contours/UVs before vanilla/OptiFine choose a baked vertex format. */
public final class DollyItemStitching {
    // M5 FaceBakery blends each UV with its opposite by .001: equivalently .002 toward the center.
    private static final float SHRINK = 0.002f;

    public static List<bgh> outline(int tint, String texture, bmi sprite) {
        if (!DollySettings.config.optimizedItems) return null;
        int width = sprite.c(), height = sprite.d();
        if (width <= 0 || height <= 0 || (long) width * height > Integer.MAX_VALUE) return null;
        BitSet[] edges = {new BitSet(), new BitSet(), new BitSet(), new BitSet()};
        List<int[][]> frames = sprite.getFramesTextureData();
        if (frames == null || frames.isEmpty()) return null;
        boolean translucent = false;
        for (int[][] levels : frames) {
            if (levels == null || levels.length == 0 || levels[0] == null || levels[0].length < (long) width * height) continue;
            int[] pixels = levels[0];
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                if (!solid(pixels, width, height, x, y)) continue;
                if ((pixels[y * width + x] >>> 24) != 255) translucent = true;
                int index = y * width + x;
                if (!solid(pixels, width, height, x, y - 1)) edges[0].set(index);
                if (!solid(pixels, width, height, x, y + 1)) edges[1].set(index);
                if (!solid(pixels, width, height, x - 1, y)) edges[2].set(index);
                if (!solid(pixels, width, height, x + 1, y)) edges[3].set(index);
            }
        }
        if (!translucent)
            mergeOpaqueSpans(edges, width, height);
        List<bgh> result = new ArrayList<bgh>();
        Map<cq, bgi> faces = new EnumMap<cq, bgi>(cq.class);
        faces.put(cq.d, face(tint, texture, 0, 0, 16, 16));
        faces.put(cq.c, face(tint, texture, 16, 0, 0, 16));
        result.add(new bgh(new Vector3f(0, 0, 7.5f), new Vector3f(16, 16, 8.5f), faces, null, true));
        float sx = 16f / width, sy = 16f / height;
        for (int side = 0; side < 2; side++) for (int y = 0; y < height; y++) {
            for (int x = 0; x < width;) {
                if (!edges[side].get(y * width + x)) { x++; continue; }
                int start = x++;
                while (x < width && edges[side].get(y * width + x)) x++;
                float boundary = 16f - (y + side) * sy;
                result.add(part(side == 0 ? cq.b : cq.a, tint, texture,
                        start * sx, boundary, x * sx, boundary,
                        start * sx, (y + 0.5f) * sy, x * sx, (y + 0.5f) * sy));
            }
        }
        for (int side = 2; side < 4; side++) for (int x = 0; x < width; x++) {
            for (int y = 0; y < height;) {
                if (!edges[side].get(y * width + x)) { y++; continue; }
                int start = y++;
                while (y < height && edges[side].get(y * width + x)) y++;
                float boundary = (x + side - 2) * sx;
                result.add(part(side == 2 ? cq.e : cq.f, tint, texture,
                        boundary, 16f - y * sy, boundary, 16f - start * sy,
                        (x + 0.5f) * sx, start * sy, (x + 0.5f) * sx, y * sy));
            }
        }
        return result;
    }

    private static void mergeOpaqueSpans(BitSet[] edges, int width, int height) {
        // Fully opaque sprites can share a strip across transparent gaps; partial alpha must keep separate spans.
        for (int side = 0; side < 2; side++) for (int y = 0; y < height; y++) {
            int start = edges[side].nextSetBit(y * width);
            int end = edges[side].previousSetBit((y + 1) * width - 1);
            if (start >= y * width && start <= end) edges[side].set(start, end + 1);
        }
        for (int side = 2; side < 4; side++) for (int x = 0; x < width; x++) {
            int start = -1, end = -1;
            for (int y = 0; y < height; y++) if (edges[side].get(y * width + x)) {
                if (start < 0) start = y;
                end = y;
            }
            for (int y = start; y >= 0 && y <= end; y++) edges[side].set(y * width + x);
        }
    }

    private static boolean solid(int[] pixels, int width, int height, int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height && (pixels[y * width + x] >>> 24) != 0;
    }

    private static bgi face(int tint, String texture, float u0, float v0, float u1, float v1) {
        return new bgi(null, tint, texture, new bgk(new float[] {u0, v0, u1, v1}, 0));
    }

    private static bgh part(cq direction, int tint, String texture, float x0, float y0, float x1, float y1,
                            float u0, float v0, float u1, float v1) {
        Map<cq, bgi> faces = new EnumMap<cq, bgi>(cq.class);
        faces.put(direction, face(tint, texture, u0, v0, u1, v1));
        return new bgh(new Vector3f(x0, y0, 7.5f), new Vector3f(x1, y1, 8.5f), faces, null, true);
    }

    public static List<bgh> unlerp(List<bgh> parts) {
        expandUvs(parts);
        return parts;
    }

    private static void expandUvs(List<bgh> parts) {
        for (bgh part : parts) for (bgi face : part.c.values()) {
            float[] uv = face.e.a;
            float centerU = (uv[0] + uv[2]) * 0.5f, centerV = (uv[1] + uv[3]) * 0.5f;
            for (int i = 0; i < 4; i++) uv[i] = (uv[i] - SHRINK * (i % 2 == 0 ? centerU : centerV)) / (1f - SHRINK);
        }
    }
}
