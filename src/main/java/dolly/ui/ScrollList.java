package dolly.ui;

/** Row-based scrolling keeps partially clipped controls from receiving clicks. */
public final class ScrollList {
    private int count, visible = 1, first;
    public void size(int count, int visible) { this.count = Math.max(0, count); this.visible = Math.max(1, visible); first = Math.min(first, maximum()); }
    public int first() { return first; }
    public int maximum() { return Math.max(0, count - visible); }
    public boolean scroll(int rows) { return select(first + rows); }
    public boolean select(int row) {
        int next = Math.max(0, Math.min(maximum(), row));
        if (next == first) return false;
        first = next; return true;
    }
    public int thumbHeight(int height) { return maximum() == 0 ? height : Math.min(height, Math.max(12, height * visible / Math.max(1, count))); }
    public int thumbTop(int height) { return maximum() == 0 ? 0 : (height - thumbHeight(height)) * first / maximum(); }
    public boolean drag(int y, int height, int grab) {
        int travel = height - thumbHeight(height);
        return travel > 0 && select(Math.round((y - grab) * maximum() / (float) travel));
    }
}
