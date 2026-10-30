import java.util.function.*;
import org.lwjgl.input.Keyboard;

/** Shared native-style numeric control; persist on release, never once per pixel. */
public final class DollySlider extends DollyButton {
    private final double min, max, step;
    private final DoubleSupplier value;
    private final DoubleConsumer change;
    private final Supplier<String> label;
    private final Runnable commit;
    private boolean dragging;
    public DollySlider(int id, int x, int y, int width, double min, double max, double step,
                       DoubleSupplier value, DoubleConsumer change, Supplier<String> label, Runnable commit) {
        super(id, x, y, width, 20, "");
        this.min = min; this.max = max; this.step = step; this.value = value; this.change = change; this.label = label; this.commit = commit;
        updateLabel();
    }
    private void updateLabel() { j = label.get(); }
    private void set(double next) {
        next = Math.max(min, Math.min(max, min + Math.round((next - min) / step) * step));
        next = Math.round(next * 10000d) / 10000d;
        if (Math.abs(next - value.getAsDouble()) > .00001) change.accept(next);
        updateLabel();
    }
    private void move(int x) { set(min + (max - min) * Math.max(0, Math.min(1, (x - h - 4d) / Math.max(1, f - 8)))); }
    @Override public boolean c(ave mc, int x, int y) {
        if (!super.c(mc, x, y)) return false;
        dragging = true; move(x); return true;
    }
    @Override protected void b(ave mc, int x, int y) {
        if (dragging) move(x);
        int thumb = h + 4 + (int) Math.round((f - 8) * (value.getAsDouble() - min) / (max - min));
        avp.a(h + 4, i + g - 5, h + f - 4, i + g - 3, 0xFF68796D);
        avp.a(thumb - 2, i + 3, thumb + 2, i + g - 3, 0xFFBFEAD1);
    }
    @Override public void a(int x, int y) { if (dragging) { dragging = false; commit.run(); updateLabel(); } }
    public boolean key(int code) {
        if (code != Keyboard.KEY_LEFT && code != Keyboard.KEY_RIGHT && code != Keyboard.KEY_HOME && code != Keyboard.KEY_END) return false;
        set(code == Keyboard.KEY_HOME ? min : code == Keyboard.KEY_END ? max
                : value.getAsDouble() + (code == Keyboard.KEY_LEFT ? -step : step) * (axu.r() ? 10 : 1));
        commit.run(); return true;
    }
}
