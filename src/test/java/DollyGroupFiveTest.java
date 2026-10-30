import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import org.lwjgl.input.Keyboard;

public final class DollyGroupFiveTest {
    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> u = Class.forName("sun.misc.Unsafe"); Field f = u.getDeclaredField("theUnsafe"); f.setAccessible(true);
        return type.cast(u.getMethod("allocateInstance", Class.class).invoke(f.get(null), type));
    }
    private static void set(Class<?> owner, Object target, String name, Object value) throws Exception {
        Field f = owner.getDeclaredField(name); f.setAccessible(true); f.set(target, value);
    }
    public static class Sound implements bpj {
        public jy a() { return new jy("minecraft:random.click"); }
        public boolean b() { return false; }
        public int d() { return 0; }
        public float e() { return .4f; }
        public float f() { return 1; }
        public float g() { return 0; }
        public float h() { return 0; }
        public float i() { return 0; }
        public bpj.a j() { return null; }
    }
    public static class Settings extends DollyRuntimeTest.Settings {
        float category = .5f;
        public float a(bpg sound) { return category; }
    }
    public static class Handler extends bpz implements DollySoundRegistry {
        int reloads, updates;
        boolean fail;
        Handler() { super(null, null); }
        public Set<jy> dolly$soundIds() {
            if (fail) throw new IllegalStateException("registry unavailable");
            return new HashSet<jy>(Arrays.asList(new jy("minecraft:random.click"), new jy("minecraft:mob.zombie.say")));
        }
        public void a(bni resources) { if (fail) throw new IllegalStateException("reload failed"); reloads++; }
        public void a(bpg category, float volume) { updates++; }
    }
    public static class Minecraft extends DollyRuntimeTest.Minecraft {
        Handler handler;
        bpz raw;
        bni resources;
        int fullReloads;
        boolean fail;
        public bpz W() { return handler; }
        public bni Q() { return resources; }
        public void e() {
            fullReloads++;
            if (DollyAudio.skipSoundReload()) raw.a((bni) null); else handler.a(resources);
            if (fail) throw new IllegalStateException("texture reload failed");
        }
    }
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("dolly-audio-test-");
        try {
            DollySettings.config = new dolly.config.DollyConfig(directory.resolve("dolly.properties")); DollyAudio.sync();
            Minecraft mc = allocate(Minecraft.class); mc.handler = allocate(Handler.class); mc.raw = allocate(bpz.class);
            mc.resources = (bni) Proxy.newProxyInstance(DollyGroupFiveTest.class.getClassLoader(), new Class<?>[] {bni.class},
                    (proxy, method, arguments) -> null);
            Settings settings = new Settings(); mc.t = settings;
            set(ave.class, null, "S", mc); set(Config.class, null, "minecraft", mc); set(Config.class, null, "gameSettings", settings);
            volume(settings, directory.resolve("dolly.properties")); reloads(mc); menu(mc); hooks(args[0]);
            System.out.println("Group 5 passed: real normalized volumes/category mute, saved custom events, sound/texture separation and exceptional cleanup, searchable sliders and unavailable-registry recovery.");
        } finally {
            try (java.util.stream.Stream<Path> files = Files.walk(directory)) {
                for (Path file : (Iterable<Path>) files.sorted(Comparator.reverseOrder())::iterator) Files.delete(file);
            }
        }
    }
    private static void volume(Settings settings, Path file) throws Exception {
        bpx manager = allocate(bpx.class); set(bpx.class, manager, "d", settings);
        Method normalized = bpx.class.getDeclaredMethod("a", bpj.class, bpw.class, bpg.class); normalized.setAccessible(true);
        Sound sound = new Sound(); bpw pool = new bpw(new jy("test:file"), 1, 1, false);
        assert Math.abs((Float) normalized.invoke(manager, sound, pool, bpg.d) - .2f) < .000001;
        DollyAudio.set("minecraft:random.click", 200);
        assert Math.abs((Float) normalized.invoke(manager, sound, pool, bpg.d) - .4f) < .000001;
        settings.category = 0; assert (Float) normalized.invoke(manager, sound, pool, bpg.d) == 0;
        settings.category = .5f; DollyAudio.set("minecraft:random.click", 0);
        assert (Float) normalized.invoke(manager, sound, pool, bpg.d) == 0;
        DollyAudio.set("pack:Custom.Sound", 175); DollyAudio.commit();
        dolly.config.DollyConfig saved = dolly.config.DollyConfig.load(file);
        assert saved.soundVolumes.equals(DollySettings.config.soundVolumes);
        assert DollySettings.config.soundVolumes.contains("pack:Custom.Sound=175");
        assert DollyAudio.percent("minecraft:unknown.event") == 100;
    }
    private static void reloads(Minecraft mc) {
        DollyAudio.reloadSounds(mc); assert mc.handler.reloads == 1 && mc.fullReloads == 0;
        DollyAudio.reloadTextures(mc); assert mc.fullReloads == 1 && mc.handler.reloads == 1;
        assert !DollyAudio.skipSoundReload();
        mc.fail = true; DollyAudio.reloadTextures(mc);
        assert DollyAudio.error != null && !DollyAudio.skipSoundReload() : "Failed reload retained audio suppression";
        mc.fail = false; DollySettings.config.separateResourceLoading = false;
        DollyAudio.soundKey(mc); DollyAudio.textureKey(mc);
        assert mc.handler.reloads == 3 : "Normal reloads must still reach the audio listener";
        DollySettings.config.separateResourceLoading = true;
        mc.handler.fail = true; DollyAudio.reloadSounds(mc); assert DollyAudio.error != null; mc.handler.fail = false;
    }
    private static void menu(Minecraft mc) throws Exception {
        DollyAudioScreen screen = new DollyAudioScreen(null);
        screen.l = 360; screen.m = 240; screen.q = allocate(DollyRuntimeTest.Font.class); screen.b();
        avs slider = null;
        for (avs button : screen.n) if (button.k >= 200) { slider = button; break; }
        assert slider != null;
        assert slider.c(mc, slider.h + 4, slider.i + 5); screen.a(slider); slider.a(slider.h + 4, slider.i + 5);
        screen.a((char) 0, Keyboard.KEY_RIGHT);
        assert mc.handler.updates > 0;
        Field input = DollyAudioScreen.class.getDeclaredField("search"); input.setAccessible(true);
        ((avw) input.get(screen)).a("zombie"); screen.b();
        int rows = 0; for (avs button : screen.n) if (button.k >= 200) rows++;
        assert rows == 1 : "Search should narrow registered sound events";
        mc.handler.fail = true; screen.b();
        assert DollyAudio.error != null && screen.n.size() >= 6 : "Unavailable registry must retain menu navigation";
        mc.handler.fail = false;
    }
    private static void hooks(String path) throws Exception {
        try (JarFile jar = new JarFile(path)) {
            int volume = 0, sound = 0, texture = 0, skip = 0;
            for (String name : new String[] {"bpx", "bpz", "ave"}) {
                ClassNode cls = new ClassNode(); new ClassReader(jar.getInputStream(jar.getJarEntry(name + ".class"))).accept(cls, 0);
                for (MethodNode m : cls.methods) for (AbstractInsnNode n : m.instructions.toArray()) if (n instanceof MethodInsnNode) {
                    MethodInsnNode c = (MethodInsnNode) n;
                    if (c.owner.equals("DollyAudio")) {
                        if (c.name.equals("volume")) volume++;
                        if (c.name.equals("soundKey")) sound++;
                        if (c.name.equals("textureKey")) texture++;
                        if (c.name.equals("skipSoundReload")) skip++;
                    }
                }
            }
            assert volume == 1 && sound == 1 && texture == 1 && skip == 1;
        }
    }
}
