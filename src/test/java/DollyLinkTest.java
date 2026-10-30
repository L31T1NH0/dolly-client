import java.nio.file.*;
import java.util.*;

/** Links changed classes with the real JVM verifier, without starting graphics or logging in. */
public final class DollyLinkTest {
    public static void main(String[] args) throws Exception {
        List<String> names = new ArrayList<String>(Files.readAllLines(Paths.get(args[0])));
        names.addAll(Files.readAllLines(Paths.get(args[1])));
        names.addAll(Arrays.asList("Config", "bqs", "bmt", "bht", "bfy", "bga", "bho", "bfr", "bna", "bgf$b",
                "net/optifine/render/VboRegion", "net/optifine/render/VboRange", "net/optifine/shaders/Shaders"));
        for (String name : names) {
            Class<?> cls = Class.forName(name.replace('/', '.'), false, DollyLinkTest.class.getClassLoader());
            cls.getDeclaredMethods(); cls.getDeclaredConstructors(); cls.getDeclaredFields();
        }
        System.out.println("JVM verified and linked " + names.size() + " changed/adapter classes without launching Minecraft.");
    }
}
