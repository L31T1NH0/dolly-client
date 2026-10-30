// Adapted from TACTILE (fallingsword), commit 66892f1005b3e249240c89a4cf3521c5f73e6cd8. MIT; see licenses/TACTILE.txt in the client JAR.


/**
 * Target of the redirected call in pr.onLivingUpdate. It is side-neutral and runs for
 * every living entity on both the client and the integrated server. Unless the installed handler
 * claims the call, it makes exactly the original virtual call.
 */
public final class DollyTactileHooks
{
    public interface MoveHandler
    {
        /** Return true if this call was handled and the vanilla move must not run. */
        boolean handleMove(pr entity, float strafe, float forward);
    }

    private static volatile MoveHandler handler;

    private DollyTactileHooks()
    {
    }

    public static void setHandler(MoveHandler h)
    {
        handler = h;
    }

    public static void move(pr entity, float strafe, float forward)
    {
        MoveHandler h = handler;
        if (h != null && h.handleMove(entity, strafe, forward)) return;
        entity.g(strafe, forward);
    }
}
