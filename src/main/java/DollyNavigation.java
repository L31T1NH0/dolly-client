public final class DollyNavigation {
    private static bde lastServer;
    public static void remember(bde server) {
        if (server == null) return;
        lastServer = new bde(server.a, server.b, server.d()); lastServer.a(server.b());
    }
    public static void rememberAddress(String host, int port) {
        String address = (host.indexOf(':') >= 0 && !host.startsWith("[") ? "[" + host + "]" : host) + ":" + port;
        remember(new bde(host, address, false));
    }
    static bde server() { return lastServer; }
    public static void pause(axu screen) {
        for (java.util.Iterator<avs> it = screen.n.iterator(); it.hasNext();) {
            avs button = it.next();
            if (button.k == 7) {
                if (DollySettings.config.lanButton == 1) it.remove();
                else if (DollySettings.config.lanButton == 2) { button.j = "Servidores"; button.l = true; }
            }
        }
    }
    private static boolean replay;
    public static boolean pauseClick(axp screen, avs button) {
        if (!button.l || replay) return false;
        if (button.k == 7 && DollySettings.config.lanButton == 2) { ave.A().a(new azh(screen)); return true; }
        if (button.k != 1 || !DollySettings.config.reconnectMenu || ave.A().E() || ave.A().al() || ave.A().D() == null) return false;
        remember(ave.A().D()); final bde target = lastServer;
        ave.A().a(new DollyChoiceScreen(screen, "Desconectar", target.a,
                new String[] {"Sair do servidor", "Reconectar"}, () -> disconnect(screen, button), () -> {
                    ave mc = ave.A(); mc.f.H(); mc.a((bdb) null);
                    mc.a(new awz(new azh(new aya()), mc, target));
                }));
        return true;
    }
    private static void disconnect(axp screen, avs button) {
        replay = true;
        try { screen.a(button); } finally { replay = false; }
    }
    public static boolean quit(aya screen, avs button) {
        if (button.k != 4 || !button.l || !DollySettings.config.confirmQuit) return false;
        ave.A().a(new DollyChoiceScreen(screen, "Fechar o Minecraft?", "Sua sessão do jogo será encerrada.",
                new String[] {"Fechar o jogo"}, () -> ave.A().m())); return true;
    }
    public static void disconnected(axu screen) {
        if (!DollySettings.config.reconnectMenu || lastServer == null || screen.n.isEmpty()) return;
        avs original = screen.n.get(0);
        int top = original.i + 24;
        if (top + 20 > screen.m - 4) { original.i = Math.max(64, screen.m - 52); top = original.i + 24; }
        screen.n.add(new avs(18990, (screen.l - 200) / 2, top, 200, 20, "Reconectar"));
    }
    public static boolean disconnectedClick(axu screen, avs button) {
        if (button.k != 18990 || !button.l || lastServer == null) return false;
        ave mc = ave.A(); mc.a(new awz(new azh(new aya()), mc, lastServer)); return true;
    }
}
