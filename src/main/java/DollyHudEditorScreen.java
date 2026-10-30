import dolly.hud.HudLayout;
import java.util.ArrayDeque;
import org.lwjgl.input.Keyboard;

/** Transparent in-world editor; live HUD rendering supplies the selection bounds. */
public final class DollyHudEditorScreen extends axu {
    private int selected;
    private static final int[] STACKING = {4, 5, 0, 1, 2, 3};
    private boolean dragging, properties;
    private float grabX, grabY;
    private DollySlider focused;
    private final DollyOptionHelp tooltip = new DollyOptionHelp();
    private final ArrayDeque<Snapshot> undo = new ArrayDeque<Snapshot>();
    private static final class Snapshot {
        final String layout = DollyHudPositions.layout().encode();
        final float chatScale = DollySettings.config.chatScale, scoreboardScale = DollySettings.config.scoreboardScale;
        final int chatOpacity = DollySettings.config.chatBackgroundOpacity, scoreboardOpacity = DollySettings.config.scoreboardOpacity;
        final int scoreboardX = DollySettings.config.scoreboardX, scoreboardY = DollySettings.config.scoreboardY;
        final boolean chatBackground = DollySettings.config.chatBackground, scoreboardBackground = DollySettings.config.scoreboardBackground;
        void restore() {
            DollyHudPositions.replace(layout);
            DollySettings.config.chatScale = chatScale; DollySettings.config.scoreboardScale = scoreboardScale;
            DollySettings.config.chatBackgroundOpacity = chatOpacity; DollySettings.config.scoreboardOpacity = scoreboardOpacity;
            DollySettings.config.chatBackground = chatBackground; DollySettings.config.scoreboardBackground = scoreboardBackground;
            DollySettings.config.scoreboardX = scoreboardX; DollySettings.config.scoreboardY = scoreboardY;
        }
    }
    public DollyHudEditorScreen(axu parent, int selected) { this.selected = Math.max(0, Math.min(5, selected)); }
    private int panelWidth() { return l >= 520 ? 160 : 118; }
    private int panelX() { return l - panelWidth() - 12; }
    private avr resolution() { return new avr(ave.A()); }
    private void checkpoint() {
        if (undo.size() == 30) undo.removeFirst();
        undo.addLast(new Snapshot());
        for (avs button : n) if (button.k == 0) button.l = true;
    }
    private void commit() { DollyHudCache.invalidate(); DollyHudPositions.commit(); }
    @Override public void b() {
        DollyScroll.finish(n); n.clear(); focused = null; tooltip.reset();
        int x = panelX(), width = panelWidth();
        n.add(new DollyButton(10, 12, 30, 20, 18, "<"));
        n.add(new DollyButton(11, 36, 30, 20, 18, ">"));
        n.add(new DollyButton(14, 60, 30, 84, 18, properties ? "Fechar opções" : "Opções…"));
        if (properties) {
            n.add(new DollySlider(20, x, 66, width, .5, 2, .05, () -> DollyHudPositions.scale(selected), value -> {
                HudLayout.Item item = DollyHudPositions.editable(selected, resolution()); item.scale = (float) value;
                if (selected == 4) DollySettings.config.scoreboardScale = (float) value;
                if (selected == 5) DollySettings.config.chatScale = (float) value;
                DollyHudCache.invalidate();
            }, () -> "Escala: " + Math.round(DollyHudPositions.scale(selected) * 100) + "%", this::commit));
            n.add(new DollySlider(21, x, 90, width, 0, 100, 5, () -> DollyHudPositions.background(selected), value -> {
                DollyHudPositions.editable(selected, resolution()).background = (int) value;
                if (selected == 4) { DollySettings.config.scoreboardBackground = value > 0; DollySettings.config.scoreboardOpacity = (int) value; }
                if (selected == 5) { DollySettings.config.chatBackground = value > 0; DollySettings.config.chatBackgroundOpacity = (int) value; }
                DollyHudCache.invalidate();
            }, () -> "Fundo: " + DollyHudPositions.background(selected) + "%", this::commit));
            n.add(new DollyButton(12, x, 114, width, 18, "Mostrar: " + (DollyHudPositions.visible(selected) ? "§aON" : "§cOFF")));
            n.add(new DollyButton(13, x, 134, width, 18, "Alinhar: " + (DollySettings.config.hudSnap ? "§aON" : "§cOFF")));
        }
        int third = Math.min(140, (l - 40) / 3);
        int left = (l - (3 * third + 16)) / 2;
        avs back = new DollyButton(0, left, m - 28, third, 20, "Desfazer"); back.l = !undo.isEmpty(); n.add(back);
        n.add(new DollyButton(1, left + 8 + third, m - 28, third, 20, DollyScreen.fit(q, "Restaurar posições", third - 8)));
        n.add(new DollyButton(2, left + 16 + 2 * third, m - 28, third, 20, "Concluir"));
    }
    @Override protected void a(avs button) {
        if (!button.l) return;
        if (button instanceof DollySlider) { focused = (DollySlider) button; return; }
        if (button.k == 2) { ave.A().a((axu) null); return; }
        if (button.k == 0) { if (!undo.isEmpty()) { undo.removeLast().restore(); commit(); } }
        else if (button.k == 1) {
            checkpoint();
            avr resolution = resolution();
            for (int id = 0; id < HudLayout.COUNT; id++) {
                HudLayout.Item item = DollyHudPositions.editable(id, resolution);
                float x = id == 4 ? 100 : 0, y = id == 4 ? 50 : id == 5 ? 100 : HudLayout.percent(4 + id * 12, m, DollyHudPositions.bounds(id, resolution).height);
                item.x = x; item.y = y;
            }
            commit();
        } else if (button.k == 10 || button.k == 11) selected = Math.floorMod(selected + (button.k == 10 ? -1 : 1), HudLayout.COUNT);
        else if (button.k == 14) properties = !properties;
        else if (button.k == 12) { checkpoint(); HudLayout.Item item = DollyHudPositions.editable(selected, resolution()); item.visible = !item.visible; commit(); }
        else if (button.k == 13) { DollySettings.config.hudSnap = !DollySettings.config.hudSnap; DollySettings.persist(ave.A().t); }
        b();
    }
    @Override protected void a(int x, int y, int button) {
        avs hovered = DollyOptionHelp.hovered(n, x, y);
        if (hovered != null) { if (hovered instanceof DollySlider && button == 0) checkpoint(); super.a(x, y, button); return; }
        if (button != 0 && button != 1) { super.a(x, y, button); return; }
        if (properties && x >= panelX() - 6 && y >= 54 && y < 160) return;
        float localX = x, localY = y;
        avr resolution = resolution(); int hit = -1;
        for (int index = STACKING.length - 1; index >= 0; index--) {
            int id = STACKING[index];
            if (DollyHudPositions.bounds(id, resolution).contains(localX, localY)) { hit = id; break; }
        }
        if (hit < 0) return;
        selected = hit; focused = null;
        if (button == 1) { properties = true; b(); return; }
        checkpoint(); DollyHudPositions.Box box = DollyHudPositions.bounds(selected, resolution);
        grabX = localX - box.x; grabY = localY - box.y; b(); dragging = true;
    }
    void move(float x, float y, boolean snap) {
        avr resolution = resolution(); DollyHudPositions.Box box = DollyHudPositions.bounds(selected, resolution);
        HudLayout.Item item = DollyHudPositions.editable(selected, resolution);
        item.x = HudLayout.percent(HudLayout.snap(x, l, box.width, snap), l, box.width);
        int screenHeight = m - (selected == 5 ? 24 : 0);
        item.y = HudLayout.percent(HudLayout.snap(y, screenHeight, box.height, snap), screenHeight, box.height);
        DollyHudCache.invalidate();
    }
    @Override protected void a(int x, int y, int button, long time) {
        if (dragging) move(x - grabX, y - grabY, DollySettings.config.hudSnap);
        else super.a(x, y, button, time);
    }
    @Override protected void b(int x, int y, int button) {
        if (dragging) { move(x - grabX, y - grabY, DollySettings.config.hudSnap); dragging = false; commit(); b(); }
        super.b(x, y, button);
    }
    @Override protected void a(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) { ave.A().a((axu) null); return; }
        if ((Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) && key == Keyboard.KEY_Z) {
            if (!undo.isEmpty()) { undo.removeLast().restore(); commit(); b(); } return;
        }
        boolean arrow = key == Keyboard.KEY_LEFT || key == Keyboard.KEY_RIGHT || key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN;
        if (focused != null && (key == Keyboard.KEY_LEFT || key == Keyboard.KEY_RIGHT || key == Keyboard.KEY_HOME || key == Keyboard.KEY_END)) {
            checkpoint(); focused.key(key); return;
        }
        if (arrow) {
            checkpoint(); DollyHudPositions.Box box = DollyHudPositions.bounds(selected, resolution()); int step = axu.r() ? 10 : 1;
            move(box.x + (key == Keyboard.KEY_LEFT ? -step : key == Keyboard.KEY_RIGHT ? step : 0),
                    box.y + (key == Keyboard.KEY_UP ? -step : key == Keyboard.KEY_DOWN ? step : 0), false);
            commit(); return;
        }
        super.a(character, key);
    }
    @Override public boolean d() { return false; }
    @Override public void a(int mouseX, int mouseY, float partial) {
        avr resolution = resolution();
        // World and the normal HUD are already drawn by Minecraft behind this screen.
        for (int id : STACKING) {
            DollyHudPositions.Box box = DollyHudPositions.bounds(id, resolution);
            int color = id == selected ? 0xFFBFEAD1 : DollyHudPositions.visible(id) ? 0xFF71897B : 0xFF987C72;
            int x = (int) box.x, y = (int) box.y, right = (int) Math.ceil(box.x + box.width), bottom = (int) Math.ceil(box.y + box.height);
            avp.a(x - 1, y - 1, right + 1, y, color); avp.a(x - 1, bottom, right + 1, bottom + 1, color);
            avp.a(x - 1, y, x, bottom, color); avp.a(right, y, right + 1, bottom, color);
            if (!DollyHudPositions.drawn(id)) {
                avp.a(x, y, right, bottom, 0x66303D34);
                q.a(q.a(DollyHudPositions.NAMES[id], Math.max(1, right - x - 2)), x + 1, y + 1, color);
            }
        }
        if (properties) avp.a(panelX() - 6, 54, l - 6, 160, 0xE01B251F);
        avp.a(l / 2 - 112, 4, l / 2 + 112, 26, 0xB01B251F);
        a(q, "Editar HUD · " + DollyHudPositions.NAMES[selected], l / 2, 6, 0xFFE8EEE9);
        a(q, "Arraste · botão direito: opções · Esc: concluir", l / 2, 17, 0xFFA7B6AA);
        if (DollySettings.lastError != null) q.a(q.a("Erro: " + DollySettings.lastError, l - 24), 12, m - 40, 0xFFFF8888);
        super.a(mouseX, mouseY, partial);
        avs hovered = DollyOptionHelp.hovered(n, mouseX, mouseY);
        int id = hovered == null ? -1 : hovered.k;
        String key = id == 20 ? "hudScale" : id == 21 ? "hudBackground" : id == 12 ? "hudVisible"
                : id == 13 ? "hudSnap" : id == 0 ? "hudUndo" : id == 1 ? "hudReset" : null;
        java.util.List<String> help = tooltip.lines(key, hovered == null ? "" : hovered.j, q, l, System.nanoTime());
        if (help != null) a(help, tooltip.anchorX(mouseX, l), mouseY);
    }
    @Override public void m() { DollyScroll.finish(n); dragging = false; commit(); }
}
