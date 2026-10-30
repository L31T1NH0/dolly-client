import java.awt.image.BufferedImage;
import org.lwjgl.opengl.GL11;

/** A single texture, created and released exclusively on the render thread. */
public final class DollyPicture {
    private final BufferedImage image;
    private jy texture;
    public DollyPicture(BufferedImage image) { this.image = image; }
    public void close() { if (texture != null) { ave.A().P().c(texture); texture = null; } }
    public void draw(int x, int y, int maxWidth, int maxHeight) {
        if (image == null || maxWidth <= 0 || maxHeight <= 0) return;
        if (texture == null) texture = ave.A().P().a("dolly-screenshot", new blz(image));
        double scale = Math.min(maxWidth / (double) image.getWidth(), maxHeight / (double) image.getHeight());
        int width = Math.max(1, (int) (image.getWidth() * scale)), height = Math.max(1, (int) (image.getHeight() * scale));
        int left = x + (maxWidth - width) / 2, top = y + (maxHeight - height) / 2;
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING);
        try {
            avp.a(left - 2, top - 2, left + width + 2, top + height + 2, 0xEE17231C);
            ave.A().P().a(texture); bfl.c(1, 1, 1, 1);
            avp.a(left, top, 0f, 0f, image.getWidth(), image.getHeight(), width, height, image.getWidth(), image.getHeight());
        } finally {
            if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST);
            if (lighting) GL11.glEnable(GL11.GL_LIGHTING);
        }
    }
}
