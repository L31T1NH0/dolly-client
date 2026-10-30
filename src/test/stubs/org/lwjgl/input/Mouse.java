package org.lwjgl.input;
public final class Mouse {
    public static final boolean[] down = new boolean[8];
    public static int wheel;
    public static boolean isCreated() { return true; }
    public static int getButtonCount() { return down.length; }
    public static boolean isButtonDown(int button) { return down[button]; }
    public static int getEventDWheel() { return wheel; }
}
