import org.objectweb.asm.tree.*;

/** Adapts OptiFine lifecycle and disables its second zoom implementation. */
final class OptiFinePatches extends PatchSupport {
    static void settings(ClassNode cls) {
        int conflicts = 0;
        for (MethodNode method : cls.methods) for (AbstractInsnNode n : method.instructions.toArray()) {
            if (!(n instanceof MethodInsnNode)) continue;
            MethodInsnNode m = (MethodInsnNode) n;
            // OptiFine's preferred C binding would clear Dolly's zoom on load.
            if (m.owner.equals("net/optifine/util/KeyUtils") && m.name.equals("fixKeyConflicts")) {
                require(m.desc.equals("([Lavb;[Lavb;)V"), "OptiFine key-conflict signature changed");
                method.instructions.set(n, new InsnNode(POP2));
                conflicts++;
            }
        }
        require(conflicts == 2, "Expected constructor and options-load OptiFine zoom conflicts");
        beforeReturns(method(cls, "loadOfOptions", "()V"), RETURN,
                () -> list(new VarInsnNode(ALOAD, 0), call("DollyOptiFine", "disableZoom", "(Lavh;)V")));
        MethodNode options = method(cls, "setOptionValueOF", "(Lavh$a;I)V");
        int changes = 0;
        for (AbstractInsnNode n : options.instructions.toArray()) {
            if (n instanceof FieldInsnNode && n.getOpcode() == PUTFIELD
                    && ((FieldInsnNode) n).name.equals("ofFastRender")) {
                options.instructions.insertBefore(n, call("DollyFramebuffer", "releaseBeforeChange", "()V"));
                changes++;
            }
        }
        require(changes == 1, "Expected one native Fast Render change");
        MethodNode reset = method(cls, "resetSettings", "()V");
        reset.instructions.insert(call("DollyFramebuffer", "releaseBeforeChange", "()V"));
        int reloads = 0;
        for (AbstractInsnNode n : reset.instructions.toArray()) {
            if (!(n instanceof MethodInsnNode)) continue;
            MethodInsnNode m = (MethodInsnNode) n;
            if (m.owner.equals("ave") && m.name.equals("e") && m.desc.equals("()V")) {
                reset.instructions.insertBefore(n, list(new VarInsnNode(ALOAD, 0), call("DollySettings", "optifineReset", "(Lavh;)V")));
                reloads++;
            }
        }
        require(reloads == 1, "Expected one OptiFine reset reload");
    }

    static void loadingBuffers(ClassNode cls, int expected) {
        int replacements = 0;
        for (MethodNode method : cls.methods) for (AbstractInsnNode n : method.instructions.toArray()) {
            if (!(n instanceof FieldInsnNode) || n.getOpcode() != PUTFIELD) continue;
            FieldInsnNode field = (FieldInsnNode) n;
            if (field.owner.equals("ave") && field.name.equals("n") && field.desc.equals("Lavi;")) {
                method.instructions.set(n, call("DollyFramebuffer", "replaceLoading", "(Lave;Lavi;)V"));
                replacements++;
            }
        }
        require(replacements == expected, "Unexpected loading framebuffer replacements in " + cls.name);
    }

    static void zoom(MethodNode fov) {
        int checks = 0;
        for (AbstractInsnNode n : fov.instructions.toArray()) {
            if (!(n instanceof MethodInsnNode)) continue;
            MethodInsnNode m = (MethodInsnNode) n;
            if (m.owner.equals("avh") && m.name.equals("a") && m.desc.equals("(Lavb;)Z")) {
                require(n.getPrevious() instanceof FieldInsnNode && ((FieldInsnNode) n.getPrevious()).name.equals("ofKeyBindZoom"), "Unexpected OptiFine zoom input");
                fov.instructions.insertBefore(n, list(new InsnNode(POP), new InsnNode(ICONST_0)));
                fov.instructions.remove(n);
                checks++;
            }
        }
        require(checks == 1, "Expected one OptiFine zoom check");
    }
}
