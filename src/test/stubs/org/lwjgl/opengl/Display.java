package org.lwjgl.opengl;
public final class Display {
    public static boolean active = true, vsync;
    public static final DisplayMode mode = new DisplayMode(1920, 1080);
    public static DisplayMode[] getAvailableDisplayModes() { return new DisplayMode[0]; }
    public static void processMessages() { }
    public static boolean isActive() { return active; }
    public static DisplayMode getDisplayMode() { return mode; }
    public static void setVSyncEnabled(boolean enabled) { vsync = enabled; }
}
