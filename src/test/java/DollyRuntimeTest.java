import dolly.config.DollyConfig;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Real Minecraft adapters with deterministic devices and GL resource doubles. */
public final class DollyRuntimeTest {
    private static final Object UNSAFE;
    private static final Method ALLOCATE;
    static {
        try {
            Class<?> type = Class.forName("sun.misc.Unsafe");
            Field field = type.getDeclaredField("theUnsafe"); field.setAccessible(true);
            UNSAFE = field.get(null); ALLOCATE = type.getMethod("allocateInstance", Class.class);
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    private static <T> T allocate(Class<T> type) throws Exception { return type.cast(ALLOCATE.invoke(UNSAFE, type)); }
    private static void set(Class<?> type, Object target, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    private static Object get(Class<?> type, Object target, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    public static class Minecraft extends ave {
        Minecraft() { super(null); }
        public void a(axu screen) { m = screen; }
        public boolean d() { return false; }
    }
    public static class EyePlayer extends SlotPlayer {
        public int eyeReads;
        EyePlayer() { super(); }
        @Override public float aS() { eyeReads++; return 1.62f; }
    }
    public static class Settings extends avh {
        public int saves;
        public void b() { saves++; DollySettings.persist(this); }
        public void a(bpg sound, float value) { }
    }
    public static class SlotPlayer extends bew {
        public boolean spectator;
        SlotPlayer() { super(null, null, null, null); }
        public boolean v() { return spectator; }
    }
    public static class Renderer extends bfr {
        public int reloads;
        Renderer() { super(null); }
        public void a() { reloads++; }
    }
    public static class Framebuffer extends bfw {
        public int releases, allocations;
        public boolean live;
        Framebuffer() { super(1, 1, false); }
        public void a() { if (bqs.i()) { releases++; live = false; } }
        public void a(int width, int height) { c = width; d = height; if (bqs.i()) { allocations++; live = true; } }
    }
    public static class EntityRenderer extends bfk {
        public int releases, resizes;
        EntityRenderer() { super(null, null); }
        public void b() { releases++; }
        public void a(int width, int height) { resizes++; }
    }
    public static class Entities extends ArrayList<pk> {
        public int reads;
        public pk get(int i) { reads++; return super.get(i); }
    }
    public static class Font extends avn {
        Font() { super(null, null, null, false); }
        public int a(String text) { return dolly.chat.ChatHistory.plain(text).length() * 6; }
        public List<String> c(String text, int width) { return Collections.singletonList(a(text, width, false)); }
        public String a(String text, int width, boolean reverse) {
            String plain = dolly.chat.ChatHistory.plain(text);
            return plain.substring(0, Math.min(plain.length(), Math.max(0, width / 6)));
        }
    }
    public static class CountingFont extends Font {
        public int measurements;
        public int a(String text) { measurements++; return super.a(text); }
    }
    public static class ButtonSkin extends DollyButton {
        final List<int[]> spans = new ArrayList<int[]>();
        ButtonSkin(int width) { super(0, 17, 23, width, 20, "Setting"); }
        @Override protected void skin(int x, int y, int u, int v, int sourceWidth, int height, int width) {
            assert y == i && height == g && v == 66;
            assert u >= 0 && sourceWidth > 0 && u + sourceWidth <= 200 : "Button samples outside its skin";
            spans.add(new int[] {x, width, u, sourceWidth});
        }
    }
    private static void wideButtonSkin() {
        for (int width : new int[] {201, 320, 505, 1024}) {
            ButtonSkin button = new ButtonSkin(width);
            button.b(button.h, button.i, 0, 66, width / 2, 20);
            button.b(button.h + width / 2, button.i, 200 - width / 2, 66, width / 2, 20);
            int[] coverage = new int[width];
            for (int[] span : button.spans) for (int x = span[0]; x < span[0] + span[1]; x++) coverage[x - button.h]++;
            for (int count : coverage) assert count == 1 : "Button skin has a gap or overlap";
            assert button.spans.get(0)[2] == 0 && button.spans.get(2)[2] == 198 : "Button lost its edge caps";
        }
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("dolly-runtime-");
        try {
            Minecraft mc = allocate(Minecraft.class);
            Settings settings = new Settings(); mc.t = settings; mc.w = true; mc.d = 1920; mc.e = 1080;
            mc.h = allocate(bew.class); mc.f = allocate(bdb.class);
            Renderer renderer = allocate(Renderer.class); mc.g = renderer;
            set(ave.class, null, "S", mc);
            set(Config.class, null, "gameSettings", settings); set(Config.class, null, "minecraft", mc);
            DollySettings.config = new DollyConfig(directory.resolve("dolly.properties"));
            DollyOptiFine.apply(settings); DollyInput.register(settings); avb.b();
            settings.f = true; bqs.l = true;
            settingsTransitions(mc, settings, renderer, directory);
            settingsScreen(mc);
            wideButtonSkin();
            optionHelp();
            input(mc, settings);
            profiling(directory);
            localAim();
            camera(mc);
            framebuffers(mc, settings);
            fps(mc, settings);
            cullingPlayers(mc);
            cullingCamera(mc);
            DollyPerformanceTest.models();
            hud(mc);
            chatAndScoreboard(mc);
            hudEditing(mc);
            presentationCaches(mc);
            slotProfile(mc);
        } finally {
            try (java.util.stream.Stream<Path> files = Files.walk(directory)) {
                for (Path path : (Iterable<Path>) files.sorted(Comparator.reverseOrder())::iterator) Files.delete(path);
            }
        }
        System.out.println("Runtime regressions passed: settings layout/navigation, failed saves/reset, zoom, input/focus, camera, framebuffers, FPS/HUD, chat, scoreboard and hotbar slot profile.");
    }

    private static void presentationCaches(Minecraft mc) throws Exception {
        CountingFont font = allocate(CountingFont.class);
        auo board = new auo(); auk objective = board.a("test", auu.b);
        aum score = board.c("Alice", objective); score.c(1);
        DollyScoreboard.clear(); DollyScoreboard.prepare(objective, font);
        int initial = font.measurements;
        DollyScoreboard.prepare(objective, font);
        assert font.measurements == initial : "Unchanged scoreboard was prepared again";
        score.c(2); DollyScoreboard.prepare(objective, font);
        assert font.measurements > initial && ((List<?>) get(DollyScoreboard.class, null, "scores")).get(0).equals("§c2");
        aul team = board.e("team"); board.a("Alice", "team"); team.b("prefix-");
        DollyScoreboard.prepare(objective, font);
        assert ((List<?>) get(DollyScoreboard.class, null, "names")).get(0).equals("prefix-Alice");
        objective.a("new title"); DollyScoreboard.prepare(objective, font);
        assert get(DollyScoreboard.class, null, "title").equals("new title");
        board.c("Zero", objective); DollyScoreboard.prepare(objective, font);
        assert ((List<?>) get(DollyScoreboard.class, null, "names")).size() == 2 : "Zero score creation was not invalidated";
        board.d("Zero", objective); DollyScoreboard.prepare(objective, font);
        assert ((List<?>) get(DollyScoreboard.class, null, "names")).size() == 1;
        initial = font.measurements; DollyTextCache.invalidate(font); DollyScoreboard.prepare(objective, font);
        assert font.measurements > initial : "Resource/font invalidation left stale widths";
        boolean numbers = DollySettings.config.scoreboardNumbers;
        initial = font.measurements; DollySettings.config.scoreboardNumbers = !numbers; DollyScoreboard.prepare(objective, font);
        assert font.measurements > initial; DollySettings.config.scoreboardNumbers = numbers;
        DollyScoreboard.clear();

        bew player = mc.h; mc.h = null;
        DollyChat chat = new DollyChat(mc);
        chat.a(new fa("cached row"), 0); chat.matches();
        Object entry = chat.history.entries().get(0);
        Map<?, ?> wrapped = (Map<?, ?>) get(DollyChat.class, chat, "wrapped");
        Object rows = wrapped.get(entry);
        chat.a(new fa("next row"), 0); chat.matches();
        assert wrapped.get(entry) == rows : "Old chat rows were recreated on arrival";
        chat.search("cached"); assert chat.matches() == 1 && wrapped.get(entry) == rows;
        chat.search(""); chat.a(new fa("cached row"), 0); chat.matches();
        assert wrapped.get(entry) != rows : "Grouped message did not refresh its counter";
        rows = wrapped.get(entry); chat.b(); chat.matches();
        assert wrapped.get(entry) != rows : "Font reload did not refresh chat rows";
        chat.c(0); chat.matches(); assert wrapped.isEmpty() : "Removed messages retained prepared rows";
        mc.h = player;
    }

    private static void localAim() throws Exception {
        bew player = allocate(bew.class);
        player.y = 90f; player.A = -45f;
        player.z = 30f; player.B = -20f;
        player.aK = 0f; player.aL = 60f;
        boolean previous = DollySettings.config.mouseFix;
        try {
            for (boolean enabled : new boolean[] {false, true}) {
                DollySettings.config.mouseFix = enabled;
                for (float partial : new float[] {0f, 0.5f, 1f}) {
                    double pitch = Math.toRadians(player.B + (player.z - player.B) * partial);
                    double yaw = Math.toRadians(enabled ? player.A + (player.y - player.A) * partial
                            : player.aL + (player.aK - player.aL) * partial);
                    aui direction = player.d(partial);
                    assert Math.abs(direction.a + Math.sin(yaw) * Math.cos(pitch)) < 0.001d
                            && Math.abs(direction.b + Math.sin(pitch)) < 0.001d
                            && Math.abs(direction.c - Math.cos(yaw) * Math.cos(pitch)) < 0.001d
                            : "CMPack aim rotation/interpolation mismatch, mouseFix=" + enabled + ", partial=" + partial;
                }
            }
        } finally { DollySettings.config.mouseFix = previous; }
    }

    private static avs button(axu screen, int id) {
        for (avs button : screen.n) if (button.k == id) return button;
        return null;
    }

    private static void controlsInside(axu screen) {
        for (avs a : screen.n) {
            assert a.h >= 0 && a.i >= 0 && a.h + a.b() <= screen.l && a.i + a.g <= screen.m : "Control outside screen";
            for (avs b : screen.n) if (a != b) assert a.h + a.b() <= b.h || b.h + b.b() <= a.h
                    || a.i + a.g <= b.i || b.i + b.g <= a.i : "Overlapping controls";
        }
    }
    private static void settingsScreen(Minecraft mc) throws Exception {
        mc.k = allocate(Font.class); mc.k.a = 9;
        Set<Integer> covered = new HashSet<Integer>();
        for (DollyModules.Module module : DollyModules.ALL) {
            assert covered.add(module.option);
            for (int option : module.settings) assert covered.add(option) : "Module setting is duplicated";
        }
        for (int[] group : DollyModules.SETTINGS) for (int option : group) assert covered.add(option) : "Technical setting is duplicated";
        for (int option = 0; option < 92; option++) assert option == 38 || covered.contains(option) : "Missing Dolly option " + option;
        for (int[] size : new int[][] {{320, 180}, {320, 240}, {427, 240}, {640, 360}, {854, 480}}) {
            DollyScreen parent = new DollyScreen(null), screen = new DollyScreen(parent);
            screen.q = mc.k; screen.l = size[0]; screen.m = size[1]; mc.m = screen; screen.b();
            DollyScroll list = (DollyScroll) get(DollyScreen.class, screen, "list");
            Set<Integer> seen = new HashSet<Integer>();
            for (int row = 0; row <= list.rows.maximum(); row++) {
                list.rows.select(row); screen.b(); controlsInside(screen);
                for (avs control : screen.n) if (control.k >= 1000) seen.add(control.k - 1000);
            }
            assert seen.size() == DollyModules.ALL.length : "Scrollbar cannot reach every module";
            assert button(screen, 101) == null && button(screen, 112) == null : "Module browser gained pagination";
            screen.a(button(screen, 131));
            for (int id : DollyModules.matching("", 1)) assert DollyScreen.enabled(DollyModules.ALL[id].option);
            screen.a(button(screen, 132));
            for (int id : DollyModules.matching("", 2)) assert !DollyScreen.enabled(DollyModules.ALL[id].option);
            assert DollyModules.matching("no-such-module", 0).length == 0;
            screen.a(button(screen, 130)); screen.a(button(screen, 121));
            DollyScroll groups = (DollyScroll) get(DollyScreen.class, screen, "groups");
            for (int group = 0; group < DollyModules.GROUPS.length; group++) {
                groups.rows.select(group); screen.b(); screen.a(button(screen, 300 + group));
                for (int row = 0; row <= list.rows.maximum(); row++) { list.rows.select(row); screen.b(); controlsInside(screen); }
            }
            DollySettingsPage zoom = new DollySettingsPage(screen, "Zoom", DollyModules.ALL[0].options(), -1);
            zoom.q = mc.k; zoom.l = size[0]; zoom.m = size[1]; mc.m = zoom; zoom.b(); controlsInside(zoom);
            float factor = DollySettings.config.zoomFactor;
            zoom.a(button(zoom, 218)); zoom.a((char) 0, Keyboard.KEY_RIGHT);
            assert DollySettings.config.zoomFactor == factor + 1;
            DollySettings.config.zoomFactor = factor;
            DollyHudEditorScreen editor = new DollyHudEditorScreen(screen, 0);
            editor.q = mc.k; editor.l = size[0]; editor.m = size[1]; editor.b(); controlsInside(editor);
            editor.a(button(editor, 14)); controlsInside(editor);
            mc.m = screen; screen.a(button(screen, 111)); assert mc.m == parent;
        }
        mc.m = null; DollySettings.persist(mc.t);
    }

    private static void optionHelp() throws Exception {
        for (int index = 0; index < 92; index++) {
            if (index == 38) continue;
            DollyOptionHelp.Tip tip = DollyOptionHelp.tip(DollyScreen.helpKey(index));
            assert tip != null && !tip.description.isEmpty() : "Missing setting help " + index;
            assert tip.impact.matches("§[7a-ce6].+") : "Missing impact text/color " + index;
        }
        for (String menu : new String[] {"CHAT", "SCOREBOARD"}) {
            for (Object option : (Object[]) get(DollySocialScreen.class, null, menu))
                assert DollyOptionHelp.tip((String) get(option.getClass(), option, "key")) != null;
        }
        Font font = allocate(Font.class);
        DollyOptionHelp help = new DollyOptionHelp();
        long now = 1_000_000_000L;
        assert help.lines("rawMouseInput", "Entrada direta do mouse", font, 320, now) == null;
        assert help.lines("rawMouseInput", "Entrada direta do mouse", font, 320, now + 299_999_999L) == null;
        List<String> lines = help.lines("rawMouseInput", "Entrada direta do mouse", font, 320, now + 300_000_000L);
        assert lines != null && lines.stream().anyMatch(line -> line.startsWith("§6"));
        assert help.lines("rawMouseInput", "Entrada direta do mouse", font, 320, now + 400_000_000L) == lines
                : "Hover rebuilt the tooltip every frame";
        int width = 0;
        for (String line : lines) { assert font.a(line) <= 280; width = Math.max(width, font.a(line)); }
        assert help.anchorX(319, 320) + 12 + width <= 320 : "Tooltip crosses the right edge";
        assert help.lines(null, "", font, 320, now + 500_000_000L) == null;
        assert help.lines("rawMouseInput", "Entrada direta do mouse", font, 320, now + 600_000_000L) == null;
        assert help.lines("rawMouseInput", "Entrada direta do mouse", font, 160, now + 900_000_000L) == null;
        lines = help.lines("rawMouseInput", "Entrada direta do mouse", font, 160, now + 1_200_000_000L);
        for (String line : lines) assert font.a(line) <= 128;
        avs disabled = new avs(1, 10, 20, 100, 16, "disabled"); disabled.l = false;
        List<avs> buttons = Collections.singletonList(disabled);
        assert DollyOptionHelp.hovered(buttons, 10, 20) == disabled : "Disabled controls still need explanations";
        assert DollyOptionHelp.hovered(buttons, 10, 36) == null;
        disabled.m = false; assert DollyOptionHelp.hovered(buttons, 10, 20) == null;
    }

    private static void chatAndScoreboard(Minecraft mc) throws Exception {
        mc.k = allocate(Font.class); mc.k.a = 9; mc.h = null;
        mc.q = allocate(avo.class);
        DollyChat chat = new DollyChat(mc); set(avo.class, mc.q, "l", chat);
        awv screen = new awv(); screen.l = 640; screen.m = 360; mc.m = screen;
        chat.a(new fa("Primeira mensagem"), 0);
        chat.a(new fa("Primeira mensagem"), 0);
        assert chat.history.entries().size() == 1 && chat.history.entries().get(0).repeats == 2;
        chat.a(new fa("Promoção da loja"), 0);
        assert chat.matches() == 2;
        DollySettings.config.chatHiddenWords = "promoção"; DollySettings.config.chatFilter = true;
        assert chat.matches() == 1;
        DollySettings.config.chatFilter = false; assert chat.matches() == 2;
        chat.search("PRIMEIRA"); assert chat.matches() == 1;
        chat.search("inexistente"); assert chat.matches() == 0;
        chat.search("");
        DollyChat.leavingMenu(chat); chat.a(new fa("Conexão seguinte"), 0);
        assert chat.history.entries().size() == 4 && chat.history.entries().get(2).separator;
        chat.a(); assert chat.history.entries().isEmpty() : "F3+D must explicitly clear retained history";
        for (int i = 0; i < 80; i++) chat.a(new fa("linha " + i), 0);
        assert chat.messageAt(10 * new avr(mc).e(), 30 * new avr(mc).e()).equals("linha 79");
        chat.b(8);
        String anchor = chat.messageAt(10 * new avr(mc).e(), 30 * new avr(mc).e());
        chat.a(new fa("nova linha"), 0);
        assert anchor.equals(chat.messageAt(10 * new avr(mc).e(), 30 * new avr(mc).e()));
        assert chat.unread() == 1;
        int badgeRight = mc.k.a("1 nova mensagem · clique para descer") + 7;
        assert DollyChatControls.click(screen, badgeRight, screen.m - 20, 0) : "Whole visible unread badge must respond";
        assert chat.unread() == 0;
        eu link = new fa("link").a(new ez().a(new et(et.a.a, "https://example.invalid")));
        chat.a(link, 0);
        eu hit = chat.a(10 * new avr(mc).e(), 30 * new avr(mc).e());
        assert hit != null && hit.b().h() != null : "Native click action lost while wrapping";
        DollyChatControls.open(screen);
        Keyboard.down[Keyboard.KEY_LCONTROL] = true;
        assert DollyChatControls.key(screen, 'f', Keyboard.KEY_F);
        Keyboard.down[Keyboard.KEY_LCONTROL] = false;
        for (char c : "linha".toCharArray()) assert DollyChatControls.key(screen, c, 0);
        assert chat.matches() == 81;
        assert DollyChatControls.key(screen, '\n', Keyboard.KEY_RETURN) && mc.m == screen : "Search Enter must not send or close chat";
        assert DollyChatControls.key(screen, '\0', Keyboard.KEY_ESCAPE);
        assert !DollyChatControls.key(screen, 'a', Keyboard.KEY_A) : "Normal typing must return to vanilla";
        DollyChatControls.close(); assert chat.matches() == 82;
        DollySettings.config.chatKeepHistory = false; DollyChat.leavingMenu(chat);
        assert chat.history.entries().isEmpty(); DollySettings.config.chatKeepHistory = true;
        chat.a(new fa("Antes de desconectar"), 0);
        DollyChat.worldChanging(mc.f); assert chat.history.entries().size() == 1 : "Dimension change must keep chat";
        DollyChat.worldChanging(null); chat.a(new fa("Reconectado"), 0);
        assert chat.history.entries().size() == 3 && chat.history.entries().get(1).separator;
        DollySettings.config.chatKeepHistory = false; DollyChat.worldChanging(null);
        assert chat.history.entries().isEmpty(); DollySettings.config.chatKeepHistory = true;
        mc.m = null; mc.h = allocate(bew.class);
        DollySettings.config.scoreboardKey = Keyboard.KEY_B; DollyInput.syncScoreboardKey();
        boolean visible = DollySettings.config.scoreboardVisible;
        DollyInput.keyState(Keyboard.KEY_B, true); DollyInput.keyState(Keyboard.KEY_B, true);
        assert DollySettings.config.scoreboardVisible != visible : "Repeated key must toggle only once";
        DollyInput.keyState(Keyboard.KEY_B, false); DollyInput.keyState(Keyboard.KEY_B, true);
        assert DollySettings.config.scoreboardVisible == visible;
        DollyInput.keyState(Keyboard.KEY_B, false);
        mc.m = screen; DollyInput.keyState(Keyboard.KEY_B, true); DollyInput.keyState(Keyboard.KEY_B, false);
        assert DollySettings.config.scoreboardVisible == visible : "Chat typing toggled scoreboard";
        for (boolean sidebar : new boolean[] {false, true}) for (int[] size : new int[][] {{320, 180}, {640, 360}}) {
            DollySocialScreen prefs = new DollySocialScreen(null, sidebar);
            prefs.q = mc.k; prefs.l = size[0]; prefs.m = size[1]; prefs.b();
            Set<Integer> seen = new HashSet<Integer>();
            while (true) {
                DollyScroll scroll = (DollyScroll) get(DollySocialScreen.class, prefs, "scroll");
                for (int first = 0; first <= scroll.rows.maximum(); first++) {
                    scroll.rows.select(first); prefs.b(); controlsInside(prefs);
                    for (avs control : prefs.n) if (control.k >= 200) seen.add(control.k);
                }
                avs next = button(prefs, 6); if (!next.l) break; prefs.a(next);
            }
            assert seen.size() == (sidebar ? 8 : 27) : "Scrolling settings lost options";
            assert button(prefs, 0) == null && button(prefs, 1) == null : "Social settings still use pages";
        }
        DollySocialScreen bindings = new DollySocialScreen(null, true);
        bindings.q = mc.k; bindings.l = 640; bindings.m = 360; bindings.b();
        for (avs button : bindings.n) if (button.k == 207) { bindings.a(button); break; }
        int saves = ((Settings) mc.t).saves;
        bindings.a('b', Keyboard.KEY_B);
        assert DollySettings.config.scoreboardKey == Keyboard.KEY_B && ((Settings) mc.t).saves == saves + 1
                : "Custom shortcut must save both Dolly config and native keybindings";
        DollySettings.config.chatHiddenWords = ""; DollySettings.config.scoreboardKey = 0; DollyInput.syncScoreboardKey(); mc.m = null;
    }

    private static void hudEditing(Minecraft mc) throws Exception {
        String previous = DollySettings.config.hudLayout; axu previousScreen = mc.m; bew previousPlayer = mc.h;
        boolean previousSnap = DollySettings.config.hudSnap;
        try {
            DollyHudPositions.replace(""); avr resolution = new avr(mc);
            DollyHudEditorScreen editor = new DollyHudEditorScreen(null, 0);
            editor.q = mc.k; editor.l = resolution.a(); editor.m = resolution.b(); mc.m = editor; editor.b();
            assert !editor.d() : "In-world HUD editing paused the game";
            assert button(editor, 20) == null : "Properties cover the live HUD by default";
            float oldX = DollyHudPositions.preview(0, resolution).x;
            editor.a((char) 0, Keyboard.KEY_RIGHT);
            assert DollyHudPositions.layout().get(0) != null;
            assert Math.abs(DollyHudPositions.preview(0, resolution).x - oldX - 1) < .01;
            assert !DollySettings.config.hudLayout.isEmpty() : "HUD position was not persisted";
            editor.a(button(editor, 0)); assert DollySettings.config.hudLayout.isEmpty() : "Undo failed to restore legacy layout";
            editor.move(-100, -100, false); editor.m();
            assert DollyHudPositions.layout().get(0).x == 0 && DollyHudPositions.layout().get(0).y == 0;
            for (int id = 0; id < 6; id++) {
                assert (Integer) get(DollyHudEditorScreen.class, editor, "selected") == id;
                editor.a(button(editor, 11));
            }
            DollyHudPositions.replace(""); DollyHudPositions.beginFrame();
            DollyHudPositions.Box actual = new DollyHudPositions.Box();
            actual.x = 80; actual.y = 75; actual.width = 130; actual.height = 54; actual.scale = 1;
            DollyHudPositions.record(5, actual);
            DollySettings.config.hudSnap = false;
            editor.a(200, 85, 0);
            assert (Integer) get(DollyHudEditorScreen.class, editor, "selected") == 5 : "Editor ignored actual rendered bounds";
            editor.a(220, 95, 0, 20L); editor.b(220, 95, 0);
            DollyHudPositions.Box moved = DollyHudPositions.bounds(5, resolution);
            assert Math.abs(moved.x - 100) < .01 && Math.abs(moved.y - 85) < .01
                    : "Dragging used preview coordinates or lost grab offset: " + moved.x + "," + moved.y + " screen=" + editor.l + "x" + editor.m;
            editor.a(105, 90, 1);
            assert button(editor, 20) != null : "Right-click did not expose selected HUD properties";
            editor.a(button(editor, 14));
            DollyHudPositions.record(1, actual);
            editor.a(200, 85, 1);
            assert (Integer) get(DollyHudEditorScreen.class, editor, "selected") == 1 : "Chat intercepted the HUD element drawn above it";
            editor.a(button(editor, 14));
            DollyHudPositions.beginFrame(); assert !DollyHudPositions.drawn(5) : "Stale HUD bounds survived into a new frame";
            editor.a(button(editor, 2)); assert mc.m == null : "Completing HUD editor did not return to the game";
            DollyHudPositions.replace(""); mc.h = null; DollyChat chat = DollyChat.current();
            chat.a(new fa("Movable link").a(new ez().a(new et(et.a.a, "https://example.invalid"))), 0);
            awv screen = new awv(); mc.m = screen;
            float width = chat.f() + 4 * chat.h(), height = chat.i() * (9 + DollySettings.config.chatSpacing) * chat.h();
            DollyHudPositions.replace("chat:50:50:1:true:0");
            float dx = DollyHudPositions.chatOffsetX(resolution, width, height), dy = DollyHudPositions.chatOffsetY(resolution, width, height);
            int rawX = Math.round((10 + dx) * resolution.e()), rawY = Math.round((30 - dy) * resolution.e());
            eu component = chat.a(rawX, rawY);
            assert component != null && component.b().h() != null : "Moving chat broke its native click action";
            DollyHudPositions.replace("chat:50:50:1:false:0");
            assert chat.a(rawX, rawY) == null : "Hidden chat retained invisible click targets";
        } finally { DollySettings.config.hudSnap = previousSnap; DollyHudPositions.replace(previous); mc.m = previousScreen; mc.h = previousPlayer; }
    }

    private static void slotProfile(Minecraft mc) throws Exception {
        SlotPlayer player = allocate(SlotPlayer.class); mc.h = player; player.bi = new wm(player);
        wm inventory = player.bi;
        DollySettings.config.blockedSlots = 10; DollySettings.config.slotProfile = true;
        inventory.c = 0; DollySlots.select(inventory, 1); assert inventory.c == 0;
        DollySlots.select(inventory, 2); assert inventory.c == 2;
        DollySlots.wheel(inventory, 120); assert inventory.c == 0;
        DollySlots.wheel(inventory, -120); assert inventory.c == 2;
        inventory.c = 0; DollySlots.wheel(inventory, 120); assert inventory.c == 8;
        inventory.c = 1; DollySlots.ensureSelection(); assert inventory.c == 2;
        assert !DollySlots.selectable(inventory, 1) && DollySlots.selectable(inventory, 2);
        zw item = new zw(); inventory.a[1] = new zx(item); inventory.c = 2;
        inventory.a(item, 0, false, false);
        assert inventory.c == 2 : "Pick-block selected a blocked hotbar slot";
        DollySettings.config.slotProfile = false; inventory.a(item, 0, false, false);
        assert inventory.c == 1 : "Disabled profile must preserve native pick-block";
        DollySettings.config.slotProfile = true;
        inventory.a[1] = null; inventory.c = 8; DollySettings.config.blockedSlots = 1;
        inventory.a(item, 0, false, true);
        assert inventory.c == 8 && inventory.a[8] == null : "Blocked creative pick must not move or replace items";
        DollySettings.config.blockedSlots = 10;
        wm other = new wm(null); DollySlots.select(other, 1); assert other.c == 1 : "Other inventories must stay native";
        player.spectator = true; DollySlots.select(inventory, 1); assert inventory.c == 1;
        player.spectator = false;
        DollySettings.config.blockedSlots = 510;
        assert !DollySlots.toggleSlot(0) && DollySettings.config.blockedSlots == 510 : "Must retain a free slot";
        DollySettings.config.blockedSlots = 10;
        DollySettings.config.slotProfileKey = Keyboard.KEY_B; DollyInput.syncSlotProfileKey();
        mc.m = null; DollyInput.keyState(Keyboard.KEY_B, true); DollyInput.keyState(Keyboard.KEY_B, true);
        assert !DollySettings.config.slotProfile : "Key repeat toggled profile twice";
        DollySlots.select(inventory, 1); assert inventory.c == 1 : "Disabled profile must release blocked slots";
        DollyInput.keyState(Keyboard.KEY_B, false); DollyInput.keyState(Keyboard.KEY_B, true);
        assert DollySettings.config.slotProfile && inventory.c == 2;
        DollyInput.keyState(Keyboard.KEY_B, false);
        mc.m = new awv(); DollyInput.keyState(Keyboard.KEY_B, true); DollyInput.keyState(Keyboard.KEY_B, false);
        assert DollySettings.config.slotProfile : "Typing in chat toggled profile";
        mc.m = null;
        for (int[] size : new int[][] {{320, 180}, {640, 360}}) {
            DollySlotScreen screen = new DollySlotScreen(null); screen.q = mc.k; screen.l = size[0]; screen.m = size[1]; screen.b();
            for (avs button : screen.n) {
                assert button.h >= 0 && button.h + button.b() <= screen.l && button.i >= 0 && button.i + 20 <= screen.m;
                assert mc.k.a(button.j) <= button.b() - 8 : "Slot state or shortcut does not fit";
                for (avs otherButton : screen.n) if (button != otherButton) assert button.h + button.b() <= otherButton.h
                        || otherButton.h + otherButton.b() <= button.h || button.i + 20 <= otherButton.i || otherButton.i + 20 <= button.i;
            }
            avs bind = null; for (avs button : screen.n) if (button.k == 1) bind = button;
            screen.a(bind); int saves = ((Settings) mc.t).saves; screen.a('a', Keyboard.KEY_A);
            assert DollySettings.config.slotProfileKey == Keyboard.KEY_A && ((Settings) mc.t).saves == saves + 1;
            screen.a(bind); screen.a('\0', Keyboard.KEY_ESCAPE);
            assert DollySettings.config.slotProfileKey == Keyboard.KEY_A;
        }
        DollySettings.config.slotProfile = false; DollySettings.config.blockedSlots = 0;
        DollySettings.config.slotProfileKey = 0; DollyInput.syncSlotProfileKey();
    }

    private static void settingsTransitions(Minecraft mc, Settings settings, Renderer renderer, Path dir) throws Exception {
        Path blocked = dir.resolve("blocked"); Files.write(blocked, new byte[] {1});
        DollySettings.config = new DollyConfig(blocked.resolve("dolly.properties"));
        DollyOptiFine.apply(settings);
        DollySettings.change("renderRegions");
        assert !settings.ofRenderRegions && !DollySettings.config.renderRegions;
        assert renderer.reloads == 1 && settings.saves == 1 && DollySettings.lastError != null;
        DollyInput.keyState(settings.ae.i(), true); DollyInput.keyState(settings.ae.i(), false);
        assert DollyInput.sprintLatched();
        DollySettings.change("toggleSprint");
        assert !DollyInput.sprintLatched() && !settings.ae.d() && !DollySettings.config.toggleSprint;
        DollyConfig before = DollySettings.config;
        int reloads = renderer.reloads;
        DollySettings.reset();
        assert DollySettings.config == before && renderer.reloads == reloads : "Failed reset changed the renderer";
        Files.delete(blocked);
        DollySettings.persist(settings);
        assert DollySettings.lastError == null;
        assert !DollyConfig.load(blocked.resolve("dolly.properties")).renderRegions;
        DollySettings.config.zoomFactor = 9.5f;
        DollySettings.change("zoomFactor");
        assert DollySettings.config.zoomFactor == 10f && DollySettings.lastError == null;
        assert DollyConfig.load(blocked.resolve("dolly.properties")).zoomFactor == 10f;
        DollySettings.change("zoomFactor"); assert DollySettings.config.zoomFactor == 11f;
        settings.ofFastRender = false;
        DollySettings.reset();
        assert DollySettings.config.fastRender && settings.ofFastRender && renderer.reloads > reloads;
        assert !Display.vsync && DollySettings.lastError == null;
    }

    private static void profiling(Path directory) throws Exception {
        DollyProfiler.directory(directory.toFile());
        DollyInput.keyState(66, true);
        assert DollyProfiler.recording && DollyProfiler.status.contains("NORMAL");
        DollyInput.keyState(66, true);
        assert DollyProfiler.recording;
        DollyInput.keyState(66, false); DollyInput.keyState(66, true);
        assert !DollyProfiler.recording;
        DollyInput.keyState(66, false);
        for (Thread t : Thread.getAllStackTraces().keySet()) if (t.getName().equals("Dolly-profiler-writer")) t.join(5000);
        Keyboard.down[42] = true;
        DollyInput.keyState(66, true);
        assert DollyProfiler.recording && DollyProfiler.status.contains("DEEP");
        DollyInput.keyState(66, false); DollyInput.keyState(66, true);
        assert !DollyProfiler.recording;
        DollyInput.keyState(66, false); Keyboard.down[42] = false;
        for (Thread t : Thread.getAllStackTraces().keySet()) if (t.getName().equals("Dolly-profiler-writer")) t.join(5000);
    }

    private static void input(Minecraft mc, Settings settings) throws Exception {
        DollyInput.tick(); DollyInput.clear();
        int sprint = settings.ae.i();
        // Both events can arrive with the physical key already up.
        DollyInput.keyState(sprint, true); DollyInput.keyState(sprint, false);
        assert DollyInput.sprintLatched() && settings.ae.d();
        DollyInput.recoverKeys(); assert settings.ae.d();
        DollyInput.keyState(sprint, true); DollyInput.keyState(sprint, true); DollyInput.keyState(sprint, false);
        assert !DollyInput.sprintLatched() && !settings.ae.d() : "Repeat toggled sprint twice";
        settings.ae.b(-96); avb.b();
        DollyInput.keyState(-96, true); DollyInput.keyState(-96, false);
        assert DollyInput.sprintLatched() && settings.ae.d();
        assert !DollyInput.physicalKey(0) && !DollyInput.physicalKey(-101) && !DollyInput.physicalKey(256);
        Mouse.down[1] = true; assert DollyInput.physicalKey(-99); Mouse.down[1] = false;
        Keyboard.down[settings.Y.i()] = true; DollyInput.recoverKeys(); assert settings.Y.d();
        Keyboard.down[settings.Y.i()] = false; DollyInput.recoverKeys(); assert !settings.Y.d();
        int oldView = settings.aB;
        Keyboard.down[DollySettings.config.perspectiveKey] = true; DollyInput.frame(); assert settings.aB == 1;
        Keyboard.down[DollySettings.config.perspectiveKey] = false; DollyInput.frame(); assert settings.aB == oldView;
        Display.active = false; DollyInput.frame(); assert !DollyInput.sprintLatched();
        DollyInput.keyState(DollySettings.config.menuKey, true); assert mc.m == null;
        Display.active = true; DollyInput.keyState(DollySettings.config.menuKey, false);
        DollyInput.keyState(DollySettings.config.menuKey, true); assert mc.m instanceof DollyScreen;
        DollyInput.keyState(DollySettings.config.menuKey, false); mc.m = null;
        Keyboard.down[DollySettings.config.zoomKey] = true; Mouse.wheel = 1;
        DollySettings.config.zoomFactor = 9.5f; assert DollyInput.wheel() == 0 && DollySettings.config.zoomFactor == 11.875f;
        Mouse.wheel = 0; DollyInput.wheel(); assert DollySettings.config.zoomFactor == 11.875f;
        Keyboard.down[DollySettings.config.zoomKey] = false; Mouse.wheel = 1;
        assert DollySettings.config.scrollLock && DollyInput.wheel() == 0;
        DollySettings.config.scrollLock = false;
        assert DollyInput.wheel() == 1;
        DollySettings.config.scrollLock = true;
        Keyboard.event = 145; DollySettings.config.linuxShiftFix = true;
        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux")) assert DollyInput.eventKey() == 3;
        DollySettings.config.linuxShiftFix = false; assert DollyInput.eventKey() == 145;
        DollySettings.config.linuxShiftFix = true;
        avb zoom = (avb) get(DollyInput.class, null, "zoom");
        zoom.b(-97); avb.b(); Mouse.down[3] = true; assert DollyInput.zoomHeld();
        Mouse.down[3] = false; assert !DollyInput.zoomHeld();
        zoom.b(46); avb.b();
        DollyInput.keyState(DollySettings.config.menuKey, true); mc.m = null;
        Display.active = false; DollyInput.frame(); Display.active = true;
        DollyInput.keyState(DollySettings.config.menuKey, true); assert mc.m instanceof DollyScreen;
        DollyInput.keyState(DollySettings.config.menuKey, false); mc.m = null;
        DollyInput.keyState(settings.ae.i(), true); DollyInput.keyState(settings.ae.i(), false);
        mc.f = allocate(bdb.class); DollyInput.frame(); assert !DollyInput.sprintLatched();
        DollyInput.clear(); settings.ae.b(29); avb.b();
    }

    private static void camera(Minecraft mc) throws Exception {
        avh settings = mc.t;
        settings.aG = false;
        Display.active = true;
        Keyboard.down[DollySettings.config.zoomKey] = true;
        assert DollyCamera.smoothCamera(settings);
        assert !settings.aG : "Zoom must not change the saved cinematic-camera setting";
        Keyboard.down[DollySettings.config.zoomKey] = false;
        assert !DollyCamera.smoothCamera(settings);
        Keyboard.down[DollySettings.config.zoomKey] = true;
        Display.active = false; assert !DollyCamera.smoothCamera(settings);
        Display.active = true;
        mc.m = allocate(DollyScreen.class); assert !DollyCamera.smoothCamera(settings); mc.m = null;
        DollySettings.config.zoom = false; assert !DollyCamera.smoothCamera(settings);
        DollySettings.config.zoom = true;
        Keyboard.down[DollySettings.config.zoomKey] = false;
        settings.aG = true; assert DollyCamera.smoothCamera(settings); settings.aG = false;
        float expectedZoom = 0, expectedEye = 0;
        for (int hz : new int[] {20, 60, 144, 240, 1000}) {
            set(DollyCamera.class, null, "lastFrame", 0L);
            DollyCamera.advance(1L, 1.62f, 1f, true);
            int steps = Math.max(1, hz / 10);
            for (int i = 1; i <= steps; i++) DollyCamera.advance(1L + i * 100_000_000L / steps, 1.54f, 0.25f, true);
            if (expectedZoom == 0) { expectedZoom = DollyCamera.fov(70f); expectedEye = DollyCamera.eyeHeight(1.54f); }
            assert Math.abs(DollyCamera.fov(70f) - expectedZoom) < 0.0001f;
            assert Math.abs(DollyCamera.eyeHeight(1.54f) - expectedEye) < 0.00001f;
        }
        float fov = DollyCamera.fov(70f);
        for (int i = 0; i < 100; i++) assert DollyCamera.fov(70f) == fov;
        DollyCamera.advance(2_000_000_001L, 1.62f, 1f, false);
        assert DollyCamera.eyeHeight(0f) == 1.62f;
        set(ave.class, mc, "ad", mc.h);
        DollyCamera.beginFrame(); assert DollyCamera.fov(70f) == 70f;
    }

    private static void framebuffers(Minecraft mc, Settings settings) throws Exception {
        Framebuffer main = allocate(Framebuffer.class), loading = allocate(Framebuffer.class);
        avi screen = allocate(avi.class); set(avi.class, screen, "g", loading); mc.n = screen;
        set(ave.class, mc, "aF", main);
        EntityRenderer renderer = allocate(EntityRenderer.class); mc.o = renderer;
        settings.ofFastRender = false;
        DollyFramebuffer.recreate(); assert main.live && loading.live;
        net.optifine.shaders.Shaders.shaderPackLoaded = true;
        DollySettings.change("fastRender");
        assert !settings.ofFastRender && main.live && main.releases == 0 && DollySettings.lastError != null;
        net.optifine.shaders.Shaders.shaderPackLoaded = false;
        for (int i = 0; i < 5; i++) {
            DollySettings.change("fastRender"); assert !main.live && !loading.live;
            DollySettings.change("fastRender"); assert main.live && loading.live;
        }
        assert main.releases == 5 && main.allocations == 6;
        assert loading.releases == 5 && loading.allocations == 6;
        mc.d = 1280; mc.e = 720; DollyFramebuffer.recreate(); assert main.c == 1280 && loading.d == 720;
        avi next = allocate(avi.class); set(avi.class, next, "g", allocate(Framebuffer.class));
        DollyFramebuffer.replaceLoading(mc, next); assert mc.n == next && !loading.live;
        bqs.l = false; DollyFramebuffer.releaseBeforeChange(); DollyFramebuffer.recreate();
        bqs.l = true;
        // The finally block restores resources even when renderer reload throws.
        bfr old = mc.g;
        mc.g = allocate(FailingRenderer.class);
        try { DollySettings.change("fastRender"); throw new AssertionError("Missing renderer failure"); }
        catch (IllegalStateException expected) { assert expected.getMessage().equals("renderer failed"); }
        assert main.c == mc.d && renderer.resizes > 0;
        mc.g = old; mc.o = null; mc.n = null; set(ave.class, mc, "aF", null);
    }
    public static class FailingRenderer extends Renderer {
        public void a() { throw new IllegalStateException("renderer failed"); }
    }

    private static void fps(Minecraft mc, Settings settings) throws Exception {
        axs terrain = allocate(axs.class);
        DollySettings.config.fastWorldLoading = true;
        assert DollyResources.loadingScreen(terrain) == null;
        axu menu = allocate(DollyScreen.class);
        assert DollyResources.loadingScreen(menu) == menu;
        assert DollyResources.loadingScreen(null) == null;
        DollyResources.loadingMessage(null, "test");
        DollyResources.loadingProgress(null, "test");
        DollySettings.config.fastWorldLoading = false;
        assert DollyResources.loadingScreen(terrain) == terrain;
        DollySettings.config.fastWorldLoading = true;
        settings.g = 260;
        assert !mc.k() && mc.j() == settings.g;
        settings.g = 120; assert mc.k() && mc.j() == 120;
        mc.m = allocate(DollyScreen.class); assert mc.j() == 120; mc.m = null;
        Display.active = false; assert mc.k() && mc.j() == 30;
        settings.g = 15; assert mc.j() == 15;
        Display.active = true;
        bdb world = mc.f; mc.f = null;
        DollySettings.config.menuFps = 0; assert mc.k() && mc.j() == 144;
        Display.mode.hz = 0; assert mc.j() == 60;
        DollySettings.config.menuFps = 300; assert mc.j() == 300 && mc.k();
        Display.active = false; settings.g = 260; assert mc.j() == 30;
        Display.active = true; mc.f = world;
        settings.g = 260; mc.m = allocate(axs.class);
        assert mc.j() == 30 && mc.k();
        DollySettings.config.loadingFps = 45; assert mc.j() == 45;
        settings.g = 15; assert mc.j() == 15;
        mc.m = allocate(DollyScreen.class); settings.g = 260;
        assert mc.j() == 260 && !mc.k() : "Loading cap leaked into gameplay menus";
        mc.m = null; DollySettings.config.loadingFps = 30;
    }

    private static void cullingPlayers(Minecraft mc) throws Exception {
        assert !DollyCulling.eligible(allocate(bew.class));
        assert !DollyCulling.eligible(allocate(bex.class)) : "Remote player was eligible for occlusion";
        DollyCulling.beginFrame(.5f);
        assert !DollyCulling.entity(mc.h) : "Player culling must always bypass world queries";
    }

    private static void cullingCamera(Minecraft mc) throws Exception {
        bew previous = mc.h; bdb world = mc.f;
        try {
            EyePlayer player = allocate(EyePlayer.class); mc.h = player;
            player.p = 2; player.q = 4; player.r = 6; player.s = 10; player.t = 12; player.u = 14;
            DollyCulling.beginFrame(.5f);
            DollyCulling.camera(mc); DollyCulling.camera(mc);
            assert player.eyeReads == 1 : "The same render pass read camera height twice";
            assert (Double) get(DollyCulling.class, null, "eyeX") == 6d;
            assert (Double) get(DollyCulling.class, null, "eyeZ") == 10d;
            assert Math.abs((Double) get(DollyCulling.class, null, "eyeY") - (8d + DollyCamera.eyeHeight(1.62f))) < .00001;
            DollyCulling.beginFrame(1f); DollyCulling.camera(mc);
            assert player.eyeReads == 2 && (Double) get(DollyCulling.class, null, "eyeX") == 10d;
            mc.f = allocate(bdb.class); DollyCulling.camera(mc);
            assert player.eyeReads == 3 : "A world change reused the old snapshot";
            EyePlayer replacement = allocate(EyePlayer.class); mc.h = replacement; DollyCulling.camera(mc);
            assert replacement.eyeReads == 1 : "A player change reused the old snapshot";
        } finally { mc.h = previous; mc.f = world; DollyCulling.beginFrame(0f); }
    }

    private static void hud(Minecraft mc) throws Exception {
        Entities entities = new Entities(); set(adm.class, mc.f, "f", entities);
        for (int i = 0; i < 500; i++) entities.add(allocate(vj.class));
        DollySettings.config.tntTimer = false; DollyHud.tick(); assert entities.reads == 0;
        DollySettings.config.tntTimer = true; DollyHud.tick();
        assert entities.reads == 8 && ((Integer) get(DollyHud.class, null, "tntCount")) == 8;
        mc.t.aC = true;
        for (int i = 0; i < 100; i++) DollyHud.render();
        assert entities.reads == 8;
        mc.t.aC = false;
        for (pk entity : entities) entity.s = 1000;
        DollyHud.tick(); assert entities.reads == 508 && ((Integer) get(DollyHud.class, null, "tntCount")) == 0;
        for (int i = 0; i < 100; i++) DollyHud.render();
        assert entities.reads == 508;
        bdb world = mc.f; mc.f = null; DollyHud.render(); DollyHud.tick(); mc.f = world;
        assert get(DollyHud.class, null, "world") == null;
    }
}
