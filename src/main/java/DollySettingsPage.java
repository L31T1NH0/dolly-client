import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** A module's own adjustments stay together, without splitting the module browser into pages. */
public final class DollySettingsPage extends axu {
    private final axu parent;
    private final String title;
    private final int[] options;
    private final int hud;
    private final DollyScroll scroll = new DollyScroll();
    private final DollyOptionHelp tooltip = new DollyOptionHelp();
    private DollySlider focused;
    public DollySettingsPage(axu parent, String title, int[] options, int hud) {
        this.parent = parent; this.title = title; this.options = options; this.hud = hud;
    }
    @Override public void b() {
        DollyScroll.finish(n); n.clear(); tooltip.reset(); focused = null;
        int left = Math.max(12, (l - 420) / 2), width = Math.min(420, l - 24);
        int visible = Math.max(1, (m - 94) / 24);
        scroll.layout(options.length, visible, left, left + width - 5, 54, m - 36);
        for (int i = scroll.first(); i < Math.min(options.length, scroll.first() + visible); i++)
            n.add(DollyScreen.option(options[i], left, 54 + (i - scroll.first()) * 24, width - 12, q));
        if (hud >= 0) {
            avs edit = new DollyButton(1, left, m - 28, (width - 8) / 2, 20, "Posicionar no HUD");
            edit.l = ave.A().f != null; n.add(edit);
        }
        n.add(new DollyButton(2, hud < 0 ? left : left + (width + 8) / 2, m - 28, hud < 0 ? width : (width - 8) / 2, 20, "Concluir"));
    }
    @Override protected void a(avs button) {
        if (!button.l) return;
        if (button instanceof DollySlider) { focused = (DollySlider) button; return; }
        if (button.k == 2) { ave.A().a(parent); return; }
        if (button.k == 1) { ave.A().a(new DollyHudEditorScreen(this, hud)); return; }
        DollyScreen.activate(button.k - 200, this);
        if (ave.A().m == this || ave.A().m == null) b();
    }
    @Override protected void a(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) ave.A().a(parent);
        else if (focused == null || !focused.key(key)) super.a(character, key);
    }
    @Override protected void a(int x, int y, int button) { if (scroll.press(x, y, button)) b(); else super.a(x, y, button); }
    @Override protected void a(int x, int y, int button, long time) { if (scroll.drag(y)) b(); else super.a(x, y, button, time); }
    @Override protected void b(int x, int y, int button) { scroll.release(); super.b(x, y, button); }
    @Override public void k() { super.k(); if (scroll.wheel(DollyScroll.mouseX(this), DollyScroll.mouseY(this), Mouse.getEventDWheel())) b(); }
    @Override public void a(int x, int y, float partial) {
        c(); a(q, "Dolly · " + title, l / 2, 12, 0xFFE8EEE9);
        a(q, DollySettings.lastError == null ? "Ajustes do módulo · role para ver mais" : q.a("Erro: " + DollySettings.lastError, l - 24), l / 2, 30, 0xFFA7B6AA);
        super.a(x, y, partial); scroll.draw();
        avs hovered = DollyOptionHelp.hovered(n, x, y); int index = hovered == null ? -1 : hovered.k - 200;
        java.util.List<String> help = tooltip.lines(index < 0 ? null : DollyScreen.helpKey(index), index < 0 ? "" : DollyScreen.label(index), q, l, System.nanoTime());
        if (help != null) a(help, tooltip.anchorX(x, l), y);
    }
    @Override public void m() { DollyScroll.finish(n); DollySettings.persist(ave.A().t); }
}
