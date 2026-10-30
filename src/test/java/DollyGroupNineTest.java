import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;

public final class DollyGroupNineTest {
    public static class Minecraft extends DollyRuntimeTest.Minecraft { public boolean E() { return false; } }
    public static class Connection extends bcy {
        List<ff> packets;
        Connection() { super(null, null, null, null); }
        public void a(ff packet) { packets.add(packet); }
    }
    static void set(Class<?> type, Object owner, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(owner, value);
    }
    public static void main(String[] args) throws Exception {
        DollyMaintenancePolicyTest.run();
        Path folder = Files.createTempDirectory("dolly-group9-");
        try {
            Minecraft mc = DollyGroupOneTest.allocate(Minecraft.class); set(ave.class, null, "S", mc);
            set(ave.class, mc, "v", folder.toFile()); mc.t = new DollyRuntimeTest.Settings();
            set(Config.class, null, "minecraft", mc); set(Config.class, null, "gameSettings", mc.t);
            DollySettings.config = new dolly.config.DollyConfig(folder.resolve("dolly.properties"));
            assert DollyDebug.simplify("120/42 fps, C: 10, E: 2+1, U: 0").equals("120 fps, C: 10, E: 2+1, U: 0");
            assert DollyDebug.suffix("/42").isEmpty() && DollyDebug.suffix("native").equals("native");
            DollySettings.config.simpleDebugFps = false; assert DollyDebug.simplify("120/42 fps").equals("120/42 fps");
            assert DollyDebug.suffix("/42").equals("/42");
            bew player = DollyGroupOneTest.allocate(bew.class); set(pr.class, player, "g", new HashMap<Integer, pf>());
            player.bH = .7f; player.bI = .6f; player.ak = true;
            player.l(1); assert player.bH == .7f && player.bI == .6f;
            Map effects = (Map) DollyGroupOneTest.field(pr.class, player, "g"); effects.put(pe.k.H, new pf(pe.k.H, 100));
            player.l(pe.k.H); assert player.bH == 0 && player.bI == 0 && effects.isEmpty() && player.ak;
            DollySettings.config.nauseaFix = false; player.bH = .4f; player.bI = .3f; player.l(pe.k.H);
            assert player.bH == .4f && player.bI == .3f;
            Connection client = DollyGroupOneTest.allocate(Connection.class); client.packets = new ArrayList<ff>();
            client.a(new hd("level://../secret/resources.zip", "hash"));
            client.a(new hd("file:///etc/passwd", "hash"));
            assert client.packets.size() == 2;
            for (ff packet : client.packets) assert DollyGroupOneTest.field(iu.class, packet, "b") == iu.a.c : "Blocked packs must report failed download";
            assert DollyResourcePacks.url(new hd("level://My%20World/resources.zip", "hash")).equals("level://My World/resources.zip");
            assert DollyDebug.voidBox(); DollySettings.config.voidBoxFix = false; assert !DollyDebug.voidBox();
            DollyDebug.graph(null); // Hidden debug overlay must return without GL access.
            DollySettings.config.lowAnimationTick = true; assert DollyParticles.ambientSamples() == 100;
            DollySettings.config.lowAnimationTick = false; assert DollyParticles.ambientSamples() == 1000;
            DollySettings.config.logRetentionDays = 7; DollySettings.config.cleanOldLogs = false; DollySettings.config.save();
            dolly.config.DollyConfig loaded = dolly.config.DollyConfig.load(folder.resolve("dolly.properties"));
            assert loaded.logRetentionDays == 7 && !loaded.cleanOldLogs && !loaded.nauseaFix;
            loaded.logRetentionDays = 0; try { loaded.save(); throw new AssertionError("Invalid retention accepted"); } catch (java.io.IOException expected) { }
            dolly.config.DollyConfig defaults = DollySettings.config.reset();
            assert defaults.logRetentionDays == 30 && defaults.cleanOldLogs && defaults.vanillaMetrics && defaults.simpleDebugFps && defaults.nauseaFix && defaults.voidBoxFix;
            hooks(args[0]);
            System.out.println("Group 9 passed: real blocked-pack handler/status, traversal/symlink policies, dated-log retention and current-log protection, native potion removal, frame ring/scaling, FPS switches, existing ambient reduction, preferences and final-JAR hooks.");
        } finally { try (java.util.stream.Stream<Path> paths = Files.walk(folder)) {
            for (Path file : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(file);
        } }
    }
    static void hooks(String path) throws Exception {
        try (JarFile jar = new JarFile(path)) {
            String[][] hooks = {{"bcy", "DollyResourcePacks", "reject"}, {"bcy", "DollyResourcePacks", "url"}, {"pr", "DollyDebug", "nausea"},
                {"bfr", "DollyDebug", "voidBox"}, {"avv", "DollyDebug", "suffix"}, {"Config", "DollyDebug", "simplify"},
                {"bfk", "DollyDebug", "graph"}, {"ave", "DollyDebug", "frame"}};
            for (String[] hook : hooks) assert DollyGroupOneTest.calls(DollyGroupOneTest.read(jar, hook[0]), hook[1], hook[2]) == 1 : Arrays.toString(hook);
        }
    }
}
