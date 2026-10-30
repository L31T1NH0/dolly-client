package dolly.audio;

import java.io.IOException;
import java.util.*;

/** Only non-default event multipliers are stored in the existing atomic preferences file. */
public final class SoundVolumes {
    public static Map<String, Integer> parse(String encoded) throws IOException {
        if (encoded == null || encoded.length() > 65536) throw new IOException("Invalid sound volume preferences");
        Map<String, Integer> values = new TreeMap<String, Integer>();
        if (encoded.isEmpty()) return values;
        for (String entry : encoded.split(";", -1)) {
            int separator = entry.indexOf('=');
            if (separator <= 0) throw new IOException("Invalid sound volume entry");
            String id = entry.substring(0, separator);
            if (!id.matches("[a-z0-9_.-]+:[A-Za-z0-9_./-]+")) throw new IOException("Invalid sound event: " + id);
            int volume;
            try { volume = Integer.parseInt(entry.substring(separator + 1)); }
            catch (NumberFormatException e) { throw new IOException("Invalid sound volume: " + id, e); }
            if (volume < 0 || volume > 200 || values.put(id, volume) != null || values.size() > 1024)
                throw new IOException("Invalid or repeated sound volume: " + id);
        }
        return values;
    }
    public static String encode(Map<String, Integer> values) {
        StringBuilder encoded = new StringBuilder();
        for (Map.Entry<String, Integer> entry : new TreeMap<String, Integer>(values).entrySet()) {
            if (entry.getValue() == 100) continue;
            if (encoded.length() > 0) encoded.append(';');
            encoded.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return encoded.toString();
    }
    public static float scale(float original, int percent) {
        if (!Float.isFinite(original) || original <= 0) return 0;
        return Math.min(1f, percent == 100 ? original : original * (percent / 100f));
    }
}
