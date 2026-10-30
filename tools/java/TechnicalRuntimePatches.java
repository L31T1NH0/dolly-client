import org.objectweb.asm.tree.*;

/** Version-bound resource hooks; policy lives in the runtime adapters. */
final class TechnicalRuntimePatches extends PatchSupport {
    static void minecraft(ClassNode cls) {
        MethodNode world = method(cls, "a", "(Lbdb;Ljava/lang/String;)V");
        world.instructions.insert(list(new VarInsnNode(ALOAD, 1), call("DollyResources", "worldChanging", "(Lbdb;)V")));
        int gc = 0, progress = 0;
        for (AbstractInsnNode n : world.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode m = (MethodInsnNode) n;
            if (m.owner.equals("java/lang/System") && m.name.equals("gc") && m.desc.equals("()V")) {
                world.instructions.set(n, call("DollyResources", "collect", "()V")); gc++;
            } else if (m.owner.equals("avi") && (m.name.equals("b") || m.name.equals("c")) && m.desc.equals("(Ljava/lang/String;)V")) {
                world.instructions.set(n, call("DollyResources", m.name.equals("b") ? "loadingMessage" : "loadingProgress", "(Lavi;Ljava/lang/String;)V")); progress++;
            }
        }
        require(progress == 2, "Expected two world loading progress calls");
        method(cls, "a", "(Laxu;)V").instructions.insert(list(new VarInsnNode(ALOAD, 1),
                call("DollyResources", "loadingScreen", "(Laxu;)Laxu;"), new VarInsnNode(ASTORE, 1)));
        require(gc == 1, "Expected one explicit GC on world transition");
        beforeReturns(method(cls, "s", "()V"), RETURN, () -> list(call("DollyResources", "tick", "()V")));
        method(cls, "e", "()V").instructions.insert(call("DollyResources", "reloaded", "()V"));
    }
    static void chunks(ClassNode cls) {
        // d() supplies build tasks; c() supplies reusable builders and must not be delayed.
        method(cls, "d", "()Lbhn;").instructions.insert(call("DollyResources", "paceChunks", "()V"));
    }
    static void players(ClassNode cls) {
        int speeds = 0, angles = 0;
        MethodNode tick = method(cls, "m", "()V");
        for (AbstractInsnNode n : tick.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode m = (MethodInsnNode) n;
            if (m.owner.equals("ns") && m.name.equals("a") && m.desc.equals("(D)F")) {
                tick.instructions.insertBefore(n, new VarInsnNode(ALOAD, 0));
                tick.instructions.set(n, call("DollyPlayers", "speed", "(DLbex;)F")); speeds++;
            } else if (m.owner.equals("java/lang/Math") && m.name.equals("atan") && m.desc.equals("(D)D")) {
                tick.instructions.insertBefore(n, new VarInsnNode(ALOAD, 0));
                tick.instructions.set(n, call("DollyPlayers", "angle", "(DLbex;)D")); angles++;
            }
        }
        require(speeds == 1 && angles == 1, "Expected remote player velocity functions");
    }
    static void textures(ClassNode cls) {
        beforeReturns(method(cls, "a", "(Ljy;)V"), RETURN, () -> list(new VarInsnNode(ALOAD, 0),
                new VarInsnNode(ALOAD, 1), call("DollyTextureCleanup", "bound", "(Lbmj;Ljy;)V")));
    }
}
