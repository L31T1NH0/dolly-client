package dolly.screenshots;
import java.io.IOException;
import java.nio.file.*;

public final class ScreenshotFiles {
    /** Copy first; an error or name collision must never remove the original. */
    public static Path favorite(Path source) throws IOException {
        Path folder = source.toAbsolutePath().getParent().getParent().resolve("favorite_screenshots");
        Files.createDirectories(folder);
        String name = source.getFileName().toString(), stem = name.endsWith(".png") ? name.substring(0, name.length() - 4) : name;
        for (int suffix = 1; ; suffix++) {
            Path target = folder.resolve(stem + (suffix == 1 ? "" : "_" + suffix) + ".png");
            try { return Files.copy(source, target); }
            catch (FileAlreadyExistsException exists) { }
        }
    }
}
