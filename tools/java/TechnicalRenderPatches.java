import org.objectweb.asm.tree.*;

/** Hooks before font measurement and generated-item baking; no vertex-format assumptions. */
final class TechnicalRenderPatches extends PatchSupport {
    static void font(ClassNode cls) {
        MethodNode width = method(cls, "a", "(Ljava/lang/String;)I");
        beforeReturns(width, IRETURN, () -> list(new VarInsnNode(ALOAD, 0), new VarInsnNode(ALOAD, 1),
                call("DollyTextCache", "remember", "(ILavn;Ljava/lang/String;)I")));
        LabelNode miss = new LabelNode();
        width.instructions.insert(list(new VarInsnNode(ALOAD, 0), new VarInsnNode(ALOAD, 1),
                call("DollyTextCache", "lookup", "(Lavn;Ljava/lang/String;)Ljava/lang/Integer;"),
                new InsnNode(DUP), new JumpInsnNode(IFNULL, miss),
                new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false),
                new InsnNode(IRETURN), miss, new InsnNode(POP)));
        for (MethodNode m : new MethodNode[] {method(cls, "a", "(Lbni;)V"),
                method(cls, "a", "(Z)V"), method(cls, "b", "(Z)V")}) {
            m.instructions.insert(list(new VarInsnNode(ALOAD, 0), call("DollyTextCache", "invalidate", "(Lavn;)V")));
        }
    }

    static void items(ClassNode cls) {
        MethodNode generate = method(cls, "a", "(ILjava/lang/String;Lbmi;)Ljava/util/List;");
        LabelNode normal = new LabelNode();
        generate.instructions.insert(list(new VarInsnNode(ILOAD, 1), new VarInsnNode(ALOAD, 2), new VarInsnNode(ALOAD, 3),
                call("DollyItemStitching", "outline", "(ILjava/lang/String;Lbmi;)Ljava/util/List;"),
                new InsnNode(DUP), new JumpInsnNode(IFNULL, normal), new InsnNode(ARETURN), normal, new InsnNode(POP)));
        beforeReturns(generate, ARETURN, () -> list(call("DollyItemStitching", "unlerp", "(Ljava/util/List;)Ljava/util/List;")));
    }
}
