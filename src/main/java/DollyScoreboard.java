import dolly.config.DollyConfig;
import java.util.*;

/** Renders the server's existing sidebar objective with local presentation settings. */
public final class DollyScoreboard {
    private static auk cachedObjective;
    private static avn cachedFont;
    private static boolean dirty = true, numbers;
    private static final List<String> names = new ArrayList<String>(), scores = new ArrayList<String>();
    private static final int[] scoreWidths = new int[15];
    private static String title;
    private static int width, titleWidth;

    public static void invalidate(auo board) {
        if (cachedObjective != null && cachedObjective.a() == board) dirty = true;
    }
    public static void clear() {
        cachedObjective = null; cachedFont = null; names.clear(); scores.clear(); title = null; dirty = true;
    }
    static void prepare(auk objective, avn font) {
        boolean showNumbers = DollySettings.config.scoreboardNumbers;
        if (!dirty && cachedObjective == objective && cachedFont == font && numbers == showNumbers) return;
        dirty = true;
        auo board = objective.a();
        List<aum> rows = new ArrayList<aum>();
        for (aum score : board.i(objective)) if (score.e() != null && !score.e().startsWith("#")) rows.add(score);
        if (rows.size() > 15) rows = rows.subList(rows.size() - 15, rows.size());
        names.clear(); scores.clear();
        title = objective.d(); width = titleWidth = font.a(title);
        for (aum score : rows) {
            String name = aul.a(board.h(score.e()), score.e()); names.add(name);
            String number = "§c" + score.c();
            scoreWidths[scores.size()] = showNumbers ? font.a(number) : 0; scores.add(number);
            width = Math.max(width, font.a(name) + (showNumbers ? font.a("  " + score.c()) : 0));
        }
        cachedObjective = objective; cachedFont = font; numbers = showNumbers; dirty = false;
    }
    public static void render(auk objective, avr resolution) {
        DollyConfig c = DollySettings.config;
        if (!c.scoreboardVisible || !DollyHudPositions.visible(4)) return;
        avn font = ave.A().k;
        prepare(objective, font);
        float scale = Math.min(c.scoreboardScale, Math.min((resolution.a() - 8f) / (width + 4),
                (resolution.b() - 8f) / ((names.size() + 1) * 9 + 4)));
        int height = (names.size() + 1) * 9;
        float x = 4 + Math.max(0, resolution.a() - 8 - (width + 4) * scale) * c.scoreboardX / 100f;
        float y = 4 + Math.max(0, resolution.b() - 8 - (height + 4) * scale) * c.scoreboardY / 100f;
        if (DollyHudPositions.layout().get(4) != null || DollyHudPositions.editing()) {
            DollyHudPositions.Box box = DollyHudPositions.box(4, resolution, width + 4, height + 4, scale, x, y);
            if (DollyHudPositions.layout().get(4) == null) { box.x = x - 2 * scale; box.y = y - 2 * scale; }
            DollyHudPositions.record(4, box);
            if (DollyHudPositions.layout().get(4) != null) { x = box.x + 2 * scale; y = box.y + 2 * scale; }
        }
        bfl.E();
        try {
            bfl.b(x, y, 0); bfl.a(scale, scale, 1);
            if (c.scoreboardBackground) avp.a(-2, -2, width + 2, height + 2, (c.scoreboardOpacity * 255 / 100) << 24);
            font.a(title, (width - titleWidth) / 2, 0, 0xFFFFFFFF);
            for (int index = 0; index < names.size(); index++) {
                int lineY = (names.size() - index) * 9;
                font.a(names.get(index), 0, lineY, 0xFFFFFFFF);
                if (c.scoreboardNumbers) {
                    String number = scores.get(index); font.a(number, width - scoreWidths[index], lineY, 0xFFFFFFFF);
                }
            }
        } finally { bfl.F(); }
    }
}
