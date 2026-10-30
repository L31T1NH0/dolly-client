import org.lwjgl.opengl.GL11;

/** Legacy presentation and use-item guards; normal input still controls every swing. */
public final class DollyOldAnimations {
    public static boolean enabled() { return DollySettings.config.oldAnimations; }
    public static float blockSwing(float swing) { return enabled() ? swing : 0f; }
    public static float blockEquip(float equipped) { return enabled() ? -0.1f : equipped; }
    public static boolean previousHearts(boolean blinking) { return blinking && !enabled(); }
    public static boolean preventSwing(boolean blocked) { return blocked && !enabled(); }
    public static boolean blockingMining() {
        return enabled() && ave.A().h != null && ave.A().h.bS();
    }
    public static void blockTransform() {
        if (enabled()) GL11.glTranslatef(-0.3f, 0.2f, 0f);
    }
}
