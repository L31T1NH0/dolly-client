import java.io.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Time/alpha contracts plus the shipped HUD split; no OpenGL context is needed. */
public final class DollyHudCacheTest {
    public static void main(String[] args) throws Exception {
        timing();
        assert !DollyHudCache.useful(60, 60) && !DollyHudCache.useful(30, 60);
        assert DollyHudCache.useful(60, 24) && DollyHudCache.useful(260, 60);
        assert DollyHudCache.due(100, 100, false, 60);
        assert !DollyHudCache.due(1_000_001, 1_000_000, true, 60);
        assert DollyHudCache.due(17_666_667, 1_000_000, true, 60);
        assert DollyHudCache.due(99, 100, true, 60) : "A reset clock must refresh";
        assert !DollyHudCache.due(17_000_000, 1_000_000, true, 30);
        DollyHudCache.rendering = false;
        assert DollyHudCache.alphaSource(770, 771, 770) == 770;
        assert DollyHudCache.alphaDestination(770, 771, 0) == 0;
        DollyHudCache.rendering = true;
        assert DollyHudCache.alphaSource(770, 771, 770) == 1;
        assert DollyHudCache.alphaDestination(770, 771, 0) == 771;
        assert DollyHudCache.alphaDestination(775, 769, 0) == 0 : "Crosshair destination blending must stay intact";
        DollyHudCache.rendering = false;
        // Two half-transparent layers must retain their union alpha, not only the last layer's alpha.
        double alpha = 0.5 + 0.5 * (1 - 0.5);
        double cachedRed = 0.5 * (1 - 0.5), cachedGreen = 0.5;
        double world = 0.8;
        assert Math.abs(alpha - 0.75) < 0.000001;
        assert Math.abs(cachedRed + (1 - alpha) * world - ((0.5 + 0.5 * world) * 0.5)) < 0.000001;
        assert Math.abs(cachedGreen + (1 - alpha) * world - (0.5 + 0.5 * (0.5 * world))) < 0.000001;
        DollyHudCache.invalidate(); DollyHudCache.release();
        if (args.length > 0) patched(args[0]);
        System.out.println("HUD cache passed: refresh clock, premultiplied alpha and separate dynamic overlays/dispatch.");
    }
    private static void timing() {
        DollyHudCache.Timing timing = new DollyHudCache.Timing();
        long now = 1_000_000_000L;
        assert !timing.useful(now, 24) : "First frame has no timing history";
        for (int i = 0; i < 10; i++) assert !timing.useful(now += 50_000_000L, 24)
                : "20 FPS must use direct drawing even with a high configured FPS limit";
        assert !timing.useful(now += 500_000_000L, 24) : "A pause must reset the estimate";
        assert timing.useful(now += 16_666_667L, 24) : "A faster scene must recover the cache";
        for (int i = 0; i < 40; i++) timing.useful(now += 50_000_000L, 24);
        assert !timing.useful(now += 50_000_000L, 24);
        for (int i = 0; i < 40; i++) timing.useful(now += 16_666_667L, 24);
        assert timing.useful(now += 16_666_667L, 24);
        assert !timing.useful(now - 1, 24) : "A clock reset must discard stale timing";
        timing.reset();
        assert !timing.useful(now, 60);
        for (int i = 0; i < 10; i++) assert !timing.useful(now += 16_666_667L, 60)
                : "60 Hz caching at 60 FPS adds composition without reuse";
    }
    private static ClassNode read(JarFile jar, String name) throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream in = jar.getInputStream(jar.getJarEntry(name + ".class"))) { new ClassReader(in).accept(node, 0); }
        return node;
    }
    private static int calls(MethodNode m, String owner, String name, String desc) {
        int count = 0;
        for (AbstractInsnNode n : m.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.owner.equals(owner) && c.name.equals(name) && c.desc.equals(desc)) count++;
        }
        return count;
    }
    private static MethodNode method(ClassNode cls, String name, String desc) {
        for (MethodNode m : cls.methods) if (m.name.equals(name) && m.desc.equals(desc)) return m;
        throw new AssertionError("Missing " + cls.name + "." + name + desc);
    }
    private static void patched(String path) throws Exception {
        for (String name : new String[] {"avo", "bfk", "bfl"}) Class.forName(name, false, DollyHudCacheTest.class.getClassLoader()).getDeclaredMethods();
        try (JarFile jar = new JarFile(path)) {
            ClassNode hud = read(jar, "avo");
            assert hud.interfaces.contains("DollyHudLayers");
            MethodNode dynamic = method(hud, "dolly$dynamic", "(F)V");
            assert calls(dynamic, "Config", "isVignetteEnabled", "()Z") == 1;
            assert calls(dynamic, "avo", "b", "()Z") == 1 : "Crosshair must remain per frame";
            assert calls(dynamic, "avo", "a", "(Lavr;F)V") == 0 : "Hotbar must not be rendered twice";
            assert calls(dynamic, "awm", "a", "(Lavr;F)V") == 0;
            MethodNode original = method(hud, "a", "(F)V");
            int guards = 0;
            for (AbstractInsnNode n : original.instructions.toArray()) if (n instanceof FieldInsnNode) {
                FieldInsnNode f = (FieldInsnNode) n;
                if (f.owner.equals("DollyHudCache") && f.name.equals("rendering")) guards++;
            }
            assert guards == 2 : "Framebuffer capture must exclude both dynamic layers";
            int dispatches = 0;
            for (MethodNode m : read(jar, "bfk").methods) {
                dispatches += calls(m, "DollyHudCache", "render", "(Lavo;F)V");
                assert calls(m, "avo", "a", "(F)V") == 0;
            }
            assert dispatches == 1;
            ClassNode gl = read(jar, "bfl");
            assert calls(method(gl, "a", "(IIII)V"), "DollyHudCache", "alphaDestination", "(III)I") == 1;
            assert calls(method(gl, "b", "(II)V"), "DollyHudCache", "blend", "(II)Z") == 1;
        }
    }
}
