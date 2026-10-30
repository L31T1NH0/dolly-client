package dolly.chat;

import java.util.*;

/** Session-only messages; wrapped display lines are owned by the game adapter. */
public final class ChatHistory<T> {
    public static final class Entry<T> {
        public final T value;
        public final String text;
        public final int id;
        public final boolean separator;
        public int repeats = 1;
        public long received;
        Entry(T value, String text, int id, long received, boolean separator) {
            this.value = value; this.text = text; this.id = id; this.received = received; this.separator = separator;
        }
    }
    private final List<Entry<T>> entries = new ArrayList<Entry<T>>();
    public List<Entry<T>> entries() { return Collections.unmodifiableList(entries); }
    public void clear() { entries.clear(); }
    public void remove(int id) { entries.removeIf(e -> e.id == id); }
    public Entry<T> add(T value, String text, int id, long now, boolean group, int limit, boolean separator) {
        return add(value, text, id, now, group, limit, separator, true, Long.MAX_VALUE);
    }
    public Entry<T> add(T value, String text, int id, long now, boolean group, int limit, boolean separator,
                        boolean consecutive, long windowMillis) {
        if (id != 0) remove(id);
        Entry<T> previous = null;
        boolean divider = text.trim().isEmpty() || text.trim().matches("[-=▬]{5,}");
        if (group && id == 0 && !separator && !divider) {
            for (int i = entries.size() - 1, checked = 0; i >= 0 && checked++ < 100; i--) {
                Entry<T> candidate = entries.get(i);
                if (candidate.separator) break;
                if (candidate.id == 0 && now - candidate.received <= windowMillis && candidate.value.equals(value)) {
                    previous = candidate; entries.remove(i); break;
                }
                if (consecutive) break;
            }
        }
        if (previous == null) previous = new Entry<T>(value, text, id, now, separator);
        else { previous.repeats++; previous.received = now; }
        entries.add(previous);
        trim(limit);
        return previous;
    }
    public void trim(int limit) {
        if (entries.size() > limit) entries.subList(0, entries.size() - limit).clear();
    }
    public static String plain(String text) { return text.replaceAll("§[0-9A-FK-ORa-fk-or]", ""); }
    public static boolean contains(String text, String words) {
        String lower = plain(text).toLowerCase(Locale.ROOT);
        for (String word : words.split(";")) {
            String needle = word.trim().toLowerCase(Locale.ROOT);
            if (!needle.isEmpty() && lower.contains(needle)) return true;
        }
        return false;
    }
    public static boolean mentions(String text, String name, String words) {
        String lower = plain(text).toLowerCase(Locale.ROOT);
        String nick = name.toLowerCase(Locale.ROOT);
        int start = 0;
        if (!nick.isEmpty()) while ((start = lower.indexOf(nick, start)) >= 0) {
            int end = start + nick.length();
            if ((start == 0 || !nickChar(lower.charAt(start - 1)))
                    && (end == lower.length() || !nickChar(lower.charAt(end)))) return true;
            start = end;
        }
        return contains(text, words);
    }
    private static boolean nickChar(char c) { return Character.isLetterOrDigit(c) || c == '_'; }
    public static float opacity(long ageMillis, int durationSeconds, int fadeSeconds) {
        long remaining = durationSeconds * 1000L - ageMillis;
        if (remaining <= 0) return 0;
        return fadeSeconds == 0 ? 1f : Math.min(1f, remaining / (Math.min(fadeSeconds, durationSeconds) * 1000f));
    }
}
