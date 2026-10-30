import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.IntBuffer;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Read pixels on the render thread; each queued PNG owns its pixels and dimensions. */
public final class DollyScreenshots {
    private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger();
    private static final ThreadPoolExecutor WRITER = newWriter();

    static ThreadPoolExecutor newWriter() { return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(2), runnable -> {
                Thread thread = new Thread(runnable, "Dolly screenshots"); thread.setDaemon(true); return thread;
            }); }

    static boolean work(Runnable task) {
        try { WRITER.execute(task); return true; }
        catch (RejectedExecutionException busy) { return false; }
    }

    public static eu nativeComplete(eu original, File file, BufferedImage image) {
        try { return DollyScreenshotManager.register(file, DollyScreenshotManager.thumbnail(image)); }
        catch (RuntimeException error) { LOG.warn("Could not prepare screenshot controls", error); return original; }
    }

    public static void shutdownScreenshots() {
        DollyScreenshotManager.clear();
        if (!drain(WRITER, 5000L)) LOG.warn("Screenshot shutdown timed out; {} capture(s) still pending",
                WRITER.getActiveCount() + WRITER.getQueue().size());
    }

    static boolean drain(ThreadPoolExecutor writer, long timeoutMillis) {
        writer.shutdown();
        try { return writer.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return false; }
    }

    public static eu capture(File directory, int width, int height, bfw framebuffer) {
        if (!DollySettings.config.asyncScreenshot || Config.getScreenshotSize() > 1)
            return avj.a(directory, width, height, framebuffer);
        if (WRITER.isShutdown()) return new fa("Cliente encerrando; captura indisponível.");
        if (WRITER.getQueue().remainingCapacity() == 0) return new fa("Capturas pendentes; aguarde a gravação.");
        final ave mc = ave.A();
        try {
            boolean buffered = bqs.i();
            final int textureWidth = buffered ? framebuffer.a : width;
            final int textureHeight = buffered ? framebuffer.b : height;
            final int imageWidth = buffered ? framebuffer.c : width;
            final int imageHeight = buffered ? framebuffer.d : height;
            int length = Math.multiplyExact(textureWidth, textureHeight);
            if (length <= 0 || imageWidth <= 0 || imageHeight <= 0 || imageWidth > textureWidth || imageHeight > textureHeight)
                throw new IOException("Dimensões inválidas da captura");
            IntBuffer buffer = BufferUtils.createIntBuffer(length);
            int pack = GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT), unpack = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
            int previousTexture = bfl.getBoundTexture();
            try {
                GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
                if (buffered) {
                    bfl.i(framebuffer.g);
                    GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, 32993, 33639, buffer);
                } else GL11.glReadPixels(0, 0, width, height, 32993, 33639, buffer);
            } finally {
                GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, pack);
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, unpack);
                if (buffered) bfl.i(previousTexture);
            }
            final int[] pixels = new int[length]; buffer.get(pixels);
            WRITER.execute(() -> {
                try {
                    BufferedImage image = image(pixels, textureWidth, imageWidth, imageHeight);
                    File file = saveImage(directory, image);
                    BufferedImage thumbnail = DollyScreenshotManager.thumbnail(image); image.flush();
                    LOG.info("Screenshot saved to {}", file);
                    mc.a((Runnable) () -> {
                        eu result = DollyScreenshotManager.register(file, thumbnail);
                        if (mc.q != null) mc.q.d().a(result);
                    });
                } catch (Exception error) {
                    LOG.warn("Could not save screenshot", error);
                    mc.a((Runnable) () -> mc.q.d().a(new fb("screenshot.failure", error.getMessage())));
                }
            });
            return new fa("Salvando captura…");
        } catch (RejectedExecutionException error) {
            return new fa("Capturas pendentes; aguarde a gravação.");
        } catch (Exception error) { return new fb("screenshot.failure", error.getMessage()); }
    }

    static File save(File directory, int[] pixels, int textureWidth, int textureHeight, int width, int height) throws IOException {
        return saveImage(directory, image(pixels, textureWidth, width, height));
    }
    static BufferedImage image(int[] pixels, int textureWidth, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // OpenGL rows start at the bottom; crop framebuffer padding while flipping into the image.
        for (int row = 0; row < height; row++) image.setRGB(0, row, width, 1, pixels,
                (height - 1 - row) * textureWidth, textureWidth);
        return image;
    }
    static File saveImage(File directory, BufferedImage image) throws IOException {
        Path screenshots = directory.toPath().resolve("screenshots");
        Files.createDirectories(screenshots);
        String name = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
        Path target;
        int suffix = 1;
        while (true) {
            target = screenshots.resolve(name + (suffix == 1 ? "" : "_" + suffix) + ".png");
            try { Files.createFile(target); break; }
            catch (FileAlreadyExistsException exists) { suffix++; }
        }
        Path temporary = null;
        try {
            temporary = Files.createTempFile(screenshots, ".dolly-", ".png");
            if (!ImageIO.write(image, "png", temporary.toFile())) throw new IOException("PNG encoder unavailable");
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
            return target.toFile().getCanonicalFile();
        } catch (IOException error) { Files.deleteIfExists(target); throw error; }
        finally { if (temporary != null) Files.deleteIfExists(temporary); }
    }
}
