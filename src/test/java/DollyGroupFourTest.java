import java.lang.reflect.*;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

/** Real early-outs plus presentation policies without a driver OpenGL context. */
public final class DollyGroupFourTest {
    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> u = Class.forName("sun.misc.Unsafe"); Field f = u.getDeclaredField("theUnsafe"); f.setAccessible(true);
        return type.cast(u.getMethod("allocateInstance", Class.class).invoke(f.get(null), type));
    }
    private static void set(Class<?> owner, Object target, String name, Object value) throws Exception {
        Field f = owner.getDeclaredField(name); f.setAccessible(true); f.set(target, value);
    }
    public static class Player extends bew {
        pf effect;
        Player() { super(null, null, null, null); }
        public pf b(pe potion) { return potion == pe.n ? effect : null; }
    }
    public static class ParticleWorld extends bdb {
        int particles;
        ParticleWorld() { super(null, null, 0, null, null); }
        public void a(cy type, double x, double y, double z, double dx, double dy, double dz, int... data) { particles++; }
    }
    public static class Model extends bbo {
        int draws;
        public void a(pk entity, float limb, float swing, float age, float yaw, float pitch, float scale) { draws++; }
    }
    public static void main(String[] args) throws Exception {
        if (!kb.a()) kb.c();
        DollyRuntimeTest.Minecraft mc = allocate(DollyRuntimeTest.Minecraft.class);
        mc.h = allocate(Player.class); mc.f = allocate(ParticleWorld.class);
        set(ave.class, null, "S", mc);
        fire((Player) mc.h); particles(mc); projectiles(); horse(mc); earlyOuts(); hooks(args[0]);
        System.out.println("Group 4 passed: fire height/alpha/resistance warning, potion preservation, water-only density, projectile two-tick delay, mount-only opacity and real renderer early-outs.");
    }
    private static void fire(Player player) {
        assert DollyVisuals.fireOffset(-.3f) == -.3f;
        assert DollyVisuals.fireAlpha(.9f, -1, 0) == .9f;
        DollySettings.config.fireHeight = 50;
        assert Math.abs(DollyVisuals.fireOffset(-.3f) + .55f) < .000001f;
        DollySettings.config.fireOpacity = 50;
        assert DollyVisuals.fireAlpha(.9f, -1, 0) == .45f;
        player.effect = new pf(12, 200);
        assert DollyVisuals.fireAlpha(.9f, .5f) == 0;
        assert player.effect.b() == 200 : "Fire rendering changed the actual effect duration";
        DollySettings.config.fireOpacity = 100;
        float min = 1, max = 0;
        for (int ticks = 0; ticks <= 100; ticks++) {
            float alpha = DollyVisuals.fireAlpha(.9f, ticks, .5f);
            assert alpha >= 0 && alpha <= .9f;
            min = Math.min(min, alpha); max = Math.max(max, alpha);
        }
        assert min < .05f && max > .85f : "Resistance warning must visibly blink";
        DollySettings.config.hideFireResistance = false; assert DollyVisuals.fireAlpha(.9f, 200, 0) == .9f;
        DollySettings.config.hideFireResistance = true; DollySettings.config.fireHeight = 0;
        assert DollyVisuals.fireAlpha(.9f, -1, 0) == 0;
        DollySettings.config.fireHeight = 100; player.effect = null;
    }
    private static void particles(DollyRuntimeTest.Minecraft mc) throws Exception {
        ParticleWorld world = (ParticleWorld) mc.f;
        DollyVisuals.potionParticle(world, null, 0, 0, 0, 0, 0, 0, new int[0], mc.h);
        assert world.particles == 0;
        DollyVisuals.potionParticle(world, null, 0, 0, 0, 0, 0, 0, new int[0], allocate(bex.class));
        assert world.particles == 1 : "Other players' potion particles must remain native";
        DollySettings.config.cleanView = false;
        DollyVisuals.potionParticle(world, null, 0, 0, 0, 0, 0, 0, new int[0], mc.h);
        assert world.particles == 2; DollySettings.config.cleanView = true;
        assert DollyVisuals.waterFog(.1f) == .1f;
        DollySettings.config.waterFogDensity = 50;
        assert Math.abs(DollyVisuals.waterFog(.01f) - .005f) < .000001f;
        assert Math.abs(DollyVisuals.waterFog(.1f) - .05f) < .000001f;
        DollySettings.config.waterFogDensity = 0; assert DollyVisuals.waterFog(.1f) == 0;
        DollySettings.config.waterFogDensity = 100;
    }
    private static void projectiles() throws Exception {
        ur hook = allocate(ur.class);
        assert DollyVisuals.hideProjectile(hook, 1000000000L);
        assert DollyVisuals.hideProjectile(hook, 1099999999L);
        assert !DollyVisuals.hideProjectile(hook, 1100000000L) : "A frozen age must not hide a projectile forever";
        assert !DollyVisuals.hideProjectile(hook, 2000000000L);
        assert hook.W == 0 : "The fallback must not mutate simulation age";
        hook = allocate(ur.class);
        hook.d(42);
        for (int age = 0; age < 4; age++) {
            hook.W = age;
            assert DollyVisuals.hideProjectile(hook) == (age < 2);
            assert hook.W == age : "Visual delay changed projectile age";
        }
        assert !DollyVisuals.hideProjectile(allocate(bex.class));
        hook.W = 0;
        allocate(biz.class).a(hook, 0, 0, 0, 0, 0);
        allocate(bkc.class).a(hook, 0, 0, 0, 0, 0);
        DollySettings.config.cleanProjectiles = false; assert !DollyVisuals.hideProjectile(hook);
        DollySettings.config.cleanProjectiles = true;
    }
    private static void horse(DollyRuntimeTest.Minecraft mc) throws Exception {
        tp mounted = allocate(tp.class), other = allocate(tp.class); Model model = new Model();
        mc.h.m = mounted;
        DollySettings.config.horseOpacity = 25;
        assert DollyVisuals.horseAlpha(mounted) == .25f;
        assert DollyVisuals.horseAlpha(other) == 1 && DollyVisuals.horseAlpha(mc.h) == 1;
        DollySettings.config.horseOpacity = 0;
        DollyVisuals.horseModel(model, mounted, 0, 0, 0, 0, 0, 1); assert model.draws == 0;
        DollyVisuals.horseModel(model, other, 0, 0, 0, 0, 0, 1); assert model.draws == 1;
        DollySettings.config.horseOpacity = 100;
        DollyVisuals.horseModel(model, mounted, 0, 0, 0, 0, 0, 1); assert model.draws == 2;
        assert mc.h.m == mounted : "Rendering changed the player's mount";
        mc.h.m = null;
    }
    private static void earlyOuts() throws Exception {
        bec particles = allocate(bec.class);
        particles.a((cj) null, (alz) null); particles.a((cj) null, (cq) null);
        particles.addBlockHitEffects(null, null);
        allocate(bih.class).a((wq) null, 0, 0, 0, 0, 0);
        bfn renderer = allocate(bfn.class);
        Method water = bfn.class.getDeclaredMethod("e", float.class); water.setAccessible(true); water.invoke(renderer, 0f);
        DollySettings.config.fireOpacity = 0;
        Method fire = bfn.class.getDeclaredMethod("f", float.class); fire.setAccessible(true); fire.invoke(renderer, 0f);
        DollySettings.config.fireOpacity = 100;
    }
    private static ClassNode read(JarFile jar, String name) throws Exception {
        ClassNode cls = new ClassNode(); new ClassReader(jar.getInputStream(jar.getJarEntry(name + ".class"))).accept(cls, 0); return cls;
    }
    private static void hooks(String path) throws Exception {
        try (JarFile jar = new JarFile(path)) {
            int fog = 0, potion = 0, model = 0;
            for (String name : new String[] {"bfk", "pr", "bjl"}) for (MethodNode m : read(jar, name).methods)
                for (AbstractInsnNode n : m.instructions.toArray()) if (n instanceof MethodInsnNode) {
                    MethodInsnNode c = (MethodInsnNode) n;
                    if (c.owner.equals("DollyVisuals")) {
                        if (c.name.equals("waterFog")) fog++;
                        if (c.name.equals("potionParticle")) potion++;
                        if (c.name.equals("horseModel")) model++;
                    }
                }
            assert fog == 2 : "Only the two water-density branches should be adjusted";
            assert potion == 1 : "Only potion visuals should be redirected";
            assert model == 1 : "Horse opacity belongs to the model, not the nametag path";
        }
    }
}
