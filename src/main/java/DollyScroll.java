import dolly.ui.ScrollList;

/** Native GUI scrollbar shared by modules, settings and their detail screens. */
public final class DollyScroll {
    final ScrollList rows = new ScrollList();
    private int left, right, top, bottom, visible, grab;
    private boolean dragging;
    void layout(int count, int visible, int left, int right, int top, int bottom) {
        this.left = left; this.right = right; this.top = top; this.bottom = bottom; this.visible = visible;
        rows.size(count, visible);
    }
    int first() { return rows.first(); }
    boolean wheel(int x, int y, int delta) {
        return delta != 0 && x >= left && x <= right + 6 && y >= top && y < bottom
                && rows.scroll((delta > 0 ? -1 : 1) * Math.max(1, Math.min(3, visible / 3)));
    }
    boolean press(int x, int y, int button) {
        if (button != 0 || rows.maximum() == 0 || x < right || x >= right + 6 || y < top || y >= bottom) return false;
        int height = bottom - top, thumb = rows.thumbTop(height), size = rows.thumbHeight(height);
        grab = y - top >= thumb && y - top < thumb + size ? y - top - thumb : size / 2;
        dragging = true; rows.drag(y - top, height, grab); return true;
    }
    boolean drag(int y) { return dragging && rows.drag(y - top, bottom - top, grab); }
    void release() { dragging = false; }
    void draw() {
        if (rows.maximum() == 0) return;
        int height = bottom - top, y = top + rows.thumbTop(height);
        avp.a(right + 1, top, right + 5, bottom, 0xFF29372E);
        avp.a(right + 1, y, right + 5, y + rows.thumbHeight(height), 0xFF9FC4AD);
    }
    static int mouseX(axu screen) { return org.lwjgl.input.Mouse.getEventX() * screen.l / Math.max(1, ave.A().d); }
    static int mouseY(axu screen) { return screen.m - org.lwjgl.input.Mouse.getEventY() * screen.m / Math.max(1, ave.A().e) - 1; }
    static void finish(java.util.List<avs> buttons) { for (avs button : buttons) if (button instanceof DollySlider) button.a(0, 0); }
}
