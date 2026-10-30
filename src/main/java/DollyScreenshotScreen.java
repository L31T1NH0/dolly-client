import org.lwjgl.input.Keyboard;

/** Recent session captures have independent controls, rather than a mutable 'last screenshot'. */
public final class DollyScreenshotScreen extends axu {
    private final axu parent;
    private DollyScreenshotManager.Capture capture;
    private DollyPicture picture;
    public DollyScreenshotScreen(axu parent, DollyScreenshotManager.Capture capture) {
        this.parent = parent; this.capture = capture;
        if (capture == null && !DollyScreenshotManager.captures.isEmpty()) this.capture = DollyScreenshotManager.captures.get(DollyScreenshotManager.captures.size() - 1);
    }
    @Override public void b() {
        n.clear(); int width = Math.min(450, l - 20), left = (l - width) / 2, column = (width - 8) / 2;
        n.add(new avs(0, left, m - 100, column, 20, "Anterior"));
        n.add(new avs(1, left + column + 8, m - 100, column, 20, "Próxima"));
        int index = DollyScreenshotManager.captures.indexOf(capture);
        n.get(0).l = index > 0; n.get(1).l = index >= 0 && index + 1 < DollyScreenshotManager.captures.size();
        String[] labels = {"Copiar imagem", "Favoritar", "Abrir pasta", "Excluir…"};
        for (int i = 0; i < labels.length; i++) {
            avs button = new avs(i + 2, left + (i % 2) * (column + 8), m - 76 + i / 2 * 24, column, 20, labels[i]);
            button.l = i == 2 ? capture == null || !capture.busy : capture != null && !capture.busy && !capture.deleted && (i != 1 || capture.favorite == null); n.add(button);
        }
        n.add(new avs(6, left, m - 28, width, 20, "Concluir"));
    }
    @Override protected void a(avs button) {
        if (!button.l) return;
        if (button.k == 4) { DollyScreenshotManager.openFolder(capture); b(); return; }
        if (button.k == 6) { ave.A().a(parent); return; }
        if (button.k <= 1) {
            int index = DollyScreenshotManager.captures.indexOf(capture) + (button.k == 0 ? -1 : 1);
            if (index < 0 || index >= DollyScreenshotManager.captures.size()) return;
            if (picture != null) picture.close(); picture = null;
            capture = DollyScreenshotManager.captures.get(index);
        } else if (button.k == 5) {
            final DollyScreenshotManager.Capture target = capture;
            ave.A().a(new DollyChoiceScreen(this, "Excluir esta captura?", capture.file.getName(),
                    new String[] {"Excluir arquivo"}, () -> DollyScreenshotManager.action(target, "delete"))); return;
        } else DollyScreenshotManager.action(capture, button.k == 2 ? "copy" : button.k == 3 ? "favorite" : "folder");
        b();
    }
    @Override public void e() { b(); }
    @Override protected void a(char ch, int code) { if (code == Keyboard.KEY_ESCAPE) ave.A().a(parent); }
    @Override public void a(int x, int y, float partial) {
        c(); a(q, "Dolly · Capturas da sessão", l / 2, 10, 0xFFE8EEE9);
        String title = capture == null ? "Nenhuma captura nesta sessão. Use F2." : capture.file.getName();
        a(q, q.a(title, l - 20), l / 2, 27, 0xFFCCCCCC);
        if (capture != null && !capture.deleted) {
            if (picture == null) picture = new DollyPicture(capture.thumbnail);
            picture.draw(10, 42, l - 20, Math.max(0, m - 158));
        }
        a(q, q.a(DollyScreenshotManager.status, l - 20), l / 2, m - 112, 0xFFCCCCCC);
        super.a(x, y, partial);
    }
    @Override public void m() { if (picture != null) picture.close(); picture = null; }
}
