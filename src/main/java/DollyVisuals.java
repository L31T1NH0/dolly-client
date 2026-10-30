import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import net.optifine.render.GlAlphaState;
import net.optifine.render.GlBlendState;
import java.util.Map;
import java.util.WeakHashMap;

/** Presentation policies: never mutates potion timers, projectile ticks or mount state. */
public final class DollyVisuals {
    private static final FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(16); // LWJGL 2 requires 16 slots even for a four-component color.
    private static final Map<pk, Long> projectileStarts = new WeakHashMap<pk, Long>();
    public static float fireAlpha(float original, int resistanceTicks, float partialTicks) {
        if (DollySettings.config.fireHeight == 0) return 0;
        float alpha = original * DollySettings.config.fireOpacity / 100f;
        if (!DollySettings.config.hideFireResistance || resistanceTicks < 0) return alpha;
        if (resistanceTicks > 100) return 0;
        return alpha * (.5f - (float) Math.sin((resistanceTicks - partialTicks) * Math.PI * .2) * .5f);
    }
    public static float fireAlpha(float original, float partialTicks) {
        int ticks = -1;
        ave mc = ave.A();
        if (DollySettings.config.hideFireResistance && mc != null && mc.h != null) {
            pf effect = mc.h.b(pe.n);
            if (effect != null) ticks = effect.b();
        }
        return fireAlpha(original, ticks, partialTicks);
    }
    public static boolean skipFire(float partialTicks) { return fireAlpha(.9f, partialTicks) <= 0; }
    public static float fireOffset(float original) { return original - (1f - DollySettings.config.fireHeight / 100f) * .5f; }
    public static float waterFog(float original) { return original * DollySettings.config.waterFogDensity / 100f; }
    public static boolean hideProjectile(pk entity) {
        return hideProjectile(entity, System.nanoTime());
    }
    static boolean hideProjectile(pk entity, long now) {
        if (entity == null || entity instanceof wn || !DollySettings.config.cleanProjectiles || entity.W >= 2) return false;
        Long start = projectileStarts.get(entity);
        if (start == null) { start = now; projectileStarts.put(entity, start); }
        // Entity age can stop advancing; initial cleanup must never hide a projectile indefinitely.
        return now - start < 100000000L;
    }
    public static boolean ownPotionParticles(pr entity) {
        ave mc = ave.A();
        return DollySettings.config.cleanView && mc != null && entity == mc.h;
    }
    public static void potionParticle(adm world, cy type, double x, double y, double z,
                                      double dx, double dy, double dz, int[] data, pr owner) {
        if (!ownPotionParticles(owner)) world.a(type, x, y, z, dx, dy, dz, data);
    }
    public static float horseAlpha(pk entity) {
        if (DollySettings.config.horseOpacity == 100 || !(entity instanceof tp)) return 1f;
        ave mc = ave.A();
        return mc != null && mc.h != null && mc.h.m == entity ? DollySettings.config.horseOpacity / 100f : 1f;
    }
    public static void horseModel(bbo model, pk entity, float limb, float swing, float age, float yaw, float pitch, float scale) {
        float opacity = horseAlpha(entity);
        if (opacity == 0) return;
        if (opacity == 1) { model.a(entity, limb, swing, age, yaw, pitch, scale); return; }
        GlBlendState blend = new GlBlendState(); bfl.getBlendState(blend);
        GlAlphaState alpha = new GlAlphaState(); bfl.getAlphaState(alpha);
        colorBuffer.clear(); GL11.glGetFloat(GL11.GL_CURRENT_COLOR, colorBuffer);
        float red = colorBuffer.get(0), green = colorBuffer.get(1), blue = colorBuffer.get(2), originalAlpha = colorBuffer.get(3);
        try {
            bfl.l(); bfl.d(); bfl.a(GL11.GL_GREATER, 1f / 255f); bfl.a(770, 771, 1, 0);
            bfl.c(red, green, blue, originalAlpha * opacity);
            model.a(entity, limb, swing, age, yaw, pitch, scale);
        } finally {
            bfl.c(red, green, blue, originalAlpha);
            bfl.setAlphaState(alpha); bfl.setBlendState(blend);
        }
    }
}
