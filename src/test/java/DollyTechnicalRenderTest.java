import java.lang.reflect.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Real patched font/model adapters, with in-memory metrics and animated sprite fixtures. */
public final class DollyTechnicalRenderTest {
    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }
    private static void set(Object object, String field, Object value) throws Exception {
        Field f = object.getClass().getDeclaredField(field); f.setAccessible(true); f.set(object, value);
    }
    public static void main(String[] args) throws Exception {
        text(); stitching();
        if (args.length > 0) reloadHook(args[0]);
        System.out.println("Technical render passed: cached font metrics/invalidation and animated item contours/UV inversion.");
    }
    private static void text() throws Exception {
        avn font = allocate(avn.class), second = allocate(avn.class);
        float[] widths = new float[256]; Arrays.fill(widths, 4f);
        set(font, "charWidthFloat", widths); set(second, "charWidthFloat", widths);
        set(font, "e", new byte[65536]); font.offsetBold = 1f;
        DollySettings.config.stringWidthCaching = true; DollyTextCache.clear();
        assert font.a((String) null) == 0;
        assert font.a("AA") == 8;
        Arrays.fill(widths, 7f);
        assert font.a("AA") == 8 : "Public width entry must serve cached metrics";
        assert second.a("AA") == 14 : "Font instances must not share metrics";
        font.b(true);
        assert font.a("AA") == 14 : "Bidi setter must invalidate widths";
        font.a(true);
        assert font.a("AA") == 0 : "Unicode setter must invalidate widths";
        font.a(false);
        assert font.a("AA") == 14;
        Arrays.fill(widths, 5f);
        DollySettings.config.stringWidthCaching = false;
        assert font.a("AA") == 10 : "Disabled cache must use original measurement";
        DollyTextCache.clear(); DollySettings.config.stringWidthCaching = true;
        assert font.a("§lAA") == 12 : "Formatting/bold measurement must be preserved";
        for (int i = 0; i < 5001; i++) DollyTextCache.remember(i, font, "entry " + i);
        assert DollyTextCache.lookup(font, "entry 0") == null;
        assert DollyTextCache.lookup(font, "entry 5000") == 5000;
        DollyTextCache.invalidate(font);
        assert DollyTextCache.lookup(font, "entry 5000") == null;
    }
    private static bmi sprite(int width, int height, int[]... frames) throws Exception {
        bmi sprite = allocate(bmi.class); sprite.b(width); sprite.c(height);
        List<int[][]> data = new ArrayList<int[][]>();
        for (int[] frame : frames) data.add(new int[][] {frame});
        sprite.a(data); return sprite;
    }
    @SuppressWarnings("unchecked") private static List<bgh> generate(bmi sprite) throws Exception {
        Method m = bgp.class.getDeclaredMethod("a", int.class, String.class, bmi.class); m.setAccessible(true);
        return (List<bgh>) m.invoke(new bgp(), 2, "layer0", sprite);
    }
    private static void stitching() throws Exception {
        int opaque = 0xffc0ffee;
        bmi solid = sprite(2, 2, new int[] {opaque, opaque, opaque, opaque});
        DollySettings.config.optimizedItems = false;
        List<bgh> expanded = generate(solid);
        float[] uv = expanded.get(0).c.get(cq.d).e.a;
        assert uv[0] < 0 && uv[2] > 16;
        assert Math.abs(uv[0] * 0.999f + uv[2] * 0.001f) < 0.00001f;
        assert Math.abs(uv[2] * 0.999f + uv[0] * 0.001f - 16) < 0.00001f;
        List<bgh> another = generate(solid);
        assert Arrays.equals(uv, another.get(0).c.get(cq.d).e.a) : "UV correction must apply exactly once per model";
        DollySettings.config.optimizedItems = true;
        assert generate(solid).size() == 5 : "Opaque rectangle needs four merged contour faces";
        assert generate(sprite(2, 2, new int[4])).size() == 1 : "Transparent sprite must have no side contour";
        int[] ring = {opaque, opaque, opaque, opaque, 0, opaque, opaque, opaque, opaque};
        assert generate(sprite(3, 3, ring)).size() == 9 : "Interior hole contour was lost";
        int[] left = {opaque, 0}, right = {0, 0x01000000};
        List<bgh> animated = generate(sprite(2, 1, left, right));
        boolean innerLeft = false, innerRight = false;
        for (bgh part : animated) {
            if (part.a.x == 8f && part.b.x == 8f) {
                innerLeft |= part.c.containsKey(cq.e); innerRight |= part.c.containsKey(cq.f);
            }
            assert part.a.x >= 0 && part.b.x <= 16 && part.a.y >= 0 && part.b.y <= 16;
            for (bgi face : part.c.values()) assert face.c == 2 && face.d.equals("layer0");
        }
        assert innerLeft && innerRight : "Edges must be accumulated per animation frame, not an opaque-pixel union";
        assert Arrays.equals(left, new int[] {opaque, 0}) && Arrays.equals(right, new int[] {0, 0x01000000});
        DollySettings.config.optimizedItems = true;
        assert generate(solid).size() == 5 : "Default optimization must use merged item contours";
        bmi separated = sprite(3, 1, new int[] {opaque, 0, opaque});
        assert generate(separated).size() == 7 : "Opaque side spans should merge across transparent gaps";
        bmi translucent = sprite(3, 1, new int[] {0x80c0ffee, 0, opaque});
        assert generate(translucent).size() == 9 : "Partial alpha must keep separate spans";
        DollySettings.config.optimizedItems = false;
        assert generate(separated).size() >= 7 : "Optimized opaque geometry must not exceed native spans";
        DollySettings.config.optimizedItems = true;
    }
    private static void reloadHook(String path) throws Exception {
        try (JarFile jar = new JarFile(path)) {
            ClassNode node = new ClassNode();
            try (java.io.InputStream in = jar.getInputStream(jar.getJarEntry("avn.class"))) { new ClassReader(in).accept(node, 0); }
            boolean checked = false;
            for (MethodNode m : node.methods) if (m.name.equals("a") && m.desc.equals("(Lbni;)V")) {
                int calls = 0;
                for (AbstractInsnNode n : m.instructions.toArray()) if (n instanceof MethodInsnNode) {
                    MethodInsnNode c = (MethodInsnNode) n;
                    if (c.owner.equals("DollyTextCache") && c.name.equals("invalidate")) calls++;
                }
                assert calls == 1 : "Font resource reload must invalidate cached widths"; checked = true;
            }
            assert checked;
        }
    }
}
