import org.objectweb.asm.tree.*;

final class MaintenancePatches extends PatchSupport {
    static void minecraft(ClassNode cls) {
        method(cls, "av", "()V").instructions.insert(call("DollyDebug", "frame", "()V"));
    }
    static void resources(ClassNode cls) {
        MethodNode method = method(cls, "a", "(Lhd;)V");
        method.instructions.insert(handled(list(new VarInsnNode(ALOAD, 0), new VarInsnNode(ALOAD, 1)),
                "DollyResourcePacks", "reject", "(Lbcy;Lhd;)Z"));
        int urls = 0;
        for (AbstractInsnNode n : method.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode invocation = (MethodInsnNode) n;
            if (invocation.owner.equals("hd") && invocation.name.equals("a") && invocation.desc.equals("()Ljava/lang/String;")) {
                method.instructions.set(n, call("DollyResourcePacks", "url", "(Lhd;)Ljava/lang/String;")); urls++;
            }
        }
        require(urls == 1, "Expected one resource-pack URL read");
    }
    static void nausea(ClassNode cls) {
        method(cls, "l", "(I)V").instructions.insert(list(new VarInsnNode(ALOAD, 0), new VarInsnNode(ILOAD, 1),
                call("DollyDebug", "nausea", "(Lpr;I)V")));
    }
    static void sky(ClassNode cls) {
        boolean eyes = false; int guarded = 0;
        MethodNode sky = method(cls, "a", "(FI)V");
        for (AbstractInsnNode n : sky.instructions.toArray()) {
            if (n instanceof MethodInsnNode && ((MethodInsnNode) n).name.equals("e") && ((MethodInsnNode) n).desc.equals("(F)Laui;")) eyes = true;
            if (eyes && n.getOpcode() == DCONST_0) {
                AbstractInsnNode compare = n.getNext(), jump = compare == null ? null : compare.getNext();
                if (compare != null && compare.getOpcode() == DCMPG && jump instanceof JumpInsnNode && jump.getOpcode() == IFGE) {
                    sky.instructions.insert(jump, list(call("DollyDebug", "voidBox", "()Z"), new JumpInsnNode(IFNE, ((JumpInsnNode) jump).label))); guarded++;
                }
            }
        }
        require(guarded == 1, "Expected one below-horizon sky box");
    }
    static void renderer(ClassNode cls) {
        int calls = 0;
        for (MethodNode method : cls.methods) for (AbstractInsnNode n : method.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode invocation = (MethodInsnNode) n;
            if (invocation.owner.equals("net/optifine/Lagometer") && invocation.name.equals("showLagometer")) {
                method.instructions.set(n, call("DollyDebug", "graph", "(Lavr;)V")); calls++;
            }
        }
        require(calls == 1, "Expected one M5 debug graph draw");
    }
    static void fps(ClassNode cls) {
        int calls = 0;
        MethodNode draw = method(cls, "drawFps", "()V");
        for (AbstractInsnNode n : draw.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode invocation = (MethodInsnNode) n;
            if (invocation.owner.equals("java/lang/StringBuilder") && invocation.name.equals("toString")) {
                draw.instructions.insert(n, call("DollyDebug", "simplify", "(Ljava/lang/String;)Ljava/lang/String;")); calls++;
            }
        }
        require(calls == 1, "Expected one M5 FPS display string");
    }
    static void debug(ClassNode cls) {
        int calls = 0;
        for (AbstractInsnNode n : method(cls, "b", "()Ljava/util/List;").instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode invocation = (MethodInsnNode) n;
            if (invocation.owner.equals("java/lang/StringBuffer") && invocation.name.equals("insert") && invocation.desc.equals("(ILjava/lang/String;)Ljava/lang/StringBuffer;")) {
                method(cls, "b", "()Ljava/util/List;").instructions.insertBefore(n, call("DollyDebug", "suffix", "(Ljava/lang/String;)Ljava/lang/String;")); calls++;
            }
        }
        require(calls == 1, "Expected one FPS-minimum insertion in debug text");
    }
}
