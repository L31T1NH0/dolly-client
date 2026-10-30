/** Read game-owned counters only on the render thread, never on the log writer. */
public final class DollyProfilerGame {
    public static void capture() {
        if (!DollyProfiler.recording) return;
        ave mc = ave.A();
        boolean world = mc.f != null && mc.g != null;
        DollyProfiler.scene(world ? mc.f.f.size() : 0,
                world ? mc.g.getCountActiveRenderers() : 0,
                world ? mc.g.getCountChunksToUpdate() : 0,
                world ? mc.g.getCountEntitiesRendered() : 0,
                world ? mc.g.getCountTileEntitiesRendered() : 0,
                world && mc.j != null ? Integer.parseInt(mc.j.b()) : 0);
    }
}
