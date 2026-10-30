import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import org.objectweb.asm.tree.*;

public final class DollyGroupSixTest {
    public static class Connection extends bcy {
        List<ff> packets;
        Connection() { super(null, null, null, null); }
        public void a(ff packet) { packets.add(packet); }
    }
    private static void set(Class<?> type, Object target, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        DollyChatFeaturesTest.run();
        DollySettings.config = new dolly.config.DollyConfig(Paths.get("/tmp/dolly-group6-unsaved.properties"));
        DollyRuntimeTest.Minecraft mc = DollyGroupOneTest.allocate(DollyRuntimeTest.Minecraft.class);
        set(ave.class, null, "S", mc);
        mc.f = DollyGroupOneTest.allocate(bdb.class); mc.h = DollyGroupOneTest.allocate(bew.class);
        Connection connection = DollyGroupOneTest.allocate(Connection.class); connection.packets = new ArrayList<ff>();
        set(bew.class, mc.h, "a", connection);
        DollySettings.config.chatSendQueue = true;
        mc.h.e("first"); mc.h.e("second");
        assert connection.packets.size() == 1 && DollyChatSending.queue.size() == 1 : "Actual patched send must respect queue";
        mc.f = null; DollyChatSending.tick(); assert DollyChatSending.queue.size() == 0;
        mc.f = DollyGroupOneTest.allocate(bdb.class); DollySettings.config.chatSendQueue = false;
        mc.h.e("native"); assert connection.packets.size() == 2;
        assert DollyChatSending.history(false, "/test");
        DollySettings.config.chatClickedHistory = false;
        assert !DollyChatSending.history(false, "/test") && DollyChatSending.history(true, "message");
        eu command = new fa("click"); command.b().a(new et(et.a.c, "/party invite name"));
        assert DollyChatActions.describe(command).contains("/party invite name");
        eu different = new fa("click"); different.b().a(new et(et.a.c, "/party kick name"));
        dolly.chat.ChatHistory<eu> history = new dolly.chat.ChatHistory<eu>();
        history.add(command, "click", 0, 0, true, 100, false, false, 1000);
        history.add(different, "click", 0, 1, true, 100, false, false, 1000);
        assert history.entries().size() == 2 : "Different actions must never merge";
        DollySettings.config.tabPing = false;
        assert !DollyTabPing.draw(10, 0, 0, null) : "Disabled ping must use native renderer";
        Path folder = Files.createTempDirectory("dolly-chat-settings-");
        try {
            dolly.config.DollyConfig config = new dolly.config.DollyConfig(folder.resolve("dolly.properties"));
            config.chatSendQueue = true; config.chatSendInterval = 2.5f; config.chatGroupSeconds = 50;
            config.chatImagePreview = false; config.tabPing = false; config.save();
            dolly.config.DollyConfig loaded = dolly.config.DollyConfig.load(folder.resolve("dolly.properties"));
            assert loaded.chatSendQueue && loaded.chatSendInterval == 2.5f && loaded.chatGroupSeconds == 50;
            assert !loaded.chatImagePreview && !loaded.tabPing;
            config.chatSendInterval = Float.NaN;
            try { config.save(); throw new AssertionError("Invalid interval accepted"); }
            catch (java.io.IOException expected) { }
            assert dolly.config.DollyConfig.load(folder.resolve("dolly.properties")).chatSendInterval == 2.5f;
            dolly.config.DollyConfig defaults = loaded.reset();
            assert !defaults.chatSendQueue && defaults.chatSendInterval == 1f && defaults.tabPing;
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.walk(folder)) {
                for (Path file : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(file);
            }
        }
        try (JarFile jar = new JarFile(args[0])) {
            assert DollyGroupOneTest.calls(DollyGroupOneTest.read(jar, "bew"), "DollyChatSending", "intercept") == 1;
            assert DollyGroupOneTest.calls(DollyGroupOneTest.read(jar, "ave"), "DollyChatSending", "tick") == 1;
            assert DollyGroupOneTest.calls(DollyGroupOneTest.read(jar, "axu"), "DollyChatSending", "history") == 1;
            assert DollyGroupOneTest.calls(DollyGroupOneTest.read(jar, "awh"), "DollyTabPing", "draw") == 1;
        }
        System.out.println("Group 6 passed: paced native sends/disconnect cancellation, bounded queue, timed nonconsecutive grouping/action preservation, image decoding/limits, preferences, action inspection and TAB hooks.");
    }
}
