package dolly.security;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

/** Remote servers never get to test for local files through level:// requests. */
public final class ResourcePackPolicy {
    public static String validate(String value, Path gameDirectory, boolean localWorld) {
        if (value == null || value.length() > 32767) return null;
        try {
            if (value.startsWith("level://")) {
                if (!localWorld) return null; // Reject before any filesystem operation.
                String relative = URLDecoder.decode(value.substring(8), StandardCharsets.UTF_8.name());
                if (relative.indexOf('\\') >= 0 || relative.indexOf('%') >= 0 || relative.indexOf('\0') >= 0) return null;
                String[] pieces = relative.split("/", -1);
                if (pieces.length != 2 || pieces[0].isEmpty() || pieces[0].equals(".") || pieces[0].equals("..")
                        || pieces[0].indexOf(':') >= 0 || !pieces[1].equals("resources.zip")) return null;
                Path root = gameDirectory.resolve("saves").toRealPath();
                Path file = root.resolve(relative).toRealPath();
                if (!file.startsWith(root) || !file.getFileName().toString().equals("resources.zip")
                        || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) return null;
                return "level://" + relative;
            }
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null) return null;
            return value;
        } catch (IOException | IllegalArgumentException | URISyntaxException e) { return null; }
    }
}
