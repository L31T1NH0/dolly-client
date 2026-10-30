package org.lwjgl.input;
/** Deterministic device state; loaded only in the isolated headless test JVM. */
public final class Keyboard {
    public static final int KEYBOARD_SIZE = 256;
    public static final int KEY_HOME = 199, KEY_END = 207, KEY_LEFT = 203, KEY_RIGHT = 205, KEY_DELETE = 211;
    public static final int KEY_ESCAPE = 1, KEY_RETURN = 28, KEY_LCONTROL = 29, KEY_A = 30, KEY_F = 33, KEY_B = 48;
    public static final boolean[] down = new boolean[KEYBOARD_SIZE];
    public static int event;
    public static char eventCharacter;
    public static boolean eventPressed;
    public static boolean isCreated() { return true; }
    public static boolean isKeyDown(int key) { return down[key]; }
    public static int getEventKey() { return event; }
    public static char getEventCharacter() { return eventCharacter; }
    public static boolean getEventKeyState() { return eventPressed; }
    public static String getKeyName(int key) { return "KEY_" + key; }
}
