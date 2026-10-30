import org.objectweb.asm.tree.*;

/** Exact vanilla movement delegation; no Forge event bus or runtime transformer. */
final class TactilePatches extends PatchSupport {
    static void minecraft(ClassNode cls) {
        MethodNode tick = method(cls, "s", "()V");
        int starts = 0;
        for (AbstractInsnNode n : tick.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.owner.equals("DollyInput") && c.name.equals("tick")) {
                tick.instructions.insert(n, call("DollyTactile", "tickStart", "()V")); starts++;
            }
        }
        require(starts == 1, "TACTILE must start after Dolly input state recovery");
        beforeReturns(tick, RETURN, () -> list(call("DollyTactile", "tickEnd", "()V")));
        method(cls, "g", "()V").instructions.insert(call("DollyTactile", "shutdown", "()V"));
    }
    static void movement(ClassNode cls) {
        MethodNode tick = method(cls, "m", "()V"); int moves = 0;
        for (AbstractInsnNode n : tick.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.owner.equals("pr") && c.name.equals("g") && c.desc.equals("(FF)V")) {
                tick.instructions.set(n, call("DollyTactileHooks", "move", "(Lpr;FF)V")); moves++;
            }
        }
        require(moves == 1, "Expected one vanilla living movement call for TACTILE");
    }
    static void renderer(ClassNode cls) {
        MethodNode render = method(cls, "a", "(FJ)V");
        LabelNode start = new LabelNode(), end = new LabelNode(), failed = new LabelNode();
        render.instructions.insert(list(new VarInsnNode(FLOAD, 1), call("DollyTactile", "frameStart", "(F)V"), start));
        beforeReturns(render, RETURN, () -> list(call("DollyTactile", "frameEnd", "()V")));
        render.instructions.add(list(end, failed, call("DollyTactile", "frameEnd", "()V"), new InsnNode(ATHROW)));
        render.tryCatchBlocks.add(new TryCatchBlockNode(start, end, failed, null));
    }
}
