import dolly.hud.HudLayout;
import java.io.IOException;

/** One parsed layout and reusable bounds; no parsing or allocation per HUD element/frame. */
public final class DollyHudPositions {
    static final String[] NAMES = {"Ping", "TNT Timer", "Sprint", "Perfil de slots", "Scoreboard", "Chat"};
    static final class Box {
        float x, y, width, height, scale;
        boolean contains(float x, float y) { return x >= this.x && y >= this.y && x < this.x + width && y < this.y + height; }
    }
    private static final Box[] boxes = new Box[HudLayout.COUNT];
    private static final Box[] live = new Box[HudLayout.COUNT];
    private static final boolean[] drawn = new boolean[HudLayout.COUNT];
    static { for (int i = 0; i < boxes.length; i++) { boxes[i] = new Box(); live[i] = new Box(); } }
    static boolean editing() { return ave.A().m instanceof DollyHudEditorScreen; }
    static void beginFrame() { if (editing()) java.util.Arrays.fill(drawn, false); }
    static void record(int id, Box box) {
        if (!editing()) return;
        Box target = live[id]; target.x = box.x; target.y = box.y; target.width = box.width;
        target.height = box.height; target.scale = box.scale; drawn[id] = true;
    }
    static boolean drawn(int id) { return drawn[id]; }
    static Box bounds(int id, avr resolution) {
        if (!drawn[id]) return preview(id, resolution);
        Box box = live[id]; HudLayout.Item item = layout().get(id);
        if (item != null) {
            box.x = HudLayout.position(item.x, resolution.a(), box.width);
            box.y = HudLayout.position(item.y, resolution.b() - (id == 5 ? 24 : 0), box.height);
        }
        return box;
    }
    private static String source;
    private static Object owner;
    private static HudLayout layout = new HudLayout();
    static HudLayout layout() {
        String text = DollySettings.config.hudLayout;
        if (owner != DollySettings.config || !text.equals(source)) {
            try { layout = HudLayout.parse(text); source = text; owner = DollySettings.config; }
            catch (IOException error) { throw new IllegalStateException("Invalid validated HUD layout", error); }
        }
        return layout;
    }
    static void replace(String text) { DollySettings.config.hudLayout = text; source = null; DollyHudCache.invalidate(); }
    static void commit() {
        HudLayout current = layout(); HudLayout.Item scoreboard = current.get(4);
        if (scoreboard != null) {
            DollySettings.config.scoreboardX = Math.round(scoreboard.x);
            DollySettings.config.scoreboardY = Math.round(scoreboard.y);
        }
        DollySettings.config.hudLayout = current.encode(); source = DollySettings.config.hudLayout;
        DollySettings.persist(ave.A().t);
    }
    static boolean visible(int id) {
        HudLayout.Item item = layout().get(id);
        return item == null || item.visible;
    }
    static float scale(int id) {
        if (id == 4) return DollySettings.config.scoreboardScale;
        if (id == 5) return DollySettings.config.chatScale;
        HudLayout.Item item = layout().get(id); return item == null ? 1 : item.scale;
    }
    static int background(int id) {
        if (id == 4) return DollySettings.config.scoreboardBackground ? DollySettings.config.scoreboardOpacity : 0;
        if (id == 5) return DollySettings.config.chatBackground ? DollySettings.config.chatBackgroundOpacity : 0;
        HudLayout.Item item = layout().get(id); return item == null ? 0 : item.background;
    }
    static Box box(int id, avr resolution, float width, float height, float scale, float legacyX, float legacyY) {
        float screenHeight = resolution.b() - (id == 5 ? 24 : 0);
        scale = Math.min(scale, Math.min((resolution.a() - 8) / Math.max(1f, width), (screenHeight - 8) / Math.max(1f, height)));
        Box box = boxes[id]; box.width = width * scale; box.height = height * scale; box.scale = scale;
        HudLayout.Item item = layout().get(id);
        box.x = item == null ? legacyX : HudLayout.position(item.x, resolution.a(), box.width);
        box.y = item == null ? legacyY : HudLayout.position(item.y, screenHeight, box.height);
        return box;
    }
    static Box preview(int id, avr resolution) {
        avn font = ave.A().k; int width = font.a(NAMES[id]);
        float scale = scale(id), height = 12;
        if (id == 4) return box(id, resolution, width + 4, height + 4, scale,
                HudLayout.position(DollySettings.config.scoreboardX, resolution.a(), (width + 4) * scale),
                HudLayout.position(DollySettings.config.scoreboardY, resolution.b(), (height + 4) * scale));
        if (id == 5) {
            DollyChat chat = DollyChat.current();
            if (chat != null) return chat(resolution, chat.f() + 4 * chat.h(), chat.i() * (9 + DollySettings.config.chatSpacing) * chat.h());
            return chat(resolution, DollySettings.config.chatWidth + 4 * scale, Math.min(DollySettings.config.chatHeight * scale, resolution.b() - 50));
        }
        return box(id, resolution, width, height, scale, 4, 4 + id * 12);
    }
    static HudLayout.Item editable(int id, avr resolution) {
        HudLayout.Item item = layout().get(id);
        if (item == null) {
            Box box = bounds(id, resolution);
            item = new HudLayout.Item(HudLayout.percent(box.x, resolution.a(), box.width), HudLayout.percent(box.y, resolution.b() - (id == 5 ? 24 : 0), box.height), scale(id), true, background(id));
            layout().set(id, item);
        }
        return item;
    }
    static void syncOption(String key) {
        HudLayout current = layout(); HudLayout.Item scoreboard = current.get(4), chat = current.get(5);
        boolean changed = false;
        if (scoreboard != null && (key.equals("scoreboardX") || key.equals("scoreboardY") || key.equals("scoreboardScale")
                || key.equals("scoreboardBackground") || key.equals("scoreboardOpacity"))) {
            if (key.equals("scoreboardX")) scoreboard.x = DollySettings.config.scoreboardX;
            if (key.equals("scoreboardY")) scoreboard.y = DollySettings.config.scoreboardY;
            scoreboard.scale = scale(4); scoreboard.background = background(4); changed = true;
        }
        if (chat != null && (key.equals("chatScale") || key.equals("chatBackground") || key.equals("chatBackgroundOpacity"))) {
            chat.scale = scale(5); chat.background = background(5); changed = true;
        }
        if (changed) {
            DollySettings.config.hudLayout = current.encode(); source = DollySettings.config.hudLayout;
        }
    }
    static void draw(int id, String[] lines, int count, int color, int legacyY, avr resolution) {
        if (!visible(id) || count == 0) return;
        HudLayout.Item item = layout().get(id);
        avn font = ave.A().k;
        if (item == null && !editing()) {
            for (int i = 0; i < count; i++) font.a(lines[i], 4, legacyY + i * 12, color);
            return;
        }
        int width = 0;
        for (int i = 0; i < count; i++) width = Math.max(width, font.a(lines[i]));
        Box box = box(id, resolution, width, count * 12, item == null ? 1 : item.scale, 4, legacyY);
        record(id, box);
        if (item == null) { for (int i = 0; i < count; i++) font.a(lines[i], 4, legacyY + i * 12, color); return; }
        bfl.E();
        try {
            bfl.b(box.x, box.y, 0); bfl.a(box.scale, box.scale, 1);
            if (item.background > 0) avp.a(-2, -2, width + 2, count * 12, item.background * 255 / 100 << 24);
            for (int i = 0; i < count; i++) font.a(lines[i], 0, i * 12, color);
        } finally { bfl.F(); }
    }
    static Box chat(avr resolution, float width, float height) {
        return box(5, resolution, width, height, 1, 2, resolution.b() - 28 - height);
    }
    static float chatOffsetX(avr resolution, float width, float height) { return layout().get(5) == null ? 0 : chat(resolution, width, height).x - 2; }
    static float chatOffsetY(avr resolution, float width, float height) {
        if (layout().get(5) == null) return 0;
        Box box = chat(resolution, width, height); return box.y + box.height - (resolution.b() - 28);
    }
}
