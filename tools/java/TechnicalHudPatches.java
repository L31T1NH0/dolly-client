import java.util.*;
import org.objectweb.asm.tree.*;

/** Split only the version-verified vignette/portal/pumpkin and inversion-crosshair paths. */
final class TechnicalHudPatches extends PatchSupport {
    private static boolean matches(AbstractInsnNode n, String owner, String name, String desc) {
        if (!(n instanceof MethodInsnNode)) return false;
        MethodInsnNode m = (MethodInsnNode) n;
        return m.owner.equals(owner) && m.name.equals(name) && m.desc.equals(desc);
    }
    private static AbstractInsnNode previous(AbstractInsnNode n, int count) {
        while (count-- > 0) { n = n.getPrevious(); while (n.getOpcode() < 0) n = n.getPrevious(); }
        return n;
    }
    static void hud(ClassNode cls) {
        MethodNode original = method(cls, "a", "(F)V");
        AbstractInsnNode vignette = null, hotbar = null, crosshair = null, crosshairStart = null, end = null;
        LabelNode crosshairEnd = null;
        for (AbstractInsnNode n : original.instructions.toArray()) {
            if (matches(n, "Config", "isVignetteEnabled", "()Z")) vignette = n;
            if (hotbar == null && matches(n, "bda", "a", "()Z")) hotbar = previous(n, 3);
            if (matches(n, "avo", "b", "()Z")) {
                crosshair = previous(n, 1);
                AbstractInsnNode jump = n.getNext(); while (jump.getOpcode() < 0) jump = jump.getNext();
                require(jump instanceof JumpInsnNode && jump.getOpcode() == IFEQ, "Unexpected crosshair branch");
                crosshairEnd = ((JumpInsnNode) jump).label;
            }
        }
        require(vignette != null && hotbar != null && crosshair != null && crosshairEnd != null,
                "Expected native HUD dynamic layers");
        // The icons binding/color reset immediately after the two hotbar branches starts with four FCONST_1s.
        for (AbstractInsnNode n = hotbar; n != crosshair; n = n.getNext()) {
            if (n.getOpcode() == FCONST_1) {
                AbstractInsnNode call = n;
                for (int i = 0; i < 4; i++) { call = call.getNext(); while (call.getOpcode() < 0) call = call.getNext(); }
                if (matches(call, "bfl", "c", "(FFFF)V")) { crosshairStart = n; break; }
            }
        }
        for (AbstractInsnNode n = crosshairEnd; n != null; n = n.getNext()) {
            if (matches(n, "bfl", "a", "(IIII)V")) { end = n.getNext(); break; }
        }
        require(crosshairStart != null && end != null, "Expected crosshair GL state reset");
        Map<LabelNode, LabelNode> labels = new HashMap<LabelNode, LabelNode>();
        for (AbstractInsnNode n : original.instructions.toArray()) if (n instanceof LabelNode) labels.put((LabelNode) n, new LabelNode());
        MethodNode dynamic = new MethodNode(ACC_PUBLIC, "dolly$dynamic", "(F)V", null, null);
        for (AbstractInsnNode n = original.instructions.getFirst(); n != hotbar; n = n.getNext()) {
            if (!(n instanceof FrameNode) && !(n instanceof LineNumberNode)) dynamic.instructions.add(n.clone(labels));
        }
        for (AbstractInsnNode n = crosshairStart; n != end; n = n.getNext()) {
            if (!(n instanceof FrameNode) && !(n instanceof LineNumberNode)) dynamic.instructions.add(n.clone(labels));
        }
        dynamic.instructions.add(new InsnNode(RETURN));
        dynamic.maxLocals = original.maxLocals;
        cls.methods.add(dynamic); cls.interfaces.add("DollyHudLayers");
        LabelNode afterBackground = new LabelNode(); original.instructions.insertBefore(hotbar, afterBackground);
        original.instructions.insertBefore(vignette, list(new FieldInsnNode(GETSTATIC, "DollyHudCache", "rendering", "Z"),
                new JumpInsnNode(IFNE, afterBackground)));
        original.instructions.insertBefore(crosshair, list(new FieldInsnNode(GETSTATIC, "DollyHudCache", "rendering", "Z"),
                new JumpInsnNode(IFNE, crosshairEnd)));
    }

    static void renderer(ClassNode cls) {
        int count = 0;
        for (MethodNode m : cls.methods) for (AbstractInsnNode n : m.instructions.toArray()) {
            if (matches(n, "avo", "a", "(F)V")) {
                m.instructions.set(n, call("DollyHudCache", "render", "(Lavo;F)V")); count++;
            }
        }
        require(count == 1, "Expected one native HUD render call");
    }

    static void blend(ClassNode cls) {
        MethodNode simple = method(cls, "b", "(II)V");
        LabelNode vanilla = new LabelNode();
        simple.instructions.insert(list(new VarInsnNode(ILOAD, 0), new VarInsnNode(ILOAD, 1),
                call("DollyHudCache", "blend", "(II)Z"), new JumpInsnNode(IFEQ, vanilla), new InsnNode(RETURN), vanilla));
        MethodNode separate = method(cls, "a", "(IIII)V");
        separate.instructions.insert(list(new VarInsnNode(ILOAD, 0), new VarInsnNode(ILOAD, 1), new VarInsnNode(ILOAD, 2),
                call("DollyHudCache", "alphaSource", "(III)I"), new VarInsnNode(ISTORE, 2),
                new VarInsnNode(ILOAD, 0), new VarInsnNode(ILOAD, 1), new VarInsnNode(ILOAD, 3),
                call("DollyHudCache", "alphaDestination", "(III)I"), new VarInsnNode(ISTORE, 3)));
    }
}
