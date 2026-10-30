import org.objectweb.asm.tree.*;

/** Legacy animation integration is kept out of the general renderer patches. */
final class LegacyAnimationPatches extends PatchSupport {
    static void items(ClassNode cls) {
        beforeReturns(method(cls, "d", "()V"), RETURN, () -> list(call("DollyOldAnimations", "blockTransform", "()V")));
        MethodNode m = method(cls, "a", "(F)V");
        int blocks = 0, eating = 0;
        for (AbstractInsnNode n : m.instructions.toArray()) {
            if (!(n instanceof MethodInsnNode)) continue;
            MethodInsnNode invocation = (MethodInsnNode) n;
            if (!invocation.owner.equals("bfn")) continue;
            if (invocation.name.equals("d") && invocation.desc.equals("()V")) {
                AbstractInsnNode transform = n.getPrevious();
                while (transform != null && !(transform instanceof MethodInsnNode)) transform = transform.getPrevious();
                require(transform != null && ((MethodInsnNode) transform).desc.equals("(FF)V"), "Blocking transform moved");
                AbstractInsnNode zero = transform.getPrevious(), equip = zero.getPrevious();
                require(zero.getOpcode() == FCONST_0 && equip instanceof VarInsnNode && ((VarInsnNode) equip).var == 2, "Blocking arguments moved");
                m.instructions.set(zero, new VarInsnNode(FLOAD, 4));
                m.instructions.insert(equip, call("DollyOldAnimations", "blockEquip", "(F)F"));
                m.instructions.insertBefore(transform, call("DollyOldAnimations", "blockSwing", "(F)F")); blocks++;
            }
            if (invocation.name.equals("b") && invocation.desc.equals("(FF)V") && n.getPrevious().getOpcode() == FCONST_0) {
                AbstractInsnNode previous = n.getPrevious();
                while (previous != null && !(previous instanceof MethodInsnNode)) previous = previous.getPrevious();
                if (previous != null && ((MethodInsnNode) previous).name.equals("a") && ((MethodInsnNode) previous).desc.equals("(Lbet;F)V")) {
                    m.instructions.set(n.getPrevious(), new VarInsnNode(FLOAD, 4));
                    m.instructions.insertBefore(n, call("DollyOldAnimations", "blockSwing", "(F)F")); eating++;
                }
            }
        }
        require(blocks == 1 && eating == 1, "Expected blocking and eating paths");
    }

    static void attack(ClassNode cls) {
        MethodNode swing = method(cls, "b", "(Z)V"); int checks = 0;
        for (AbstractInsnNode n : swing.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode invocation = (MethodInsnNode) n;
            if (invocation.owner.equals("bew") && invocation.name.equals("bS") && invocation.desc.equals("()Z")) {
                swing.instructions.insert(n, call("DollyOldAnimations", "preventSwing", "(Z)Z")); checks++;
            }
        }
        require(checks == 1, "Expected one use-item swing guard");
    }

    static void controller(ClassNode cls) {
        beforeReturns(method(cls, "m", "()Z"), IRETURN, () -> list(call("DollyOldAnimations", "preventSwing", "(Z)Z")));
        LabelNode vanilla = new LabelNode();
        method(cls, "c", "(Lcj;Lcq;)Z").instructions.insert(list(call("DollyOldAnimations", "blockingMining", "()Z"),
                new JumpInsnNode(IFEQ, vanilla), new InsnNode(ICONST_1), new InsnNode(IRETURN), vanilla));
    }

    static void hearts(ClassNode cls) {
        MethodNode stats = method(cls, "d", "(Lavr;)V");
        int matches = 0;
        for (AbstractInsnNode n : stats.instructions.toArray()) if (n instanceof VarInsnNode && n.getOpcode() == ILOAD && ((VarInsnNode) n).var == 4) {
            if (++matches == 2) stats.instructions.insert(n, call("DollyOldAnimations", "previousHearts", "(Z)Z"));
        }
        require(matches == 2, "Expected container blink and previous-health blink");
    }

    static void armor(ClassNode cls) {
        for (MethodNode method : cls.methods) require(!method.name.equals("b") || !method.desc.equals("()Z"), "Unexpected armor override");
        MethodNode combine = new MethodNode(ACC_PUBLIC, "b", "()Z", null, null);
        combine.instructions.add(list(call("DollyOldAnimations", "enabled", "()Z"), new InsnNode(IRETURN)));
        cls.methods.add(combine);
    }
}
