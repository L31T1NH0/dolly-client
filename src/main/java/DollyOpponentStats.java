import java.util.Collection;
import java.util.UUID;

/** The server owns /stats; only an unambiguous two-player roster can supply its target. */
public final class DollyOpponentStats {
    static String command(Collection<bdc> players, UUID self) {
        if (players == null || self == null || players.size() != 2) return null;
        String enemy = null;
        boolean present = false;
        for (bdc player : players) {
            if (player.b() == adp.a.d) return null;
            if (self.equals(player.a().getId())) present = true;
            else enemy = player.a().getName();
        }
        return present && enemy != null && enemy.matches("[A-Za-z0-9_]{1,16}") ? "/stats " + enemy : null;
    }

    public static void open() {
        ave mc = ave.A();
        if (mc.h == null || mc.f == null || mc.h.a == null || mc.m != null) return;
        String command = command(mc.h.a.d(), mc.h.a.e().getId());
        if (command != null) mc.h.e(command);
    }
}
