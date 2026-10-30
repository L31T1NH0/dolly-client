import dolly.audio.SoundVolumes;
import java.io.IOException;
import java.util.*;

public final class DollySoundVolumesTest {
    public static void run() throws Exception {
        assert SoundVolumes.parse("").isEmpty();
        Map<String, Integer> values = SoundVolumes.parse("pack:Custom.Sound=200;minecraft:random.click=0;minecraft:mob.cat.say=100");
        String encoded = SoundVolumes.encode(values);
        assert encoded.equals("minecraft:random.click=0;pack:Custom.Sound=200");
        assert SoundVolumes.parse(encoded).get("pack:Custom.Sound") == 200;
        for (String invalid : new String[] {"click=10", "minecraft:x=-1", "minecraft:x=201", "minecraft:x=NaN", "minecraft:x=1;",
                "minecraft:x=1;minecraft:x=2", "minecraft:x=1=2", "minecraft:x=1;bad event=10"}) {
            try { SoundVolumes.parse(invalid); throw new AssertionError("Invalid sound preference accepted: " + invalid); }
            catch (IOException expected) { }
        }
        assert SoundVolumes.scale(.25f, 100) == .25f;
        assert SoundVolumes.scale(.25f, 200) == .5f;
        assert SoundVolumes.scale(.8f, 200) == 1;
        assert SoundVolumes.scale(.8f, 0) == 0;
        assert SoundVolumes.scale(0, 200) == 0;
        assert SoundVolumes.scale(Float.NaN, 100) == 0;
    }
}
