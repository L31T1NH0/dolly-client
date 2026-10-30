import dolly.audio.SoundVolumes;
import java.io.IOException;
import java.util.*;

public final class DollyAudio {
    private static volatile Map<String, Integer> volumes = Collections.emptyMap();
    private static volatile Map<jy, Integer> locations = Collections.emptyMap();
    private static final ThreadLocal<Boolean> texturesOnly = new ThreadLocal<Boolean>();
    public static String error, status;

    public static void sync() {
        try { publish(SoundVolumes.parse(DollySettings.config.soundVolumes)); }
        catch (IOException e) { throw new IllegalStateException("Invalid saved sound preferences", e); }
    }
    private static void publish(Map<String, Integer> values) {
        Map<jy, Integer> byLocation = new HashMap<jy, Integer>();
        for (Map.Entry<String, Integer> entry : values.entrySet()) byLocation.put(new jy(entry.getKey()), entry.getValue());
        volumes = Collections.unmodifiableMap(new TreeMap<String, Integer>(values));
        locations = Collections.unmodifiableMap(byLocation);
    }
    public static int percent(String id) { Integer value = volumes.get(id); return value == null ? 100 : value; }
    public static void set(String id, int percent) {
        if (percent < 0 || percent > 200) throw new IllegalArgumentException("Volume must be between 0 and 200");
        try { SoundVolumes.parse(id + "=" + percent); }
        catch (IOException e) { throw new IllegalArgumentException(e); }
        Map<String, Integer> next = new TreeMap<String, Integer>(volumes);
        if (percent == 100) next.remove(id); else next.put(id, percent);
        try { SoundVolumes.parse(SoundVolumes.encode(next)); }
        catch (IOException e) { throw new IllegalArgumentException(e.getMessage(), e); }
        publish(next);
    }
    public static float volume(float original, bpj sound) {
        Integer percent = sound == null ? null : locations.get(sound.a());
        return SoundVolumes.scale(original, percent == null ? 100 : percent);
    }
    public static void reset() { publish(Collections.<String, Integer>emptyMap()); }
    public static void commit() {
        DollySettings.config.soundVolumes = SoundVolumes.encode(volumes);
        DollySettings.persist(ave.A().t);
        try {
            ave mc = ave.A(); bpz handler = mc.W();
            if (handler != null) for (bpg category : bpg.values()) if (category != bpg.a)
                handler.a(category, mc.t.a(category));
        } catch (RuntimeException e) { failed("Atualizar volumes", e); }
    }
    public static List<String> ids() {
        SortedSet<String> ids = new TreeSet<String>(volumes.keySet());
        try {
            bpz handler = ave.A().W();
            if (handler == null) throw new IllegalStateException("O áudio ainda não está disponível");
            for (jy id : ((DollySoundRegistry) handler).dolly$soundIds()) ids.add(id.toString());
        } catch (RuntimeException e) { failed("Carregar lista de sons", e); }
        return new ArrayList<String>(ids);
    }
    public static boolean skipSoundReload() { return Boolean.TRUE.equals(texturesOnly.get()); }
    public static void soundKey(ave mc) { if (DollySettings.config.separateResourceLoading) reloadSounds(mc); else mc.e(); }
    public static void textureKey(ave mc) { if (DollySettings.config.separateResourceLoading) reloadTextures(mc); else mc.e(); }
    public static void reloadSounds(ave mc) {
        error = null;
        try {
            if (mc.W() == null || mc.Q() == null) throw new IllegalStateException("Áudio ou recursos indisponíveis");
            mc.W().a(mc.Q()); status = "Recarga de sons solicitada.";
        } catch (RuntimeException e) { failed("Recarregar sons", e); }
    }
    public static void reloadTextures(ave mc) {
        error = null;
        Boolean previous = texturesOnly.get(); texturesOnly.set(true);
        try { mc.e(); status = "Recursos visuais recarregados."; }
        catch (RuntimeException e) { failed("Recarregar texturas", e); }
        finally { if (previous == null) texturesOnly.remove(); else texturesOnly.set(previous); }
    }
    private static void failed(String action, RuntimeException e) {
        error = action + ": " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        System.err.println("[Dolly áudio] " + error);
    }
}
