package dolly.input;

/** Owns one polling worker and transfers both relative axes atomically. */
public final class RawMouseInput {
    public interface Source {
        int WAITING = 0, READY = 1, FAILED = -1;
        int poll(float[] axes);
    }
    public interface Factory { Source open(); }

    private final Factory factory;
    private boolean enabled, ready, armed;
    private long generation;
    private double dx, dy;
    private Thread worker;

    public RawMouseInput(Factory factory) { this.factory = factory; }

    public synchronized void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        generation++;
        ready = armed = false;
        dx = dy = 0;
        if (worker != null) worker.interrupt();
        if (value && worker == null) startWorker();
    }

    private void startWorker() {
        final long token = generation;
        worker = new Thread(() -> run(token), "Dolly raw mouse");
        worker.setDaemon(true);
        worker.start();
    }

    private synchronized boolean current(long token) { return enabled && generation == token; }

    private void run(long token) {
        try {
            Source source = factory.open();
            if (source == null) return;
            float[] axes = new float[2];
            while (current(token)) {
                int status = source.poll(axes);
                if (status == Source.FAILED || status == Source.READY
                        && (!Float.isFinite(axes[0]) || !Float.isFinite(axes[1]))) return;
                synchronized (this) {
                    if (!current(token)) return;
                    ready = status == Source.READY;
                    if (!ready) { armed = false; dx = dy = 0; }
                    if (ready && armed) {
                        dx += axes[0]; dy += axes[1];
                    }
                }
                Thread.sleep(1L);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException | LinkageError unavailable) {
            // A missing native backend or disconnected device keeps LWJGL in charge.
        } finally {
            synchronized (this) {
                worker = null;
                if (generation == token) { ready = armed = false; dx = dy = 0; }
                else if (enabled) startWorker();
            }
        }
    }

    /** False means use the already sampled LWJGL deltas, including during handoff. */
    public synchronized boolean drain(int[] axes) {
        if (!ready) return false;
        if (!armed) {
            armed = true;
            dx = dy = 0;
            return false;
        }
        axes[0] = (int) dx; axes[1] = (int) dy;
        dx -= axes[0]; dy -= axes[1];
        return true;
    }
}
