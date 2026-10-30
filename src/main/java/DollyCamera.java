import org.lwjgl.opengl.Display;

/** Camera presentation; never writes player yaw or pitch for perspective. */
public final class DollyCamera {
    private static boolean freeLook;
    private static int previousView;
    private static float freeYaw, freePitch, zoomAmount = 1f, sneakHeight = 1.62f;
    private static long lastFrame, zoomStarted;
    private static boolean zoomSmooth = true;
    private static float zoomFrom = 1f, zoomTarget = 1f;
    public static boolean debugDirections;
    private static pk lastView;
    private static Object lastWorld;
    public static void updateFreeLook(ave mc, boolean looking) {
        if (looking && !freeLook) { freeYaw = mc.h.y; freePitch = mc.h.z; previousView = mc.t.aB; mc.t.aB = 1; freeLook = true; }
        if (!looking) endFreeLook(mc);
    }
    public static void endFreeLook(ave mc) {
        if (freeLook && mc.t != null) mc.t.aB = previousView;
        freeLook = false;
    }

    public static void mouse(avf helper) {
        ave mc = ave.A();
        if (freeLook && mc.h != null && mc.m == null) {
            float sensitivity = DollyZoom.sensitivity(mc.t) * 0.6f + 0.2f;
            float scale = sensitivity * sensitivity * sensitivity * 8f * 0.15f;
            freeYaw += helper.a * scale;
            freePitch = Math.max(-90f, Math.min(90f, freePitch - helper.b * scale * (mc.t.b ? -1 : 1)));
            helper.a = helper.b = 0;
        }
    }

    public static float cameraYaw(pk entity) { return freeLook && entity == ave.A().h ? freeYaw : entity.y; }
    public static float cameraPrevYaw(pk entity) { return freeLook && entity == ave.A().h ? freeYaw : entity.A; }
    public static float cameraPitch(pk entity) { return freeLook && entity == ave.A().h ? freePitch : entity.z; }
    public static float cameraPrevPitch(pk entity) { return freeLook && entity == ave.A().h ? freePitch : entity.B; }

    public static boolean zooming(ave mc) {
        return DollyZoom.active(DollyInput.zoomHeld(), DollySettings.config.zoom && mc.m == null
                && mc.h != null && Display.isActive());
    }

    public static boolean smoothCamera(avh settings) {
        return settings.aG || zooming(ave.A()) && !DollySettings.config.directZoomCamera;
    }

    public static void beginFrame() {
        ave mc = ave.A();
        DollyInput.frame();
        DollyLighting.sync(mc);
        pk view = mc.ac();
        if (lastView != view || lastWorld != mc.f) {
            lastFrame = 0;
            DollyZoom.reset();
            lastView = view;
            lastWorld = mc.f;
        }
        boolean zooming = zooming(mc);
        float previousZoom = zoomAmount;
        advance(System.nanoTime(), view == null ? 1.62f : view.aS(),
                zooming ? 1f / DollySettings.config.zoomFactor : 1f,
                DollySettings.config.sneakAnimation && mc.h != null);
        // Widening the FOV must refresh visibility, or recently hidden chunks can remain missing.
        if (mc.g != null && Math.abs(previousZoom - zoomAmount) > .000001f) mc.g.m();
    }

    static void advance(long now, float eyeTarget, float zoomTarget, boolean smoothSneak) {
        if (lastFrame == 0) {
            sneakHeight = eyeTarget;
            zoomAmount = zoomFrom = 1f;
            DollyCamera.zoomTarget = 1f; zoomStarted = now;
        } else {
            double seconds = Math.max(0L, now - lastFrame) / 1_000_000_000d;
            sneakHeight += (eyeTarget - sneakHeight) * (float) -Math.expm1(-8.5d * seconds);
        }
        boolean scrolling = DollyCamera.zoomTarget != 1f && zoomTarget != 1f;
        if (DollyCamera.zoomTarget != zoomTarget) {
            zoomSmooth = scrolling ? DollySettings.config.smoothScrollZoom : DollySettings.config.smoothZoom;
            zoomFrom = zoomAmount;
            DollyCamera.zoomTarget = zoomTarget;
            zoomStarted = lastFrame == 0 ? now : lastFrame;
        }
        float progress = Math.max(0f, Math.min(1f, (now - zoomStarted) / 200_000_000f));
        zoomAmount = zoomSmooth ? zoomFrom + (zoomTarget - zoomFrom) * DollyZoom.ease(progress, DollySettings.config.zoomEasing) : zoomTarget;
        lastFrame = now;
        if (!smoothSneak) sneakHeight = eyeTarget;
    }

    public static float eyeHeight(float target) { return lastFrame == 0 ? target : sneakHeight; }
    public static float fov(float original, boolean worldFov) {
        return !worldFov && DollySettings.config.renderHandZoom ? original : fov(original);
    }
    public static float fov(float original) { return original * zoomAmount; }
}
