import org.objectweb.asm.tree.*;
final class MenuPatches extends PatchSupport {
    static void pause(ClassNode cls) {
        beforeReturns(method(cls, "b", "()V"), RETURN, () -> list(new VarInsnNode(ALOAD, 0), call("DollyNavigation", "pause", "(Laxu;)V")));
        method(cls, "a", "(Lavs;)V").instructions.insert(handled(list(new VarInsnNode(ALOAD, 0), new VarInsnNode(ALOAD, 1)),
                "DollyNavigation", "pauseClick", "(Laxp;Lavs;)Z"));
    }
    static void main(ClassNode cls) {
        method(cls, "a", "(Lavs;)V").instructions.insert(handled(list(new VarInsnNode(ALOAD, 0), new VarInsnNode(ALOAD, 1)),
                "DollyNavigation", "quit", "(Laya;Lavs;)Z"));
    }
    static void connecting(ClassNode cls) {
        method(cls, "<init>", "(Laxu;Lave;Ljava/lang/String;I)V").instructions.insert(list(new VarInsnNode(ALOAD, 3), new VarInsnNode(ILOAD, 4),
                call("DollyNavigation", "rememberAddress", "(Ljava/lang/String;I)V")));
        method(cls, "<init>", "(Laxu;Lave;Lbde;)V").instructions.insert(list(new VarInsnNode(ALOAD, 3), call("DollyNavigation", "remember", "(Lbde;)V")));
    }
    static void disconnected(ClassNode cls) {
        beforeReturns(method(cls, "b", "()V"), RETURN, () -> list(new VarInsnNode(ALOAD, 0), call("DollyNavigation", "disconnected", "(Laxu;)V")));
        method(cls, "a", "(Lavs;)V").instructions.insert(handled(list(new VarInsnNode(ALOAD, 0), new VarInsnNode(ALOAD, 1)),
                "DollyNavigation", "disconnectedClick", "(Laxu;Lavs;)Z"));
    }
}
