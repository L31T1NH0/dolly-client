import java.nio.FloatBuffer;
import java.util.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Bounded, per-font display lists; native OptiFine glyphs/formatting remain the source of truth. */
public final class DollyRenderedText {
    private static final int LIMIT = 512;
    private static final Map<avn, FontCache> fonts = new IdentityHashMap<avn, FontCache>();
    private static final FloatBuffer color = BufferUtils.createFloatBuffer(16); // LWJGL 2 requires 16 slots even for a four-component color.
    private static boolean drawing;
    private static final float[] scratch = new float[15];
    private static final Key lookup = new Key("", false, scratch);

    static boolean cacheable(String text) {
        if (text == null || text.isEmpty() || text.length() > 256) return false;
        for (int i = 0; i + 1 < text.length(); i++)
            if (text.charAt(i) == '§' && (text.charAt(i + 1) == 'k' || text.charAt(i + 1) == 'K')) return false;
        return true;
    }
    public static void draw(avn font, String text, boolean shadow) {
        DollyFontAccess access = (DollyFontAccess) font;
        if (!DollySettings.config.optimizedText || font.getClass() != avn.class || drawing
                || !cacheable(text) || Config.isShaders() || GL11.glGetInteger(GL11.GL_LIST_INDEX) != 0) {
            access.dolly$drawText(text, shadow); return;
        }
        float[] before = scratch;
        access.dolly$readFontState(before);
        // Obfuscated style can carry across the shadow/main passes even without §k in this fragment.
        if (before[7] != 0) { access.dolly$drawText(text, shadow); return; }
        lookup.set(text, shadow, before);
        FontCache fontCache = fonts.get(font);
        if (fontCache == null) { fontCache = new FontCache(); fonts.put(font, fontCache); }
        LinkedHashMap<Key, Entry> cache = fontCache.entries;
        Entry entry = cache.get(lookup);
        if (entry == null && !fontCache.admission.admit(lookup)) {
            access.dolly$drawText(text, shadow); return;
        }
        float x = before[0], y = before[1];
        bfl.E(); bfl.b(x, y, 0f);
        try {
            if (entry == null) {
                Key key = new Key(text, shadow, before);
                int list = GL11.glGenLists(1);
                if (list == 0) { bfl.b(-x, -y, 0f); access.dolly$drawText(text, shadow); return; }
                boolean complete = false;
                before[0] = before[1] = 0;
                access.dolly$fontState(before);
                drawing = true;
                GL11.glNewList(list, GL11.GL_COMPILE_AND_EXECUTE);
                try {
                    // State-manager shortcuts must not make a list depend on the previous string.
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, bfl.getBoundTexture());
                    GL11.glColor4f(before[2], before[3], before[4], before[5]);
                    access.dolly$drawText(text, shadow);
                    complete = true;
                } finally {
                    GL11.glEndList(); drawing = false;
                    if (!complete) GL11.glDeleteLists(list, 1);
                }
                float[] after = access.dolly$fontState();
                color.clear(); GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
                entry = new Entry(list, after, bfl.getBoundTexture(), new float[] {color.get(0), color.get(1), color.get(2), color.get(3)});
                if (cache.size() >= LIMIT) {
                    Iterator<Entry> oldest = cache.values().iterator();
                    GL11.glDeleteLists(oldest.next().list, 1); oldest.remove();
                }
                cache.put(key, entry);
            } else {
                GL11.glCallList(entry.list);
                // Playback bypasses Java's GL state cache: restore both bookkeeping and actual state.
                bfl.i(entry.texture);
                bfl.G(); bfl.c(entry.color[0], entry.color[1], entry.color[2], entry.color[3]);
            }
            System.arraycopy(entry.state, 0, scratch, 0, scratch.length);
            scratch[0] += x; scratch[1] += y;
            access.dolly$fontState(scratch);
        } finally { bfl.F(); }
    }
    public static void invalidate(avn font) {
        FontCache cache = fonts.remove(font);
        if (cache != null) for (Entry entry : cache.entries.values()) GL11.glDeleteLists(entry.list, 1);
    }
    public static void clear() {
        for (FontCache cache : fonts.values()) for (Entry entry : cache.entries.values()) GL11.glDeleteLists(entry.list, 1);
        fonts.clear();
    }
    private static final class FontCache {
        final LinkedHashMap<Key, Entry> entries = new LinkedHashMap<Key, Entry>(16, .75f, true);
        final Admission admission = new Admission();
    }
    static final class Admission {
        private final Set<Key> pending = new LinkedHashSet<Key>();
        boolean admit(Key key) {
            if (pending.remove(key)) return true;
            if (pending.size() >= LIMIT) {
                Iterator<Key> oldest = pending.iterator(); oldest.next(); oldest.remove();
            }
            // The lookup key is mutable and must never be retained by a hash table.
            pending.add(new Key(key));
            return false;
        }
    }
    private static final class Entry {
        final int list, texture;
        final float[] state, color;
        Entry(int list, float[] state, int texture, float[] color) { this.list = list; this.state = state; this.texture = texture; this.color = color; }
    }
    static final class Key {
        String text;
        boolean shadow;
        final float[] state;
        Key(Key key) { text = key.text; shadow = key.shadow; state = key.state.clone(); }
        Key(String text, boolean shadow, float[] state) {
            this.state = new float[state.length - 2]; set(text, shadow, state);
        }
        void set(String text, boolean shadow, float[] state) {
            this.text = text; this.shadow = shadow;
            System.arraycopy(state, 2, this.state, 0, this.state.length);
        }
        @Override public int hashCode() { return 31 * (31 * text.hashCode() + (shadow ? 1 : 0)) + Arrays.hashCode(state); }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Key)) return false;
            Key key = (Key) other;
            return shadow == key.shadow && text.equals(key.text) && Arrays.equals(state, key.state);
        }
    }
}
