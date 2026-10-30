import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;

/** Offline pixel/PNG and resolver regressions: no GPU, DNS request or game connection. */
public final class DollyTechnicalLoadingTest {
    public static void main(String[] args) throws Exception {
        screenshots(); screenshotQueue(); resolver(); language();
        if (args.length > 0) patches(Paths.get(args[0]));
        System.out.println("Technical loading passed: screenshot pixel ownership/crop/PNG and offline SRV cache/fallback/validation.");
    }
    private static void screenshots() throws Exception {
        Path directory = Files.createTempDirectory("dolly-screenshots-");
        try {
            int red = 0xFF0000, green = 0x00FF00, blue = 0x0000FF, white = 0xFFFFFF;
            int[] first = {red, green, 0, blue, white, 0, 0, 0, 0};
            File file = DollyScreenshots.save(directory.toFile(), first, 3, 3, 2, 2);
            BufferedImage image = ImageIO.read(file);
            assert image.getWidth() == 2 && image.getHeight() == 2;
            assert (image.getRGB(0, 0) & 0xFFFFFF) == blue;
            assert (image.getRGB(1, 0) & 0xFFFFFF) == white;
            assert (image.getRGB(0, 1) & 0xFFFFFF) == red;
            assert (image.getRGB(1, 1) & 0xFFFFFF) == green;
            File second = DollyScreenshots.save(directory.toFile(), new int[] {green}, 1, 1, 1, 1);
            assert !file.equals(second) && file.isFile() && second.isFile();
            assert (ImageIO.read(file).getRGB(0, 0) & 0xFFFFFF) == blue : "Another capture changed its snapshot";
            Files.write(directory.resolve("blocked"), new byte[] {1});
            try { DollyScreenshots.save(directory.resolve("blocked").toFile(), first, 3, 3, 2, 2); throw new AssertionError("Write error hidden"); }
            catch (java.io.IOException expected) { }
        } finally {
            try (java.util.stream.Stream<Path> files = Files.walk(directory)) {
                for (Path file : (Iterable<Path>) files.sorted(Comparator.reverseOrder())::iterator) Files.delete(file);
            }
        }
    }
    private static void screenshotQueue() throws Exception {
        java.util.concurrent.ThreadPoolExecutor writer = DollyScreenshots.newWriter();
        java.util.concurrent.CountDownLatch started = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        AtomicInteger written = new AtomicInteger();
        try {
            writer.execute(() -> {
                started.countDown();
                try { release.await(); written.incrementAndGet(); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            });
            assert started.await(2, java.util.concurrent.TimeUnit.SECONDS);
            writer.execute(written::incrementAndGet); writer.execute(written::incrementAndGet);
            try { writer.execute(written::incrementAndGet); throw new AssertionError("Screenshot queue must be bounded"); }
            catch (java.util.concurrent.RejectedExecutionException expected) { }
            assert !DollyScreenshots.drain(writer, 1) : "Blocked worker should time out";
            assert writer.getQueue().size() == 2 : "Shutdown discarded accepted captures";
            release.countDown();
            assert DollyScreenshots.drain(writer, 2000) && written.get() == 3 : "Accepted captures were not drained";
        } finally { release.countDown(); writer.shutdown(); writer.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS); }
    }

    public static class Minecraft extends ave {
        public int reloads;
        public bns language;
        public bni resources;
        Minecraft() { super(null); }
        public bns S() { return language; }
        public bni Q() { return resources; }
        public void e() { reloads++; }
    }
    public static class BrokenLanguage extends bns {
        BrokenLanguage() { super(null, "en_US"); }
        public void a(bni resources) { throw new IllegalStateException("Resource reload failed"); }
    }
    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        java.lang.reflect.Field field = unsafe.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }
    private static void language() throws Exception {
        Minecraft mc = allocate(Minecraft.class);
        boolean before = DollySettings.config.fastLanguageSwitch;
        try {
            DollySettings.config.fastLanguageSwitch = false;
            DollyLanguage.reload(mc); assert mc.reloads == 1;
            DollySettings.config.fastLanguageSwitch = true;
            DollyLanguage.reload(mc); assert mc.reloads == 2 : "Missing resource manager must use full reload";
            mc.language = allocate(BrokenLanguage.class);
            mc.resources = (bni) java.lang.reflect.Proxy.newProxyInstance(bni.class.getClassLoader(), new Class<?>[] {bni.class},
                    (proxy, method, arguments) -> { throw new AssertionError("Resources must be handled by language manager"); });
            DollyLanguage.reload(mc); assert mc.reloads == 3 : "Failed quick reload must fall back";
        } finally { DollySettings.config.fastLanguageSwitch = before; }
    }
    private static void patches(Path jarPath) throws Exception {
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath.toFile())) {
            String[][] expected = {{"ave", "DollyScreenshots", "capture"}, {"axl$a", "DollyLanguage", "reload"},
                    {"bdd", "DollyServerAddress", "resolve"}};
            for (String[] target : expected) {
                org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
                try (java.io.InputStream in = jar.getInputStream(jar.getJarEntry(target[0] + ".class"))) {
                    new org.objectweb.asm.ClassReader(in).accept(node, 0);
                }
                int calls = 0, flags = 0, shutdowns = 0;
                for (org.objectweb.asm.tree.MethodNode method : node.methods)
                    for (org.objectweb.asm.tree.AbstractInsnNode instruction : method.instructions.toArray()) {
                        if (!(instruction instanceof org.objectweb.asm.tree.MethodInsnNode)) continue;
                        org.objectweb.asm.tree.MethodInsnNode call = (org.objectweb.asm.tree.MethodInsnNode) instruction;
                        if (call.owner.equals(target[1]) && call.name.equals(target[2])) calls++;
                        if (call.owner.equals("DollyScreenshots") && call.name.equals("shutdownScreenshots")) shutdowns++;
                        if (method.name.equals("a") && method.desc.equals("(IZII)V") && call.owner.equals("avn") && call.desc.equals("(Z)V")) flags++;
                    }
                assert calls == 1 : "Missing technical loading hook: " + target[0];
                if (target[0].equals("ave")) assert shutdowns == 1 : "Screenshot writer must drain before game shutdown";
                if (target[0].equals("axl$a")) assert flags == 2 : "Language screen lost Unicode/Bidi updates";
            }
        }
    }

    private static void resolver() throws Exception {
        AtomicInteger queries = new AtomicInteger();
        DollyServerAddress.Lookup lookup = (host, alternate) -> {
            queries.incrementAndGet();
            assert host.equals("cache.example");
            return Arrays.asList("0 0 25566 target.example.");
        };
        String[] result = DollyServerAddress.resolve("cache.example", true, false, 0, lookup);
        assert result[0].equals("target.example") && result[1].equals("25566");
        result[0] = "mutated";
        assert DollyServerAddress.resolve("cache.example", true, false, 1, lookup)[0].equals("target.example");
        assert queries.get() == 1;
        DollyServerAddress.resolve("cache.example", true, false, 60_000_000_001L, lookup);
        assert queries.get() == 2 : "SRV cache did not expire";
        AtomicInteger alternateCalls = new AtomicInteger();
        DollyServerAddress.Lookup failure = (host, alternate) -> {
            alternateCalls.incrementAndGet();
            if (alternate) throw new java.io.IOException("offline");
            return Arrays.asList("0 0 25567 fallback.example");
        };
        assert DollyServerAddress.resolve("fallback.example", true, true, 0, failure)[1].equals("25567");
        assert alternateCalls.get() == 2;
        assert DollyServerAddress.resolve("no-fallback.example", true, false, 0, failure)[0].equals("no-fallback.example");
        assert alternateCalls.get() == 3;
        DollyServerAddress.Lookup never = (host, alternate) -> { throw new AssertionError("Literal or invalid host queried"); };
        assert DollyServerAddress.resolve("127.0.0.1", true, true, 0, never)[0].equals("127.0.0.1");
        assert DollyServerAddress.resolve("::1", true, true, 0, never)[0].equals("::1");
        assert DollyServerAddress.resolve("invalid host", true, true, 0, never)[0].equals("invalid host");
        String[] record = DollyServerAddress.select(Arrays.asList("broken", "0 1 0 bad.example", "2 0 25565 secondary.example", "1 0 25566 primary.example."));
        assert record[0].equals("primary.example") && record[1].equals("25566");
        assert DollyServerAddress.select(Arrays.asList("0 0 25565 .", "0 0 65536 bad.example")) == null;
        java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(8);
        try {
            List<java.util.concurrent.Future<String[]>> results = new ArrayList<java.util.concurrent.Future<String[]>>();
            for (int i = 0; i < 32; i++) results.add(workers.submit(() -> DollyServerAddress.resolve("concurrent.example", true, false, 0,
                    (host, alternate) -> Arrays.asList("0 0 25568 shared.example."))));
            for (java.util.concurrent.Future<String[]> future : results) {
                String[] address = future.get(2, java.util.concurrent.TimeUnit.SECONDS);
                assert address[0].equals("shared.example") && address[1].equals("25568");
                address[0] = "changed";
            }
        } finally { workers.shutdownNow(); }
        assert DollyServerAddress.resolve("cache.example", false, false, 1,
                (host, alternate) -> { assert !alternate; return Arrays.asList("0 0 25569 system.example."); })[0].equals("system.example")
                : "DNS cache must isolate resolver configurations";
    }
}
