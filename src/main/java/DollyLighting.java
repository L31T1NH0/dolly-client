import java.util.Arrays;

/** Client-world lighting only: integrated-server calculations keep their native values. */
public final class DollyLighting {
    private static boolean ready, wasActive;
    private static Object world;
    public static boolean active(adm world) {
        return world instanceof bdb && DollySettings.config.fullBright
                && (!DollySettings.config.smartFullbright || !Config.isShaders());
    }
    public static void invalidate() { ready = false; }
    public static void sync(ave mc) {
        boolean active = active(mc.f);
        if (world != mc.f || wasActive != active) {
            ready = false; world = mc.f;
            if (wasActive != active && mc.g != null) mc.g.a();
            wasActive = active;
        }
    }
    public static boolean skipLightmap() { return ready && active(ave.A().f); }
    public static void lightmap(int[] pixels) {
        ready = active(ave.A().f);
        if (ready) Arrays.fill(pixels, 0xFFFFFFFF);
    }
}
