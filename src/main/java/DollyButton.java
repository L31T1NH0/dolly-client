/** Stretch the middle of the vanilla skin; its two 100 px halves cannot span wide controls. */
public class DollyButton extends avs {
    public DollyButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }
    @Override public void b(int x, int y, int u, int v, int width, int height) {
        if (f <= 200) { super.b(x, y, u, v, width, height); return; }
        boolean left = x == h;
        if (!left) width = h + f - x;
        int capX = left ? x : x + width - 2;
        skin(capX, y, left ? 0 : 198, v, 2, height, 2);
        skin(left ? x + 2 : x, y, left ? 2 : 100, v, 98, height, width - 2);
    }
    protected void skin(int x, int y, int u, int v, int sourceWidth, int height, int width) {
        avp.a(x, y, u, v, sourceWidth, height, width, height, 256f, 256f);
    }
}
