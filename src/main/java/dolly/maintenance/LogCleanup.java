package dolly.maintenance;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/** Only dated Minecraft log archives in this game's own logs directory are eligible. */
public final class LogCleanup {
    public static int prune(Path gameDirectory, int days, long now) throws IOException {
        if (days < 1 || days > 3650) throw new IllegalArgumentException("Invalid retention period");
        Path folder = gameDirectory.resolve("logs");
        if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) return 0;
        long cutoff = now - days * 86400000L;
        int removed = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder)) {
            for (Path file : files) {
                if (!file.getFileName().toString().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]+\\.log(?:\\.gz)?")) continue;
                try {
                    BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    if (attributes.isRegularFile() && attributes.lastModifiedTime().toMillis() < cutoff && Files.deleteIfExists(file)) removed++;
                } catch (NoSuchFileException changed) { }
            }
        }
        return removed;
    }
}
