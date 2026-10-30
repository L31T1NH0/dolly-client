// Adapted from TACTILE (fallingsword), commit 66892f1005b3e249240c89a4cf3521c5f73e6cd8. MIT; see licenses/TACTILE.txt in the client JAR.

import java.util.ArrayList;
import java.util.List;

import dolly.tactile.core.Box;
import dolly.tactile.core.CollisionWorld;


/**
 * CollisionWorld backed by the live client world. It calls the same query as vanilla's moveEntity
 * (adm.getCollidingBoundingBoxes, including entity collision boxes and the world border), so the
 * ported collider sees exactly what vanilla's would. Gate A shows the port matches vanilla on
 * identical boxes. Collision queries are counted for profiling.
 */
final class DollyTactileWorld implements CollisionWorld
{
    private final adm world;
    private final pk entity;
    long queries;

    DollyTactileWorld(adm world, pk entity)
    {
        this.world = world;
        this.entity = entity;
    }

    @Override
    public List<Box> getCollidingBoxes(Box q)
    {
        ++queries;
        List<aug> list = world.a(entity, new aug(q.minX, q.minY, q.minZ, q.maxX, q.maxY, q.maxZ));
        List<Box> out = new ArrayList<Box>(list.size());
        for (aug b : list) out.add(new Box(b.a, b.b, b.c, b.d, b.e, b.f));
        return out;
    }

    @Override
    public float slipperiness(int x, int y, int z)
    {
        return world.p(new cj(x, y, z)).c().L;
    }

    private static aug aabb(Box b)
    {
        return new aug(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
    }

    /**
     * Vanilla's own adm.handleMaterialAcceleration on the player, so flowing water, Forge's
     * isEntityInsideMaterial hooks and modded fluids behave exactly as in vanilla. It adds the current to the
     * entity's motion; the motion is zeroed before and restored after, and the added part is returned.
     */
    @Override
    public double[] waterPush(Box bb)
    {
        double mx = entity.v, my = entity.w, mz = entity.x;
        entity.v = entity.w = entity.x = 0.0D;
        try
        {
            boolean in = world.a(aabb(bb), arm.h, entity);
            return in ? new double[] {entity.v, entity.w, entity.x} : null;
        }
        finally
        {
            entity.v = mx;
            entity.w = my;
            entity.x = mz;
        }
    }

    @Override
    public boolean lavaIn(Box bb)
    {
        return world.a(aabb(bb), arm.i);
    }

    @Override
    public boolean anyLiquid(Box bb)
    {
        return world.d(aabb(bb));
    }

    /** pk.moveEntity's block1: the block at (posX, posY - 0.2, posZ), or a fence/wall/gate below an air block. */
    @Override
    public int landingBlock(double posX, double posY, double posZ)
    {
        cj pos = new cj(ns.c(posX), ns.c(posY - 0.20000000298023224D), ns.c(posZ));
        afh block = world.p(pos).c();
        // (a fence/wall/gate under an air block is never slime, so the air case needs no lookup below)
        return block instanceof ajn ? LANDING_SLIME : LANDING_DEFAULT;
    }

    /** pk.doBlockCollisions: blocks in the box contracted by 0.001. */
    @Override
    public int[] blockCollisions(Box box)
    {
        int x0 = ns.c(box.minX + 0.001D), y0 = ns.c(box.minY + 0.001D), z0 = ns.c(box.minZ + 0.001D);
        int x1 = ns.c(box.maxX - 0.001D), y1 = ns.c(box.maxY - 0.001D), z1 = ns.c(box.maxZ - 0.001D);
        int sand = 0, web = 0;
        for (int x = x0; x <= x1; ++x)
            for (int y = y0; y <= y1; ++y)
                for (int z = z0; z <= z1; ++z)
                {
                    afh b = world.p(new cj(x, y, z)).c();
                    if (b instanceof ajq) ++sand;
                    else if (b instanceof ako) web = 1;
                }
        return new int[] {sand, web};
    }

    /** ForgeHooks.isLivingOnLadder for this box (both of Forge's ladder modes). */
    @Override
    public boolean onLadder(Box box)
    {
        pr living = (pr)entity;
        if (living instanceof wn && ((wn)living).v()) return false;
        cj pos = new cj(ns.c(box.centerX()), ns.c(box.minY), ns.c(box.centerZ()));
        afh block = world.p(pos).c();
        return block == afi.au || block == afi.bn;
    }
}
