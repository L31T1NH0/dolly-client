import java.util.Arrays;
import java.util.Locale;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Input state only: key recovery, Linux keycodes, and sprint latching. */
public final class DollyInput {
    private static boolean menuDown, sprintDown, sprintLatched, profilerDown, scoreboardDown, slotProfileDown, statsDown;
    private static Object lastWorld;
    private static final boolean LINUX = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    private static avb menu, zoom, perspective, scoreboard, slotProfile;

    public static void register(avh settings) {
        menu = new avb("key.dolly.menu", DollySettings.config.menuKey, "key.categories.dolly");
        zoom = new avb("key.dolly.zoom", DollySettings.config.zoomKey, "key.categories.dolly");
        perspective = new avb("key.dolly.perspective", DollySettings.config.perspectiveKey, "key.categories.dolly");
        scoreboard = new avb("key.dolly.scoreboard", DollySettings.config.scoreboardKey, "key.categories.dolly");
        slotProfile = new avb("key.dolly.slots", DollySettings.config.slotProfileKey, "key.categories.dolly");
        int length = settings.ax.length;
        settings.ax = Arrays.copyOf(settings.ax, length + 5);
        settings.ax[length] = menu; settings.ax[length + 1] = zoom; settings.ax[length + 2] = perspective;
        settings.ax[length + 3] = scoreboard;
        settings.ax[length + 4] = slotProfile;
    }

    public static void syncScoreboardKey() {
        if (scoreboard != null) { scoreboard.b(DollySettings.config.scoreboardKey); avb.b(); scoreboardDown = physicalKey(scoreboard.i()); }
    }
    public static void captureScoreboardKey() { if (scoreboard != null) DollySettings.config.scoreboardKey = scoreboard.i(); }
    public static void syncSlotProfileKey() {
        if (slotProfile != null) { slotProfile.b(DollySettings.config.slotProfileKey); avb.b(); slotProfileDown = physicalKey(slotProfile.i()); }
    }
    public static void captureSlotProfileKey() { if (slotProfile != null) DollySettings.config.slotProfileKey = slotProfile.i(); }

    public static boolean physicalKey(int code) {
        if (code > 0 && code < Keyboard.KEYBOARD_SIZE) return Keyboard.isCreated() && Keyboard.isKeyDown(code);
        return code < 0 && Mouse.isCreated() && code + 100 >= 0 && code + 100 < Mouse.getButtonCount() && Mouse.isButtonDown(code + 100);
    }
    public static boolean zoomHeld() { return zoom != null && physicalKey(zoom.i()); }
    public static boolean sprintLatched() { return sprintLatched; }
    public static void clear() {
        sprintLatched = false;
        sprintDown = false;
        ave mc = ave.A();
        if (mc != null && mc.t != null) avb.a(mc.t.ae.i(), playing(mc) && physicalKey(mc.t.ae.i()));
    }

    public static void recoverKeys() {
        ave mc = ave.A();
        if (!DollySettings.config.keyFix || !Keyboard.isCreated() || !Display.isActive() || mc.t == null) return;
        recover(mc.t.Y); recover(mc.t.Z); recover(mc.t.aa); recover(mc.t.ab);
        recover(mc.t.ac); recover(mc.t.ad); recover(mc.t.ae);
        applySprint(mc);
    }

    private static void recover(avb binding) {
        avb.a(binding.i(), physicalKey(binding.i()));
    }

    public static boolean textEvent() {
        return textEvent(Keyboard.getEventKeyState(), Keyboard.getEventKey(), Keyboard.getEventCharacter());
    }

    static boolean textEvent(boolean pressed, int key, char character) {
        return pressed || DollySettings.config.inputFix && key == 0 && Character.isDefined(character);
    }

    public static int mouseInventoryKey(int button) {
        return DollySettings.config.mouseBindFix && button > 2 ? button - 100 : 0;
    }

    public static int eventKey() {
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_T) {
            boolean pressed = Keyboard.getEventKeyState();
            if (!pressed) statsDown = false;
            else if (playing(ave.A())
                    && (physicalKey(Keyboard.KEY_LCONTROL) || physicalKey(Keyboard.KEY_RCONTROL))
                    && (physicalKey(Keyboard.KEY_LSHIFT) || physicalKey(Keyboard.KEY_RSHIFT))) {
                if (!statsDown) DollyOpponentStats.open();
                statsDown = true;
                return Keyboard.KEY_NONE;
            }
        }
        if (DollySettings.config.linuxShiftFix && LINUX) {
            if (key == Keyboard.KEY_AT) return Keyboard.KEY_2;
            if (key == Keyboard.KEY_CIRCUMFLEX) return Keyboard.KEY_6;
        }
        return key;
    }

    private static boolean playing(ave mc) {
        return mc.t != null && mc.h != null && mc.m == null && mc.w && Display.isActive();
    }

    private static void context(ave mc) {
        if (lastWorld != mc.f || !Display.isActive()) {
            clear();
            profilerDown = physicalKey(Keyboard.KEY_F8);
            statsDown = physicalKey(Keyboard.KEY_T);
            lastWorld = mc.f;
            menuDown = menu != null && physicalKey(menu.i());
            scoreboardDown = scoreboard != null && physicalKey(scoreboard.i());
            slotProfileDown = slotProfile != null && physicalKey(slotProfile.i());
            if (mc.t != null) sprintDown = physicalKey(mc.t.ae.i());
            DollyCamera.endFreeLook(mc);
            DollyZoom.reset();
        }
    }

    public static void tick() {
        ave mc = ave.A();
        if (!Keyboard.isCreated() || mc.t == null) return;
        context(mc);
        if (!playing(mc)) {
            sprintDown = physicalKey(mc.t.ae.i());
            menuDown = menu != null && physicalKey(menu.i());
            DollyCamera.endFreeLook(mc);
            return;
        }
        if (DollySettings.config.keyFix) recoverKeys();
        else applySprint(mc);
        DollySlots.ensureSelection();
    }

    // Called for both keyboard and mouse events, before vanilla consumes the queue.
    public static void keyState(int code, boolean pressed) {
        ave mc = ave.A();
        avb.a(code, pressed);
        if (code == Keyboard.KEY_F8) {
            if (pressed && !profilerDown && Display.isActive()) DollyProfiler.toggle(physicalKey(Keyboard.KEY_LSHIFT) || physicalKey(Keyboard.KEY_RSHIFT));
            profilerDown = pressed;
        }
        if (mc.t == null) return;
        if (slotProfile != null && code == slotProfile.i() && code != 0) {
            if (pressed && !slotProfileDown && playing(mc)) DollySlots.toggle();
            slotProfileDown = pressed;
        }
        if (scoreboard != null && code == scoreboard.i() && code != 0) {
            if (pressed && !scoreboardDown && playing(mc)) {
                DollySettings.config.scoreboardVisible = !DollySettings.config.scoreboardVisible;
                DollySettings.persist(mc.t);
            }
            scoreboardDown = pressed;
        }
        if (menu != null && code == menu.i() && code != 0) {
            if (pressed && !menuDown && mc.m == null && Display.isActive()) mc.a(new DollyScreen(null));
            menuDown = pressed;
        }
        if (code == mc.t.ae.i() && code != 0) {
            if (pressed && !sprintDown && playing(mc) && DollySettings.config.toggleSprint) sprintLatched = !sprintLatched;
            sprintDown = pressed;
            applySprint(mc);
        }
    }

    private static void applySprint(ave mc) {
        if (!DollySettings.config.toggleSprint) sprintLatched = false;
        if (playing(mc)) avb.a(mc.t.ae.i(), physicalKey(mc.t.ae.i()) || sprintLatched);
    }

    public static void frame() {
        DollyRawMouse.frame();
        ave mc = ave.A();
        context(mc);
        DollyCamera.updateFreeLook(mc, playing(mc) && DollySettings.config.perspective
                && perspective != null && physicalKey(perspective.i()));
    }

    public static int wheel() {
        return wheel(Mouse.getEventDWheel(), ave.A().m != null, zoomHeld());
    }

    static int wheel(int delta, boolean screenOpen, boolean zoomHeld) {
        if (delta != 0 && DollySettings.config.zoomScroll
                && DollyZoom.active(zoomHeld, DollySettings.config.zoom && !screenOpen)) {
            DollySettings.config.zoomFactor = DollyZoom.scroll(DollySettings.config.zoomFactor, delta);
            return 0;
        }
        return DollySettings.config.scrollLock && !screenOpen ? 0 : delta;
    }
}
