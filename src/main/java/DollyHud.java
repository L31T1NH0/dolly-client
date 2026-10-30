/** Entity selection and labels update once per client tick, drawing stays per frame. */
public final class DollyHud {
    private static final String[] tntLabels = new String[8];
    private static int tntCount;
    private static String pingLabel;
    private static Object world, player;
    private static final String[] ping = new String[1], sprint = {"Sprint alternado"}, slots = {"Perfil de slots ativo"};

    public static void tick() {
        ave mc = ave.A();
        world = mc.f;
        player = mc.h;
        pingLabel = null;
        java.util.Arrays.fill(tntLabels, null);
        tntCount = 0;
        if (mc.h == null || mc.t.aC) return;
        if (DollySettings.config.pingDisplay && DollyHudPositions.visible(0) && mc.u() != null) {
            bdc info = mc.u().a(mc.h.aK());
            if (info != null) pingLabel = "Ping: " + info.c() + " ms";
        }
        if (DollySettings.config.tntTimer && DollyHudPositions.visible(1) && mc.f != null) {
            for (int i = 0, size = mc.f.f.size(); i < size; i++) {
                Object object = mc.f.f.get(i);
                if (!(object instanceof vj)) continue;
                vj tnt = (vj) object;
                double dx = mc.h.s - tnt.s, dy = mc.h.t - tnt.t, dz = mc.h.u - tnt.u;
                if (dx * dx + dy * dy + dz * dz > 64 * 64) continue;
                int tenths = Math.max(0, (tnt.a + 1) / 2);
                tntLabels[tntCount++] = "TNT: " + (tenths / 10) + "." + (tenths % 10) + "s";
                if (tntCount == tntLabels.length) break;
            }
        }
    }

    public static void render() {
        DollyScreenshotManager.render();
        DollyTactile.renderHud();
        ave mc = ave.A();
        if (DollyProfiler.recording || DollyProfiler.status.startsWith("Profiler: erro")) mc.k.a(DollyProfiler.status, 4, 40, 0xFFFFCC66);
        if (mc.h == null || mc.t.aC || mc.f != world || mc.h != player) return;
        avr resolution = new avr(mc);
        int y = 4;
        if (DollySettings.config.pingDisplay && pingLabel != null) {
            ping[0] = pingLabel; DollyHudPositions.draw(0, ping, 1, 0xFFFFFFFF, y, resolution); y += 12;
        }
        if (DollySettings.config.tntTimer) { DollyHudPositions.draw(1, tntLabels, tntCount, 0xFFFF7777, y, resolution); y += tntCount * 12; }
        if (DollyInput.sprintLatched() && DollySettings.config.toggleSprint) { DollyHudPositions.draw(2, sprint, 1, 0xFFBFEAD1, y, resolution); y += 12; }
        if (DollySettings.config.slotProfile && DollySettings.config.blockedSlots != 0) DollyHudPositions.draw(3, slots, 1, 0xFFBFEAD1, y, resolution);
    }
}
