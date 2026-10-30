import net.optifine.render.GlAlphaState;
import net.optifine.render.GlBlendState;

/** Render-thread-only HUD cache; the game world and destination-sensitive overlays are never cached. */
public final class DollyHudCache {
    public static boolean rendering;
    private static bfw buffer;
    private static Object world, player, hud;
    private static int width, height, scale;
    private static long refreshed;
    private static boolean valid, failed;
    private static int selectedSlot = -1;
    private static final Timing timing = new Timing();

    static final class Timing {
        private long previous;
        private double interval;
        private boolean saving;
        void reset() { previous = 0; interval = 0; saving = false; }
        boolean useful(long now, int refreshRate) {
            long elapsed = now - previous;
            boolean first = previous == 0;
            previous = now;
            if (first || elapsed <= 0 || elapsed > 250_000_000L) {
                interval = 0; saving = false; return false;
            }
            interval = interval == 0 ? elapsed : interval + (elapsed - interval) * .125;
            double period = 1_000_000_000d / Math.max(1, refreshRate);
            // Resume with headroom to avoid switching paths repeatedly near the refresh rate.
            saving = interval < period * (saving ? 1 : .9);
            return saving;
        }
    }

    public static void invalidate() { valid = false; failed = false; }
    public static void release() {
        if (buffer != null) buffer.a();
        buffer = null; world = player = hud = null; width = height = scale = 0;
        timing.reset();
        invalidate();
    }

    static boolean due(long now, long previous, boolean ready, int fps) {
        return !ready || now < previous || now - previous >= 1_000_000_000L / Math.max(1, fps);
    }

    static boolean useful(int frameLimit, int refreshRate) { return refreshRate < frameLimit; }

    public static void render(avo overlay, float partialTicks) {
        ave mc = ave.A();
        DollyHudPositions.beginFrame();
        if (!DollySettings.config.hudCaching || !bqs.i() || Config.isShaders() || mc.b() == null
                || !useful(mc.t.g, DollySettings.config.hudCacheFps)
                || mc.h == null || mc.f == null || mc.m != null || mc.t.aC || mc.h.cg() > 0
                || mc.ac() != mc.h || !(overlay instanceof DollyHudLayers)) {
            if (buffer != null) release();
            timing.reset();
            overlay.a(partialTicks); return;
        }
        avr resolution = new avr(mc);
        if (resolution.b() <= 60) { release(); overlay.a(partialTicks); return; }
        int currentScale = resolution.e();
        if (mc.f != world || mc.h != player || overlay != hud || mc.d != width || mc.e != height || currentScale != scale) {
            release(); world = mc.f; player = mc.h; hud = overlay; width = mc.d; height = mc.e; scale = currentScale;
        }
        if (selectedSlot != mc.h.bi.c) { selectedSlot = mc.h.bi.c; valid = false; }
        if (failed) { overlay.a(partialTicks); return; }
        long now = System.nanoTime();
        if (!timing.useful(now, DollySettings.config.hudCacheFps)) {
            // Keep an existing buffer for recovery, but never reuse its stale contents.
            valid = false; overlay.a(partialTicks); return;
        }
        if (buffer == null) {
            try {
                buffer = new bfw(width, height, true);
                buffer.a(0f, 0f, 0f, 0f);
            } catch (RuntimeException exception) {
                failed = true;
                System.err.println("[Dolly HUD cache] " + exception);
            } finally { mc.b().a(true); }
            if (failed) { overlay.a(partialTicks); return; }
        }
        ((DollyHudLayers) overlay).dolly$dynamic(partialTicks);
        if (due(now, refreshed, valid, DollySettings.config.hudCacheFps)) {
            GlBlendState blend = new GlBlendState(); bfl.getBlendState(blend);
            GlAlphaState alpha = new GlAlphaState(); bfl.getAlphaState(alpha);
            try {
                buffer.f(); buffer.a(true);
                rendering = true;
                bfl.a(true, true, true, true); bfl.l(); bfl.a(770, 771, 1, 771);
                overlay.a(partialTicks);
                refreshed = now; valid = true;
            } finally {
                rendering = false;
                mc.b().a(true); mc.o.j();
                bfl.setBlendState(blend); bfl.setAlphaState(alpha);
            }
        }
        GlBlendState blend = new GlBlendState(); bfl.getBlendState(blend);
        GlAlphaState alpha = new GlAlphaState(); bfl.getAlphaState(alpha);
        try {
            // RGB in the transparent framebuffer is already multiplied by alpha.
            bfl.l(); bfl.a(1, 771, 1, 771);
            buffer.a(width, height, false);
        } finally {
            mc.b().a(true); mc.o.j();
            bfl.a(true, true, true, true); bfl.a(true); bfl.j(); bfl.f(); bfl.w(); bfl.c(1f, 1f, 1f, 1f);
            bfl.setBlendState(blend); bfl.setAlphaState(alpha);
        }
    }

    public static boolean blend(int source, int destination) {
        if (!rendering || source != 770 || destination != 771) return false;
        bfl.a(source, destination, 1, 771);
        return true;
    }
    public static int alphaSource(int source, int destination, int original) {
        return rendering && source == 770 && destination == 771 ? 1 : original;
    }
    public static int alphaDestination(int source, int destination, int original) {
        return rendering && source == 770 && destination == 771 ? 771 : original;
    }
}
