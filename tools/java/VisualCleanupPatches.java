import org.objectweb.asm.tree.*;

/** Exact M5 visual hooks, kept separate from simulation and network handling. */
final class VisualCleanupPatches extends PatchSupport {
    static void overlays(ClassNode cls) {
        method(cls, "e", "(F)V").instructions.insert(flagReturn("removeWaterOverlay", list(new InsnNode(RETURN))));
        MethodNode fire = method(cls, "f", "(F)V");
        fire.instructions.insert(handled(list(new VarInsnNode(FLOAD, 1)), "DollyVisuals", "skipFire", "(F)Z"));
        int opacity = 0, offset = 0;
        for (AbstractInsnNode n : fire.instructions.toArray()) if (n instanceof LdcInsnNode) {
            Object value = ((LdcInsnNode) n).cst;
            if (Float.valueOf(.9f).equals(value)) {
                fire.instructions.insert(n, list(new VarInsnNode(FLOAD, 1), call("DollyVisuals", "fireAlpha", "(FF)F"))); opacity++;
            }
            if (Float.valueOf(-.3f).equals(value)) {
                fire.instructions.insert(n, call("DollyVisuals", "fireOffset", "(F)F")); offset++;
            }
        }
        require(opacity == 1 && offset == 1, "Expected native fire alpha and vertical translation");
    }
    static void fog(ClassNode cls) {
        MethodNode fog = method(cls, "a", "(IF)V"); boolean water = false; int densities = 0;
        for (AbstractInsnNode n : fog.instructions.toArray()) {
            if (n instanceof FieldInsnNode) {
                FieldInsnNode f = (FieldInsnNode) n;
                if (f.owner.equals("arm") && f.name.equals("h")) water = true;
                if (f.owner.equals("arm") && f.name.equals("i")) water = false;
            }
            if (water && n instanceof MethodInsnNode) {
                MethodInsnNode c = (MethodInsnNode) n;
                if (c.owner.equals("bfl") && c.name.equals("a") && c.desc.equals("(F)V")) {
                    fog.instructions.insertBefore(n, call("DollyVisuals", "waterFog", "(F)F")); densities++;
                }
            }
        }
        require(densities == 2, "Expected water breathing and ordinary water fog branches");
    }
    static void potion(ClassNode cls) {
        MethodNode update = method(cls, "bi", "()V"); int count = 0;
        for (AbstractInsnNode n : update.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.owner.equals("adm") && c.name.equals("a") && c.desc.equals("(Lcy;DDDDDD[I)V")) {
                update.instructions.insertBefore(n, new VarInsnNode(ALOAD, 0));
                update.instructions.set(n, call("DollyVisuals", "potionParticle", "(Ladm;Lcy;DDDDDD[ILpr;)V")); count++;
            }
        }
        require(count == 1, "Expected only the cosmetic spawn in potion updates");
    }
    static void breaking(ClassNode cls) {
        for (String desc : new String[] {"(Lcj;Lalz;)V", "(Lcj;Lcq;)V"})
            method(cls, "a", desc).instructions.insert(flagReturn("hideBreakingParticles", list(new InsnNode(RETURN))));
        method(cls, "addBlockHitEffects", "(Lcj;Lauh;)V").instructions.insert(
                flagReturn("hideBreakingParticles", list(new InsnNode(RETURN))));
    }
    static void lightning(ClassNode cls) {
        method(cls, "a", "(Lwq;DDDFF)V").instructions.insert(flagReturn("hideLightning", list(new InsnNode(RETURN))));
    }
    static void projectile(ClassNode cls, String desc) {
        method(cls, "a", desc).instructions.insert(handled(list(new VarInsnNode(ALOAD, 1)),
                "DollyVisuals", "hideProjectile", "(Lpk;)Z"));
    }
    static void horse(ClassNode cls) {
        MethodNode model = method(cls, "a", "(Lpr;FFFFFF)V"); int count = 0;
        for (AbstractInsnNode n : model.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.owner.equals("bbo") && c.name.equals("a") && c.desc.equals("(Lpk;FFFFFF)V")) {
                model.instructions.set(n, call("DollyVisuals", "horseModel", "(Lbbo;Lpk;FFFFFF)V")); count++;
            }
        }
        require(count == 1, "Expected one living base-model draw");
    }
}
