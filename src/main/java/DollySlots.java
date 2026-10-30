import dolly.input.SlotProfile;

/** Local hotbar selection only; inventory contents and screen interactions stay native. */
public final class DollySlots {
    private static boolean active(wm inventory) {
        ave mc = ave.A();
        return DollySettings.config.slotProfile && mc != null && mc.h != null && mc.h.bi == inventory && !mc.h.v();
    }
    public static boolean selectable(wm inventory, int requested) {
        return !active(inventory) || !SlotProfile.blocked(DollySettings.config.blockedSlots, requested);
    }
    public static void select(wm inventory, int requested) {
        inventory.c = active(inventory) ? SlotProfile.select(DollySettings.config.blockedSlots, inventory.c, requested) : requested;
    }
    public static void wheel(wm inventory, int delta) {
        if (!active(inventory)) { inventory.d(delta); return; }
        if (delta != 0) inventory.c = SlotProfile.next(DollySettings.config.blockedSlots, inventory.c, delta > 0 ? -1 : 1);
    }
    public static void ensureSelection() {
        ave mc = ave.A();
        if (mc != null && mc.h != null && mc.h.bi != null) select(mc.h.bi, mc.h.bi.c);
    }
    public static void toggle() {
        DollySettings.config.slotProfile = !DollySettings.config.slotProfile;
        ensureSelection(); DollySettings.persist(ave.A().t);
    }
    public static boolean toggleSlot(int slot) {
        int mask = DollySettings.config.blockedSlots ^ (1 << slot);
        if (mask == 511) return false;
        DollySettings.config.blockedSlots = mask;
        ensureSelection(); DollySettings.persist(ave.A().t);
        return true;
    }
}
