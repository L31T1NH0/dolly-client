import java.lang.reflect.*;
import java.util.*;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

public final class DollyGroupThreeTest {
    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> u = Class.forName("sun.misc.Unsafe"); Field f = u.getDeclaredField("theUnsafe"); f.setAccessible(true);
        return type.cast(u.getMethod("allocateInstance", Class.class).invoke(f.get(null), type));
    }
    private static void set(Class<?> owner, Object target, String name, Object value) throws Exception {
        Field f = owner.getDeclaredField(name); f.setAccessible(true); f.set(target, value);
    }
    public static class Clock extends anm {
        long seen;
        public String k() { return "test"; }
        public String l() { return "test"; }
        public float a(long time, float partial) { seen = time; return super.a(time, partial); }
    }
    public static class CameraBlock extends afh {
        int calls;
        CameraBlock() { super(arm.e); }
        public aug a(adm world, cj pos, alz state) { calls++; return new aug(0, 0, 0, 1, 1, 1); }
    }
    public static class TraceWorld extends bdb {
        boolean fail, ignored;
        int nativeCalls;
        CameraBlock block;
        TraceWorld() { super(null, null, 0, null, null); }
        public auh a(aui from, aui to) { nativeCalls++; return null; }
        public auh a(aui from, aui to, boolean liquid, boolean noBox, boolean miss) {
            assert !liquid && noBox && miss;
            ignored = DollyCameraTrace.collision(block, this, null, null) == null;
            if (fail) throw new IllegalStateException("trace failed");
            return null;
        }
    }
    public static void main(String[] args) throws Exception {
        DollyRuntimeTest.Minecraft mc = allocate(DollyRuntimeTest.Minecraft.class);
        mc.t = new DollyRuntimeTest.Settings(); mc.h = allocate(bew.class); mc.f = allocate(bdb.class);
        mc.g = allocate(DollyRuntimeTest.Renderer.class);
        set(ave.class, null, "S", mc);
        set(Config.class, null, "minecraft", mc); set(Config.class, null, "gameSettings", mc.t);
        DollyInput.register(mc.t); Display.active = true;
        if (!kb.a()) kb.c();
        zoom(mc); time(); lighting(mc); camera(mc); hooks(args[0]);
        System.out.println("Group 3 passed: toggle/scroll/easing/sensitivity, direct celestial time with unchanged world data, client-only fullbright, scoped camera trace, debug-axis cleanup and sky hooks.");
    }
    private static void zoom(DollyRuntimeTest.Minecraft mc) {
        DollySettings.config.toggleZoom = true; DollyZoom.reset();
        assert DollyZoom.active(true, true);
        assert DollyZoom.active(true, true) : "Repeat must not toggle";
        assert DollyZoom.active(false, true);
        assert !DollyZoom.active(true, true);
        assert !DollyZoom.active(false, false);
        DollySettings.config.toggleZoom = false;
        assert DollyZoom.active(true, true) && !DollyZoom.active(false, true);
        assert DollyZoom.scroll(4, 1) == 5 && DollyZoom.scroll(4, -1) == 3;
        assert DollyZoom.scroll(600, 1) == 600 && DollyZoom.scroll(1, -1) == 1;
        for (int mode = 0; mode < 3; mode++) {
            assert DollyZoom.ease(0, mode) == 0 && DollyZoom.ease(1, mode) == 1;
            float previous = 0;
            for (int i = 0; i <= 100; i++) { float next = DollyZoom.ease(i / 100f, mode); assert next >= previous && next <= 1; previous = next; }
        }
        Keyboard.down[DollySettings.config.zoomKey] = true;
        mc.t.a = .5f; DollySettings.config.zoomSensitivity = .5f;
        assert DollyZoom.sensitivity(mc.t) == .25f && mc.t.a == .5f;
        DollySettings.config.dynamicZoomSensitivity = true; DollySettings.config.zoomFactor = 8;
        assert DollyZoom.sensitivity(mc.t) == .03125f;
        DollySettings.config.directZoomCamera = true; assert !DollyCamera.smoothCamera(mc.t);
        assert DollyZoom.hideHand(); DollySettings.config.renderHandZoom = true; assert !DollyZoom.hideHand();
        assert DollyCamera.fov(70f, false) == 70f : "Visible hand should keep its native field of view";
        Keyboard.down[DollySettings.config.zoomKey] = false; assert DollyZoom.sensitivity(mc.t) == .5f;
        DollySettings.config.dynamicZoomSensitivity = false; DollySettings.config.zoomSensitivity = 1;
    }
    private static void time() throws Exception {
        bdb world = allocate(bdb.class); ato data = allocate(ato.class); Clock clock = allocate(Clock.class);
        data.b(123456L); data.c(66000L);
        set(adm.class, world, "x", data); set(adm.class, world, "t", clock);
        DollySettings.config.timeChanger = true; DollySettings.config.timeOfDay = 6000;
        world.c(.5f);
        assert clock.seen == 54000L : "Celestial angle must use the chosen time, not the direct WorldInfo time";
        assert world.L() == 54000L && world.K() == 123456L;
        assert data.g() == 66000L : "Server-provided world time must not be overwritten";
        DollySettings.config.timeChanger = false; world.c(.5f); assert clock.seen == 66000L;
        adm server = allocate(le.class); set(adm.class, server, "x", data); set(adm.class, server, "t", clock);
        DollySettings.config.timeChanger = true; server.c(.5f); assert clock.seen == 66000L;
        DollySettings.config.timeChanger = false;
    }
    private static void lighting(DollyRuntimeTest.Minecraft mc) throws Exception {
        DollySettings.config.fullBright = true;
        assert DollyLighting.active(mc.f) && !DollyLighting.active(allocate(le.class));
        assert mc.f.a((ads) null, (cj) null) == 15;
        assert mc.f.l(null) == 15;
        assert mc.f.c((ads) null, (cj) null);
        amy chunk = allocate(amy.class); set(amy.class, chunk, "i", mc.f);
        assert chunk.a((ads) null, (cj) null) == 15 && chunk.a((cj) null, 0) == 15;
        int[] pixels = new int[256]; DollyLighting.lightmap(pixels);
        for (int pixel : pixels) assert pixel == 0xFFFFFFFF;
        assert DollyLighting.skipLightmap(); DollyLighting.invalidate(); assert !DollyLighting.skipLightmap();
        Field shader = net.optifine.shaders.Shaders.class.getDeclaredField("shaderPackLoaded"); shader.setAccessible(true);
        boolean previous = shader.getBoolean(null); shader.setBoolean(null, true);
        try {
            assert !DollyLighting.active(mc.f);
            DollySettings.config.smartFullbright = false; assert DollyLighting.active(mc.f);
        } finally { shader.setBoolean(null, previous); DollySettings.config.smartFullbright = true; }
        DollySettings.config.fullBright = false;
        Arrays.fill(pixels, 123); DollyLighting.lightmap(pixels); assert pixels[0] == 123;
    }
    private static void camera(DollyRuntimeTest.Minecraft mc) throws Exception {
        DollySettings.config.parallaxFix = false; assert DollyCameraTrace.parallax(-.1f) == -.1f;
        DollySettings.config.parallaxFix = true; assert DollyCameraTrace.parallax(-.1f) == .05f;
        DollyCamera.debugDirections = true; assert DollyCameraTrace.parallax(-.1f) == -.1f; DollyCamera.debugDirections = false;
        bfk renderer = allocate(bfk.class); set(bfk.class, renderer, "h", mc);
        Method debug = bfk.class.getDeclaredMethod("h", float.class); debug.setAccessible(true);
        mc.t.aC = false; debug.invoke(renderer, .5f); assert !DollyCamera.debugDirections;
        mc.t.aC = true; bew player = mc.h; mc.h = null;
        try { debug.invoke(renderer, .5f); throw new AssertionError("Expected missing player"); }
        catch (InvocationTargetException expected) { assert expected.getCause() instanceof NullPointerException; }
        finally { mc.h = player; mc.t.aC = false; }
        assert !DollyCamera.debugDirections : "Exception retained debug camera offset";
        CameraBlock block = allocate(CameraBlock.class);
        set(DollyCameraTrace.class, null, "ignored", Collections.singleton(block));
        TraceWorld world = allocate(TraceWorld.class); world.block = block;
        DollySettings.config.betterCamera = true;
        DollyCameraTrace.trace(world, null, null); assert world.ignored && block.calls == 0;
        assert DollyCameraTrace.collision(block, world, null, null) != null && block.calls == 1;
        world.fail = true;
        try { DollyCameraTrace.trace(world, null, null); throw new AssertionError(); }
        catch (IllegalStateException expected) { }
        assert DollyCameraTrace.collision(block, world, null, null) != null;
        DollySettings.config.betterCamera = false; DollyCameraTrace.trace(world, null, null); assert world.nativeCalls == 1;
        DollySettings.config.customSkyFix = true; assert DollyCameraTrace.skyDistance(64) == 256 && !DollyCameraTrace.skyVbo(true);
        DollySettings.config.customSkyFix = false; assert DollyCameraTrace.skyDistance(64) == 64 && DollyCameraTrace.skyVbo(true);
    }
    private static void hooks(String path) throws Exception {
        try (JarFile jar = new JarFile(path)) {
            for (String name : new String[] {"bfk", "adm", "amy", "bfr", "bfn"}) {
                ClassNode cls = new ClassNode(); new ClassReader(jar.getInputStream(jar.getJarEntry(name + ".class"))).accept(cls, 0);
                int hooks = 0;
                for (MethodNode m : cls.methods) for (AbstractInsnNode n : m.instructions.toArray()) if (n instanceof MethodInsnNode) {
                    MethodInsnNode c = (MethodInsnNode) n;
                    if (c.owner.equals("DollyLighting") || c.owner.equals("DollyCameraTrace") || c.owner.equals("DollyZoom") || c.owner.equals("DollyWorldTime")) hooks++;
                }
                assert hooks > 0 : "Missing group 3 hooks in " + name;
            }
        }
    }
}
