import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class DollyPackIcons {
    public static BufferedImage resize(BufferedImage image) {
        if (!DollySettings.config.smallPackIcons || image == null
                || image.getWidth() <= 64 && image.getHeight() <= 64) return image;
        BufferedImage icon = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = icon.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(image, 0, 0, 64, 64, null);
        } finally { graphics.dispose(); }
        return icon;
    }
}
