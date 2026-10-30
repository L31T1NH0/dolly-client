import org.objectweb.asm.tree.*;

final class SettingsPatches extends PatchSupport {
    static void settings(ClassNode cls) {
        MethodNode constructor = method(cls, "<init>", "(Lave;Ljava/io/File;)V");
        int calls = 0;
        for (AbstractInsnNode n : constructor.instructions.toArray()) {
            if (n instanceof LdcInsnNode && "options.txt".equals(((LdcInsnNode) n).cst)) ((LdcInsnNode) n).cst = "options-dolly.txt";
            if (n instanceof MethodInsnNode) {
                MethodInsnNode m = (MethodInsnNode) n;
                if (m.owner.equals("avh") && m.name.equals("a") && m.desc.equals("()V")) {
                    constructor.instructions.insertBefore(n, list(new VarInsnNode(ALOAD, 0), new VarInsnNode(ALOAD, 0),
                            new FieldInsnNode(GETFIELD, "avh", "bb", "Ljava/util/Map;"), new VarInsnNode(ALOAD, 2),
                            call("DollySettings", "defaults", "(Lavh;Ljava/util/Map;Ljava/io/File;)V"))); calls++;
                    constructor.instructions.insert(n, list(new VarInsnNode(ALOAD, 0), call("DollyOptiFine", "apply", "(Lavh;)V")));
                }
            }
        }
        require(calls == 1, "Expected one settings loader");
        beforeReturns(method(cls, "b", "()V"), RETURN, () -> list(new VarInsnNode(ALOAD, 0), call("DollySettings", "persist", "(Lavh;)V")));
        OptiFinePatches.settings(cls);
    }
    static void screen(ClassNode cls) {
        beforeReturns(method(cls, "b", "()V"), RETURN, () -> list(new VarInsnNode(ALOAD, 0), call("DollyMenu", "addButton", "(Laxu;)V")));
        method(cls, "a", "(Lavs;)V").instructions.insert(list(new VarInsnNode(ALOAD, 0), new VarInsnNode(ALOAD, 1), call("DollyMenu", "clicked", "(Laxu;Lavs;)V")));
    }
    static void hud(ClassNode cls) { beforeReturns(method(cls, "a", "(F)V"), RETURN, () -> list(call("DollyHud", "render", "()V"))); }
}
