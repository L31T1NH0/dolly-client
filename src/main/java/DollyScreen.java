import dolly.config.DollyConfig;
import java.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** One scrolling module list; technical settings live in their own grouped workspace. */
public final class DollyScreen extends axu {
    private final axu parent;
    private int view, group, filter, selected;
    private String query = "";
    private avw search;
    private DollySlider focusedSlider;
    private final DollyScroll list = new DollyScroll(), groups = new DollyScroll();
    private final DollyOptionHelp tooltip = new DollyOptionHelp();
    private List<String> detail = Collections.emptyList();
    private int[] matches = new int[0];
    public DollyScreen(axu parent) { this.parent = parent; }
    private boolean wide() { return l >= 520; }
    private int bottom() { return m - 36; }
    private int listRight() { return wide() ? l - 196 : l - 22; }

    private static final String[] keys = {"clickFix", "mouseFix", "keyFix", "linuxShiftFix",
            "oldAnimations", "toggleSprint", "sneakAnimation", "zoom", "perspective", "fullBright", "timeChanger", "tntTimer", "pingDisplay",
            "ambientParticles", "scrollLock", "backgroundFps", "menuFps", "particleLimit", "zoomFactor", "timeOfDay",
            "entityCulling", "mergeTerrain", "loadingFps", null, null, null,
            "mouseBindFix", "inputFix", "rawMouseInput", "stringWidthCaching", "optimizedPlayerUpdating",
            "asyncScreenshot", "fastLanguageSwitch", "fastWorldLoading", "lazyChunkLoading", "hudCaching",
            "hudCacheFps", "memoryCleanup", null, "alternateDns", "fallbackDns",
            "optimizedText", "batchModels", "modelBackFaces", "optimizedItems", "staticParticleColor", "smallPackIcons", "lowAnimationTick",
            "hideEnchantmentBooks", "singleGroundItem", "staticGroundItems",
            "zoomScroll", "smoothZoom", "smoothScrollZoom", "toggleZoom", "directZoomCamera", "dynamicZoomSensitivity",
            "renderHandZoom", "smartFullbright", "parallaxFix", "betterCamera", "customSkyFix", "zoomSensitivity", "zoomEasing",
            "fireHeight", "fireOpacity", "hideFireResistance", "removeWaterOverlay", "waterFogDensity", "cleanView",
            "hideBreakingParticles", "hideLightning", "cleanProjectiles", "horseOpacity", null, "separateResourceLoading", null, "lanButton", "reconnectMenu", "confirmQuit", "screenshotPreview", "screenshotAutoCopy", "screenshotCompact", "simpleDebugFps", "vanillaMetrics", "voidBoxFix", "nauseaFix", "cleanOldLogs", "logRetentionDays", "slotProfile", "scoreboardVisible", null};
    private static final String[] labels = {"Corrigir cliques", "Corrigir mouse", "Recuperar teclas", "Shift no Linux",
            "Old Animations", "Toggle Sprint", "Agachar suave", "Zoom", "Perspective", "Full Bright", "Time Changer", "TNT Timer", "Exibir ping",
            "Partículas de ambiente", "Bloquear scroll", "FPS sem foco", "FPS no menu", "Limite de partículas", "Fator do zoom", "Horário visual",
            "Ocultar objetos cobertos", "Mesclar terreno", "FPS ao carregar", null, null, null,
            "Mouse no inventário", "Corrigir texto", "Raw Mouse Input", "Medidas de texto", "Otimizar jogadores",
            "Screenshot assíncrona", "Idioma rápido", "Carregar mundo rápido", "Atraso de chunks", "Cache do HUD",
            "Hz do HUD", "Limpar ao desconectar", "Frestas dos itens", "DNS alternativo", "DNS de fallback",
            "Desenho de textos", "Agrupar modelos", "Só faces externas", "Modelos de itens", "Luz fixa de partículas", "Ícones leves dos packs", "Menos animações ambiente",
            "Ocultar livros de encanto", "Item único por pilha", "Itens sem balanço",
            "Zoom pela roda", "Transição do zoom", "Transição pela roda", "Zoom por alternância", "Mouse direto no zoom", "Sensibilidade dinâmica",
            "Mão durante zoom", "Fullbright inteligente", "Parallax Fix", "Better Camera", "Corrigir céu OptiFine", "Sensibilidade do zoom", "Curva do zoom",
            "Altura do fogo", "Opacidade do fogo", "Ocultar fogo protegido", "Remover camada da água", "Névoa subaquática", "Ocultar partículas próprias",
            "Ocultar fragmentos", "Ocultar raios", "Ocultação de projéteis", "Opacidade do cavalo", null, "Separar recargas", "Gerenciar capturas", "Botão LAN", "Menu de reconexão", "Confirmar saída", "Prévia da captura", "Copiar captura ao salvar", "Aviso compacto de captura", "FPS simples no F3", "Gráfico original Alt+F3", "Corrigir caixa do vazio", "Corrigir fim da náusea", "Limpar logs antigos", "Guardar logs (dias)", "Perfil de slots", "Exibir scoreboard", "Configurar TACTILE"};

    static String text(int index) {
        if (index == 91) return "Configurar TACTILE…";
        if (index == 76) return "Gerenciar capturas…";
        if (index == 74) return "Configurar sons…";
        if (index == 23) return "Configurar chat…";
        if (index == 24) return "Configurar scoreboard…";
        if (index == 25) return "Perfil de slots…";
        try {
            Object value = DollyConfig.class.getField(keys[index]).get(DollySettings.config);
            String display = value instanceof Boolean ? ((Boolean) value ? "§aON" : "§cOFF") : String.valueOf(value);
            if (keys[index].equals("lanButton")) display = new String[] {"Original", "Oculto", "Servidores"}[(Integer) value];
            if (keys[index].equals("menuFps") && ((Integer) value) == 0) display = "Monitor";
            if (keys[index].equals("hudCaching") && (Boolean) value) {
                if (Config.isFastRender()) display = "Pausado (Fast Render)";
                else if (Config.isShaders()) display = "Pausado (Shaders)";
                else if (!bqs.i()) display = "Indisponível";
                else if (!DollyHudCache.useful(ave.A().t.g, DollySettings.config.hudCacheFps)) display = "Sem economia";
            }
            if (keys[index].equals("lazyChunkLoading")) display = new String[] {"OFF", "15 ms", "50 ms", "110 ms", "150 ms", "200 ms"}[(Integer) value];
            if (keys[index].equals("fireHeight") || keys[index].equals("fireOpacity") || keys[index].equals("waterFogDensity")
                    || keys[index].equals("horseOpacity")) display += "%";
            if (keys[index].equals("zoomSensitivity")) display = Math.round(((Float) value) * 100) + "%";
            if (keys[index].equals("zoomEasing")) display = new String[] {"Quadrática", "Circular", "Quíntica"}[(Integer) value];
            if (keys[index].equals("zoomFactor")) display += "x";
            if (keys[index].equals("timeOfDay")) {
                int minutes = (((Integer) value + 6000) % 24000) * 60 / 1000;
                display = String.format(java.util.Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60);
            }
            return labels[index] + ": " + display;
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    static String helpKey(int index) {
        switch (index) {
            case 91: return "tactileSettings";
            case 23: return "chatSettings";
            case 24: return "scoreboardSettings";
            case 25: return "slotSettings";
            case 74: return "soundSettings";
            case 76: return "screenshots";
            default: return keys[index];
        }
    }

    static String key(int index) { return keys[index]; }
    static String label(int index) { return labels[index] == null ? text(index) : labels[index]; }
    static boolean enabled(int index) {
        try { return DollyConfig.class.getField(keys[index]).getBoolean(DollySettings.config); }
        catch (ReflectiveOperationException error) { throw new IllegalArgumentException(keys[index], error); }
    }
    static String fit(avn font, String text, int width) {
        if (font == null || font.a(text) <= width) return text;
        int split = text.lastIndexOf(": ");
        if (split < 0) return font.a(text, Math.max(1, width - font.a("…"))) + "…";
        String value = text.substring(split);
        if (font.a(value) > width - 12) value = font.a(value, Math.max(1, width - 12));
        return font.a(text.substring(0, split), Math.max(0, width - font.a("…" + value))) + "…" + value;
    }
    static avs option(int index, int x, int y, int width, avn font) {
        double[] range = DollyNumeric.range(keys[index]);
        if (range == null) return new DollyButton(200 + index, x, y, width, 20, fit(font, text(index), width - 8));
        return DollyNumeric.slider(200 + index, x, y, width, keys[index], range[0], range[1], range[2], () -> fit(font, text(index), width - 8));
    }
    static void activate(int index, axu parent) {
        ave mc = ave.A();
        if (index == 91) mc.a(new DollyTactileScreen(parent));
        else if (index == 76) mc.a(new DollyScreenshotScreen(parent, null));
        else if (index == 74) mc.a(new DollyAudioScreen(parent));
        else if (index == 23 || index == 24) mc.a(new DollySocialScreen(parent, index == 24));
        else if (index == 25) mc.a(new DollySlotScreen(parent));
        else if (index == 89) DollySlots.toggle();
        else DollySettings.change(keys[index]);
    }
    @Override public void b() {
        DollyScroll.finish(n); DollyOptiFine.capture(ave.A().t); tooltip.reset(); focusedSlider = null; n.clear();
        for (int i = 0; i < 3; i++) {
            avs tab = new DollyButton(120 + i, 18 + i * ((l - 44) / 3 + 4), 40, (l - 44) / 3, 20,
                    new String[] {"Módulos", "Configurações", "Editar HUD"}[i]);
            tab.l = i == 2 ? ave.A().f != null : i != view; n.add(tab);
        }
        if (view == 0) modules(); else settings();
        int half = (l - 44) / 2;
        n.add(new DollyButton(110, 18, m - 28, half, 20, "Restaurar padrões"));
        n.add(new DollyButton(111, 26 + half, m - 28, half, 20, "Concluir"));
    }
    private void modules() {
        boolean focus = search != null && search.m();
        if (q != null) {
            search = new avw(0, q, 18, 68, Math.max(40, listRight() - 150), 18);
            search.f(80); search.a(query); search.b(focus);
        }
        for (int i = 0; i < 3; i++) {
            avs button = new DollyButton(130 + i, listRight() - 126 + i * 42, 68, 39, 18, new String[] {"Todos", "ON", "OFF"}[i]);
            button.l = filter != i; n.add(button);
        }
        matches = DollyModules.matching(query, filter);
        int visible = Math.max(1, (bottom() - 94) / 30);
        list.layout(matches.length, visible, 18, listRight() - 4, 94, bottom());
        if (matches.length > 0 && Arrays.stream(matches).noneMatch(value -> value == selected)) selected = matches[0];
        for (int row = list.first(); row < Math.min(matches.length, list.first() + visible); row++) {
            int index = matches[row], y = 94 + (row - list.first()) * 30;
            DollyModules.Module module = DollyModules.ALL[index];
            n.add(new DollyModuleButton(1000 + index, 18, y, listRight() - 76, module, selected == index));
            n.add(new DollyButton(200 + module.option, listRight() - 52, y + 3, 40, 20, enabled(module.option) ? "§aON" : "§cOFF"));
        }
        detail = Collections.emptyList();
        if (wide() && matches.length > 0) {
            DollyModules.Module module = DollyModules.ALL[selected];
            DollyOptionHelp.Tip tip = DollyOptionHelp.tip(keys[module.option]);
            if (q != null && tip != null) detail = q.c(tip.description, 156);
            n.add(new DollyButton(100, listRight() + 14, bottom() - 22, 160, 20, "Configurar módulo"));
        }
    }
    private void settings() {
        search = null;
        int visibleGroups = Math.max(1, (bottom() - 68) / 24);
        groups.layout(DollyModules.GROUPS.length, visibleGroups, 18, 132, 68, bottom());
        for (int i = groups.first(); i < Math.min(DollyModules.GROUPS.length, groups.first() + visibleGroups); i++) {
            avs button = new DollyButton(300 + i, 18, 68 + (i - groups.first()) * 24, 108, 20,
                    (i == group ? "§a" : "") + (q == null ? DollyModules.GROUPS[i] : q.a(DollyModules.GROUPS[i], 100)));
            button.l = i != group; n.add(button);
        }
        int[] options = DollyModules.SETTINGS[group];
        int visible = Math.max(1, (bottom() - 94) / 24);
        list.layout(options.length, visible, 148, l - 22, 94, bottom());
        for (int i = list.first(); i < Math.min(options.length, list.first() + visible); i++)
            n.add(option(options[i], 148, 94 + (i - list.first()) * 24, l - 178, q));
    }
    private void openModule() {
        if (matches.length == 0) return;
        DollyModules.Module module = DollyModules.ALL[selected];
        ave.A().a(new DollySettingsPage(this, module.name, module.options(), module.hud));
    }
    @Override protected void a(avs button) {
        if (!button.l) return;
        if (button instanceof DollySlider) { focusedSlider = (DollySlider) button; return; }
        if (button.k == 111) { ave.A().a(parent); return; }
        if (button.k == 122) { ave.A().a(new DollyHudEditorScreen(this, 0)); return; }
        if (button.k == 110) DollySettings.reset();
        else if (button.k == 100) { openModule(); return; }
        else if (button.k == 120 || button.k == 121) { view = button.k - 120; list.rows.select(0); }
        else if (button.k >= 130 && button.k < 133) { filter = button.k - 130; list.rows.select(0); }
        else if (button.k >= 300 && button.k < 300 + DollyModules.GROUPS.length) { group = button.k - 300; list.rows.select(0); }
        else if (button.k >= 1000 && button.k < 1000 + DollyModules.ALL.length) {
            selected = button.k - 1000;
            if (!wide()) { openModule(); return; }
        } else if (button.k >= 200 && button.k < 200 + keys.length) {
            activate(button.k - 200, this);
            if (ave.A().m != this && ave.A().m != null) return;
        }
        b();
    }
    @Override protected void a(char character, int code) {
        if (code == Keyboard.KEY_ESCAPE) { ave.A().a(parent); return; }
        if (view == 0 && search != null && search.a(character, code)) {
            query = search.b(); list.rows.select(0);
            avw input = search; b(); search = input; return;
        }
        if (focusedSlider != null && focusedSlider.key(code)) return;
        super.a(character, code);
    }
    @Override protected void a(int x, int y, int button) {
        if (list.press(x, y, button) || view == 1 && groups.press(x, y, button)) { b(); return; }
        if (search != null) search.a(x, y, button);
        super.a(x, y, button);
    }
    @Override protected void a(int x, int y, int button, long elapsed) {
        if (list.drag(y) || view == 1 && groups.drag(y)) b();
        else super.a(x, y, button, elapsed);
    }
    @Override protected void b(int x, int y, int button) { list.release(); groups.release(); super.b(x, y, button); }
    @Override public void k() {
        super.k(); int x = DollyScroll.mouseX(this), y = DollyScroll.mouseY(this), delta = Mouse.getEventDWheel();
        if (list.wheel(x, y, delta) || view == 1 && groups.wheel(x, y, delta)) b();
    }
    @Override public void e() { if (search != null) search.a(); }
    @Override public void a(int x, int y, float partial) {
        c(); a(q, "Dolly Client", l / 2, 10, 0xFFE8EEE9);
        String hint = DollySettings.lastError != null ? "Erro: " + DollySettings.lastError
                : "Passe o mouse para ver a função e o custo";
        a(q, q.a(hint, l - 24), l / 2, 25, DollySettings.lastError == null ? 0xFFA7B6AA : 0xFFFF8888);
        if (view == 0) {
            if (search != null) { search.g(); if (query.isEmpty() && !search.m()) q.a("Buscar módulo…", 23, 73, 0xFFA7B6AA); }
            if (matches.length == 0) q.a("Nenhum módulo encontrado", 20, 100, 0xFFA7B6AA);
            if (wide() && matches.length > 0) {
                int left = listRight() + 14; DollyModules.Module module = DollyModules.ALL[selected];
                q.a(q.a(module.name, 160), left, 72, 0xFFBFEAD1);
                int lineY = 94;
                for (String line : detail) { if (lineY + 9 > bottom() - 30) break; q.a(line, left, lineY, 0xFFA7B6AA); lineY += 11; }
            }
        } else q.a(q.a(DollyModules.GROUPS[group], l - 170), 148, 73, 0xFFBFEAD1);
        super.a(x, y, partial); list.draw(); if (view == 1) groups.draw();
        avs hovered = DollyOptionHelp.hovered(n, x, y);
        int index = hovered == null ? -1 : hovered.k - 200;
        if (hovered != null && hovered.k >= 1000) index = DollyModules.ALL[hovered.k - 1000].option;
        String key = index >= 0 && index < keys.length ? helpKey(index) : null;
        String title = key == null ? "" : label(index);
        if (hovered != null && hovered.k == 110) { key = "resetSettings"; title = "Restaurar padrões"; }
        List<String> help = tooltip.lines(key, title, q, l, System.nanoTime());
        if (help != null) a(help, tooltip.anchorX(x, l), y);
    }
    @Override public void m() { DollyScroll.finish(n); DollySettings.persist(ave.A().t); }
}
