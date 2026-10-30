// Adapted from TACTILE (fallingsword), commit 66892f1005b3e249240c89a4cf3521c5f73e6cd8. MIT; see licenses/TACTILE.txt in the client JAR.

import dolly.tactile.core.TickInput;


/**
 * Replaces EntityPlayerSP.b while high-resolution mode controls the player. Vanilla's tick
 * still reads movementInput: av() (d packets, rendering), sprint rules and the c check.
 * This reports the engine's input at the tick boundary, so packets carry the same d state the
 * physics used. It reports c as never pressed, so vanilla's once-per-tick c never fires (the
 * engine applies jumps per substep). Any vanilla sprint/timer mutations made from these values are
 * undone by the move hook, which restores the engine's state.
 */
final class DollyTactileMovement extends beu
{
    private TickInput current;

    void c(TickInput in)
    {
        current = in;
    }

    @Override
    public void a()
    {
        if (current == null)
        {
            a = 0.0F;
            b = 0.0F;
            c = false;
            d = false;
            return;
        }
        a = current.moveStrafe();
        b = current.moveForward();
        c = false;
        d = current.sneak;
    }
}
