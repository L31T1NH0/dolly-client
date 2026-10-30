import dolly.tactile.ServerProfiles;
import dolly.tactile.ServerProfiles.Profile;
import dolly.tactile.ServerProfiles.Mode;
import dolly.tactile.ServerProfiles.Source;
import java.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** A server's movement policy and advanced controls, using Dolly's existing scrolling form. */
public final class DollyTactileScreen extends axu {
    private final axu parent;
    private final DollyScroll scroll = new DollyScroll();
    private String target;
    private Profile profile;
    private DollySlider focused;
    public DollyTactileScreen(axu parent) {
        this.parent = parent;
        target = DollyTactile.server();
        if (target == null) target = ServerProfiles.DEFAULT;
        load();
    }
    private void load() {
        profile = ServerProfiles.resolve(DollyTactile.profiles(), target);
        profile.mode = ServerProfiles.effectiveMode(target, profile);
    }
    private boolean simulation() { return profile.mode == Mode.EXACT || profile.mode == Mode.STEPPED; }
    private void save() { DollyTactile.save(target, profile); }
    private avs slider(int id, int x, int y, int width, int min, int max, int step) {
        return new DollySlider(id, x, y, width, min, max, step,
                () -> id == 1 ? profile.physics : id == 2 ? profile.steps : profile.polling,
                value -> { if (id == 1) profile.physics = (int)value; else if (id == 2) profile.steps = (int)value; else profile.polling = (int)value; },
                () -> id == 1 ? "Física: " + profile.physics + " Hz" : id == 2 ? "Passos da entrada: " + profile.steps + " Hz" : "Leitura: " + profile.polling + " Hz", this::save);
    }
    @Override public void b() {
        DollyScroll.finish(n); n.clear(); focused = null;
        int left = Math.max(12, (l - 420) / 2), width = Math.min(420, l - 24);
        int visible = Math.max(1, (m - 106) / 24);
        scroll.layout(8, visible, left, left + width - 5, 64, m - 36);
        for (int row = scroll.first(); row < Math.min(8, scroll.first() + visible); row++) {
            int y = 64 + (row - scroll.first()) * 24; avs button;
            if (row == 1) button = slider(row, left, y, width - 12, 20, 1920, 20);
            else if (row == 2) button = slider(row, left, y, width - 12, 1, 1920, 1);
            else if (row == 4) button = slider(row, left, y, width - 12, 250, 8000, 250);
            else {
                String text = row == 0 ? "Modo: " + new String[] {"Vanilla", "Entrada antecipada · física vanilla", "EXACT", "STEPPED"}[profile.mode.ordinal()]
                        : row == 3 ? "Fonte de teclas: " + profile.source
                        : row == 5 ? "Preservar toques curtos: " + (profile.latch ? "ON" : "OFF")
                        : row == 6 ? "Exibir estado: " + new String[] {"OFF", "Simples", "Detalhado"}[profile.overlay]
                        : "Usar padrão global neste servidor";
                button = new DollyButton(row, left, y, width - 12, 20, DollyScreen.fit(q, text, width - 20));
            }
            if (row == 1 || row == 3) button.l = simulation();
            if (row == 2 || row == 5) button.l = profile.mode == Mode.STEPPED;
            if (row == 4) button.l = simulation() && (profile.source == Source.AUTO || profile.source == Source.POLLER);
            if (row == 7) button.l = !target.equals(ServerProfiles.DEFAULT) && DollyTactile.profiles().containsKey(target);
            n.add(button);
        }
        int half = (width - 8) / 2;
        avs scope = new DollyButton(98, left, m - 28, half, 20, target.equals(ServerProfiles.DEFAULT) ? "Editar servidor atual" : "Editar padrão global");
        scope.l = DollyTactile.server() != null && !DollyTactile.server().equals(ServerProfiles.DEFAULT); n.add(scope);
        n.add(new DollyButton(99, left + half + 8, m - 28, half, 20, "Concluir"));
    }
    @Override protected void a(avs button) {
        if (!button.l) return;
        if (button instanceof DollySlider) { focused = (DollySlider) button; return; }
        if (button.k == 99) { ave.A().a(parent); return; }
        if (button.k == 98) {
            target = target.equals(ServerProfiles.DEFAULT) ? DollyTactile.server() : ServerProfiles.DEFAULT;
            load(); scroll.rows.select(0); b(); return;
        }
        if (button.k == 7) { DollyTactile.save(target, null); load(); b(); return; }
        if (button.k == 0) {
            profile.mode = Mode.values()[(profile.mode.ordinal() + 1) % (ServerProfiles.hypixel(target) ? 2 : 4)];
        } else if (button.k == 3) profile.source = Source.values()[(profile.source.ordinal() + 1) % 4];
        else if (button.k == 5) profile.latch = !profile.latch;
        else if (button.k == 6) profile.overlay = (profile.overlay + 1) % 3;
        save(); b();
    }
    @Override protected void a(char character, int code) {
        if (code == Keyboard.KEY_ESCAPE) ave.A().a(parent);
        else if (focused == null || !focused.key(code)) super.a(character, code);
    }
    @Override protected void a(int x, int y, int button) { if (scroll.press(x, y, button)) b(); else super.a(x, y, button); }
    @Override protected void a(int x, int y, int button, long time) { if (scroll.drag(y)) b(); else super.a(x, y, button, time); }
    @Override protected void b(int x, int y, int button) { scroll.release(); super.b(x, y, button); }
    @Override public void k() { super.k(); if (scroll.wheel(DollyScroll.mouseX(this), DollyScroll.mouseY(this), Mouse.getEventDWheel())) b(); }
    @Override public void a(int x, int y, float partial) {
        c(); a(q, "Dolly · TACTILE", l / 2, 12, 0xFFE8EEE9);
        String name = target.equals(ServerProfiles.DEFAULT) ? "Padrão global" : target.equals(ServerProfiles.SINGLEPLAYER) ? "Mundo local" : target;
        a(q, q.a(name, l - 24), l / 2, 30, 0xFFBFEAD1);
        String message = DollySettings.lastError != null ? "Erro ao salvar: " + DollySettings.lastError
                : ServerProfiles.hypixel(DollyTactile.server()) ? "Hypixel: física vanilla em todos os perfis"
                : simulation() ? "Mudanças salvas · EXACT/STEPPED dependem das regras do servidor"
                : "Mudanças salvas · movimento a 20 Hz";
        a(q, q.a(message, l - 24), l / 2, 44, DollySettings.lastError == null ? 0xFFA7B6AA : 0xFFFF8888);
        super.a(x, y, partial); scroll.draw();
        avs hovered = DollyOptionHelp.hovered(n, x, y);
        if (hovered != null && hovered.k >= 0 && hovered.k <= 6) {
            String[] help = {"EXACT aplica teclas no instante observado. STEPPED usa passos fixos.",
                    "Frequência das consultas de física; não altera a velocidade do jogador.",
                    "Só STEPPED: frequência em que as teclas passam a valer.",
                    "EVENTS/POLLER usam Windows; outros sistemas usam leitura por quadro.",
                    "Acima de 2000 Hz, POLLER pode ocupar um núcleo de CPU.",
                    "Só STEPPED: conserva um toque mais curto que um passo da entrada.",
                    "Simples mostra o modo; detalhado inclui tempos, correções e falhas."};
            a(q.c(help[hovered.k], Math.max(80, l - 40)), 16, Math.max(54, y - 28));
        }
    }
    @Override public void m() { DollyScroll.finish(n); DollySettings.persist(ave.A().t); }
}
