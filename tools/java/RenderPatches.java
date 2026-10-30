import org.objectweb.asm.tree.*;

final class RenderPatches extends PatchSupport {
    static void camera(ClassNode cls) {
        method(cls, "a", "(FJ)V").instructions.insert(call("DollyCamera", "beginFrame", "()V"));
        method(cls, "a", "(FJ)V").instructions.insert(list(new VarInsnNode(FLOAD, 1), call("DollyCulling", "beginFrame", "(F)V")));
        // Use the native cinematic filters in both the tick and mouse paths.
        for (MethodNode m : new MethodNode[] {method(cls, "e", "()V"), method(cls, "a", "(FJ)V")}) {
            int checks = 0;
            for (AbstractInsnNode n : m.instructions.toArray()) {
                if (!(n instanceof FieldInsnNode) || n.getOpcode() != GETFIELD) continue;
                FieldInsnNode field = (FieldInsnNode) n;
                if (field.owner.equals("avh") && field.name.equals("aG") && field.desc.equals("Z")) {
                    m.instructions.set(n, call("DollyCamera", "smoothCamera", "(Lavh;)Z"));
                    checks++;
                }
            }
            require(checks == 1, "Expected one smooth-camera check in " + m.name + m.desc);
        }
        MethodNode fov = method(cls, "a", "(FZ)F");
        OptiFinePatches.zoom(fov);
        beforeReturns(fov, FRETURN, () -> list(new VarInsnNode(ILOAD, 2), call("DollyCamera", "fov", "(FZ)F")));
        MethodNode orient = method(cls, "f", "(F)V");
        for (AbstractInsnNode n : orient.instructions.toArray()) {
            if (n instanceof MethodInsnNode) {
                MethodInsnNode m = (MethodInsnNode) n;
                if (m.owner.equals("pk") && m.name.equals("aS") && m.desc.equals("()F")) orient.instructions.insert(n, call("DollyCamera", "eyeHeight", "(F)F"));
            }
            if (n instanceof FieldInsnNode) {
                FieldInsnNode f = (FieldInsnNode) n;
                if (f.getOpcode() != GETFIELD || !f.owner.equals("pk") || !f.desc.equals("F")) continue;
                String name = f.name.equals("y") ? "cameraYaw" : f.name.equals("A") ? "cameraPrevYaw" : f.name.equals("z") ? "cameraPitch" : f.name.equals("B") ? "cameraPrevPitch" : null;
                if (name != null) orient.instructions.set(n, call("DollyCamera", name, "(Lpk;)F"));
            }
        }
    }
    static void particles(ClassNode cls) {
        MethodNode m = method(cls, "a", "(Lbeb;)V"); int count = 0;
        for (AbstractInsnNode n : m.instructions.toArray()) if (n instanceof IntInsnNode && ((IntInsnNode) n).operand == 4000) {
            m.instructions.set(n, call("DollyParticles", "limit", "()I")); count++;
        }
        require(count == 1, "Missing particle cap");
    }
    static void ambient(ClassNode cls) {
        MethodNode m = method(cls, "b", "(III)V"); int count = 0;
        for (AbstractInsnNode n : m.instructions.toArray()) if (n instanceof IntInsnNode && ((IntInsnNode) n).operand == 1000) {
            m.instructions.set(n, call("DollyParticles", "ambientSamples", "()I")); count++;
        }
        require(count == 1, "Missing ambient sample limit");
    }
}
