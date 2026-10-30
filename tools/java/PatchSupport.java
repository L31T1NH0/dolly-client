import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;

class PatchSupport implements Opcodes {
    static MethodNode method(ClassNode cls, String name, String descriptor) {
        for (MethodNode m : cls.methods) if (m.name.equals(name) && m.desc.equals(descriptor)) return m;
        throw new IllegalStateException("Missing target: " + cls.name + "." + name + descriptor);
    }
    static MethodInsnNode call(String owner, String name, String descriptor) { return new MethodInsnNode(INVOKESTATIC, owner, name, descriptor, false); }
    static InsnList list(AbstractInsnNode... nodes) { InsnList list = new InsnList(); for (AbstractInsnNode n : nodes) list.add(n); return list; }
    static void beforeReturns(MethodNode method, int opcode, java.util.function.Supplier<InsnList> code) {
        int count = 0;
        for (AbstractInsnNode n : method.instructions.toArray()) if (n.getOpcode() == opcode) { method.instructions.insertBefore(n, code.get()); count++; }
        require(count > 0, "No return in " + method.name + method.desc);
    }
    static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    static void clear(MethodNode method, InsnList code) {
        method.instructions.clear(); method.instructions.add(code); method.tryCatchBlocks.clear(); method.localVariables = null;
    }
    static InsnList flagReturn(String field, InsnList action) {
        LabelNode vanilla = new LabelNode();
        InsnList code = list(new FieldInsnNode(GETSTATIC, "DollySettings", "config", "Ldolly/config/DollyConfig;"),
                new FieldInsnNode(GETFIELD, "dolly/config/DollyConfig", field, "Z"), new JumpInsnNode(IFEQ, vanilla));
        code.add(action); code.add(vanilla); return code;
    }
    static InsnList handled(InsnList args, String owner, String method, String desc) {
        LabelNode vanilla = new LabelNode(); args.add(call(owner, method, desc)); args.add(new JumpInsnNode(IFEQ, vanilla)); args.add(new InsnNode(RETURN)); args.add(vanilla); return args;
    }
}
