import dolly.input.SlotProfile;

public final class DollySlotProfileTest {
    public static void run() {
        for (int mask = 0; mask < 511; mask++) for (int current = 0; current < 9; current++) {
            for (int direction : new int[] {-1, 1}) {
                int next = SlotProfile.next(mask, current, direction);
                assert next >= 0 && next < 9 && !SlotProfile.blocked(mask, next);
            }
            for (int requested = 0; requested < 9; requested++) {
                int selected = SlotProfile.select(mask, current, requested);
                assert !SlotProfile.blocked(mask, selected);
                if (!SlotProfile.blocked(mask, requested)) assert selected == requested;
                else if (!SlotProfile.blocked(mask, current)) assert selected == current;
            }
        }
        assert SlotProfile.next(0, 0, -1) == 8 && SlotProfile.next(0, 8, 1) == 0;
        assert SlotProfile.next(511, 3, 1) == 3 : "Defensive fallback must not loop forever";
        assert SlotProfile.select(1, 0, 0) == 1;
    }
}
