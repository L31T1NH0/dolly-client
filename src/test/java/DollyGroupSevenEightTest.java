import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarFile;
import javax.imageio.ImageIO;
import org.lwjgl.input.Keyboard;

public final class DollyGroupSevenEightTest {
    public static class Minecraft extends DollyRuntimeTest.Minecraft {
        int quits;
        bde server;
        public void m() { quits++; }
        public boolean E() { return false; }
        public boolean al() { return false; }
        public bde D() { return server; }
        public com.google.common.util.concurrent.ListenableFuture a(Runnable task) { task.run(); return com.google.common.util.concurrent.Futures.immediateFuture(null); }
    }
    private static void set(Class<?> type, Object object, String name, Object value) throws Exception {
        Field f = type.getDeclaredField(name); f.setAccessible(true); f.set(object, value);
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path directory = Files.createTempDirectory("dolly-screens-menus-");
        try {
            Minecraft mc = DollyGroupOneTest.allocate(Minecraft.class);
            set(ave.class, null, "S", mc); mc.t = new DollyRuntimeTest.Settings();
            mc.k = DollyGroupOneTest.allocate(DollyRuntimeTest.Font.class);
            set(Config.class, null, "minecraft", mc); set(Config.class, null, "gameSettings", mc.t);
            DollySettings.config = new dolly.config.DollyConfig(directory.resolve("dolly.properties"));
            captures(mc, directory); sliders(mc); navigation(mc); preferences(directory); hooks(args[0]);
            DollyScreenshots.shutdownScreenshots();
            System.out.println("Groups 7/8 passed: independent screenshot controls, clipboard flavors, lossless favorite copies/collisions, confirmed deletion, bounded session history, numeric dragging/keys/ranges, LAN modes, quit/cancel and reconnect choices, preferences and final-JAR hooks.");
        } finally {
            try (java.util.stream.Stream<Path> files = Files.walk(directory)) {
                for (Path file : (Iterable<Path>) files.sorted(Comparator.reverseOrder())::iterator) Files.delete(file);
            }
        }
    }
    private static void captures(Minecraft mc, Path directory) throws Exception {
        BufferedImage image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        BufferedImage thumbnail = DollyScreenshotManager.thumbnail(image);
        assert thumbnail.getWidth() == 480 && thumbnail.getHeight() == 270;
        File first = DollyScreenshots.saveImage(directory.toFile(), image);
        byte[] original = Files.readAllBytes(first.toPath());
        Path favorite = dolly.screenshots.ScreenshotFiles.favorite(first.toPath());
        Path duplicate = dolly.screenshots.ScreenshotFiles.favorite(first.toPath());
        assert !favorite.equals(duplicate) && Arrays.equals(original, Files.readAllBytes(favorite));
        assert Arrays.equals(original, Files.readAllBytes(first.toPath())) : "Favorite must preserve original";
        DollyScreenshots.nativeComplete(new fa("native"), first, image);
        DollyScreenshotManager.Capture one = DollyScreenshotManager.captures.get(0);
        File second = DollyScreenshots.saveImage(directory.toFile(), image);
        DollyScreenshotManager.register(second, thumbnail);
        DollyScreenshotManager.Capture two = DollyScreenshotManager.captures.get(1);
        assert !one.id.equals(two.id) && one.file.equals(first) && two.file.equals(second);
        eu feedback = DollyScreenshotManager.feedback(one);
        boolean action = false;
        for (eu component : feedback) if (component.b().h() != null && component.b().h().b().contains(one.id)) action = true;
        assert action : "Each capture needs an independent local manager action";
        assert !DollyChatSending.history(false, "/dolly-screenshot " + one.id);
        assert DollyScreenshotManager.command("/dolly-screenshot " + one.id) && mc.m instanceof DollyScreenshotScreen;
        DollyScreenshotScreen screen = (DollyScreenshotScreen) mc.m;
        screen.l = 320; screen.m = 180; screen.q = mc.k; screen.b();
        screen.a(button(screen, 5)); assert mc.m instanceof DollyChoiceScreen && first.isFile();
        DollyChoiceScreen confirm = (DollyChoiceScreen) mc.m; confirm.l = 320; confirm.m = 180; confirm.b();
        confirm.a(button(confirm, 1)); assert first.isFile() : "Cancelling must retain screenshot";
        DollyScreenshotManager.action(one, "delete");
        CountDownLatch done = new CountDownLatch(1); assert DollyScreenshots.work(done::countDown); assert done.await(5, TimeUnit.SECONDS);
        assert !first.exists() && one.deleted && second.isFile() && Files.isRegularFile(favorite);
        DollyScreenshotManager.ImageSelection selection = new DollyScreenshotManager.ImageSelection(image);
        assert selection.isDataFlavorSupported(DataFlavor.imageFlavor) && selection.getTransferData(DataFlavor.imageFlavor) == image;
        try { selection.getTransferData(DataFlavor.stringFlavor); throw new AssertionError("Unsupported clipboard flavor accepted"); }
        catch (UnsupportedFlavorException expected) { }
        for (int i = 0; i < 40; i++) DollyScreenshotManager.register(second, thumbnail);
        assert DollyScreenshotManager.captures.size() == 32;
        Path blocked = directory.resolve("blocked"); Files.createDirectories(blocked.resolve("screenshots"));
        Path source = blocked.resolve("screenshots/test.png"); Files.write(source, original); Files.write(blocked.resolve("favorite_screenshots"), new byte[0]);
        try { dolly.screenshots.ScreenshotFiles.favorite(source); throw new AssertionError("Expected destination failure"); }
        catch (IOException expected) { assert Arrays.equals(original, Files.readAllBytes(source)); }
    }
    private static avs button(axu screen, int id) { for (avs b : screen.n) if (b.k == id) return b; throw new AssertionError("Missing button " + id); }
    private static void sliders(Minecraft mc) {
        double[] current = {50}; int[] saves = {0};
        DollySlider slider = new DollySlider(1, 10, 10, 108, 0, 100, 1, () -> current[0], n -> current[0] = n, () -> "Value: " + current[0], () -> saves[0]++);
        assert slider.c(mc, 14, 15); assert current[0] == 0 && saves[0] == 0;
        slider.a(14, 15); assert saves[0] == 1;
        assert slider.key(Keyboard.KEY_END) && current[0] == 100;
        assert slider.key(Keyboard.KEY_LEFT) && current[0] == 99;
        assert slider.key(Keyboard.KEY_HOME) && current[0] == 0;
        slider.key(Keyboard.KEY_LEFT); assert current[0] == 0;
        assert slider.c(mc, 117, 15) && current[0] == 100;
        slider.a(117, 15);
        for (String key : new String[] {"zoomFactor", "timeOfDay", "hudCacheFps", "fireHeight", "backgroundFps"}) assert DollyNumeric.range(key) != null;
        assert DollyNumeric.range("zoomEasing") == null && DollyNumeric.range("itemStitching") == null;
    }
    private static void navigation(Minecraft mc) throws Exception {
        axp pause = new axp(); pause.n.add(new avs(7, 0, 0, "LAN"));
        DollySettings.config.lanButton = 2; DollyNavigation.pause(pause);
        assert button(pause, 7).j.equals("Servidores") && button(pause, 7).l;
        DollySettings.config.lanButton = 1; DollyNavigation.pause(pause); assert pause.n.isEmpty();
        pause.n.add(new avs(7, 0, 0, "LAN")); DollySettings.config.lanButton = 0; DollyNavigation.pause(pause);
        assert button(pause, 7).j.equals("LAN");
        aya main = DollyGroupOneTest.allocate(aya.class);
        assert DollyNavigation.quit(main, new avs(4, 0, 0, "Quit"));
        DollyChoiceScreen confirm = (DollyChoiceScreen) mc.m; confirm.l = 320; confirm.m = 180; confirm.b();
        confirm.a(button(confirm, 1)); assert mc.quits == 0 && mc.m == main;
        DollyNavigation.quit(main, new avs(4, 0, 0, "Quit")); confirm = (DollyChoiceScreen) mc.m; confirm.a(new avs(0, 0, 0, "Confirm")); assert mc.quits == 1;
        DollySettings.config.confirmQuit = false; assert !DollyNavigation.quit(main, new avs(4, 0, 0, "Quit"));
        mc.server = new bde("Example", "example.invalid:25565", false); DollyNavigation.remember(mc.server);
        mc.server.b = "changed.invalid"; assert DollyNavigation.server().b.equals("example.invalid:25565");
        axu disconnected = new axu() {}; disconnected.l = 320; disconnected.m = 180; disconnected.n.add(new avs(0, 60, 140, "Back"));
        DollyNavigation.disconnected(disconnected); assert button(disconnected, 18990).i + 20 <= 180;
        mc.f = DollyGroupOneTest.allocate(bdb.class);
        avs leave = new avs(1, 0, 0, "Leave");
        assert DollyNavigation.pauseClick(pause, leave); confirm = (DollyChoiceScreen) mc.m; confirm.l = 320; confirm.m = 180; confirm.b();
        confirm.a(button(confirm, 2)); assert mc.f != null : "Cancellation must keep the world";
    }
    private static void preferences(Path folder) throws Exception {
        dolly.config.DollyConfig c = new dolly.config.DollyConfig(folder.resolve("prefs.properties"));
        assert c.asyncScreenshot && c.screenshotPreview && !c.screenshotAutoCopy && c.screenshotCompact;
        assert c.lanButton == 2 && c.reconnectMenu && c.confirmQuit;
        c.confirmQuit = false; c.reconnectMenu = false; c.lanButton = 1; c.screenshotAutoCopy = true; c.save();
        dolly.config.DollyConfig loaded = dolly.config.DollyConfig.load(folder.resolve("prefs.properties"));
        assert !loaded.confirmQuit && !loaded.reconnectMenu && loaded.lanButton == 1 && loaded.screenshotAutoCopy;
        c.lanButton = 3; try { c.save(); throw new AssertionError("Invalid LAN mode accepted"); } catch (IOException expected) { }
        assert loaded.reset().lanButton == 2;
    }
    private static void hooks(String path) throws Exception {
        try (JarFile jar = new JarFile(path)) {
            String[][] hooks = {{"avj", "DollyScreenshots", "nativeComplete"}, {"axp", "DollyNavigation", "pauseClick"},
                {"aya", "DollyNavigation", "quit"}, {"awz", "DollyNavigation", "remember"}, {"axh", "DollyNavigation", "disconnectedClick"}};
            for (String[] hook : hooks) assert DollyGroupOneTest.calls(DollyGroupOneTest.read(jar, hook[0]), hook[1], hook[2]) == 1 : Arrays.toString(hook);
        }
    }
}
