package dolly.hud;

import java.io.IOException;

/** Optional entries preserve legacy placement until an element is actually edited. */
public final class HudLayout {
    public static final String[] KEYS = {"ping", "tnt", "sprint", "slots", "scoreboard", "chat"};
    public static final int COUNT = 6;
    public static final class Item {
        public float x, y, scale = 1;
        public int background;
        public boolean visible = true;
        public Item(float x, float y, float scale, boolean visible, int background) {
            this.x = x; this.y = y; this.scale = scale; this.visible = visible; this.background = background;
        }
    }
    private final Item[] items = new Item[COUNT];
    public Item get(int id) { return items[id]; }
    public void set(int id, Item item) { items[id] = item; }
    public static float position(float percent, float screen, float size) { return 4 + Math.max(0, screen - 8 - size) * percent / 100; }
    public static float percent(float position, float screen, float size) {
        float available = screen - 8 - size;
        return available <= 0 ? 0 : Math.max(0, Math.min(100, (position - 4) * 100 / available));
    }
    public static float snap(float position, float screen, float size, boolean enabled) {
        float end = Math.max(4, screen - 4 - size), center = (4 + end) / 2;
        position = Math.max(4, Math.min(end, position));
        if (enabled) {
            if (Math.abs(position - 4) <= 4) return 4;
            if (Math.abs(position - center) <= 4) return center;
            if (Math.abs(position - end) <= 4) return end;
        }
        return position;
    }
    public static HudLayout parse(String text) throws IOException {
        if (text == null || text.length() > 2048) throw new IOException("Invalid HUD layout");
        HudLayout layout = new HudLayout();
        if (text.isEmpty()) return layout;
        String[] entries = text.split(";", -1);
        if (entries.length > COUNT) throw new IOException("Too many HUD elements");
        try {
            for (String entry : entries) {
                String[] fields = entry.split(":", -1);
                if (fields.length != 6) throw new IllegalArgumentException();
                int id = -1;
                for (int i = 0; i < COUNT; i++) if (KEYS[i].equals(fields[0])) id = i;
                if (id < 0 || layout.items[id] != null || !(fields[4].equals("true") || fields[4].equals("false"))) throw new IllegalArgumentException();
                Item item = new Item(Float.parseFloat(fields[1]), Float.parseFloat(fields[2]), Float.parseFloat(fields[3]),
                        Boolean.parseBoolean(fields[4]), Integer.parseInt(fields[5]));
                validate(item); layout.items[id] = item;
            }
        } catch (IllegalArgumentException error) { throw new IOException("Malformed HUD layout", error); }
        return layout;
    }
    private static void validate(Item item) {
        if (!Float.isFinite(item.x) || !Float.isFinite(item.y) || !Float.isFinite(item.scale)
                || item.x < 0 || item.x > 100 || item.y < 0 || item.y > 100 || item.scale < .5f || item.scale > 2
                || item.background < 0 || item.background > 100) throw new IllegalArgumentException("HUD setting outside supported range");
    }
    public String encode() {
        StringBuilder text = new StringBuilder();
        for (int id = 0; id < COUNT; id++) {
            Item item = items[id]; if (item == null) continue;
            validate(item);
            if (text.length() > 0) text.append(';');
            text.append(KEYS[id]).append(':').append(item.x).append(':').append(item.y).append(':').append(item.scale)
                    .append(':').append(item.visible).append(':').append(item.background);
        }
        return text.toString();
    }
}
