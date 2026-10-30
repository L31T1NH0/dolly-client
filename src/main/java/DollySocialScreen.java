import dolly.config.DollyConfig;
import java.lang.reflect.Field;
import org.lwjgl.input.Keyboard;

/** Chat and sidebar preferences, using the existing Dolly menu controls. */
public final class DollySocialScreen extends axu {
    private final axu parent;
    private final boolean scoreboard;
    private final DollyScroll scroll = new DollyScroll();
    private int section;
    private String editing;
    private avw input;
    private DollySlider focusedSlider;
    private final DollyOptionHelp tooltip = new DollyOptionHelp();
    private static final class Option {
        final String key, label;
        final float min, max, step;
        Option(String key, String label, float min, float max, float step) {
            this.key = key; this.label = label; this.min = min; this.max = max; this.step = step;
        }
    }
    private static Option flag(String key, String label) { return new Option(key, label, 0, 1, 1); }
    private static Option number(String key, String label, float min, float max, float step) { return new Option(key, label, min, max, step); }
    private static final Option[] CHAT = {
        flag("chatGroup", "Agrupar repetidas"), flag("chatKeepHistory", "Manter entre servidores"),
        flag("chatMentions", "Destacar menções"), flag("chatMentionSound", "Som de menção"),
        flag("chatFilter", "Filtro de palavras"), flag("chatHiddenWords", "Palavras ocultadas"),
        flag("chatHideNotices", "Ocultar avisos"), flag("chatNoticeWords", "Frases de avisos"),
        flag("chatMentionWords", "Palavras destacadas"), number("chatHistory", "Histórico", 100, 5000, 100),
        number("chatWidth", "Largura", 80, 640, 40), number("chatHeight", "Altura", 40, 320, 20),
        number("chatScale", "Escala", 0.5f, 2, 0.25f), number("chatSpacing", "Espaçamento", 0, 6, 1),
        number("chatOpacity", "Opacidade do texto", 10, 100, 10), flag("chatBackground", "Fundo"),
        number("chatBackgroundOpacity", "Opacidade do fundo", 0, 100, 10),
        number("chatDuration", "Exibição (segundos)", 1, 120, 1), number("chatFade", "Fade (segundos)", 0, 10, 1),
        flag("chatGroupConsecutive", "Só consecutivas"),
        number("chatGroupSeconds", "Agrupar por (s)", 1, 600, 1),
        flag("chatSendQueue", "Fila de envio"), number("chatSendInterval", "Intervalo de envio (s)", 0.1f, 60, 0.1f),
        flag("chatInspectActions", "Mostrar ações dos links"), flag("chatClickedHistory", "Histórico dos cliques"),
        flag("chatImagePreview", "Prévia de imagens"), flag("tabPing", "Ping numérico no TAB")
    };
    private static final Option[] SCOREBOARD = {
        flag("scoreboardVisible", "Exibir scoreboard"), flag("scoreboardNumbers", "Números laterais"),
        number("scoreboardX", "Posição horizontal (%)", 0, 100, 5), number("scoreboardY", "Posição vertical (%)", 0, 100, 5),
        number("scoreboardScale", "Escala", 0.5f, 2, 0.25f), flag("scoreboardBackground", "Fundo"),
        number("scoreboardOpacity", "Opacidade do fundo", 0, 100, 5), flag("scoreboardKey", "Atalho de visibilidade")
    };
    private static final String[] CHAT_SECTIONS = {"Histórico e repetição", "Menções e filtros", "Aparência e leitura", "Envio, links e TAB"};
    private static final int[][] CHAT_OPTIONS = {{0, 1, 9, 19, 20}, {2, 3, 8, 4, 5, 6, 7}, {10, 11, 12, 13, 14, 15, 16, 17, 18}, {21, 22, 23, 24, 25, 26}};
    private static final String[] SCOREBOARD_SECTIONS = {"Visibilidade e atalho", "Posição e aparência"};
    private static final int[][] SCOREBOARD_OPTIONS = {{0, 1, 7}, {2, 3, 4, 5, 6}};
    public DollySocialScreen(axu parent, boolean scoreboard) { this.parent = parent; this.scoreboard = scoreboard; }
    private Option[] options() { return scoreboard ? SCOREBOARD : CHAT; }
    private String[] sections() { return scoreboard ? SCOREBOARD_SECTIONS : CHAT_SECTIONS; }
    private int[] sectionOptions() { return (scoreboard ? SCOREBOARD_OPTIONS : CHAT_OPTIONS)[section]; }
    private Field field(String key) {
        try { return DollyConfig.class.getField(key); } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private Object value(String key) {
        try { return field(key).get(DollySettings.config); } catch (IllegalAccessException e) { throw new AssertionError(e); }
    }
    @Override public void b() {
        DollyScroll.finish(n); n.clear(); focusedSlider = null; tooltip.reset();
        int width = Math.min(350, l - 20), left = (l - width) / 2, column = (width - 8) / 2;
        if (editing != null) {
            String text = input == null ? (String) value(editing) : input.b();
            input = new avw(0, q, left, 77, width, 20); input.f(1000); input.a(text); input.b(true);
            n.add(new DollyButton(3, left, m - 28, column, 20, "Salvar"));
            n.add(new DollyButton(4, left + column + 8, m - 28, column, 20, "Cancelar"));
            return;
        }
        section = Math.min(section, sections().length - 1);
        avs previousSection = new DollyButton(5, left, 54, 24, 16, "<"); previousSection.l = section > 0; n.add(previousSection);
        avs nextSection = new DollyButton(6, left + width - 24, 54, 24, 16, ">");
        nextSection.l = section + 1 < sections().length; n.add(nextSection);
        int[] indices = sectionOptions();
        int visible = Math.max(1, (m - 132) / 24);
        scroll.layout(indices.length, visible, left, left + width - 5, 78, m - 54);
        for (int position = scroll.first(); position < Math.min(indices.length, scroll.first() + visible); position++) {
            int local = position - scroll.first(), i = indices[position];
            String label = text(options()[i]);
            if (q != null && q.a(label) > width - 24) {
                int split = label.indexOf(": ");
                String name = split < 0 ? label : label.substring(0, split);
                String state = split < 0 ? "" : label.substring(split);
                while (!name.isEmpty() && q.a(name + "…" + state) > width - 24) name = name.substring(0, name.length() - 1);
                label = name + "…" + state;
            }
            int x = left, y = 78 + local * 24;
            final Option option = options()[i];
            if (value(option.key) instanceof Number && !option.key.equals("scoreboardKey"))
                n.add(DollyNumeric.slider(200 + i, x, y, width - 12, option.key, option.min, option.max, option.step,
                        () -> fitLabel(text(option), width - 24)));
            else n.add(new DollyButton(200 + i, x, y, width - 12, 20, label));
        }
        avs edit = new DollyButton(7, left, m - 28, column, 20, "Posicionar no HUD");
        edit.l = ave.A().f != null; n.add(edit);
        n.add(new DollyButton(2, left + column + 8, m - 28, column, 20, "Concluir"));
    }
    private String fitLabel(String label, int width) {
        if (q == null || q.a(label) <= width) return label;
        int split = label.indexOf(": ");
        String name = split < 0 ? label : label.substring(0, split), state = split < 0 ? "" : label.substring(split);
        return q.a(name, Math.max(0, width - q.a("…" + state))) + "…" + state;
    }
    private String text(Option option) {
        Object v = value(option.key);
        if (v instanceof String) return option.label + ": " + (v.equals("") ? "Editar…" : "Definidas…");
        if (option.key.equals("scoreboardKey")) return "Atalho: " + ((Integer) v == 0 ? "Nenhum" : (Integer) v < 0 ? "Mouse " + ((Integer) v + 101) : Keyboard.getKeyName((Integer) v));
        return option.label + ": " + (v instanceof Boolean ? ((Boolean) v ? "§aON" : "§cOFF") : v);
    }
    private boolean binding;
    @Override protected void a(avs button) {
        if (!button.l) return;
        if (button instanceof DollySlider) { focusedSlider = (DollySlider) button; return; }
        if (button.k == 5 || button.k == 6) binding = false;
        if (button.k == 2) { ave.A().a(parent); return; }
        else if (button.k == 7) { ave.A().a(new DollyHudEditorScreen(this, scoreboard ? 4 : 5)); return; }
        else if (button.k == 5 || button.k == 6) { section += button.k == 5 ? -1 : 1; scroll.rows.select(0); }
        else if (button.k == 3) {
            try { field(editing).set(DollySettings.config, input.b()); } catch (IllegalAccessException e) { throw new AssertionError(e); }
            editing = null; input = null; DollySettings.persist(ave.A().t);
        } else if (button.k == 4) { editing = null; input = null; }
        else if (button.k >= 200) {
            Option option = options()[button.k - 200]; Object v = value(option.key);
            if (option.key.equals("scoreboardKey")) { binding = true; button.j = "Pressione uma tecla…"; return; }
            if (v instanceof String) { editing = option.key; b(); return; }
            try {
                if (v instanceof Boolean) field(option.key).setBoolean(DollySettings.config, !(Boolean) v);
                else {
                    boolean backwards = axu.r(); float current = ((Number) v).floatValue();
                    float next = Math.round((current + (backwards ? -option.step : option.step)) * 100f) / 100f;
                    if (next > option.max) next = option.min; else if (next < option.min) next = option.max;
                    if (v instanceof Integer) field(option.key).setInt(DollySettings.config, (int) next);
                    else field(option.key).setFloat(DollySettings.config, next);
                }
            } catch (IllegalAccessException e) { throw new AssertionError(e); }
            DollyHudPositions.syncOption(option.key);
            DollySettings.persist(ave.A().t);
        }
        b();
    }
    @Override protected void a(char character, int code) {
        if (editing == null && !binding && focusedSlider != null && focusedSlider.key(code)) return;
        if (binding) {
            if (code != Keyboard.KEY_ESCAPE) {
                DollySettings.config.scoreboardKey = code == Keyboard.KEY_DELETE || code == Keyboard.KEY_BACK ? 0 : code;
                DollyInput.syncScoreboardKey(); ave.A().t.b();
            }
            binding = false; b(); return;
        }
        if (editing != null) {
            if (code == Keyboard.KEY_ESCAPE) { editing = null; input = null; b(); }
            else if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_NUMPADENTER) a((avs) n.get(0));
            else input.a(character, code);
        } else if (code == Keyboard.KEY_ESCAPE) ave.A().a(parent);
        else super.a(character, code);
    }
    @Override protected void a(int x, int y, int button) {
        if (editing == null && scroll.press(x, y, button)) { binding = false; b(); return; }
        if (input != null) input.a(x, y, button); super.a(x, y, button);
    }
    @Override protected void a(int x, int y, int button, long time) {
        if (editing == null && scroll.drag(y)) { binding = false; b(); }
        else super.a(x, y, button, time);
    }
    @Override protected void b(int x, int y, int button) { scroll.release(); super.b(x, y, button); }
    @Override public void k() {
        super.k();
        if (editing == null && scroll.wheel(DollyScroll.mouseX(this), DollyScroll.mouseY(this), org.lwjgl.input.Mouse.getEventDWheel())) { binding = false; b(); }
    }
    @Override public void e() { if (input != null) input.a(); }
    @Override public void a(int mouseX, int mouseY, float partialTicks) {
        c();
        a(q, scoreboard ? "Dolly · Scoreboard" : "Dolly · Chat", l / 2, 15, 0xFFE8EEE9);
        String hint = editing != null ? "Separe palavras ou frases com ;" : scoreboard ? "Arraste valores · Setas ajustam a barra" : "Arraste valores · Ctrl+F busca no chat";
        a(q, q.a(hint, l - 20), l / 2, 34, 0xFFCCCCCC);
        if (editing != null) {
            String label = editing.equals("chatHiddenWords") ? "Palavras ocultadas" : editing.equals("chatNoticeWords") ? "Frases de avisos" : "Palavras destacadas";
            a(q, label, l / 2, 60, 0xFFFFFFFF); input.g();
            a(q, "Vazio desativa a lista. Busca ignora maiúsculas.", l / 2, 108, 0xFFCCCCCC);
        } else {
            a(q, q.a(sections()[section] + " · " + (section + 1) + "/" + sections().length, Math.min(350, l - 20) - 60), l / 2, 58, 0xFFBFEAD1);

        }
        if (DollySettings.lastError != null) a(q, q.a("Erro ao salvar: " + DollySettings.lastError, l - 20), l / 2, m - 42, 0xFFFF8888);
        super.a(mouseX, mouseY, partialTicks);
        if (editing == null) scroll.draw();
        avs hovered = DollyOptionHelp.hovered(n, mouseX, mouseY);
        Option option = editing == null && !binding && hovered != null && hovered.k >= 200
                && hovered.k < 200 + options().length ? options()[hovered.k - 200] : null;
        java.util.List<String> help = tooltip.lines(option == null ? null : option.key,
                option == null ? "" : option.label, q, l, System.nanoTime());
        if (help != null) a(help, tooltip.anchorX(mouseX, l), mouseY);
    }
    @Override public void m() { DollyScroll.finish(n); DollySettings.persist(ave.A().t); }
}
