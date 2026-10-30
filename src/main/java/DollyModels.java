import java.util.*;

/** Share equal, baked vertex data only after model construction; the pool dies with this reload. */
public final class DollyModels {
    public static long savedBytes;

    public static void deduplicate(db<bov, boq> registry) {
        Map<Vertices, int[]> pool = new HashMap<Vertices, int[]>();
        Set<bgg> seen = Collections.newSetFromMap(new IdentityHashMap<bgg, Boolean>());
        savedBytes = 0;
        for (boq model : registry) {
            // Custom/dynamic model implementations may have stateful getters.
            if (model == null || model.getClass() != bow.class) continue;
            share(model.a(), pool, seen);
            for (cq face : cq.values()) share(model.a(face), pool, seen);
        }
    }

    private static void share(List<bgg> quads, Map<Vertices, int[]> pool, Set<bgg> seen) {
        for (bgg quad : quads) {
            if (quad.getClass() != bgg.class || !seen.add(quad)) continue;
            int[] data = quad.a();
            int[] shared = pool.get(new Vertices(data));
            if (shared == null) pool.put(new Vertices(data), data);
            else if (shared != data) {
                quad.a = shared;
                savedBytes += 16L + data.length * 4L;
            }
        }
    }

    private static final class Vertices {
        final int[] data;
        final int hash;
        Vertices(int[] data) { this.data = data; hash = Arrays.hashCode(data); }
        public int hashCode() { return hash; }
        public boolean equals(Object other) { return other instanceof Vertices && Arrays.equals(data, ((Vertices) other).data); }
    }
}
