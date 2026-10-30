/** Presentation of dropped items only; never changes quantities, physics, pickup or rotation. */
public final class DollyWorldObjects {
    public static int copies(int original) {
        return DollySettings.config.singleGroundItem ? 1 : original;
    }
    public static float bobSine(float original) {
        // M5 computes the vertical offset as sin(phase) * .1 + .1.
        return DollySettings.config.staticGroundItems ? -1f : original;
    }
}
