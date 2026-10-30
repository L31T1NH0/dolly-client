import dolly.render.Occlusion;

/** Bounded render-thread queries: no world snapshots, stale results or cross-thread world access. */
public final class DollyCulling {
    private static int remaining;
    private static long deadline;
    private static float partialTicks;
    private static boolean cameraReady;
    private static Object cameraWorld;
    private static bew cameraPlayer;
    private static double eyeX, eyeY, eyeZ;
    private static final cj.a position = new cj.a();
    private static final Occlusion.Cache blocks = new Occlusion.Cache(new Occlusion.Blocks() {
        public boolean exhausted() { return !budgetAvailable(); }
        public boolean opaque(int x, int y, int z) {
            remaining--;
            position.c(x, y, z);
            bdb world = ave.A().f;
            return world.e(position) && world.p(position).c().c();
        }
    });

    public static void beginFrame(float partial) {
        remaining = 512; deadline = 0; partialTicks = partial; blocks.reset();
        cameraReady = false; cameraWorld = null; cameraPlayer = null;
    }

    static boolean budgetAvailable() {
        return remaining > 0 && (deadline == 0 || System.nanoTime() - deadline < 0);
    }

    static void camera(ave mc) {
        if (cameraReady && cameraWorld == mc.f && cameraPlayer == mc.h) return;
        if (cameraReady) blocks.reset();
        eyeX = mc.h.p + (mc.h.s - mc.h.p) * partialTicks;
        eyeY = mc.h.q + (mc.h.t - mc.h.q) * partialTicks + DollyCamera.eyeHeight(mc.h.aS());
        eyeZ = mc.h.r + (mc.h.u - mc.h.r) * partialTicks;
        cameraWorld = mc.f; cameraPlayer = mc.h; cameraReady = true;
    }

    static boolean eligible(pk entity) {
        // Player bodies and names always stay on their original render path, including NPC player models.
        if (entity instanceof wn || entity.l != null || entity.m != null) return false;
        return entity instanceof ps && !((ps) entity).cc() || entity instanceof um
                || entity instanceof uz || entity instanceof vj;
    }

    public static boolean entity(pk entity) {
        if (!DollySettings.config.entityCulling) return false;
        if (!eligible(entity) || !budgetAvailable()) return false;
        aug box = entity.aR();
        if (box == null) return false;
        // Include interpolation, held items and item bobbing, not just collision geometry.
        double pad = 1 + Math.max(Math.abs(entity.s - entity.p), Math.max(Math.abs(entity.t - entity.q), Math.abs(entity.u - entity.r)));
        pad = Math.max(pad, 1 + Math.max(Math.abs(entity.s - entity.P), Math.max(Math.abs(entity.t - entity.Q), Math.abs(entity.u - entity.R))));
        return hidden(box.a - pad, box.b - pad, box.c - pad, box.d + pad, box.e + pad, box.f + pad);
    }

    public static boolean tile(akw tile) {
        if (!DollySettings.config.entityCulling || !budgetAvailable()) return false;
        // Exclude beams, portals, moving pistons and unknown renderers whose geometry exceeds a block.
        if (!(tile instanceof aky || tile instanceof alf || tile instanceof aln || tile instanceof alo || tile instanceof aku || tile instanceof ale)) return false;
        cj p = tile.v();
        return hidden(p.n() - 1, p.o() - 1, p.p() - 1, p.n() + 2, p.o() + 3, p.p() + 2);
    }

    private static boolean hidden(double a, double b, double c, double d, double e, double f) {
        ave mc = ave.A();
        if (!DollySettings.config.entityCulling || mc.f == null || mc.h == null || mc.t.aB != 0
                || mc.h.v() || mc.h.bJ() || mc.t.d || mc.t.aH || mc.ac() != mc.h
                || net.optifine.shaders.Shaders.isShadowPass || remaining <= 0) return false;
        // Resolve lazily: the camera animation update runs after beginFrame's reset hook.
        camera(mc);
        double x = eyeX, y = eyeY, z = eyeZ;
        double dx = (a + d) * .5 - x, dy = (b + e) * .5 - y, dz = (c + f) * .5 - z;
        double distance = dx * dx + dy * dy + dz * dz;
        if (distance < 64 || distance > 4096) return false;
        if (deadline == 0) deadline = System.nanoTime() + 750000;
        return Occlusion.hidden(x, y, z, a, b, c, d, e, f, blocks);
    }
}
