import org.objectweb.asm.tree.*;

final class TechnicalInputPatches extends PatchSupport {
    static void minecraft(ClassNode cls) {
        // LWJGL 2 normally pumps events at the end of the frame, after the ticks consumed input.
        method(cls, "av", "()V").instructions.insert(call("org/lwjgl/opengl/Display", "processMessages", "()V"));
    }

    static void inventoryPosition(ClassNode cls) {
        beforeReturns(method(cls, "a", "()V"), RETURN, () -> list(new VarInsnNode(ALOAD, 0),
                new VarInsnNode(ALOAD, 0), new FieldInsnNode(GETFIELD, "ayw", "l", "I"),
                new VarInsnNode(ALOAD, 0), new FieldInsnNode(GETFIELD, "ayw", "f", "I"),
                new InsnNode(ISUB), new InsnNode(ICONST_2), new InsnNode(IDIV),
                new FieldInsnNode(PUTFIELD, "ayw", "i", "I")));
    }

    static void screen(ClassNode cls) {
        int checks = 0;
        for (AbstractInsnNode n : method(cls, "l", "()V").instructions.toArray()) {
            if (n instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) n;
                if (call.owner.equals("org/lwjgl/input/Keyboard") && call.name.equals("getEventKeyState")) {
                    method(cls, "l", "()V").instructions.set(n, call("DollyInput", "textEvent", "()Z")); checks++;
                }
            }
        }
        require(checks == 1, "Expected one GUI keyboard event guard");
    }

    static void inventory(ClassNode cls) {
        LabelNode vanilla = new LabelNode();
        // Consume successful hotbar swaps before pick-block can handle the same mouse button.
        method(cls, "a", "(III)V").instructions.insert(list(new VarInsnNode(ILOAD, 3),
                call("DollyInput", "mouseInventoryKey", "(I)I"), new VarInsnNode(ISTORE, 4),
                new VarInsnNode(ILOAD, 4), new JumpInsnNode(IFEQ, vanilla),
                new VarInsnNode(ALOAD, 0), new VarInsnNode(ILOAD, 4),
                new MethodInsnNode(INVOKEVIRTUAL, "ayl", "b", "(I)Z", false),
                new JumpInsnNode(IFEQ, vanilla), new InsnNode(RETURN), vanilla));
    }

    static void mouse(ClassNode cls) {
        int axes = 0;
        MethodNode sample = method(cls, "c", "()V");
        for (AbstractInsnNode n : sample.instructions.toArray()) if (n instanceof FieldInsnNode && n.getOpcode() == PUTFIELD) {
            FieldInsnNode field = (FieldInsnNode) n;
            if (field.owner.equals("avf") && field.name.equals("b")) {
                sample.instructions.insert(n, list(new VarInsnNode(ALOAD, 0), call("DollyRawMouse", "sample", "(Lavf;)V"))); axes++;
            }
        }
        require(axes == 1, "Expected mouse Y delta assignment");
        method(cls, "a", "()V").instructions.insert(call("DollyRawMouse", "reset", "()V"));
        method(cls, "b", "()V").instructions.insert(call("DollyRawMouse", "reset", "()V"));
    }
}
