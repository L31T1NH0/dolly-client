import dolly.security.ResourcePackPolicy;
import dolly.maintenance.LogCleanup;
import dolly.debug.FrameHistory;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;

public final class DollyMaintenancePolicyTest {
    static void run() throws Exception {
        Path root = Files.createTempDirectory("dolly-maintenance-");
        try { packs(root); logs(root); frames(); }
        finally { try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            for (Path file : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(file);
        } }
    }
    private static void packs(Path root) throws Exception {
        assert ResourcePackPolicy.validate("https://example.com/pack?signature=abc", null, false) != null;
        assert ResourcePackPolicy.validate("http://example.com/pack.zip", null, false) != null;
        assert ResourcePackPolicy.validate("level://World/resources.zip", null, false) == null : "Remote level requests must not touch the filesystem";
        String[] bad = {"file:///etc/passwd", "ftp://example.com/a.zip", "http://", "http://user:password@example.com/p", "https://example.com/p#fragment", "https://bad host/p", "level://../resources.zip", "level://%2e%2e/resources.zip", "level://%252e%252e/resources.zip", "level:///tmp/resources.zip", "level://World/../resources.zip", "level://World%5c..%5cprivate/resources.zip", "level://World/level.dat", "level://World/resources.zip?x", "level://World/resources.zip%00", "LEVEL://World/resources.zip"};
        for (String value : bad) assert ResourcePackPolicy.validate(value, root, true) == null : value;
        Path world = root.resolve("saves/My World"); Files.createDirectories(world); Files.write(world.resolve("resources.zip"), new byte[] {1});
        assert ResourcePackPolicy.validate("level://My%20World/resources.zip", root, true).equals("level://My World/resources.zip");
        assert ResourcePackPolicy.validate("level://My%20World/resources.zip", root, false) == null;
        Path outside = root.resolve("private"); Files.createDirectory(outside); Files.write(outside.resolve("resources.zip"), new byte[] {2});
        Files.createSymbolicLink(root.resolve("saves/Escape"), outside);
        assert ResourcePackPolicy.validate("level://Escape/resources.zip", root, true) == null : "Symlink escaped saves";
    }
    private static void logs(Path root) throws Exception {
        Path logs = root.resolve("logs"); Files.createDirectory(logs);
        long now = System.currentTimeMillis(), day = 86400000L;
        String[] names = {"2026-01-01-1.log.gz", "2026-01-01-2.log", "latest.log", "debug.log", "2026-01-01-3.log.gz", "other.txt", "2026-01-01-4.log.gz"};
        long[] ages = {60, 60, 60, 60, 1, 60, 30};
        for (int i = 0; i < names.length; i++) { Path file = logs.resolve(names[i]); Files.write(file, new byte[] {1}); Files.setLastModifiedTime(file, FileTime.fromMillis(now - ages[i] * day)); }
        Path outside = root.resolve("external.log.gz"); Files.write(outside, new byte[] {9}); Files.setLastModifiedTime(outside, FileTime.fromMillis(now - 90 * day));
        Files.createSymbolicLink(logs.resolve("2026-01-01-5.log.gz"), outside);
        Files.createDirectory(logs.resolve("2026-01-01-6.log.gz"));
        assert LogCleanup.prune(root, 30, now) == 2;
        for (int i = 2; i < names.length; i++) assert Files.exists(logs.resolve(names[i])) : names[i];
        assert Files.exists(outside) && Files.isSymbolicLink(logs.resolve("2026-01-01-5.log.gz"));
        assert LogCleanup.prune(root, 30, now) == 0;
        try { LogCleanup.prune(root, 0, now); throw new AssertionError("Invalid age accepted"); } catch (IllegalArgumentException expected) { }
        Path linkedRoot = root.resolve("linked"); Files.createDirectory(linkedRoot); Files.createSymbolicLink(linkedRoot.resolve("logs"), logs);
        assert LogCleanup.prune(linkedRoot, 1, now) == 0 : "Must not clean an external logs directory through a symlink";
    }
    private static void frames() {
        FrameHistory history = new FrameHistory(); Object world = new Object();
        history.frame(world, 100); history.frame(world, 200); assert history.size() == 1 && history.get(0) == 100;
        history.frame(world, 150); assert history.size() == 1 : "Backwards time must not add a sample";
        history.frame(new Object(), 300); assert history.size() == 0;
        for (int i = 1; i <= 300; i++) history.add(i);
        assert history.size() == 240 && history.get(0) == 61 && history.get(239) == 300;
        history.frame(null, 400); assert history.size() == 0;
        assert FrameHistory.height(16_666_667L, true) == 30;
        assert FrameHistory.height(33_333_334L, true) == 60;
        assert FrameHistory.height(50_000_000L, false) == 60;
        assert FrameHistory.height(Long.MAX_VALUE, true) == 60;
        assert FrameHistory.color(0) == 0xFF00FF00 && FrameHistory.color(60) == 0xFFFF0000;
    }
}
