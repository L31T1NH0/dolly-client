import dolly.hud.HudLayout;
import dolly.ui.ScrollList;
import java.io.IOException;

/** Coordinate and persistence contracts independent of Minecraft, fonts and graphics. */
public final class DollyLayoutTest {
    static void run() throws Exception {
        ScrollList scroll = new ScrollList(); scroll.size(21, 4);
        assert scroll.maximum() == 17 && scroll.scroll(999) && scroll.first() == 17;
        assert !scroll.scroll(1) && scroll.scroll(-1) && scroll.first() == 16;
        scroll.size(21, 21); assert scroll.first() == 0 && scroll.maximum() == 0;
        scroll.size(21, 4); assert scroll.drag(200, 100, 0) && scroll.first() == 17;
        assert scroll.thumbTop(100) + scroll.thumbHeight(100) == 100;
        scroll.size(0, 0); assert scroll.first() == 0 && !scroll.scroll(1);
        assert HudLayout.parse("").encode().isEmpty();
        HudLayout layout = new HudLayout();
        layout.set(0, new HudLayout.Item(25, 50, 1.25f, false, 35));
        String text = layout.encode(); HudLayout loaded = HudLayout.parse(text);
        assert loaded.get(0).x == 25 && loaded.get(0).y == 50 && loaded.get(0).scale == 1.25f;
        assert !loaded.get(0).visible && loaded.get(0).background == 35 && loaded.get(1) == null;
        assert loaded.encode().equals(text);
        for (float screen : new float[] {180, 320, 640, 1080}) for (float size : new float[] {9, 50, 120}) {
            for (float percent : new float[] {0, 25, 50, 100}) {
                float position = HudLayout.position(percent, screen, size);
                assert Math.abs(HudLayout.percent(position, screen, size) - percent) < .001;
                assert position >= 4 && position + size <= screen - 4;
            }
        }
        assert HudLayout.percent(20, 100, 120) == 0;
        assert HudLayout.snap(6, 320, 80, true) == 4;
        assert HudLayout.snap(122, 320, 80, true) == 120;
        assert HudLayout.snap(122, 320, 80, false) == 122;
        assert HudLayout.snap(999, 320, 80, false) == 236;
        for (String invalid : new String[] {"unknown:0:0:1:true:0", "ping:0:0:NaN:true:0", "ping:0:0:Infinity:true:0",
                "ping:-1:0:1:true:0", "ping:0:101:1:true:0", "ping:0:0:0:true:0", "ping:0:0:1:true:101",
                "ping:0:0:1:perhaps:0", "ping:0:0:1:true:0;ping:0:0:1:true:0", "ping:0:0:1:true:0;"}) {
            try { HudLayout.parse(invalid); throw new AssertionError("Accepted malformed HUD layout: " + invalid); }
            catch (IOException expected) { }
        }
    }
}
