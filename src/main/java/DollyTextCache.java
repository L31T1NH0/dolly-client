import java.util.*;

/** Widths belong to a font's current metrics, never to a resource pack globally. */
public final class DollyTextCache {
    private static final Map<avn, Map<String, Integer>> caches = new WeakHashMap<avn, Map<String, Integer>>();
    private static final int LIMIT = 5000;

    public static synchronized Integer lookup(avn font, String text) {
        if (!DollySettings.config.stringWidthCaching || text == null) return null;
        Map<String, Integer> cache = caches.get(font);
        return cache == null ? null : cache.get(text);
    }

    public static synchronized int remember(int width, avn font, String text) {
        if (!DollySettings.config.stringWidthCaching || text == null) return width;
        Map<String, Integer> cache = caches.get(font);
        if (cache == null) { cache = new HashMap<String, Integer>(); caches.put(font, cache); }
        if (cache.size() >= LIMIT && !cache.containsKey(text)) cache.clear();
        cache.put(text, width);
        return width;
    }

    public static synchronized void invalidate(avn font) { caches.remove(font); DollyScoreboard.clear(); }
    public static synchronized void clear() { caches.clear(); DollyScoreboard.clear(); }
}
