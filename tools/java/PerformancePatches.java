import org.objectweb.asm.tree.*;

/** Version-bound hooks for the selected FoamFix, Krypton, EntityCulling and MCOpt ideas. */
final class PerformancePatches extends PatchSupport {
    static void packetBuffer(ClassNode cls) {
        clear(method(cls, "a", "(I)I"), list(new VarInsnNode(ILOAD, 0),
                call("DollyNetwork", "varIntSize", "(I)I"), new InsnNode(IRETURN)));
        clear(method(cls, "b", "(I)V"), list(new VarInsnNode(ALOAD, 0), new VarInsnNode(ILOAD, 1),
                call("DollyNetwork", "writeVarInt", "(Lio/netty/buffer/ByteBuf;I)V"), new InsnNode(RETURN)));
    }

    static void decoder(ClassNode cls) {
        clear(method(cls, "decode", "(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Ljava/util/List;)V"),
                list(new VarInsnNode(ALOAD, 2), new VarInsnNode(ALOAD, 3),
                call("DollyNetwork", "decode", "(Lio/netty/buffer/ByteBuf;Ljava/util/List;)V"), new InsnNode(RETURN)));
    }

    static void models(ClassNode cls) {
        beforeReturns(method(cls, "a", "(Lbni;)V"), RETURN, () -> list(new VarInsnNode(ALOAD, 0),
                new FieldInsnNode(GETFIELD, "bou", "a", "Ldb;"), call("DollyModels", "deduplicate", "(Ldb;)V")));
    }

    static void entities(ClassNode cls) {
        MethodNode m = method(cls, "a", "(Lpk;DDDFFZ)Z");
        LabelNode visible = new LabelNode();
        m.instructions.insert(list(new VarInsnNode(ALOAD, 1), call("DollyCulling", "entity", "(Lpk;)Z"),
                new JumpInsnNode(IFEQ, visible), new InsnNode(ICONST_0), new InsnNode(IRETURN), visible));
    }

    static void tiles(ClassNode cls) {
        method(cls, "a", "(Lakw;DDDFI)V").instructions.insert(handled(list(new VarInsnNode(ALOAD, 1)), "DollyCulling", "tile", "(Lakw;)Z"));
    }

    static void regions(ClassNode cls) {
        MethodNode m = method(cls, "finishDraw", "(Lbfy;)V");
        int count = 0;
        for (AbstractInsnNode n : m.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.owner.equals("bfl") && c.name.equals("glMultiDrawArrays") && c.desc.equals("(ILjava/nio/IntBuffer;Ljava/nio/IntBuffer;)V")) {
                m.instructions.set(n, call("DollyTerrain", "draw", c.desc)); count++;
            }
        }
        require(count == 1, "Expected one regional multi-draw");
    }
}
