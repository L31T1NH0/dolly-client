// Adapted from TACTILE (fallingsword), commit 66892f1005b3e249240c89a4cf3521c5f73e6cd8. MIT; see licenses/TACTILE.txt in the client JAR.

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


/**
 * Decides whether the high-resolution engine may control the player; if not, it returns the fallback
 * reason. The engine supports on-foot movement on ordinary blocks only.
 *
 * <p>afh side effects (Entity.doBlockCollisions, afh.onLanded, afh.onFallenUpon) run per move in
 * vanilla. Repeating them per substep would multiply them (soul sand slows motion x0.4 per call), and
 * skipping them would change behavior. So any block near the player whose class overrides one of those
 * callbacks is unsupported, unless it is on an allowlist of blocks whose callbacks have no client-side
 * effect on motion (pressure plates, tripwire, buttons, cactus/fire damage, farmland trampling,
 * hay-bale fall damage, redstone-ore glow).
 */
final class DollyTactileSupport
{
    private static final Map<Class<?>, Boolean> SPECIAL = new HashMap<Class<?>, Boolean>();
    // BlockStairs only forwards onEntityCollidedWithBlock to its material block (stone, wood, brick...),
    // none of which affects motion in vanilla.
    private static final Class<?>[] ALLOWED = {afd.class, aki.class, afn.class, afo.class,
        agv.class, ags.class, ahm.class, aja.class, aju.class};
    private static final java.lang.reflect.Field IS_IN_WEB = DollyTactileAccess.findField(pk.class, "isInWeb", "H");

    private DollyTactileSupport()
    {
    }

    /**
     * Null if supported, else a short reason. {@code media}: water, lava, ladders, vines, soul sand, slime, cobwebs
     * and creative flight are modeled (GRID contacts); otherwise they fall back to vanilla.
     */
    static String unsupportedReason(bew p, adm world, boolean media)
    {
        if (p.I || p.bn() <= 0.0F) return "dead";
        if (p.au()) return "riding";
        // Creative flight is modeled (GRID). Flying while on a ladder (ladder clamps on a flying move) is not.
        if (p.bA.b && (!media || p.k_())) return "flying";
        if (p.v() || p.T) return "spectator/noclip";
        if (p.bJ()) return "sleeping";
        // Using an item (eating, drinking, drawing a bow, blocking) is modeled: input x0.2, no sprint start (0.8.0).
        // Vanilla effects (ids 1-23) are modeled or irrelevant: speed and slowness act through the movement-speed
        // attribute, which the engine reads every tick; jump boost and blindness are modeled; the rest
        // (night vision, haste, regeneration, ...) do not touch movement. A modded effect may move the player
        // in ways the engine does not know about.
        for (pf e : p.bl())
            if (e.a() < 1 || e.a() > 23) return "effect " + e.g();
        if (!media && p.V()) return "in water";
        if (!media && p.ab()) return "in lava";
        if (!media && p.k_()) return "on ladder/vine";
        try
        {
            if (!media && IS_IN_WEB.getBoolean(p)) return "in web";
        }
        catch (IllegalAccessException e)
        {
            return "reflection failure";
        }
        if (!world.e(new cj(p.s, 0.0D, p.u))) return "chunk not loaded";

        // Blocks overlapping the box, grown 0.3 sideways and 1.0 downward.
        aug bb = p.aR();
        int x0 = ns.c(bb.a - 0.3D), x1 = ns.c(bb.d + 0.3D);
        int y0 = ns.c(bb.b - 1.0D), y1 = ns.c(bb.e);
        int z0 = ns.c(bb.c - 0.3D), z1 = ns.c(bb.f + 0.3D);
        cj.a pos = new cj.a();
        for (int x = x0; x <= x1; ++x)
            for (int y = y0; y <= y1; ++y)
                for (int z = z0; z <= z1; ++z)
                {
                    pos.c(x, y, z);
                    afh b = world.p(pos).c();
                    if (b.t() == arm.a) continue;
                    if (!media && b.t().d()) return "liquid nearby";
                    // soul sand, slime and cobwebs are modeled with GRID contacts (their exact vanilla classes only)
                    if (media && (b.getClass() == ajq.class || b.getClass() == ajn.class
                        || b.getClass() == ako.class)) continue;
                    if (isSpecial(b.getClass())) return "block " + afh.c.c(b);
                }
        return null;
    }

    private static boolean isSpecial(Class<?> c)
    {
        Boolean cached = SPECIAL.get(c);
        if (cached != null) return cached;
        boolean special = false;
        boolean allowed = false;
        for (Class<?> a : ALLOWED) if (a.isAssignableFrom(c)) allowed = true;
        if (!allowed)
        {
            for (Class<?> k = c; k != null && k != afh.class; k = k.getSuperclass())
            {
                for (Method m : k.getDeclaredMethods())
                {
                    Class<?>[] parameters = m.getParameterTypes();
                    if (m.getName().equals("a") && m.getReturnType() == void.class
                        && (java.util.Arrays.equals(parameters, new Class<?>[] {adm.class, cj.class, alz.class, pk.class})
                            || java.util.Arrays.equals(parameters, new Class<?>[] {adm.class, cj.class, pk.class})
                            || java.util.Arrays.equals(parameters, new Class<?>[] {adm.class, pk.class})
                            || java.util.Arrays.equals(parameters, new Class<?>[] {adm.class, cj.class, pk.class, float.class})))
                    {
                        special = true;
                    }
                }
            }
        }
        SPECIAL.put(c, special);
        return special;
    }
}
