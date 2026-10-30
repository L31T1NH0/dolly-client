import java.io.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Checks the shipped integration points, including the native OptiFine paths. */
public final class DollyPatchTest {
    private static ClassNode read(JarFile jar, String name) throws IOException {
        ClassNode node = new ClassNode();
        try (InputStream in = jar.getInputStream(jar.getJarEntry(name + ".class"))) { new ClassReader(in).accept(node, 0); }
        return node;
    }
    private static MethodNode method(ClassNode node, String name, String desc) {
        for (MethodNode m : node.methods) if (m.name.equals(name) && m.desc.equals(desc)) return m;
        throw new AssertionError(name + desc);
    }
    private static int calls(MethodNode method, String owner, String name) {
        int count = 0;
        for (AbstractInsnNode n : method.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode call = (MethodInsnNode) n;
            if (call.owner.equals(owner) && call.name.equals(name)) count++;
        }
        return count;
    }
    private static boolean isCall(AbstractInsnNode node, String owner, String name) {
        return node instanceof MethodInsnNode && ((MethodInsnNode) node).owner.equals(owner)
                && ((MethodInsnNode) node).name.equals(name);
    }
    private static void loading(ClassNode node, int expected) {
        int count = 0;
        for (MethodNode method : node.methods) {
            count += calls(method, "DollyFramebuffer", "replaceLoading");
            for (AbstractInsnNode n : method.instructions.toArray()) if (n instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) n;
                assert !(field.getOpcode() == Opcodes.PUTFIELD && field.owner.equals("ave") && field.name.equals("n"));
            }
        }
        assert count == expected;
    }
    public static void main(String[] args) throws Exception {
        try (JarFile jar = new JarFile(args[0])) {
            ClassNode minecraft = read(jar, "ave"), settings = read(jar, "avh");
            MethodNode clientLook = method(read(jar, "bet"), "d", "(F)Laui;");
            assert calls(clientLook, "wn", "d") == 1 : "Local aim must reach the mouse fix through OptiFine";
            assert calls(method(read(jar, "pr"), "d", "(F)Laui;"), "pk", "d") == 1;
            ClassNode chatScreen = read(jar, "awv");
            assert calls(method(chatScreen, "a", "(CI)V"), "DollyChatControls", "key") == 1;
            assert calls(method(chatScreen, "a", "(III)V"), "DollyChatControls", "click") == 1;
            assert calls(method(chatScreen, "a", "(IIF)V"), "DollyChatControls", "draw") == 1;
            assert calls(method(chatScreen, "m", "()V"), "DollyChatControls", "close") == 1;
            assert calls(method(read(jar, "avo"), "<init>", "(Lave;)V"), "DollyChat", "<init>") == 1;
            assert calls(method(read(jar, "avo"), "a", "(Lauk;Lavr;)V"), "DollyScoreboard", "render") == 1;
            assert calls(method(minecraft, "a", "(Laxu;)V"), "DollyChat", "leavingMenu") == 1;
            assert calls(method(minecraft, "a", "(Lbdb;Ljava/lang/String;)V"), "DollyChat", "worldChanging") == 1;
            MethodNode tick = method(minecraft, "s", "()V");
            assert calls(tick, "DollySlots", "select") == 1;
            assert calls(tick, "DollySlots", "wheel") == 1;
            assert calls(method(read(jar, "wm"), "a", "(Lzw;IZZ)V"), "DollySlots", "selectable") == 2;
            assert calls(tick, "avt", "a") == 1 : "F3+D must still clear chat manually";
            assert calls(tick, "DollyInput", "keyState") == 2;
            assert calls(tick, "DollyInput", "tick") == 1;
            assert calls(tick, "DollyHud", "tick") == 1;
            for (AbstractInsnNode n : tick.instructions.toArray()) if (n instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) n;
                assert !(call.owner.equals("avb") && call.name.equals("a") && call.desc.equals("(IZ)V"));
            }
            MethodNode frame = method(read(jar, "bfk"), "a", "(FJ)V");
            assert calls(frame, "DollyCamera", "beginFrame") == 1;
            assert calls(frame, "DollyCamera", "smoothCamera") == 1;
            assert calls(method(read(jar, "bfk"), "e", "()V"), "DollyCamera", "smoothCamera") == 1;
            assert calls(frame, "DollyProfiler", "begin") == 1;
            assert calls(frame, "DollyProfiler", "end") >= 2;
            MethodNode loop = method(minecraft, "av", "()V");
            assert calls(loop, "org/lwjgl/opengl/Display", "processMessages") == 1;
            boolean inputReady = false;
            for (AbstractInsnNode n : loop.instructions.toArray()) {
                if (isCall(n, "org/lwjgl/opengl/Display", "processMessages")) inputReady = true;
                if (isCall(n, "avl", "a") || isCall(n, "ave", "s"))
                    assert inputReady : "The OS event pump must run before timer/tick input consumption";
            }
            assert calls(loop, "DollyProfiler", "frameBegin") == 1;
            assert calls(loop, "DollyProfiler", "frameEnd") >= 2;
            assert calls(method(read(jar, "bfr"), "a", "(Lpk;Lbia;F)V"), "DollyProfiler", "begin") > 0;
            assert calls(method(read(jar, "bht"), "b", "(FFFLbhn;)V"), "DollyProfiler", "begin") > 0;
            assert jar.getJarEntry("dolly-profiler-symbols.txt") != null;
            assert calls(frame, "DollyProfilerGpu", "begin") == 1;
            assert calls(frame, "DollyProfilerGpu", "end") >= 2;
            assert calls(method(read(jar, "fh"), "a", "(Lff;Lep;Lod;)V"), "DollyProfilerNetwork", "queue") == 1;
            assert calls(method(read(jar, "en"), "decode", "(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Ljava/util/List;)V"), "DollyProfilerNetwork", "traffic") > 0;
            assert calls(method(read(jar, "eo"), "a", "(Lio/netty/channel/ChannelHandlerContext;Lff;Lio/netty/buffer/ByteBuf;)V"), "DollyProfilerNetwork", "traffic") > 0;
            for (MethodNode m : read(jar, "DollyProfilerGpu$Lwjgl").methods) {
                assert calls(m, "DollyProfiler", "begin") == 0 && calls(m, "DollyProfiler", "gl") == 0;
            }
            MethodNode nativeOptions = method(settings, "setOptionValueOF", "(Lavh$a;I)V");
            int writes = 0;
            for (AbstractInsnNode n : nativeOptions.instructions.toArray()) if (n instanceof FieldInsnNode
                    && n.getOpcode() == Opcodes.PUTFIELD && ((FieldInsnNode) n).name.equals("ofFastRender")) {
                assert isCall(n.getPrevious(), "DollyFramebuffer", "releaseBeforeChange"); writes++;
            }
            assert writes == 1;
            assert calls(method(settings, "resetSettings", "()V"), "DollyFramebuffer", "releaseBeforeChange") == 1;
            assert calls(method(settings, "loadOfOptions", "()V"), "DollyOptiFine", "disableZoom") > 0;
            loading(minecraft, 2); loading(read(jar, "Config"), 1);
            assert calls(frame, "DollyCulling", "beginFrame") == 1;
            assert calls(method(read(jar, "biu"), "a", "(Lpk;DDDFFZ)Z"), "DollyCulling", "entity") == 1;
            assert calls(method(read(jar, "bhc"), "a", "(Lakw;DDDFI)V"), "DollyCulling", "tile") == 1;
            assert calls(method(read(jar, "bou"), "a", "(Lbni;)V"), "DollyModels", "deduplicate") == 1;
            assert calls(method(read(jar, "eq"), "decode", "(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Ljava/util/List;)V"), "DollyNetwork", "decode") == 1;
            assert calls(method(read(jar, "em"), "b", "(I)V"), "DollyNetwork", "writeVarInt") == 1;
            assert calls(method(read(jar, "net/optifine/render/VboRegion"), "finishDraw", "(Lbfy;)V"), "DollyTerrain", "draw") == 1;
        }
        System.out.println("Patched-bytecode regressions passed: input, HUD, camera, native Fast Render/reset, resize and zoom reload.");
    }
}
