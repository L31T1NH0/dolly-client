import dolly.tactile.ServerProfiles;
import dolly.tactile.ServerProfiles.*;
import java.util.*;
import java.io.IOException;

public final class DollyTactileProfilesTest {
    public static void run() throws Exception {
        assert ServerProfiles.key(" MC.HYPIXEL.NET.:25565 ").equals("mc.hypixel.net");
        assert ServerProfiles.key("localhost:25566").equals("localhost:25566");
        assert ServerProfiles.key("[::1]:25565").equals("[::1]");
        for (String address : Arrays.asList("host/path", "user@host", "host?query", "host#fragment", "host:0", "host:65536", "bad address"))
            assert ServerProfiles.key(address) == null : address;
        Profile p = new Profile();
        assert ServerProfiles.effectiveMode("mc.hypixel.net", p) == Mode.VANILLA;
        p.mode = Mode.EXACT;
        assert ServerProfiles.effectiveMode("hypixel.net", p) == Mode.INPUT;
        assert ServerProfiles.effectiveMode("mc.hypixel.net:25566", p) == Mode.INPUT;
        assert ServerProfiles.effectiveMode("mc.hypixel.net.evil.com", p) == Mode.EXACT;
        assert ServerProfiles.effectiveMode("fakehypixel.net", p) == Mode.EXACT;
        p.mode = Mode.STEPPED;
        assert ServerProfiles.effectiveMode("mc.hypixel.net", p) == Mode.INPUT;
        assert p.mode == Mode.STEPPED : "Restrictions must not overwrite a saved choice";
        Map<String, Profile> profiles = new LinkedHashMap<String, Profile>();
        profiles.put("*", p);
        Profile own = new Profile(); own.mode = Mode.INPUT; own.overlay = 2;
        profiles.put("example.org:25566", own);
        String saved = ServerProfiles.encode(profiles);
        profiles = ServerProfiles.parse(saved);
        assert ServerProfiles.resolve(profiles, "elsewhere.net").mode == Mode.STEPPED;
        assert ServerProfiles.resolve(profiles, "example.org:25566").overlay == 2;
        Profile resolved = ServerProfiles.resolve(profiles, "example.org:25566");
        resolved.mode = Mode.EXACT;
        assert profiles.get("example.org:25566").mode == Mode.INPUT : "Editing a profile must not mutate the saved map";
        assert ServerProfiles.encode(profiles).equals(saved);
        for (String invalid : Arrays.asList("bad", "HOST=VANILLA,960,20,AUTO,2000,true,0", "*=EXACT,19,20,AUTO,2000,true,0",
                "*=EXACT,21,20,AUTO,2000,true,0", "*=EXACT,960,0,AUTO,2000,true,0", "*=EXACT,960,20,UNKNOWN,2000,true,0",
                "*=EXACT,960,20,AUTO,9000,true,0", "*=EXACT,960,20,AUTO,2000,yes,0", "*=EXACT,960,20,AUTO,2000,true,3",
                saved + "\n" + saved.split("\n")[0])) {
            try { ServerProfiles.parse(invalid); throw new AssertionError(invalid); }
            catch (IOException expected) { }
        }
    }
}
