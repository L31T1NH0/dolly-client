import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DollyMaintenance {
    private static final AtomicBoolean scheduled = new AtomicBoolean();
    public static void start(File gameDirectory) {
        if (!DollySettings.config.cleanOldLogs || !scheduled.compareAndSet(false, true)) return;
        final int days = DollySettings.config.logRetentionDays;
        Thread worker = new Thread(() -> {
            try {
                int removed = dolly.maintenance.LogCleanup.prune(gameDirectory.toPath(), days, System.currentTimeMillis());
                if (removed > 0) org.apache.logging.log4j.LogManager.getLogger().info("[Dolly] Removed {} old log archive(s).", removed);
            } catch (Exception error) { org.apache.logging.log4j.LogManager.getLogger().warn("[Dolly] Could not clean old log archives", error); }
        }, "Dolly log cleanup");
        worker.setDaemon(true); worker.start();
    }
}
