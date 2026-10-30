import dolly.chat.LinkImage;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import org.lwjgl.opengl.GL11;

/** Worker owns downloads; texture creation/deletion stays on the render thread. */
public final class DollyImagePreview {
    private static final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(1), runnable -> { Thread t = new Thread(runnable, "Dolly image preview"); t.setDaemon(true); return t; });
    private static final Map<String, Preview> cache = new LinkedHashMap<String, Preview>(8, .75f, true);
    private static String hovered;
    private static long since;
    private static final class Preview {
        volatile BufferedImage image;
        volatile boolean finished;
        jy texture;
        int width, height;
    }
    public static void close() {
        hovered = null;
        for (Preview preview : cache.values()) if (preview.texture != null) ave.A().P().c(preview.texture);
        cache.clear(); worker.getQueue().clear();
    }
    public static void draw(String link, int x, int y, int screenWidth, int screenHeight) {
        URI uri = DollySettings.config.chatImagePreview ? LinkImage.compatible(link) : null;
        if (uri == null) { hovered = null; return; }
        long now = System.nanoTime() / 1000000L;
        if (!link.equals(hovered)) { hovered = link; since = now; return; }
        if (now - since < 350) return;
        Preview preview = cache.get(link);
        if (preview == null) {
            if (cache.size() >= 8) {
                Iterator<Preview> oldest = cache.values().iterator(); Preview old = oldest.next();
                if (old.texture != null) ave.A().P().c(old.texture);
                oldest.remove();
            }
            preview = new Preview(); final Preview target = preview;
            cache.put(link, preview);
            try { worker.execute(() -> {
                try { target.image = LinkImage.fetch(uri); }
                catch (Exception ignored) { }
                finally { target.finished = true; }
            }); } catch (RejectedExecutionException busy) { cache.remove(link); return; }
        }
        if (preview.finished && preview.image != null && preview.texture == null) {
            preview.width = preview.image.getWidth(); preview.height = preview.image.getHeight();
            preview.texture = ave.A().P().a("dolly-link-preview", new blz(preview.image)); preview.image = null;
        }
        if (preview.texture == null) {
            ave.A().k.a(preview.finished ? "Prévia indisponível" : "Carregando imagem…", Math.max(2, x), Math.max(2, y), 0xFFFFFFFF);
            return;
        }
        int width = Math.min(preview.width, Math.max(1, screenWidth - 12));
        int height = Math.max(1, preview.height * width / preview.width);
        if (height > screenHeight - 60) { height = Math.max(1, screenHeight - 60); width = Math.max(1, preview.width * height / preview.height); }
        int left = Math.max(4, Math.min(x, screenWidth - width - 4));
        int top = Math.max(4, Math.min(y, screenHeight - height - 4));
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING);
        try {
            avp.a(left - 2, top - 2, left + width + 2, top + height + 2, 0xEE17231C);
            ave.A().P().a(preview.texture); bfl.c(1, 1, 1, 1);
            avp.a(left, top, 0f, 0f, preview.width, preview.height, width, height, preview.width, preview.height);
        } finally {
            if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST);
            if (lighting) GL11.glEnable(GL11.GL_LIGHTING);
        }
    }
}
