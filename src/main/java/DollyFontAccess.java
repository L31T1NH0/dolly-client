/** Implemented on the verified font class at build time. State includes cursor, color and styles. */
public interface DollyFontAccess {
    float[] dolly$fontState();
    void dolly$readFontState(float[] state);
    void dolly$fontState(float[] state);
    void dolly$drawText(String text, boolean shadow);
}
