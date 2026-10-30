import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Local, session-scoped controls use unguessable per-capture IDs, never server paths. */
public final class DollyScreenshotManager {
    static final java.util.List<Capture> captures = new ArrayList<Capture>();
    static String status = "";
    private static DollyPicture preview;
    private static long previewUntil;
    public static final class Capture {
        final String id = UUID.randomUUID().toString();
        final File file;
        final BufferedImage thumbnail;
        Path favorite;
        boolean deleted, busy;
        Capture(File file, BufferedImage thumbnail) { this.file = file; this.thumbnail = thumbnail; }
    }
    static BufferedImage thumbnail(BufferedImage image) {
        double scale = Math.min(1, Math.min(480d / image.getWidth(), 270d / image.getHeight()));
        BufferedImage small = new BufferedImage(Math.max(1, (int) (image.getWidth() * scale)), Math.max(1, (int) (image.getHeight() * scale)), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = small.createGraphics();
        try { g.drawImage(image, 0, 0, small.getWidth(), small.getHeight(), null); } finally { g.dispose(); }
        return small;
    }
    static eu register(File file, BufferedImage thumbnail) {
        Capture capture = new Capture(file, thumbnail);
        if (captures.size() == 32) captures.remove(0);
        captures.add(capture); status = "";
        if (preview != null) preview.close();
        preview = DollySettings.config.screenshotPreview ? new DollyPicture(thumbnail) : null;
        previewUntil = System.nanoTime() / 1000000L + 5000;
        if (DollySettings.config.screenshotAutoCopy) action(capture, "copy");
        return feedback(capture);
    }
    static eu feedback(Capture capture) {
        eu message = new fa(DollySettings.config.screenshotCompact ? "Captura salva. " : "Captura salva: " + capture.file.getName() + " (" + capture.file.length() / 1024 + " KiB). ");
        eu controls = new fa("§b[Gerenciar]");
        controls.b().a(new et(et.a.c, "/dolly-screenshot " + capture.id));
        message.a(controls);
        eu open = new fa(" §7[Abrir]"); open.b().a(new et(et.a.b, capture.file.getAbsolutePath())); message.a(open);
        return message;
    }
    public static boolean command(String message) {
        if (!message.startsWith("/dolly-screenshot ")) return false;
        String id = message.substring("/dolly-screenshot ".length());
        for (Capture capture : captures) if (capture.id.equals(id)) {
            ave.A().a(new DollyScreenshotScreen(ave.A().m, capture)); return true;
        }
        status = "Essa captura saiu do histórico da sessão. Abra a pasta de screenshots.";
        if (ave.A().q != null) ave.A().q.d().a(new fa(status));
        return true;
    }
    public static void render() {
        if (preview == null) return;
        if (!DollySettings.config.screenshotPreview || System.nanoTime() / 1000000L >= previewUntil) { preview.close(); preview = null; return; }
        avr resolution = new avr(ave.A());
        preview.draw(resolution.a() - 132, 8, 124, 70);
    }
    public static void clear() {
        if (preview != null) preview.close();
        preview = null; captures.clear();
    }
    public static void openFolder(Capture capture) {
        if (capture != null) { action(capture, "folder"); return; }
        final File folder = new File(ave.A().v, "screenshots");
        if (!DollyScreenshots.work(() -> {
            String result;
            try { Files.createDirectories(folder.toPath()); Desktop.getDesktop().open(folder); result = "Pasta de screenshots aberta."; }
            catch (Exception e) { result = "Não foi possível abrir a pasta: " + e.getClass().getSimpleName(); }
            final String feedback = result; ave.A().a((Runnable) () -> status = feedback);
        })) status = "Capturas pendentes; tente novamente.";
    }
    public static void action(Capture capture, String action) {
        if (capture.busy) return;
        capture.busy = true; status = "Processando…";
        if (!DollyScreenshots.work(() -> {
            String result;
            try {
                if (action.equals("copy")) {
                    BufferedImage image = ImageIO.read(capture.file);
                    if (image == null) throw new IOException("Imagem indisponível");
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageSelection(image), null);
                    result = "Imagem copiada.";
                } else if (action.equals("favorite")) {
                    capture.favorite = dolly.screenshots.ScreenshotFiles.favorite(capture.file.toPath());
                    result = "Cópia salva em favorite_screenshots.";
                } else if (action.equals("delete")) {
                    Files.delete(capture.file.toPath()); capture.deleted = true; result = "Captura excluída.";
                } else {
                    File target = action.equals("folder") ? capture.file.getParentFile() : capture.file;
                    Desktop.getDesktop().open(target); result = "Arquivo ou pasta aberto.";
                }
            } catch (Exception e) { result = "Falha: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()); }
            final String feedback = result;
            ave.A().a((Runnable) () -> { capture.busy = false; status = feedback; });
        })) { capture.busy = false; status = "Capturas pendentes; tente novamente."; }
    }
    static final class ImageSelection implements Transferable {
        private final Image image;
        ImageSelection(Image image) { this.image = image; }
        public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[] {DataFlavor.imageFlavor}; }
        public boolean isDataFlavorSupported(DataFlavor flavor) { return DataFlavor.imageFlavor.equals(flavor); }
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
            return image;
        }
    }
}
