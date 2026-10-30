import java.lang.reflect.*;
import java.util.*;
import dolly.tactile.ServerProfiles;
import dolly.tactile.ServerProfiles.Profile;
import dolly.tactile.ServerProfiles.Mode;
import dolly.tactile.core.PlayerState;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Real obfuscated movement adapters with a deterministic flat world and no graphics/native devices. */
public final class DollyTactileTest {
    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> u = Class.forName("sun.misc.Unsafe"); Field f = u.getDeclaredField("theUnsafe"); f.setAccessible(true);
        return type.cast(u.getMethod("allocateInstance", Class.class).invoke(f.get(null), type));
    }
    private static void set(Class<?> type, Object target, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    public static class Minecraft extends DollyRuntimeTest.Minecraft {
        bde server;
        boolean local;
        Minecraft() { super(); }
        public bde D() { return server; }
        public boolean F() { return local; }
    }
    public static class World extends bdb {
        boolean broken;
        World() { super(null, null, 0, null, null); }
        public boolean e(cj position) { return true; }
        public alz p(cj position) { return (position.o() < 0 ? afi.b : afi.a).Q(); }
        public List<aug> a(pk player, aug query) {
            if (broken) throw new IllegalStateException("test collision failure");
            return query.b < 0 ? Collections.singletonList(new aug(-1000, -1, -1000, 1000, 0, 1000)) : Collections.<aug>emptyList();
        }
        public boolean a(aug box, arm material, pk player) { return false; }
        public boolean a(aug box, arm material) { return false; }
        public boolean d(aug box) { return false; }
    }
    public static class Attribute implements qc {
        final Map<UUID, qd> modifiers = new HashMap<UUID, qd>();
        double base = .1;
        boolean fail;
        public qb a() { return vy.d; }
        public double b() { return base; }
        public void a(double base) { this.base = base; }
        public Collection<qd> a(int operation) { return modifiers.values(); }
        public Collection<qd> c() { return modifiers.values(); }
        public boolean a(qd modifier) { return modifiers.containsKey(modifier.a()); }
        public qd a(UUID id) { return modifiers.get(id); }
        public void b(qd modifier) { modifiers.put(modifier.a(), modifier); }
        public void c(qd modifier) { modifiers.remove(modifier.a()); }
        public void d() { }
        public double e() {
            if (fail && !modifiers.isEmpty()) throw new IllegalStateException("test attribute failure");
            return modifiers.isEmpty() ? base : base * 1.3;
        }
    }
    public static class Player extends bew {
        Attribute speed;
        xg food;
        boolean sprint;
        int nativeMoves, abilities;
        Player() { super(null, null, null, null); }
        public boolean v() { return false; }
        public boolean bJ() { return false; }
        public boolean bS() { return false; }
        public boolean k_() { return false; }
        public boolean aw() { return sprint; }
        public void d(boolean sprint) { this.sprint = sprint; }
        public Collection<pf> bl() { return Collections.emptyList(); }
        public pf b(pe effect) { return null; }
        public boolean a(pe effect) { return false; }
        public qc a(qb attribute) { return speed; }
        public xg cl() { return food; }
        public void t() { abilities++; }
        public void b(mw stat) { }
        public void a(float exhaustion) { }
        protected void a(cj position, afh block) { }
        public void a(eu message) { }
        public void g(float strafe, float forward) { nativeMoves++; }
    }
    private static Player player(Minecraft mc) throws Exception {
        Player p = allocate(Player.class); p.speed = new Attribute(); p.food = new xg();
        p.ac = new pz(p); p.ac.a(6, Float.valueOf(20f)); p.ac.a(0, Byte.valueOf((byte)0));
        p.bA = new wl(); p.bi = new wm(p); p.b = new bev(mc.t); p.o = mc.f;
        p.J = .6f; p.K = 1.8f; p.S = .6f; p.C = true; p.aM = .02f;
        p.s = .5; p.u = .5; p.w = -.0784000015258789;
        p.a(new aug(.2, 0, .2, .8, 1.8, .8));
        return p;
    }
    public static void main(String[] args) throws Exception {
        if (!kb.a()) kb.c();
        Minecraft mc = allocate(Minecraft.class); mc.t = allocate(avh.class); mc.w = true;
        mc.t.Y = new avb("test.forward", 17, "test"); mc.t.aa = new avb("test.back", 31, "test");
        mc.t.Z = new avb("test.left", 30, "test"); mc.t.ab = new avb("test.right", 32, "test");
        mc.t.ac = new avb("test.jump", 57, "test"); mc.t.ad = new avb("test.sneak", 42, "test");
        mc.t.ae = new avb("test.sprint", 29, "test");
        mc.f = allocate(World.class); mc.h = player(mc);
        set(ave.class, null, "S", mc);
        avl timer = allocate(avl.class); timer.d = 1;
        set(avl.class, timer, "f", System.nanoTime() / 1e9); set(avl.class, timer, "j", 1d);
        set(ave.class, mc, "Y", timer);
        poller();
        controller(mc);
        adapters(mc);
        screen(mc);
        hooks(args[0]);
        System.out.println("TACTILE passed: profiles/Hypixel vanilla gate, real engine activation/jump/handover/fault recovery, vanilla move delegation, server switches, scrolling controls and shipped hooks.");
    }
    private static void waitFor(java.util.function.BooleanSupplier check) throws Exception {
        long deadline = System.nanoTime() + 3000000000L;
        while (!check.getAsBoolean()) {
            if (System.nanoTime() > deadline) throw new AssertionError("Poller test timed out");
            Thread.sleep(1);
        }
    }
    private static void poller() throws Exception {
        com.sun.jna.Pointer game = new com.sun.jna.Pointer(1), other = new com.sun.jna.Pointer(2);
        java.util.concurrent.atomic.AtomicReference<com.sun.jna.Pointer> foreground = new java.util.concurrent.atomic.AtomicReference<com.sun.jna.Pointer>(game);
        java.util.concurrent.atomic.AtomicInteger reads = new java.util.concurrent.atomic.AtomicInteger();
        dolly.tactile.client.input.Win32.User32 api = (dolly.tactile.client.input.Win32.User32) Proxy.newProxyInstance(
                DollyTactileTest.class.getClassLoader(), new Class<?>[] {dolly.tactile.client.input.Win32.User32.class}, (proxy, method, args) -> {
                    if (method.getName().equals("GetForegroundWindow")) return foreground.get();
                    if (method.getName().equals("GetAsyncKeyState")) { reads.incrementAndGet(); return (short)0; }
                    throw new AssertionError(method.getName());
                });
        dolly.tactile.client.input.AsyncKeyPoller poller = new dolly.tactile.client.input.AsyncKeyPoller(api, new int[] {87}, 1000000L, 0);
        poller.start();
        try {
            waitFor(() -> poller.polls() >= 3);
            assert reads.get() == 0 : "An unfocused poller must never read keys";
            poller.setFocused(true); waitFor(() -> reads.get() >= 3);
            foreground.set(other);
            final long first = poller.polls(); waitFor(() -> poller.polls() >= first + 3);
            int stoppedAt = reads.get(); final long again = poller.polls();
            waitFor(() -> poller.polls() >= again + 3);
            assert reads.get() == stoppedAt : "Switching foreground windows must stop key reads before the next frame";
            foreground.set(game); waitFor(() -> reads.get() > stoppedAt);
            poller.setFocused(false);
        } finally { poller.stop(); }
        assert !poller.isAlive();
    }
    private static void controller(Minecraft mc) throws Exception {
        DollySettings.config.tactileProfiles = "*=EXACT,960,20,FRAME,2000,true,0";
        mc.server = new bde("Hypixel", "MC.HYPIXEL.NET.:25565", false);
        DollyTactile.tickStart();
        assert DollyTactile.server().equals("mc.hypixel.net");
        Field engine = DollyTactile.class.getDeclaredField("engine"); engine.setAccessible(true);
        assert engine.get(null) == null : "Hypixel must never construct the substep engine";
        beu input = mc.h.b;
        DollyTactile.frameStart(.5f); DollyTactile.frameEnd();
        assert mc.h.b == input;
        DollyTactileHooks.move(mc.h, 0, 1);
        assert ((Player)mc.h).nativeMoves == 1 : "Restricted profiles must call the native movement method";
        DollySettings.config.tactileProfiles = ""; mc.server = new bde("Other", "other.example:25566", false);
        DollyTactile.tickStart(); assert engine.get(null) == null;
        assert DollyTactile.server().equals("other.example:25566");
        DollySettings.config.tactileProfiles = "*=EXACT,960,20,FRAME,2000,true,0";
        DollyTactile.tickStart();
        assert ((DollyTactileEngine)engine.get(null)).isActive();
        Player previous = (Player)mc.h;
        mc.f = allocate(World.class); mc.h = player(mc);
        mc.server = new bde("Hypixel", "mc.hypixel.net", false);
        beu freshInput = mc.h.b;
        DollyTactile.tickStart();
        assert !((DollyTactileEngine)engine.get(null)).isActive() && previous.b instanceof bev;
        assert mc.h.b == freshInput && mc.h.t == 0 : "Joining Hypixel must retain the new world's native state";
        DollyTactileHooks.move(mc.h, 0, 1); assert ((Player)mc.h).nativeMoves == 1;
        mc.server = null;
        DollyTactile.tickStart();
        assert !((DollyTactileEngine)engine.get(null)).isActive() : "A multiplayer connection without an address must stay vanilla";
        assert DollyTactile.server() == null;
        DollySettings.config.tactileProfiles = ""; mc.local = true;
        DollyTactile.tickStart(); DollyTactile.shutdown();
        assert DollyTactile.server().equals(ServerProfiles.SINGLEPLAYER);
    }
    private static void adapters(Minecraft mc) throws Exception {
        DollyTactileConfig cfg = new DollyTactileConfig(); cfg.enabled = true; cfg.mode = DollyTactileConfig.Mode.EXACT;
        cfg.inputSource = DollyTactileConfig.InputSource.FRAME;
        DollyTactileEngine e = new DollyTactileEngine(cfg); DollyTactileHooks.setHandler(e);
        Keyboard.down[17] = true; Keyboard.down[57] = true;
        e.onClientTick(true);
        assert e.isActive() && !e.isFaulted() : e.fallbackReason();
        assert mc.h.b instanceof DollyTactileMovement;
        int nativeMoves = ((Player)mc.h).nativeMoves;
        DollyTactileHooks.move(mc.h, 0, 1);
        assert ((Player)mc.h).nativeMoves == nativeMoves;
        e.onClientTick(false);
        e.onClientTick(true); DollyTactileHooks.move(mc.h, 0, 1); e.onClientTick(false);
        assert mc.h.t > .1 : "Held jump must be simulated by the real adapter";
        assert e.diag.jumps == 1 : "One held key must not duplicate jumps";
        Player other = player(mc); DollyTactileHooks.move(other, 0, 1);
        assert other.nativeMoves == 1 : "Other entities must retain vanilla movement";
        e.onRenderTick(true, .5f);
        aug physical = mc.h.aR();
        e.onRenderTick(false, 0);
        assert Math.abs(mc.h.s - (physical.a + physical.d) / 2) < 1e-12
                && mc.h.t == physical.b && Math.abs(mc.h.u - (physical.c + physical.f) / 2) < 1e-12
                : "Rendering must restore the simulated position, keeping render prediction out of gameplay";
        cfg.mode = DollyTactileConfig.Mode.VANILLA; cfg.enabled = false;
        e.onClientTick(true);
        assert !e.isActive() && mc.h.b instanceof bev;
        DollyTactileHooks.move(mc.h, 0, 1); assert ((Player)mc.h).nativeMoves == nativeMoves + 1;
        e.shutdown();
        Keyboard.down[17] = false; Keyboard.down[57] = false;
        mc.h = player(mc); cfg.mode = DollyTactileConfig.Mode.STEPPED; cfg.enabled = true;
        e = new DollyTactileEngine(cfg); e.onClientTick(true); assert e.isActive();
        ((World)mc.f).broken = true;
        e.onClientTick(true);
        assert e.isFaulted() && !e.isActive() && mc.h.b instanceof bev : "A physics failure must restore native input";
        ((World)mc.f).broken = false;
        e.onClientTick(true); assert !e.isActive() : "A faulted engine must stay off until restart";
        e.shutdown();
        mc.h = player(mc); ((Player)mc.h).speed.fail = true;
        e = new DollyTactileEngine(cfg); e.onClientTick(true);
        assert e.isFaulted() && ((Player)mc.h).speed.modifiers.isEmpty()
                : "A failed speed read must restore the sprint modifier before handing control to vanilla";
        e.shutdown(); DollyTactileHooks.setHandler(null);
    }
    private static void screen(Minecraft mc) throws Exception {
        mc.k = allocate(DollyRuntimeTest.Font.class); mc.k.a = 9;
        for (int[] size : new int[][] {{320,180}, {427,240}, {854,480}}) {
            DollyTactileScreen screen = new DollyTactileScreen(null); screen.l = size[0]; screen.m = size[1]; screen.q = mc.k;
            screen.b();
            for (avs b : screen.n) assert b.h >= 0 && b.i >= 0 && b.h + b.b() <= screen.l && b.i + b.g <= screen.m;
        }
    }
    private static void hooks(String path) throws Exception {
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(path)) {
            assert jar.getJarEntry("licenses/TACTILE.txt") != null;
            assert jar.getJarEntry("dolly/tactile/harness/MovementTests.class") == null;
            assert jar.getJarEntry("dolly/tactile/core/Course.class") == null;
            org.objectweb.asm.tree.ClassNode cls = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(jar.getInputStream(jar.getJarEntry("pr.class"))).accept(cls, 0);
            int hooks = 0;
            for (org.objectweb.asm.tree.MethodNode m : cls.methods) if (m.name.equals("m") && m.desc.equals("()V"))
                for (org.objectweb.asm.tree.AbstractInsnNode n : m.instructions.toArray()) if (n instanceof org.objectweb.asm.tree.MethodInsnNode) {
                    org.objectweb.asm.tree.MethodInsnNode c = (org.objectweb.asm.tree.MethodInsnNode)n;
                    if (c.owner.equals("DollyTactileHooks") && c.name.equals("move")) hooks++;
                }
            assert hooks == 1;
        }
    }
}
