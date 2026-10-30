import dolly.tactile.ServerProfiles;
import dolly.tactile.ServerProfiles.Profile;
import dolly.tactile.ServerProfiles.Mode;
import java.io.IOException;
import java.util.*;

/** Selects the server policy; vanilla/input profiles never simulate between ticks. */
public final class DollyTactile {
    private static final DollyTactileConfig config = new DollyTactileConfig();
    private static DollyTactileEngine engine;
    private static String applied, failure;
    private static Profile profile = new Profile();
    private static Mode mode = Mode.VANILLA;
    private static List<String> overlay = Collections.emptyList();

    public static String server() {
        ave mc = ave.A();
        if (mc == null || mc.f == null) return ServerProfiles.DEFAULT;
        if (mc.F()) return ServerProfiles.SINGLEPLAYER;
        bde data = mc.D();
        return data == null ? null : ServerProfiles.key(data.b);
    }
    static Map<String, Profile> profiles() {
        try { return ServerProfiles.parse(DollySettings.config.tactileProfiles); }
        catch (IOException e) { throw new IllegalStateException("Invalid saved TACTILE profiles", e); }
    }
    static void save(String key, Profile profile) {
        Map<String, Profile> profiles = profiles();
        if (profile == null) profiles.remove(key); else profiles.put(key, profile.copy());
        try { DollySettings.config.tactileProfiles = ServerProfiles.encode(profiles); }
        catch (IOException e) { throw new IllegalArgumentException(e); }
        DollySettings.persist(ave.A().t);
    }
    private static void sync() {
        String key = server();
        String signature = key + "\n" + DollySettings.config.tactileProfiles;
        if (signature.equals(applied)) return;
        profile = key == null ? new Profile() : ServerProfiles.resolve(profiles(), key);
        mode = ServerProfiles.effectiveMode(key, profile);
        config.enabled = mode == Mode.EXACT || mode == Mode.STEPPED;
        config.mode = config.enabled ? DollyTactileConfig.Mode.valueOf(mode.name()) : DollyTactileConfig.Mode.VANILLA;
        config.physicsRateHz = profile.physics; config.stepRateHz = profile.steps; config.pollRateHz = profile.polling;
        config.inputSource = DollyTactileConfig.InputSource.valueOf(profile.source.name());
        config.pressLatch = profile.latch; config.overlay = DollyTactileConfig.Overlay.values()[profile.overlay];
        config.generation++;
        applied = signature;
    }
    public static void tickStart() {
        sync();
        if (engine == null && config.enabled && failure == null && ave.A().h != null) {
            try { engine = new DollyTactileEngine(config); DollyTactileHooks.setHandler(engine); }
            catch (Throwable e) { failure = "Falha ao iniciar; movimento vanilla até reiniciar"; e.printStackTrace(); }
        }
        if (engine != null) engine.onClientTick(true);
    }
    public static void tickEnd() {
        if (engine != null) engine.onClientTick(false);
        if (profile.overlay == 0) { overlay = Collections.emptyList(); return; }
        List<String> lines = new ArrayList<String>();
        lines.add(status());
        if (profile.overlay == 2 && engine != null && config.enabled) lines.addAll(engine.diag.lines(engine));
        overlay = lines;
    }
    public static void frameStart(float partial) {
        if (engine != null && config.enabled) engine.onRenderTick(true, partial);
    }
    public static void frameEnd() {
        if (engine != null) engine.onRenderTick(false, 0);
    }
    static String status() {
        if (failure != null) return "§cTACTILE: " + failure;
        if (mode == Mode.INPUT) return "§aTACTILE: entrada antecipada · física vanilla (20 Hz)";
        if (mode == Mode.VANILLA) return "§7TACTILE: movimento vanilla (20 Hz)";
        return engine == null ? "§7TACTILE: aguardando mundo" : engine.statusLine();
    }
    public static void renderHud() {
        ave mc = ave.A();
        if (mc.h == null || mc.t.aC || overlay.isEmpty()) return;
        avr screen = new avr(mc); int y = 4;
        for (String line : overlay) {
            String text = mc.k.a(line, Math.max(20, screen.a() - 8));
            mc.k.a(text, Math.max(4, screen.a() - 4 - mc.k.a(text)), y, 0xFFFFFFFF);
            y += 11;
        }
    }
    public static void shutdown() {
        if (engine != null) engine.shutdown();
        DollyTactileHooks.setHandler(null);
    }
}
