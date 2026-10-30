import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.*;
import java.util.*;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

/** Behavioral policies and real memory cleanup, plus integration in the final JAR. */
public final class DollyGroupOneTest {
    static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field f = unsafe.getDeclaredField("theUnsafe"); f.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(f.get(null), type));
    }
    static Object field(Class<?> owner, Object object, String name) throws Exception {
        Field f = owner.getDeclaredField(name); f.setAccessible(true); return f.get(object);
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        icons(); particles(); fontState(); textAdmission(); colorQueryBuffers(); memory();
        assert DollySettings.config.hudCacheFps == 24;
        assert !DollyHudCache.due(41_666_665L, 0, true, 24);
        assert DollyHudCache.due(41_666_667L, 0, true, 24);
        if (args.length > 0) hooks(args[0]);
        System.out.println("Group 1 passed: icon alpha/size, native particle fallback, font state/styles, 24 Hz HUD, memory cleanup and final-JAR hooks.");
    }
    static void icons() {
        assert DollyPackIcons.resize(null) == null;
        BufferedImage small = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        assert DollyPackIcons.resize(small) == small;
        BufferedImage large = new BufferedImage(256, 128, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 128; y++) for (int x = 0; x < 256; x++) large.setRGB(x, y, 0x80402010);
        BufferedImage scaled = DollyPackIcons.resize(large);
        assert scaled.getWidth() == 64 && scaled.getHeight() == 64;
        assert (scaled.getRGB(20, 20) >>> 24) == 128 : "Icon alpha was lost";
        assert large.getWidth() == 256 : "Source image was changed";
        DollySettings.config.smallPackIcons = false;
        assert DollyPackIcons.resize(large) == large;
        DollySettings.config.smallPackIcons = true;
    }
    public static class Particle extends beb {
        private Particle() { super(null, 0, 0, 0); }
        public int b(float partialTicks) { return 12345; }
    }
    static void particles() throws Exception {
        Particle p = allocate(Particle.class);
        assert DollyParticles.brightness(p, .5f) == 15728880;
        DollySettings.config.staticParticleColor = false;
        assert DollyParticles.brightness(p, .5f) == 12345 : "Native lighting fallback was lost";
        DollySettings.config.staticParticleColor = true;
        assert DollyParticles.ambientSamples() == 100;
        DollySettings.config.lowAnimationTick = false;
        assert DollyParticles.ambientSamples() == 1000;
        DollySettings.config.lowAnimationTick = true;
    }
    static void fontState() throws Exception {
        DollyFontAccess font = (DollyFontAccess) allocate(avn.class);
        float[] state = {2.5f, 8.5f, 1, .5f, .25f, .75f, 0xABCDEF, 0, 1, 0, 1, 0, 1, 0, 1.5f};
        font.dolly$fontState(state);
        assert Arrays.equals(state, font.dolly$fontState()) : "Font cursor/color/styles must round-trip exactly";
        float[] reused = new float[15];
        font.dolly$readFontState(reused);
        assert Arrays.equals(state, reused) : "Reusable font snapshot lost state";
        reused[0] = -12; font.dolly$fontState(reused); font.dolly$readFontState(reused);
        assert reused[0] == -12 && reused[14] == state[14];
        assert DollyRenderedText.cacheable("§lBold §rnormal");
        assert !DollyRenderedText.cacheable("§Kanimated");
        assert !DollyRenderedText.cacheable(null) && !DollyRenderedText.cacheable("");
        char[] longText = new char[257]; Arrays.fill(longText, 'a');
        assert !DollyRenderedText.cacheable(new String(longText));
    }
    static void textAdmission() throws Exception {
        DollyRenderedText.Admission admission = new DollyRenderedText.Admission();
        float[] state = new float[15];
        DollyRenderedText.Key lookup = new DollyRenderedText.Key("one", false, state);
        assert !admission.admit(lookup) : "First occurrence must keep native drawing";
        lookup.set("two", false, state);
        assert !admission.admit(lookup);
        lookup.set("one", false, state);
        assert admission.admit(lookup) : "Mutable lookup corrupted the retained first occurrence";
        assert !admission.admit(lookup) : "Admitted keys must leave the pending set";
        lookup.set("one", true, state);
        assert !admission.admit(lookup) : "Shadow and main text have separate state";
        state[2] = .5f; lookup.set("two", false, state);
        assert !admission.admit(lookup) : "Colors/styles must not share admission";
        for (int i = 0; i < 2000; i++) {
            lookup.set("unique-" + i, false, state);
            assert !admission.admit(lookup);
        }
        assert ((Set<?>) field(DollyRenderedText.Admission.class, admission, "pending")).size() == 512;
        lookup.set("unique-0", false, state); assert !admission.admit(lookup) : "Old entries must be evicted";
        lookup.set("unique-1999", false, state); assert admission.admit(lookup);
        assert !new DollyRenderedText.Admission().admit(lookup) : "A new font/resource cache starts cold";
    }
    static void colorQueryBuffers() throws Exception {
        // Exercise the real LWJGL validation without requiring an OpenGL context.
        for (Object[] target : new Object[][] {{DollyRenderedText.class, "color"}, {DollyVisuals.class, "colorBuffer"}}) {
            java.nio.FloatBuffer buffer = (java.nio.FloatBuffer) field((Class<?>) target[0], null, (String) target[1]);
            buffer.clear();
            assert buffer.isDirect();
            org.lwjgl.BufferChecks.checkBuffer(buffer, 16);
        }
        try {
            org.lwjgl.BufferChecks.checkBuffer(org.lwjgl.BufferUtils.createFloatBuffer(4), 16);
            throw new AssertionError("Expected the reported LWJGL buffer validation failure");
        } catch (IllegalArgumentException expected) { }
    }
    static void memory() throws Exception {
        ow source = allocate(ow.class);
        ack.a(new zx[0], source);
        Object damage = field(ack.class, null, "b");
        assert field(damage.getClass(), damage, "b") == null;
        try { ack.a((zx[]) null, source); throw new AssertionError("Expected invalid inventory"); }
        catch (NullPointerException expected) { }
        assert field(damage.getClass(), damage, "b") == null : "Exceptional exit retained damage source";
        pk target = allocate(vj.class);
        ack.a((pr) null, target); ack.b((pr) null, target);
        for (String name : new String[] {"d", "e"}) {
            Object holder = field(ack.class, null, name);
            assert field(holder.getClass(), holder, "a") == null;
            assert field(holder.getClass(), holder, "b") == null : "Enchantment iterator retained entity";
        }
        assert field(ajd.class, null, "b") instanceof WeakHashMap : "Redstone must not retain unloaded worlds";
        Class<?> nodeType = Class.forName("asz");
        asw node = (asw) allocate(nodeType);
        Field access = asw.class.getDeclaredField("a"); access.setAccessible(true);
        access.set(node, allocate(bdb.class));
        node.a();
        assert access.get(node) == null : "Finished path search retained world";
    }
    static ClassNode read(JarFile jar, String name) throws Exception {
        ClassNode cls = new ClassNode();
        try (InputStream in = jar.getInputStream(jar.getJarEntry(name + ".class"))) { new ClassReader(in).accept(cls, 0); }
        return cls;
    }
    static int calls(ClassNode cls, String owner, String name) {
        int count = 0;
        for (MethodNode m : cls.methods) for (AbstractInsnNode n : m.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.owner.equals(owner) && c.name.equals(name)) count++;
        }
        return count;
    }
    static void hooks(String path) throws Exception {
        try (JarFile jar = new JarFile(path)) {
            assert calls(read(jar, "avn"), "DollyRenderedText", "draw") == 1;
            assert calls(read(jar, "bct"), "DollyModelBatch", "begin") == 1;
            assert calls(read(jar, "bct"), "DollyModelBatch", "end") == 1;
            assert calls(read(jar, "bbs"), "DollyModelBatch", "quadBegin") == 2;
            assert calls(read(jar, "bbs"), "DollyModelBatch", "quadEnd") == 1;
            assert calls(read(jar, "beb"), "DollyParticles", "brightness") == 1;
            assert calls(read(jar, "bmx"), "DollyPackIcons", "resize") == 1;
            assert calls(read(jar, "bdb"), "DollyParticles", "ambientSamples") == 1;
            // M5 already removes deleted textures; keep that fix instead of applying it twice.
            assert calls(read(jar, "bmj"), "java/util/Map", "remove") > 0;
        }
    }
}
