import org.objectweb.asm.tree.*;

final class TechnicalLoadingPatches extends PatchSupport {
    static void minecraft(ClassNode cls) {
        int screenshots = 0;
        for (MethodNode method : cls.methods) for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode invocation = (MethodInsnNode) instruction;
            if (invocation.owner.equals("avj") && invocation.name.equals("a") && invocation.desc.equals("(Ljava/io/File;IILbfw;)Leu;")) {
                method.instructions.set(instruction, call("DollyScreenshots", "capture", invocation.desc)); screenshots++;
            }
        }
        require(screenshots == 1, "Expected one screenshot key path");
        method(cls, "g", "()V").instructions.insert(call("DollyScreenshots", "shutdownScreenshots", "()V"));
        method(cls, "g", "()V").instructions.insert(call("DollyRenderedText", "clear", "()V"));
    }
    static void screenshot(ClassNode cls) {
        MethodNode capture = method(cls, "a", "(Ljava/io/File;Ljava/lang/String;IILbfw;)Leu;");
        int success = 0;
        for (AbstractInsnNode n : capture.instructions.toArray()) {
            if (n instanceof LdcInsnNode && "screenshot.success".equals(((LdcInsnNode) n).cst)) {
                AbstractInsnNode end = n;
                while (end != null && end.getOpcode() != ARETURN) end = end.getNext();
                require(end != null, "Missing screenshot success return");
                capture.instructions.insertBefore(end, list(new VarInsnNode(ALOAD, 14), new VarInsnNode(ALOAD, 13),
                        call("DollyScreenshots", "nativeComplete", "(Leu;Ljava/io/File;Ljava/awt/image/BufferedImage;)Leu;"))); success++;
            }
        }
        require(success == 1, "Expected one M5 screenshot success path");
    }
    static void language(ClassNode cls) {
        MethodNode selection = method(cls, "a", "(IZII)V");
        int reloads = 0;
        for (AbstractInsnNode instruction : selection.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode invocation = (MethodInsnNode) instruction;
            if (invocation.owner.equals("ave") && invocation.name.equals("e") && invocation.desc.equals("()V")) {
                selection.instructions.set(instruction, call("DollyLanguage", "reload", "(Lave;)V")); reloads++;
            }
        }
        require(reloads == 1, "Expected one language-screen reload");
    }
    static void serverAddress(ClassNode cls) {
        clear(method(cls, "b", "(Ljava/lang/String;)[Ljava/lang/String;"), list(new VarInsnNode(ALOAD, 0),
                call("DollyServerAddress", "resolve", "(Ljava/lang/String;)[Ljava/lang/String;"), new InsnNode(ARETURN)));
    }
}
