import java.util.*;

/** Ignore transparent camera obstructions only during this trace, never during player picking. */
public final class DollyCameraTrace {
    private static final ThreadLocal<Boolean> camera = new ThreadLocal<Boolean>();
    private static Set<afh> ignored;
    private static boolean ignored(afh block) {
        if (ignored == null) {
            ignored = Collections.newSetFromMap(new IdentityHashMap<afh, Boolean>());
            for (String name : new String[] {"glass", "stained_glass", "glass_pane", "stained_glass_pane", "iron_bars"})
                ignored.add(afh.b(name));
        }
        return ignored.contains(block);
    }
    public static auh trace(bdb world, aui from, aui to) {
        if (!DollySettings.config.betterCamera) return world.a(from, to);
        Boolean previous = camera.get(); camera.set(true);
        try { return world.a(from, to, false, true, true); }
        finally { if (previous == null) camera.remove(); else camera.set(previous); }
    }
    public static aug collision(afh block, adm world, cj position, alz state) {
        return Boolean.TRUE.equals(camera.get()) && ignored(block) ? null : block.a(world, position, state);
    }
    public static float parallax(float original) {
        return DollySettings.config.parallaxFix && !DollyCamera.debugDirections ? .05f : original;
    }
    public static int skyDistance(int original) { return DollySettings.config.customSkyFix ? 256 : original; }
    public static boolean skyVbo(boolean original) { return original && !DollySettings.config.customSkyFix; }
}
