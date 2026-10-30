/** Implemented by the build-time HUD patch; destination-sensitive layers stay per frame. */
public interface DollyHudLayers {
    void dolly$dynamic(float partialTicks);
}
