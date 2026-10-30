import java.util.*;
import org.lwjgl.input.Keyboard;

/** Explicit choices keep cancellation available on click and Escape. */
public final class DollyChoiceScreen extends axu {
    private final axu parent;
    private final String title, detail;
    private final String[] labels;
    private final Runnable[] actions;
    public DollyChoiceScreen(axu parent, String title, String detail, String[] labels, Runnable... actions) {
        this.parent = parent; this.title = title; this.detail = detail; this.labels = labels; this.actions = actions;
    }
    @Override public void b() {
        n.clear(); int width = Math.min(300, l - 20), top = Math.min(m / 2, m - (labels.length + 1) * 24 - 8);
        for (int i = 0; i < labels.length; i++) n.add(new avs(i, (l - width) / 2, top + i * 24, width, 20, labels[i]));
        n.add(new avs(labels.length, (l - width) / 2, top + labels.length * 24, width, 20, "Cancelar"));
    }
    @Override protected void a(avs button) {
        if (!button.l) return;
        ave.A().a(parent);
        if (button.k < actions.length) actions[button.k].run();
    }
    @Override protected void a(char ch, int code) { if (code == Keyboard.KEY_ESCAPE) ave.A().a(parent); }
    @Override public void a(int x, int y, float partial) {
        c(); a(q, q.a(title, l - 20), l / 2, 24, 0xFFE8EEE9);
        a(q, q.a(detail, l - 20), l / 2, 44, 0xFFCCCCCC); super.a(x, y, partial);
    }
}
