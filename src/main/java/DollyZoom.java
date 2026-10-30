public final class DollyZoom {
    private static boolean heldBefore, latched;
    public static boolean active(boolean held, boolean allowed) {
        if (!allowed) { latched = false; heldBefore = held; return false; }
        if (!DollySettings.config.toggleZoom) { latched = false; heldBefore = held; return held; }
        if (held && !heldBefore) latched = !latched;
        heldBefore = held;
        return latched;
    }
    public static void reset() { latched = heldBefore = false; }
    static float scroll(float factor, int direction) {
        return Math.max(1f, Math.min(600f, factor * (direction > 0 ? 1.25f : .75f)));
    }
    static float ease(float progress, int mode) {
        float x = Math.max(0f, Math.min(1f, progress));
        if (mode == 1) return x < .5f ? (1f - (float) Math.sqrt(1 - 4 * x * x)) / 2
                : ((float) Math.sqrt(1 - Math.pow(-2 * x + 2, 2)) + 1) / 2;
        if (mode == 2) return 1f - (float) Math.pow(1f - x, 5);
        return x < .5f ? 2 * x * x : 1 - (float) Math.pow(-2 * x + 2, 2) / 2;
    }
    public static float sensitivity(avh settings) {
        if (!DollyCamera.zooming(ave.A())) return settings.a;
        float scale = DollySettings.config.zoomSensitivity;
        if (DollySettings.config.dynamicZoomSensitivity) scale /= Math.max(1f, DollySettings.config.zoomFactor);
        return settings.a * scale;
    }
    public static boolean hideHand() {
        return DollyCamera.zooming(ave.A()) && !DollySettings.config.renderHandZoom;
    }
}
