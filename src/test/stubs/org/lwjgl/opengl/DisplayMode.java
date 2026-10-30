package org.lwjgl.opengl;
public final class DisplayMode {
    public int hz = 144;
    private final int width, height;
    public DisplayMode(int width, int height) { this.width = width; this.height = height; }
    public int getFrequency() { return hz; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
