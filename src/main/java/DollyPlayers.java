import java.util.Map;
import java.util.WeakHashMap;

/** Reuses expensive velocity functions while preserving every vanilla animation update. */
public final class DollyPlayers {
    private static final Map<bex, double[]> values = new WeakHashMap<bex, double[]>();

    private static double[] state(bex player) {
        double[] state = values.get(player);
        if (state == null) {
            state = new double[6];
            values.put(player, state);
        }
        return state;
    }
    public static float speed(double squared, bex player) {
        if (!DollySettings.config.optimizedPlayerUpdating || !Double.isFinite(squared)) return ns.a(squared);
        double[] state = state(player);
        if (state[4] == 0 || Double.doubleToRawLongBits(state[0]) != Double.doubleToRawLongBits(squared)) {
            state[0] = squared; state[1] = ns.a(squared); state[4] = 1;
        }
        return (float) state[1];
    }
    public static double angle(double motion, bex player) {
        if (!DollySettings.config.optimizedPlayerUpdating || !Double.isFinite(motion)) return Math.atan(motion);
        double[] state = state(player);
        if (state[5] == 0 || Double.doubleToRawLongBits(state[2]) != Double.doubleToRawLongBits(motion)) {
            state[2] = motion; state[3] = Math.atan(motion); state[5] = 1;
        }
        return state[3];
    }
    public static void clear() { values.clear(); }
}
