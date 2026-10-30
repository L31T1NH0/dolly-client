import org.lwjgl.opengl.Display;

public final class DollyFrameLimiter {
    public static int frameLimit(int vanilla) {
        ave mc = ave.A();
        if (!Display.isActive()) return Math.min(vanilla, DollySettings.config.backgroundFps);
        if (mc.m instanceof axs) return Math.min(vanilla, DollySettings.config.loadingFps);
        if (mc.f == null) {
            int hz = Display.getDisplayMode().getFrequency();
            int menu = DollySettings.config.menuFps == 0 ? (hz > 0 ? hz : 60) : DollySettings.config.menuFps;
            return menu;
        }
        return vanilla;
    }

    public static boolean shouldLimit(boolean vanilla) {
        return vanilla || ave.A().f == null || ave.A().m instanceof axs || !Display.isActive();
    }
}
