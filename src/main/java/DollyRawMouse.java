import dolly.input.RawMouseInput;
import java.util.ArrayList;
import java.util.List;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import org.lwjgl.opengl.Display;

/** Optional JInput capture; unavailable devices leave the LWJGL mouse helper intact. */
public final class DollyRawMouse {
    private static final RawMouseInput input = new RawMouseInput(DollyRawMouse::open);
    private static final int[] axes = new int[2];
    private static Object world;

    public static void reset() { input.setEnabled(false); world = null; }

    public static void frame() {
        ave mc = ave.A();
        if (mc == null) { reset(); return; }
        if (world != mc.f) { input.setEnabled(false); world = mc.f; }
        input.setEnabled(DollySettings.config.rawMouseInput && mc.f != null && mc.h != null
                && mc.m == null && mc.w && Display.isActive());
    }

    public static void sample(avf helper) {
        frame();
        if (input.drain(axes)) { helper.a = axes[0]; helper.b = -axes[1]; }
    }

    private static RawMouseInput.Source open() {
        return open(ControllerEnvironment.getDefaultEnvironment().getControllers());
    }

    static RawMouseInput.Source open(Controller[] controllers) {
        List<Device> mice = new ArrayList<Device>();
        for (Controller controller : controllers) {
            if (controller.getType() != Controller.Type.MOUSE) continue;
            Component x = controller.getComponent(Component.Identifier.Axis.X);
            Component y = controller.getComponent(Component.Identifier.Axis.Y);
            if (x != null && y != null && x.isRelative() && y.isRelative()) mice.add(new Device(controller, x, y));
        }
        if (mice.isEmpty()) return null;
        return new RawMouseInput.Source() {
            private Device selected;
            public int poll(float[] axes) {
                if (selected != null) return selected.read(axes) ? READY : FAILED;
                boolean connected = false;
                for (Device mouse : mice) {
                    if (!mouse.read(axes)) continue;
                    connected = true;
                    if (axes[0] != 0f || axes[1] != 0f) { selected = mouse; return READY; }
                }
                return connected ? WAITING : FAILED;
            }
        };
    }

    private static final class Device {
        final Controller controller;
        final Component x, y;
        Device(Controller controller, Component x, Component y) { this.controller = controller; this.x = x; this.y = y; }
        boolean read(float[] axes) {
            if (!controller.poll()) return false;
            axes[0] = x.getPollData(); axes[1] = y.getPollData();
            return true;
        }
    }
}
