public final class DollyTabPing {
    public static boolean draw(int width, int x, int y, bdc player) {
        if (!DollySettings.config.tabPing) return false;
        int ping = player.c();
        String text = ping < 0 ? "?" : Integer.toString(ping);
        int color = ping < 0 ? 0xAAAAAA : ping < 150 ? 0x55FF55 : ping < 300 ? 0xFFFF55 : 0xFF5555;
        // Fit in the vanilla ping column without changing player-name or score layout.
        bfl.E();
        try {
            bfl.b(x + width - 1, y + 1, 0);
            float scale = Math.min(.5f, 10f / Math.max(1, ave.A().k.a(text))); bfl.a(scale, scale, 1f);
            ave.A().k.a(text, -ave.A().k.a(text), 0, color);
        } finally { bfl.F(); }
        return true;
    }
}
