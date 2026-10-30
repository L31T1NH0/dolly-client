/** Existing Minecraft item icons, resolved once after the item registry is initialized. */
public final class DollyIcons {
    private static final String[] ITEMS = {"ender_eye", "compass", "leather_boots", "tripwire_hook", "chest",
            "redstone", "tnt", "sign", "iron_sword", "glowstone_dust", "clock", "firework_charge",
            "stone", "blaze_rod", "arrow", "book", "slime_ball", "water_bucket", "flint_and_steel"};
    private static final zx[] STACKS = new zx[ITEMS.length];
    static void draw(int icon, int x, int y) {
        int index = Math.floorMod(icon, ITEMS.length);
        if (STACKS[index] == null) STACKS[index] = new zx(zw.d(ITEMS[index]));
        bfl.E();
        try {
            avb.c();
            ave.A().ag().b(STACKS[index], x, y);
        } finally {
            avb.a(); bfl.k(); bfl.f(); bfl.F(); bfl.c(1, 1, 1, 1);
        }
    }
}
