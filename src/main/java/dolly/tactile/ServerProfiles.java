package dolly.tactile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/** Persisted per-address preferences; restrictive servers never enter the substep engine. */
public final class ServerProfiles {
    public enum Mode { VANILLA, INPUT, EXACT, STEPPED }
    public enum Source { AUTO, EVENTS, POLLER, FRAME }
    public static final String DEFAULT = "*", SINGLEPLAYER = "@singleplayer";
    public static final class Profile {
        public Mode mode = Mode.VANILLA;
        public Source source = Source.AUTO;
        public int physics = 960, steps = 20, polling = 2000, overlay;
        public boolean latch = true;
        public Profile copy() {
            Profile p = new Profile(); p.mode = mode; p.source = source; p.physics = physics;
            p.steps = steps; p.polling = polling; p.overlay = overlay; p.latch = latch; return p;
        }
        public void validate() throws IOException {
            if (mode == null || source == null || physics < 20 || physics > 1920 || physics % 20 != 0
                    || steps < 1 || steps > 1920 || polling < 250 || polling > 8000 || overlay < 0 || overlay > 2)
                throw new IOException("Perfil TACTILE inválido");
        }
        public String encode() {
            return mode + "," + physics + "," + steps + "," + source + "," + polling + "," + latch + "," + overlay;
        }
    }
    private ServerProfiles() { }
    public static String key(String address) {
        if (address == null || address.trim().isEmpty()) return DEFAULT;
        if (address.equals(DEFAULT) || address.equals(SINGLEPLAYER)) return address;
        try {
            URI uri = new URI("minecraft://" + address.trim());
            String host = uri.getHost(); int port = uri.getPort();
            if (host == null || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !uri.getRawPath().isEmpty() || port == 0 || port > 65535) return null;
            host = host.toLowerCase(Locale.ROOT);
            while (host.endsWith(".")) host = host.substring(0, host.length() - 1);
            if (host.isEmpty() || host.length() > 253) return null;
            return host + (port == -1 || port == 25565 ? "" : ":" + port);
        } catch (URISyntaxException e) { return null; }
    }
    public static boolean hypixel(String key) {
        if (key == null) return false;
        String host = key.toLowerCase(Locale.ROOT).split(":", 2)[0];
        return host.equals("hypixel.net") || host.endsWith(".hypixel.net");
    }
    public static Mode effectiveMode(String key, Profile p) {
        return hypixel(key) && (p.mode == Mode.EXACT || p.mode == Mode.STEPPED) ? Mode.INPUT : p.mode;
    }
    public static Map<String, Profile> parse(String saved) throws IOException {
        Map<String, Profile> result = new LinkedHashMap<String, Profile>();
        if (saved.isEmpty()) return result;
        if (saved.length() > 65536) throw new IOException("Muitos perfis TACTILE");
        for (String line : saved.split("\n", -1)) {
            int equal = line.indexOf('=');
            if (equal <= 0) throw new IOException("Perfil TACTILE sem servidor");
            String address = line.substring(0, equal), key = key(address);
            if (key == null || !key.equals(address) || result.containsKey(key)) throw new IOException("Servidor TACTILE inválido ou repetido");
            String[] values = line.substring(equal + 1).split(",", -1);
            if (values.length != 7) throw new IOException("Perfil TACTILE incompleto");
            Profile p = new Profile();
            try {
                p.mode = Mode.valueOf(values[0]); p.physics = Integer.parseInt(values[1]); p.steps = Integer.parseInt(values[2]);
                p.source = Source.valueOf(values[3]); p.polling = Integer.parseInt(values[4]);
                if (!values[5].equals("true") && !values[5].equals("false")) throw new IllegalArgumentException();
                p.latch = Boolean.parseBoolean(values[5]); p.overlay = Integer.parseInt(values[6]); p.validate();
            } catch (IllegalArgumentException e) { throw new IOException("Valor inválido no perfil TACTILE", e); }
            result.put(key, p);
        }
        return result;
    }
    public static String encode(Map<String, Profile> profiles) throws IOException {
        StringBuilder saved = new StringBuilder();
        for (Map.Entry<String, Profile> entry : profiles.entrySet()) {
            entry.getValue().validate();
            if (saved.length() > 0) saved.append('\n');
            saved.append(entry.getKey()).append('=').append(entry.getValue().encode());
        }
        parse(saved.toString());
        return saved.toString();
    }
    public static Profile resolve(Map<String, Profile> profiles, String key) {
        Profile p = profiles.get(key);
        if (p == null) p = profiles.get(DEFAULT);
        return p == null ? new Profile() : p.copy();
    }
}
