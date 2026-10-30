import org.objectweb.asm.tree.*;

final class InputPatches extends PatchSupport {
    static void minecraft(ClassNode cls) {
        MethodNode attack = method(cls, "aw", "()V");
        int hits = 0;
        for (AbstractInsnNode n : attack.instructions.toArray()) {
            if (n instanceof IntInsnNode && ((IntInsnNode) n).operand == 10) { attack.instructions.set(n, call("DollyCombat", "missDelay", "()I")); hits++; }
        }
        require(hits == 2, "Unexpected attack delay layout");
        MethodNode tick = method(cls, "s", "()V");
        tick.instructions.insert(call("DollyInput", "tick", "()V"));
        beforeReturns(tick, RETURN, () -> list(call("DollyHud", "tick", "()V")));
        int ambient = 0, states = 0, selections = 0, wheels = 0;
        for (AbstractInsnNode n : tick.instructions.toArray()) {
            if (n instanceof FieldInsnNode && n.getOpcode() == PUTFIELD) {
                FieldInsnNode field = (FieldInsnNode) n;
                if (field.owner.equals("wm") && field.name.equals("c") && field.desc.equals("I")) {
                    tick.instructions.set(n, call("DollySlots", "select", "(Lwm;I)V")); selections++;
                }
            }
            if (!(n instanceof MethodInsnNode)) continue;
            MethodInsnNode m = (MethodInsnNode) n;
            if (m.owner.equals("wm") && m.name.equals("d") && m.desc.equals("(I)V")) {
                tick.instructions.set(n, call("DollySlots", "wheel", "(Lwm;I)V")); wheels++;
            }
            if (m.owner.equals("avb") && m.name.equals("a") && m.desc.equals("(IZ)V")) {
                tick.instructions.set(n, call("DollyInput", "keyState", "(IZ)V")); states++;
            }
            if (m.owner.equals("org/lwjgl/input/Keyboard") && m.name.equals("getEventKey")) tick.instructions.set(n, call("DollyInput", "eventKey", "()I"));
            if (m.owner.equals("bdb") && m.name.equals("b") && m.desc.equals("(III)V")) {
                tick.instructions.set(n, call("DollyParticles", "ambient", "(Lbdb;III)V")); ambient++;
            }
            if (m.owner.equals("org/lwjgl/input/Mouse") && m.name.equals("getEventDWheel")) tick.instructions.set(n, call("DollyInput", "wheel", "()I"));
        }
        require(states == 2, "Expected keyboard and mouse binding state updates");
        require(selections == 1 && wheels == 1, "Expected one hotbar key selection and one wheel selection");
        OptiFinePatches.loadingBuffers(cls, 2);
        require(ambient == 1, "Expected one ambient particle update");
        MethodNode focus = method(cls, "n", "()V");
        beforeReturns(focus, RETURN, () -> list(call("DollyInput", "recoverKeys", "()V")));
        MethodNode frame = method(cls, "j", "()I");
        clear(frame, list(new VarInsnNode(ALOAD, 0), new FieldInsnNode(GETFIELD, "ave", "t", "Lavh;"),
                new FieldInsnNode(GETFIELD, "avh", "g", "I"), call("DollyFrameLimiter", "frameLimit", "(I)I"), new InsnNode(IRETURN)));
        beforeReturns(method(cls, "k", "()Z"), IRETURN, () -> list(call("DollyFrameLimiter", "shouldLimit", "(Z)Z")));
        for (MethodNode m : cls.methods) for (AbstractInsnNode n : m.instructions.toArray()) {
            if (n instanceof LdcInsnNode && "Minecraft 1.8.9".equals(((LdcInsnNode) n).cst)) ((LdcInsnNode) n).cst = "Dolly Client 1.8.9";
        }
    }
    static void look(ClassNode cls) {
        MethodNode m = method(cls, "d", "(F)Laui;");
        LabelNode vanilla = new LabelNode();
        InsnList code = list(new VarInsnNode(ALOAD, 0), new TypeInsnNode(INSTANCEOF, "bew"), new JumpInsnNode(IFEQ, vanilla));
        code.add(flagReturn("mouseFix", list(new VarInsnNode(ALOAD, 0), new VarInsnNode(FLOAD, 1),
                new MethodInsnNode(INVOKESPECIAL, "pk", "d", "(F)Laui;", false), new InsnNode(ARETURN))));
        code.add(vanilla); m.instructions.insert(code);
    }
    static void clientLook(ClassNode cls) {
        LabelNode optifine = new LabelNode();
        // OptiFine overrides getLook; route the local player through the CMPack-style toggle.
        method(cls, "d", "(F)Laui;").instructions.insert(list(new VarInsnNode(ALOAD, 0),
                new TypeInsnNode(INSTANCEOF, "bew"), new JumpInsnNode(IFEQ, optifine),
                new VarInsnNode(ALOAD, 0), new VarInsnNode(FLOAD, 1),
                new MethodInsnNode(INVOKESPECIAL, "wn", "d", "(F)Laui;", false), new InsnNode(ARETURN), optifine));
    }
    static void mouse(ClassNode cls) {
        beforeReturns(method(cls, "c", "()V"), RETURN, () -> list(new VarInsnNode(ALOAD, 0), call("DollyCamera", "mouse", "(Lavf;)V")));
    }
    static void inventory(ClassNode cls) {
        MethodNode pick = method(cls, "a", "(Lzw;IZZ)V");
        int guards = 0;
        for (AbstractInsnNode n : pick.instructions.toArray()) if (n instanceof FieldInsnNode && n.getOpcode() == PUTFIELD) {
            FieldInsnNode field = (FieldInsnNode) n;
            if (field.owner.equals("wm") && field.name.equals("c") && field.desc.equals("I")) {
                LabelNode allowed = new LabelNode();
                pick.instructions.insertBefore(n, list(new InsnNode(DUP2), call("DollySlots", "selectable", "(Lwm;I)Z"),
                        new JumpInsnNode(IFNE, allowed), new InsnNode(POP2), new InsnNode(RETURN), allowed)); guards++;
            }
        }
        require(guards == 2, "Expected two pick-block selection paths");
    }
}
