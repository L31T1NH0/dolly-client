import java.lang.reflect.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public final class DollyTechnicalRuntimeTest {
    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field field = unsafeType.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return type.cast(unsafeType.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }
    public static class Textures extends bmj {
        public final Set<jy> removed = new HashSet<jy>();
        Textures() { super(null); }
        public void c(jy location) { removed.add(location); }
    }
    public static void main(String[] args) throws Exception {
        bex first = allocate(bex.class), second = allocate(bex.class);
        double[] cases = {Double.NaN, 0d, -0d, 1d, 2d, 100d, Double.POSITIVE_INFINITY, -1d, Double.longBitsToDouble(0x7ff8000020000000L), Double.longBitsToDouble(0xfff8000040000000L)};
        for (boolean enabled : new boolean[] {true, false, true}) {
            DollySettings.config.optimizedPlayerUpdating = enabled;
            for (bex player : new bex[] {first, second}) for (double value : cases) for (int repeat = 0; repeat < 2; repeat++) {
                assert Float.floatToRawIntBits(DollyPlayers.speed(value, player)) == Float.floatToRawIntBits(ns.a(value));
                assert Double.doubleToRawLongBits(DollyPlayers.angle(value, player)) == Double.doubleToRawLongBits(Math.atan(value));
            }
        }
        DollyPlayers.clear();
        assert DollyResources.delay(0) == 0 && DollyResources.delay(1) == 15 && DollyResources.delay(5) == 200;
        DollySettings.config.lazyChunkLoading = 1; DollyResources.sync();
        Thread.currentThread().interrupt();
        try { DollyResources.paceChunks(); throw new AssertionError("Chunk pacing must remain interruptible"); }
        catch (InterruptedException expected) { }
        DollySettings.config.lazyChunkLoading = 0; DollyResources.sync();
        textures();
        if (args.length != 0) patched(args[0]);
        System.out.println("Technical runtime passed: exact velocity functions, pacing/interruption, safe texture selection and hooks.");
    }
    @SuppressWarnings("unchecked")
    private static void textures() throws Exception {
        Textures manager = new Textures();
        Field field = bmj.class.getDeclaredField("b"); field.setAccessible(true);
        Map<jy, bmk> map = (Map<jy, bmk>) field.get(manager);
        jy stale = new jy("test:stale"), recent = new jy("test:recent"), dynamic = new jy("test:dynamic"), alias = new jy("test:alias"), second = new jy("test:second"), emissive = new jy("test:emissive");
        map.put(stale, new bme(stale)); map.put(recent, new bme(recent));
        map.put(dynamic, allocate(blz.class));
        bme shared = new bme(alias); map.put(alias, shared); map.put(second, shared);
        bme glow = new bme(emissive); glow.isEmissive = true; map.put(emissive, glow);
        long now = 31L * 60 * 1_000_000_000L;
        for (jy location : map.keySet()) DollyTextureCleanup.record(manager, location, 0);
        DollyTextureCleanup.record(manager, recent, now);
        assert DollyTextureCleanup.cleanup(manager, now) == 1;
        assert manager.removed.equals(Collections.singleton(stale)) : "Active, aliased, dynamic or emissive texture evicted";
        manager.removed.clear(); assert DollyTextureCleanup.cleanup(manager, now) == 0;
        assert manager.removed.isEmpty() : "Cleanup must consume evicted candidates";
        assert DollyTextureCleanup.cleanup(manager, now + 31L * 60 * 1_000_000_000L) == 1;
        assert manager.removed.equals(Collections.singleton(recent)) : "Recent textures must remain tracked for the next disconnect";
        DollyTextureCleanup.clear();
    }
    private static void patched(String file) throws Exception {
        try (JarFile jar = new JarFile(file)) {
            assert calls(jar, "ave", "a", "(Lbdb;Ljava/lang/String;)V", "DollyResources", "collect") == 1;
            assert calls(jar, "bho", "d", "()Lbhn;", "DollyResources", "paceChunks") == 1;
            assert calls(jar, "bho", "c", "()Lbfg;", "DollyResources", "paceChunks") == 0;
            assert calls(jar, "bex", "m", "()V", "DollyPlayers", "speed") == 1;
            assert calls(jar, "bex", "m", "()V", "DollyPlayers", "angle") == 1;
            assert calls(jar, "bmj", "a", "(Ljy;)V", "DollyTextureCleanup", "bound") == 1;
        }
    }
    private static int calls(JarFile jar, String cls, String method, String desc, String owner, String name) throws Exception {
        ClassNode node = new ClassNode();
        try (java.io.InputStream in = jar.getInputStream(jar.getEntry(cls + ".class"))) { new ClassReader(in).accept(node, 0); }
        int count = 0;
        for (MethodNode m : node.methods) if (m.name.equals(method) && m.desc.equals(desc)) for (AbstractInsnNode n : m.instructions.toArray())
            if (n instanceof MethodInsnNode && ((MethodInsnNode) n).owner.equals(owner) && ((MethodInsnNode) n).name.equals(name)) count++;
        return count;
    }
}
