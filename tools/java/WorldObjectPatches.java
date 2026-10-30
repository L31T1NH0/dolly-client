import org.objectweb.asm.tree.*;

final class WorldObjectPatches extends PatchSupport {
    static void book(ClassNode cls) {
        // Only the book is rendered here; the table block and enchantment GUI keep their native paths.
        method(cls, "a", "(Lale;DDDFI)V").instructions.insert(
                flagReturn("hideEnchantmentBooks", list(new InsnNode(RETURN))));
    }
    static void items(ClassNode cls) {
        beforeReturns(method(cls, "a", "(Lzx;)I"), IRETURN,
                () -> list(call("DollyWorldObjects", "copies", "(I)I")));
        MethodNode transform = method(cls, "a", "(Luz;DDDFLboq;)I");
        int count = 0;
        for (AbstractInsnNode n : transform.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.owner.equals("ns") && c.name.equals("a") && c.desc.equals("(F)F")) {
                transform.instructions.insert(n, call("DollyWorldObjects", "bobSine", "(F)F")); count++;
            }
        }
        require(count == 1, "Expected one M5 dropped-item bobbing expression");
    }
}
