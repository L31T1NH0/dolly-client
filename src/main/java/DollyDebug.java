import dolly.debug.FrameHistory;
import java.util.Locale;
import org.lwjgl.opengl.GL11;

public final class DollyDebug {
    static final FrameHistory frames = new FrameHistory();
    public static void frame() { frames.frame(ave.A().f, System.nanoTime()); }
    public static String simplify(String value) {
        return DollySettings.config.simpleDebugFps && value != null ? value.replaceFirst("([0-9]+)/[0-9]+(?= fps)", "$1") : value;
    }
    public static String suffix(String value) { return DollySettings.config.simpleDebugFps && value.startsWith("/") ? "" : value; }
    public static void nausea(pr entity, int potion) {
        if (DollySettings.config.nauseaFix && entity instanceof bew && potion == pe.k.H) {
            ((bew) entity).bH = 0; ((bew) entity).bI = 0;
        }
    }
    public static boolean voidBox() { return DollySettings.config.voidBoxFix && !Config.isShaders(); }
    public static void graph(avr resolution) {
        if (!DollySettings.config.vanillaMetrics) { net.optifine.Lagometer.showLagometer(resolution); return; }
        ave mc = ave.A();
        if (!mc.t.aC || !(mc.t.aE || mc.t.ofLagometer)) return;
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING);
        try {
            int width = Math.min(240, resolution.a() / 2);
            int count = Math.min(width, frames.size());
            long[] values = new long[count];
            for (int i = 0; i < count; i++) values[i] = frames.get(frames.size() - count + i);
            draw(values, 0, resolution.b(), width, true);
            if (mc.G() != null) {
                net.minecraft.server.MinecraftServer server = mc.G();
                int ticks = server.at(), n = Math.min(width, Math.min(Math.max(0, ticks), server.h.length));
                long[] times = new long[n];
                for (int i = 0; i < n; i++) times[i] = server.h[Math.floorMod(ticks - n + 1 + i, server.h.length)];
                draw(times, resolution.a() - width, resolution.b(), width, false);
            }
        } finally {
            if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST);
            if (lighting) GL11.glEnable(GL11.GL_LIGHTING);
            if (blend) bfl.l(); else bfl.k();
            if (texture) bfl.w(); else bfl.x();
            bfl.c(1, 1, 1, 1);
        }
    }
    private static void draw(long[] values, int x, int bottom, int width, boolean fps) {
        if (width < 40 || values.length == 0) return;
        avn font = ave.A().k;
        avp.a(x, bottom - 60, x + width, bottom, 0x90505050);
        bfl.x(); bfd renderer = bfx.a().c(); renderer.a(GL11.GL_QUADS, bms.f);
        long min = Long.MAX_VALUE, max = 0; double sum = 0; int count = 0;
        for (int i = 0; i < values.length; i++) {
            long nanos = values[i]; if (nanos <= 0) continue;
            min = Math.min(min, nanos); max = Math.max(max, nanos); sum += nanos; count++;
            int height = FrameHistory.height(nanos, fps), color = FrameHistory.color(height);
            int red = color >> 16 & 255, green = color >> 8 & 255;
            renderer.b((double) x + i, bottom, 0d).b(red, green, 0, 255).d();
            renderer.b((double) x + i + 1, bottom, 0d).b(red, green, 0, 255).d();
            renderer.b((double) x + i + 1, bottom - height, 0d).b(red, green, 0, 255).d();
            renderer.b((double) x + i, bottom - height, 0d).b(red, green, 0, 255).d();
        }
        bfx.a().b(); bfl.w(); bfl.c(1, 1, 1, 1);
        avp.a(x, bottom - 60, x + width, bottom - 59, 0xFFFFFFFF);
        avp.a(x, bottom - 1, x + width, bottom, 0xFFFFFFFF);
        if (fps) { avp.a(x, bottom - 30, x + width, bottom - 29, 0xFFFFFFFF); font.a("60 FPS", x + 2, bottom - 28, 0xFFFFFF); }
        font.a(fps ? "30 FPS" : "20 TPS", x + 2, bottom - 58, 0xFFFFFF);
        if (count > 0) {
            String[] labels = {String.format(Locale.ROOT, "%.1f min", min / 1e6), String.format(Locale.ROOT, "%.1f avg", sum / count / 1e6), String.format(Locale.ROOT, "%.1f max", max / 1e6)};
            for (int i = 0; i < 3; i++) font.a(font.a(labels[i], Math.max(1, width / 3 - 3)), x + i * width / 3 + 1, bottom - 70, 0xFFFFFF);
        }
    }
}
