package dolly.input;

/** Hotbar slots are zero-based internally; one bit marks each blocked slot. */
public final class SlotProfile {
    public static boolean blocked(int mask, int slot) { return slot >= 0 && slot < 9 && (mask & (1 << slot)) != 0; }
    public static int next(int mask, int current, int direction) {
        int slot = Math.floorMod(current, 9);
        for (int attempt = 0; attempt < 9; attempt++) {
            slot = Math.floorMod(slot + direction, 9);
            if (!blocked(mask, slot)) return slot;
        }
        return Math.floorMod(current, 9);
    }
    public static int select(int mask, int current, int requested) {
        if (requested >= 0 && requested < 9 && !blocked(mask, requested)) return requested;
        return blocked(mask, current) || current < 0 || current > 8 ? next(mask, current, 1) : current;
    }
}
