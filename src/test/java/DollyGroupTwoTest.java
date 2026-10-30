import java.io.InputStream;
import java.lang.reflect.*;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

/** Exercises real patched item counts and book early-out without requiring a graphics context. */
public final class DollyGroupTwoTest {
    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field f = unsafe.getDeclaredField("theUnsafe"); f.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(f.get(null), type));
    }
    public static class NamedStand extends um {
        public NamedStand() { super(null); }
        @Override public boolean l_() { return true; }
    }
    public static class NamedTnt extends vj {
        public NamedTnt() { super(null); }
        @Override public boolean l_() { return true; }
    }
    public static class BudgetStand extends NamedStand {
        @Override public aug aR() { throw new AssertionError("Expired culling must not prepare a bounding box"); }
    }
    public static void main(String[] args) throws Exception {
        playersAndObjects(); budget(); copies(); bobbing(); book();
        if (args.length > 0) hooks(args[0]);
        System.out.println("Group 2 passed: player exclusions, named/armorstand candidates, native item count fallback, unchanged stack quantity, static bobbing and book early-out.");
    }
    private static void playersAndObjects() throws Exception {
        for (pk player : new pk[] {allocate(bew.class), allocate(bex.class)}) {
            assert !DollyCulling.eligible(player) : "Player must be excluded before name/world/box queries";
            assert !DollyCulling.entity(player) : "Player must keep its original render path";
        }
        NamedStand stand = allocate(NamedStand.class);
        assert DollyCulling.eligible(stand) : "Named hologram should be a candidate behind an opaque barrier";
        assert DollyCulling.eligible(allocate(NamedTnt.class)) : "Names should not exempt non-player objects";
        assert DollyCulling.eligible(allocate(uz.class));
        stand.m = allocate(bex.class);
        assert !DollyCulling.eligible(stand) : "Mounted geometry must retain the conservative exception";
        stand.m = null;
        DollySettings.config.entityCulling = false;
        assert !DollyCulling.entity(stand) : "Disabled culling must bypass box/world queries";
        DollySettings.config.entityCulling = true;
    }
    private static void budget() throws Exception {
        DollyCulling.beginFrame(.5f);
        Field deadline = DollyCulling.class.getDeclaredField("deadline"); deadline.setAccessible(true);
        Field remaining = DollyCulling.class.getDeclaredField("remaining"); remaining.setAccessible(true);
        assert DollyCulling.budgetAvailable();
        deadline.setLong(null, System.nanoTime() - 1_000_000L);
        assert !DollyCulling.budgetAvailable();
        assert !DollyCulling.entity(allocate(BudgetStand.class));
        assert !DollyCulling.tile(allocate(ale.class)) : "Expired tile culling must not read its position";
        DollyCulling.beginFrame(.5f); remaining.setInt(null, 0);
        assert !DollyCulling.budgetAvailable() && !DollyCulling.entity(allocate(BudgetStand.class));
        DollyCulling.beginFrame(.5f); assert DollyCulling.budgetAvailable();
    }
    private static void copies() throws Exception {
        bjf renderer = allocate(bjf.class);
        Method count = bjf.class.getDeclaredMethod("a", zx.class); count.setAccessible(true);
        zx stack = allocate(zx.class);
        int[] sizes = {1, 2, 16, 17, 32, 33, 48, 49, 64};
        int[] nativeCounts = {1, 2, 2, 3, 3, 4, 4, 5, 5};
        for (int i = 0; i < sizes.length; i++) {
            stack.b = sizes[i];
            DollySettings.config.singleGroundItem = false;
            assert ((Integer) count.invoke(renderer, stack)) == nativeCounts[i];
            DollySettings.config.singleGroundItem = true;
            assert ((Integer) count.invoke(renderer, stack)) == 1;
            assert stack.b == sizes[i] : "Display count changed the real inventory quantity";
        }
    }
    private static void bobbing() {
        for (float sine : new float[] {-1f, -.5f, 0, .5f, 1f}) {
            DollySettings.config.staticGroundItems = false;
            assert DollyWorldObjects.bobSine(sine) == sine;
            DollySettings.config.staticGroundItems = true;
            assert DollyWorldObjects.bobSine(sine) * .1f + .1f == 0f;
        }
    }
    private static void book() throws Exception {
        DollySettings.config.hideEnchantmentBooks = true;
        // Null table is intentional: the configured early-out must happen before data or OpenGL is touched.
        allocate(bhf.class).a((ale) null, 0, 0, 0, 0f, -1);
    }
    private static ClassNode read(JarFile jar, String name) throws Exception {
        ClassNode cls = new ClassNode();
        try (InputStream in = jar.getInputStream(jar.getJarEntry(name + ".class"))) { new ClassReader(in).accept(cls, 0); }
        return cls;
    }
    private static MethodNode method(ClassNode cls, String name, String desc) {
        for (MethodNode m : cls.methods) if (m.name.equals(name) && m.desc.equals(desc)) return m;
        throw new AssertionError("Missing method " + cls.name + name + desc);
    }
    private static int calls(MethodNode m, String owner, String name) {
        int count = 0;
        for (AbstractInsnNode n : m.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.owner.equals(owner) && c.name.equals(name)) count++;
        }
        return count;
    }
    private static void hooks(String path) throws Exception {
        try (JarFile jar = new JarFile(path)) {
            ClassNode items = read(jar, "bjf");
            MethodNode transform = method(items, "a", "(Luz;DDDFLboq;)I");
            assert calls(transform, "DollyWorldObjects", "bobSine") == 1;
            assert calls(transform, "ns", "a") == 1 : "Native sine remains available when disabled";
            assert calls(transform, "bfl", "b") >= 2 : "Translation and rotation must be retained";
            assert calls(method(items, "a", "(Lzx;)I"), "DollyWorldObjects", "copies") == 1;
            MethodNode book = method(read(jar, "bhf"), "a", "(Lale;DDDFI)V");
            boolean control = false;
            for (AbstractInsnNode n : book.instructions.toArray()) if (n instanceof FieldInsnNode)
                control |= ((FieldInsnNode) n).name.equals("hideEnchantmentBooks");
            assert control && calls(book, "bfl", "E") == 1 : "Book must keep its native renderer when enabled";
            assert calls(method(read(jar, "biu"), "a", "(Lpk;DDDFFZ)Z"), "DollyCulling", "entity") == 1;
        }
    }
}
