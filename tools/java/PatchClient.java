import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

/** Build-time only; no transformer or ASM dependency is loaded by the client. */
public final class PatchClient {
    public static void main(String[] args) throws Exception {
        Path input = Paths.get(args[0]), output = Paths.get(args[1]), classes = Paths.get(args[2]);
        Set<String> patched = new TreeSet<String>(), basePatched = new TreeSet<String>();
        try (ZipFile source = new ZipFile(input.toFile()); JarOutputStream target = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<? extends ZipEntry> entries = source.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || name.startsWith("META-INF/") || name.equals("assets/minecraft/lang/en_US.lang") || name.equals("assets/minecraft/lang/pt_BR.lang")) continue;
                byte[] bytes;
                try (InputStream in = source.getInputStream(entry)) { bytes = read(in); }
                if (name.endsWith(".class")) {
                    ClassNode cls = new ClassNode(); new ClassReader(bytes).accept(cls, 0);
                    boolean basePatch = patch(cls);
                    if (basePatch) basePatched.add(cls.name);
                    boolean profilePatch = ProfilerPatches.patch(cls);
                    if (profilePatch || basePatch) {
                        ClassWriter writer = new HierarchyWriter();
                        cls.accept(writer); bytes = writer.toByteArray(); patched.add(cls.name);
                    }
                }
                write(target, name, bytes);
            }
            try (java.util.stream.Stream<Path> paths = Files.walk(classes)) {
                for (Path file : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator) {
                    byte[] bytes = Files.readAllBytes(file);
                    if (file.toString().endsWith(".class")) {
                        ClassNode cls = new ClassNode(); new ClassReader(bytes).accept(cls, 0);
                        if (ProfilerPatches.patch(cls)) { ClassWriter writer = new HierarchyWriter(); cls.accept(writer); bytes = writer.toByteArray(); }
                    }
                    write(target, classes.relativize(file).toString().replace(File.separatorChar, '/'), bytes);
                }
            }
            write(target, "dolly-profiler-symbols.txt", ProfilerPatches.symbols());
            write(target, "assets/minecraft/lang/en_US.lang", translations(source, "en_US"));
            if (source.getEntry("assets/minecraft/lang/pt_BR.lang") != null) write(target, "assets/minecraft/lang/pt_BR.lang", translations(source, "pt_BR"));
        }
        PatchSupport.require(basePatched.size() == 61, "Unexpected base patched class count: " + basePatched);
        Files.write(output.resolveSibling("patches.txt"), patched);
        System.out.println("Patched " + patched.size() + " Minecraft/OptiFine classes; no Forge loader.");
    }
    private static boolean patch(ClassNode c) {
        switch (c.name) {
            case "ave": TechnicalInputPatches.minecraft(c); MaintenancePatches.minecraft(c); AudioPatches.minecraft(c); InputPatches.minecraft(c); LegacyAnimationPatches.attack(c); ChatPatches.minecraft(c); TechnicalLoadingPatches.minecraft(c); TechnicalRuntimePatches.minecraft(c); TactilePatches.minecraft(c); break;
            case "bpx": AudioPatches.manager(c); break;
            case "bpz": AudioPatches.handler(c); break;
            case "Config": OptiFinePatches.loadingBuffers(c, 1); MaintenancePatches.fps(c); break;
            case "bcy": MaintenancePatches.resources(c); break;
            case "avv": MaintenancePatches.debug(c); break;
            case "avh": SettingsPatches.settings(c); break;
            case "pr": MaintenancePatches.nausea(c); InputPatches.look(c); VisualCleanupPatches.potion(c); TactilePatches.movement(c); break;
            case "bet": InputPatches.clientLook(c); break;
            case "avf": InputPatches.mouse(c); TechnicalInputPatches.mouse(c); break;
            case "axu": TechnicalInputPatches.screen(c); ChatPatches.actions(c); break;
            case "auo": ChatPatches.scoreboard(c); break;
            case "bew": ChatPatches.sending(c); break;
            case "awh": ChatPatches.tab(c); break;
            case "ayl": TechnicalInputPatches.inventory(c); break;
            case "ayw": TechnicalInputPatches.inventoryPosition(c); break;
            case "axl$a": TechnicalLoadingPatches.language(c); break;
            case "bdd": TechnicalLoadingPatches.serverAddress(c); break;
            case "avn": TechnicalRenderPatches.font(c); GroupOnePatches.font(c); break;
            case "bct": GroupOnePatches.model(c); break;
            case "bbs": GroupOnePatches.quad(c); break;
            case "beb": GroupOnePatches.particle(c); break;
            case "bmx": GroupOnePatches.pack(c); break;
            case "asw": GroupOnePatches.node(c); break;
            case "ajd": GroupOnePatches.torch(c); break;
            case "ack": GroupOnePatches.enchantments(c); break;
            case "bjf": WorldObjectPatches.items(c); break;
            case "bhf": WorldObjectPatches.book(c); break;
            case "bgp": TechnicalRenderPatches.items(c); break;
            case "bfl": TechnicalHudPatches.blend(c); break;
            case "bho": TechnicalRuntimePatches.chunks(c); break;
            case "bex": TechnicalRuntimePatches.players(c); break;
            case "bmj": TechnicalRuntimePatches.textures(c); break;
            case "wm": InputPatches.inventory(c); break;
            case "aya": SettingsPatches.screen(c); MenuPatches.main(c); break;
            case "axn": SettingsPatches.screen(c); break;
            case "axp": MenuPatches.pause(c); break;
            case "awz": MenuPatches.connecting(c); break;
            case "axh": MenuPatches.disconnected(c); break;
            case "avj": TechnicalLoadingPatches.screenshot(c); break;
            case "avo": SettingsPatches.hud(c); LegacyAnimationPatches.hearts(c); ChatPatches.hud(c); TechnicalHudPatches.hud(c); break;
            case "awv": ChatPatches.screen(c); break;
            case "bfk": MaintenancePatches.renderer(c); VisualCleanupPatches.fog(c); RenderPatches.camera(c); TechnicalHudPatches.renderer(c); CameraLightingPatches.renderer(c); TactilePatches.renderer(c); break;
            case "adm": CameraLightingPatches.world(c); break;
            case "amy": CameraLightingPatches.chunk(c); break;
            case "bfr": MaintenancePatches.sky(c); CameraLightingPatches.sky(c); break;
            case "bdb": RenderPatches.ambient(c); break;
            case "bec": VisualCleanupPatches.breaking(c); RenderPatches.particles(c); break;
            case "bfn": VisualCleanupPatches.overlays(c); LegacyAnimationPatches.items(c); CameraLightingPatches.hand(c); break;
            case "bjl": VisualCleanupPatches.horse(c); break;
            case "bih": VisualCleanupPatches.lightning(c); break;
            case "bkc": VisualCleanupPatches.projectile(c, "(Lpk;DDDFF)V"); break;
            case "biz": VisualCleanupPatches.projectile(c, "(Lur;DDDFF)V"); break;
            case "bda": LegacyAnimationPatches.controller(c); break;
            case "bkx": LegacyAnimationPatches.armor(c); break;
            case "em": PerformancePatches.packetBuffer(c); break;
            case "eq": PerformancePatches.decoder(c); break;
            case "bou": PerformancePatches.models(c); break;
            case "biu": PerformancePatches.entities(c); break;
            case "bhc": PerformancePatches.tiles(c); break;
            case "net/optifine/render/VboRegion": PerformancePatches.regions(c); break;
            default: return false;
        }
        return true;
    }
    private static byte[] translations(ZipFile source, String locale) throws IOException {
        ZipEntry entry = source.getEntry("assets/minecraft/lang/" + locale + ".lang");
        byte[] original = entry == null ? new byte[0] : read(source.getInputStream(entry));
        String extra = "\nkey.dolly.menu=Dolly settings\nkey.dolly.zoom=Dolly zoom\nkey.dolly.perspective=Dolly perspective\nkey.dolly.scoreboard=Dolly scoreboard\nkey.dolly.slots=Dolly slot profile\nkey.categories.dolly=Dolly Client\n";
        return (new String(original, "UTF-8") + extra).getBytes("UTF-8");
    }
    private static void write(JarOutputStream output, String path, byte[] bytes) throws IOException { JarEntry entry = new JarEntry(path); entry.setTime(0); output.putNextEntry(entry); output.write(bytes); output.closeEntry(); }
    private static byte[] read(InputStream in) throws IOException { try (InputStream stream = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) { byte[] b = new byte[8192]; int n; while ((n = stream.read(b)) >= 0) out.write(b, 0, n); return out.toByteArray(); } }
}
