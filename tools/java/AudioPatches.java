import org.objectweb.asm.tree.*;

final class AudioPatches extends PatchSupport {
    static void manager(ClassNode cls) {
        beforeReturns(method(cls, "a", "(Lbpj;Lbpw;Lbpg;)F"), FRETURN,
                () -> list(new VarInsnNode(ALOAD, 1), call("DollyAudio", "volume", "(FLbpj;)F")));
    }
    static void handler(ClassNode cls) {
        method(cls, "a", "(Lbni;)V").instructions.insert(handled(new InsnList(), "DollyAudio", "skipSoundReload", "()Z"));
        cls.interfaces.add("DollySoundRegistry");
        MethodNode ids = new MethodNode(ACC_PUBLIC, "dolly$soundIds", "()Ljava/util/Set;", null, null);
        ids.instructions.add(list(new VarInsnNode(ALOAD, 0), new FieldInsnNode(GETFIELD, cls.name, "e", "Lbqa;"),
                new MethodInsnNode(INVOKEVIRTUAL, "bqa", "c", "()Ljava/util/Set;", false), new InsnNode(ARETURN)));
        cls.methods.add(ids);
    }
    static void minecraft(ClassNode cls) {
        int reloads = 0;
        MethodNode tick = method(cls, "s", "()V");
        for (AbstractInsnNode n : tick.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.owner.equals("ave") && c.name.equals("e") && c.desc.equals("()V")) {
                tick.instructions.set(n, call("DollyAudio", reloads++ == 0 ? "soundKey" : "textureKey", "(Lave;)V"));
            }
        }
        require(reloads == 2, "Expected F3+S and F3+T resource reloads");
    }
}
