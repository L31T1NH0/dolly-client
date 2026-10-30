/** World/resource transitions and chunk-worker pacing. */
public final class DollyResources {
    private static volatile int chunkDelay;
    private static long cleanupAt;
    private static final int[] DELAYS = {0, 15, 50, 110, 150, 200};

    public static void sync() { chunkDelay = delay(DollySettings.config.lazyChunkLoading); }
    static int delay(int level) { return DELAYS[Math.max(0, Math.min(5, level))]; }
    public static void paceChunks() throws InterruptedException {
        int delay = chunkDelay;
        if (delay > 0) Thread.sleep(delay);
    }
    public static axu loadingScreen(axu screen) {
        return DollySettings.config.fastWorldLoading && screen instanceof axs ? null : screen;
    }
    public static void loadingMessage(avi loading, String message) {
        if (!DollySettings.config.fastWorldLoading) loading.b(message);
    }
    public static void loadingProgress(avi loading, String message) {
        if (!DollySettings.config.fastWorldLoading) loading.c(message);
    }
    public static void collect() {
        if (!DollySettings.config.fastWorldLoading) System.gc();
    }
    public static void worldChanging(bdb next) {
        ave mc = ave.A();
        cleanupAt = next == null && mc.f != null && DollySettings.config.memoryCleanup
                ? System.nanoTime() + 1_000_000_000L : 0;
        DollyLighting.invalidate();
        DollyRenderedText.clear();
        DollyPlayers.clear();
        DollyScoreboard.clear();
        DollyHudCache.release();
    }
    public static void tick() {
        if (!DollySettings.config.memoryCleanup) { cleanupAt = 0; DollyTextureCleanup.clear(); return; }
        ave mc = ave.A();
        if (cleanupAt != 0 && System.nanoTime() >= cleanupAt) {
            cleanupAt = 0;
            if (mc.f == null && mc.P() != null && !Config.isShaders()
                    && DollyTextureCleanup.cleanup(mc.P(), System.nanoTime()) > 0) {
                // OptiFine resource listeners may hold raw texture IDs (for example custom skies).
                mc.e();
            }
        }
    }
    public static void reloaded() {
        DollyLighting.invalidate();
        DollyRenderedText.clear();
        DollyPlayers.clear();
        DollyScoreboard.clear();
        DollyHudCache.release();
        DollyTextureCleanup.clear();
    }
}
