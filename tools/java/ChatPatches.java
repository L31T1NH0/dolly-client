import org.objectweb.asm.tree.*;

/** Small integration hooks; chat and sidebar behavior lives in the adapters. */
final class ChatPatches extends PatchSupport {
    static void hud(ClassNode cls) {
        int allocations = 0, constructors = 0;
        for (AbstractInsnNode n : method(cls, "<init>", "(Lave;)V").instructions.toArray()) {
            if (n instanceof TypeInsnNode && n.getOpcode() == NEW && ((TypeInsnNode) n).desc.equals("avt")) {
                ((TypeInsnNode) n).desc = "DollyChat"; allocations++;
            }
            if (n instanceof MethodInsnNode && ((MethodInsnNode) n).owner.equals("avt") && ((MethodInsnNode) n).name.equals("<init>")) {
                ((MethodInsnNode) n).owner = "DollyChat"; constructors++;
            }
        }
        require(allocations == 1 && constructors == 1, "Expected one native chat allocation");
        clear(method(cls, "a", "(Lauk;Lavr;)V"), list(new VarInsnNode(ALOAD, 1), new VarInsnNode(ALOAD, 2),
                call("DollyScoreboard", "render", "(Lauk;Lavr;)V"), new InsnNode(RETURN)));
    }
    static void scoreboard(ClassNode cls) {
        for (MethodNode m : cls.methods) {
            if (m.name.startsWith("<") || (m.access & ACC_STATIC) != 0) continue;
            // Include creation of zero-valued scores and membership changes as well as notifications.
            if (m.desc.endsWith("V") || m.name.equals("c") && m.desc.equals("(Ljava/lang/String;Lauk;)Laum;")
                    || m.desc.equals("(Ljava/lang/String;Ljava/lang/String;)Z") || m.desc.equals("(Ljava/lang/String;)Z"))
                m.instructions.insert(list(new VarInsnNode(ALOAD, 0), call("DollyScoreboard", "invalidate", "(Lauo;)V")));
        }
    }
    static void minecraft(ClassNode cls) {
        method(cls, "s", "()V").instructions.insert(call("DollyChatSending", "tick", "()V"));
        method(cls, "a", "(Lbdb;Ljava/lang/String;)V").instructions.insert(list(new VarInsnNode(ALOAD, 1),
                call("DollyChat", "worldChanging", "(Lbdb;)V")));
        int count = 0;
        for (AbstractInsnNode n : method(cls, "a", "(Laxu;)V").instructions.toArray()) {
            if (n instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) n;
                if (call.owner.equals("avt") && call.name.equals("a") && call.desc.equals("()V")) {
                    call.setOpcode(INVOKESTATIC); call.owner = "DollyChat"; call.name = "leavingMenu"; call.desc = "(Lavt;)V"; count++;
                }
            }
        }
        require(count == 1, "Expected menu chat clear; F3+D must retain its manual clear");
    }
    static void sending(ClassNode cls) {
        method(cls, "e", "(Ljava/lang/String;)V").instructions.insert(handled(list(new VarInsnNode(ALOAD, 0),
                new VarInsnNode(ALOAD, 1)), "DollyChatSending", "intercept", "(Lbew;Ljava/lang/String;)Z"));
    }
    static void actions(ClassNode cls) {
        method(cls, "b", "(Ljava/lang/String;Z)V").instructions.insert(list(new VarInsnNode(ILOAD, 2),
                new VarInsnNode(ALOAD, 1), call("DollyChatSending", "history", "(ZLjava/lang/String;)Z"), new VarInsnNode(ISTORE, 2)));
    }
    static void tab(ClassNode cls) {
        method(cls, "a", "(IIILbdc;)V").instructions.insert(handled(list(new VarInsnNode(ILOAD, 1),
                new VarInsnNode(ILOAD, 2), new VarInsnNode(ILOAD, 3), new VarInsnNode(ALOAD, 4)),
                "DollyTabPing", "draw", "(IIILbdc;)Z"));
    }
    static void screen(ClassNode cls) {
        beforeReturns(method(cls, "b", "()V"), RETURN, () -> list(new VarInsnNode(ALOAD, 0), call("DollyChatControls", "open", "(Lawv;)V")));
        beforeReturns(method(cls, "m", "()V"), RETURN, () -> list(call("DollyChatControls", "close", "()V")));
        beforeReturns(method(cls, "e", "()V"), RETURN, () -> list(call("DollyChatControls", "tick", "()V")));
        beforeReturns(method(cls, "a", "(IIF)V"), RETURN, () -> list(new VarInsnNode(ALOAD, 0), new VarInsnNode(ILOAD, 1), new VarInsnNode(ILOAD, 2), call("DollyChatControls", "draw", "(Lawv;II)V")));
        LabelNode vanillaKey = new LabelNode();
        method(cls, "a", "(CI)V").instructions.insert(list(new VarInsnNode(ALOAD, 0), new VarInsnNode(ILOAD, 1), new VarInsnNode(ILOAD, 2),
                call("DollyChatControls", "key", "(Lawv;CI)Z"), new JumpInsnNode(IFEQ, vanillaKey), new InsnNode(RETURN), vanillaKey));
        LabelNode vanillaMouse = new LabelNode();
        method(cls, "a", "(III)V").instructions.insert(list(new VarInsnNode(ALOAD, 0), new VarInsnNode(ILOAD, 1),
                new VarInsnNode(ILOAD, 2), new VarInsnNode(ILOAD, 3), call("DollyChatControls", "click", "(Lawv;III)Z"),
                new JumpInsnNode(IFEQ, vanillaMouse), new InsnNode(RETURN), vanillaMouse));
    }
}
