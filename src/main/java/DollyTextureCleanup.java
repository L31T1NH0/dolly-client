import java.lang.reflect.Field;
import java.util.*;

/** Only evicts reloadable, manager-owned static textures; never downloaded skins or dynamic textures. */
public final class DollyTextureCleanup {
    private static final Map<bmj, LinkedHashMap<jy, Long>> used = new WeakHashMap<bmj, LinkedHashMap<jy, Long>>();
    private static final Field TEXTURES;
    static {
        try { TEXTURES = bmj.class.getDeclaredField("b"); TEXTURES.setAccessible(true); }
        catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }
    public static void bound(bmj manager, jy location) {
        if (!DollySettings.config.memoryCleanup || location == null) return;
        record(manager, location, System.nanoTime());
    }
    static void record(bmj manager, jy location, long now) {
        LinkedHashMap<jy, Long> times = used.get(manager);
        if (times == null) { times = new LinkedHashMap<jy, Long>(16, .75f, true); used.put(manager, times); }
        times.put(location, now);
        if (times.size() > 8192) times.remove(times.keySet().iterator().next());
    }
    @SuppressWarnings("unchecked")
    static int cleanup(bmj manager, long now) {
        LinkedHashMap<jy, Long> times = used.get(manager);
        if (times == null) return 0;
        int released = 0;
        try {
            Map<jy, bmk> textures = (Map<jy, bmk>) TEXTURES.get(manager);
            IdentityHashMap<bmk, Integer> owners = new IdentityHashMap<bmk, Integer>();
            for (bmk texture : textures.values()) owners.put(texture, owners.containsKey(texture) ? owners.get(texture) + 1 : 1);
            for (Iterator<Map.Entry<jy, Long>> it = times.entrySet().iterator(); it.hasNext();) {
                Map.Entry<jy, Long> entry = it.next();
                if (now - entry.getValue() < 30L * 60 * 1_000_000_000L) continue;
                bmk texture = textures.get(entry.getKey());
                if (texture == null) { it.remove(); continue; }
                if (texture.getClass() != bme.class) continue;
                bme simple = (bme) texture;
                if (simple.isEmissive || simple.locationEmissive != null) continue;
                if (owners.get(texture) != 1) continue;
                manager.c(entry.getKey());
                released++;
                it.remove();
            }
        } catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        return released;
    }
    public static void clear() { used.clear(); }
}
