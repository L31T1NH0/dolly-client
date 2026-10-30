import java.nio.file.*;
import java.util.*;

/** Optional headless calibration; this does not measure in-game FPS or driver overhead. */
public final class DollyProfilerBenchmark {
    private static volatile long sink;
    private static final int WORK = DollyProfiler.id("benchmark.hot"), PHASE = DollyProfiler.id("benchmark.phase");
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("dolly-profiler-benchmark-");
        DollyProfiler.directory(root.toFile());
        try {
            for (String mode : new String[] {"OFF", "NORMAL", "DEEP"}) {
                long[] times = new long[5];
                for (int round = 0; round < times.length; round++) {
                    DollyProfiler.Session session = null;
                    if (!mode.equals("OFF")) {
                        if (!DollyProfiler.start(DollyProfiler.Mode.valueOf(mode))) throw new IllegalStateException(DollyProfiler.status);
                        session = DollyProfiler.session();
                    }
                    long value = 1, start = System.nanoTime();
                    for (int frame = 0; frame < 400; frame++) {
                        long f = DollyProfiler.frameBegin(), phase = DollyProfiler.begin(PHASE, false);
                        for (int i = 0; i < 256; i++) {
                            long token = DollyProfiler.begin(WORK, true);
                            value = (value ^ (value << 13)) + i;
                            DollyProfiler.end(token);
                        }
                        DollyProfiler.end(phase); DollyProfiler.frameEnd(f);
                    }
                    times[round] = System.nanoTime() - start; sink = value;
                    if (session != null) {
                        DollyProfiler.stop(); session.writer.join(5000);
                        if (session.writer.isAlive()) throw new IllegalStateException("Writer did not finish");
                    }
                }
                long[] measured = Arrays.copyOfRange(times, 2, 5); Arrays.sort(measured);
                System.out.printf(Locale.ROOT, "%s: %.1f ns/hot-call (median of 3; 2 warmups; 256 hot calls/frame)%n", mode, measured[1] / (400.0 * 256));
            }
        } finally {
            DollyProfiler.stop();
            try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
                for (Path p : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(p);
            }
        }
    }
}
