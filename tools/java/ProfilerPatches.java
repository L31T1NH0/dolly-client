import org.objectweb.asm.tree.*;
import java.util.*;

/** Instrument the shipped bytecode, including OptiFine's VBO/region and shader paths. */
final class ProfilerPatches extends PatchSupport {
    private static final Map<String, Integer> SYMBOLS = new LinkedHashMap<String, Integer>();
    static int symbol(String name) {
        Integer id = SYMBOLS.get(name);
        if (id == null) { id = SYMBOLS.size() + 1; SYMBOLS.put(name, id); }
        return id;
    }
    static byte[] symbols() { return (String.join("\n", SYMBOLS.keySet()) + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8); }

    private static final Set<String> DEEP = new HashSet<String>(Arrays.asList(
            "ave", "bfk", "bfr", "bec", "avo", "bht", "bhn", "bho", "bhp", "bfl", "bmh", "bfd", "bmv", "biu", "bfn"));

    static boolean patch(ClassNode cls) {
        if (cls.name.startsWith("DollyProfiler")) return false;
        boolean changed = false;
        if (cls.interfaces.contains("ff")) symbol("packet." + cls.name.replace('/', '.'));
        if (cls.name.equals("fh")) { networkQueue(cls); changed = true; }
        if (cls.name.equals("en") || cls.name.equals("eo")) { traffic(cls); changed = true; }
        boolean dolly = cls.name.startsWith("Dolly") && !cls.name.startsWith("DollyProfiler");
        boolean deep = DEEP.contains(cls.name) || cls.name.startsWith("net/optifine/render/") || cls.name.startsWith("net/optifine/shaders/");
        for (MethodNode m : cls.methods) {
            if ((m.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0 || m.name.startsWith("<")) continue;
            boolean frame = cls.name.equals("ave") && m.name.equals("av") && m.desc.equals("()V");
            String label = normalLabel(cls.name, m);
            if (instrumentGL(m)) changed = true;
            if (frame || label != null || dolly || deep) {
                wrap(m, label == null ? cls.name + "." + m.name + m.desc : label, label == null && !(dolly && normalDolly(m.name)), frame);
                changed = true;
            }
        }
        return changed;
    }
    private static boolean normalDolly(String name) {
        return name.equals("tick") || name.equals("render") || name.equals("frame") || name.equals("beginFrame")
                || name.equals("keyState") || name.equals("mouse") || name.equals("change") || name.equals("ambient");
    }
    private static String normalLabel(String c, MethodNode m) {
        String key = c + "." + m.name + m.desc;
        switch (key) {
            case "ek.a(Lio/netty/channel/ChannelHandlerContext;Lff;)V": return "network.receive";
            case "en.decode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Ljava/util/List;)V": return "network.decode";
            case "eo.a(Lio/netty/channel/ChannelHandlerContext;Lff;Lio/netty/buffer/ByteBuf;)V": return "network.encode";
            case "ave.av()V": return "frame";
            case "ave.s()V": return "tick";
            case "ave.h()V": return "display.update";
            case "bfk.a(FJ)V": return "render.frame";
            case "bfr.a(Ladf;DILpk;)I": return "render.chunk_layer";
            case "bfr.a(Lpk;Lbia;F)V": return "render.entities";
            case "bfr.a(J)V": return "chunks.update";
            case "bht.b(FFFLbhn;)V": return "chunks.rebuild";
            case "bht.a(FFFLbhn;)V": return "chunks.transparency";
            case "bec.a(Lpk;F)V": return "render.particles";
            case "bec.b(Lpk;F)V": return "render.particles_lit";
            case "bec.a()V": return "particles.tick";
            case "avo.a(F)V": return "render.hud";
            default: return null;
        }
    }
    private static boolean instrumentGL(MethodNode m) {
        boolean changed = false;
        for (AbstractInsnNode n : m.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.getOpcode() != INVOKESTATIC || !c.owner.startsWith("org/lwjgl/opengl/") || !c.name.startsWith("gl")) continue;
            boolean draw = c.name.startsWith("glDraw") && !c.name.startsWith("glDrawBuffer")
                    || c.name.startsWith("glMultiDraw") || c.name.equals("glCallList") || c.name.equals("glCallLists") || c.name.equals("glBegin");
            int token = m.maxLocals; m.maxLocals += 2;
            LabelNode invoke = new LabelNode(), done = new LabelNode();
            m.instructions.insertBefore(n, list(new InsnNode(LCONST_0), new VarInsnNode(LSTORE, token),
                    new FieldInsnNode(GETSTATIC, "DollyProfiler", "recording", "Z"), new JumpInsnNode(IFEQ, invoke),
                    new InsnNode(draw ? ICONST_1 : ICONST_0), call("DollyProfiler", "gl", "(Z)V"),
                    new LdcInsnNode(symbol(c.owner + "." + c.name + c.desc)), new InsnNode(ICONST_1), call("DollyProfiler", "begin", "(IZ)J"), new VarInsnNode(LSTORE, token), invoke));
            m.instructions.insert(n, list(new VarInsnNode(LLOAD, token), new InsnNode(LCONST_0), new InsnNode(LCMP), new JumpInsnNode(IFEQ, done),
                    new VarInsnNode(LLOAD, token), call("DollyProfiler", "end", "(J)V"), done));
            changed = true;
        }
        return changed;
    }
    static void wrap(MethodNode m, String name, boolean deepOnly, boolean frame) {
        symbol(name);
        int token = m.maxLocals; m.maxLocals += 2;
        int error = m.maxLocals++;
        LabelNode begin = new LabelNode(), end = new LabelNode(), handler = new LabelNode();
        for (AbstractInsnNode n : m.instructions.toArray()) {
            int op = n.getOpcode();
            if (op >= IRETURN && op <= RETURN) {
                InsnList exit = new InsnList();
                if (frame) exit.add(call("DollyProfilerGame", "capture", "()V"));
                exit.add(finish(name, token, frame));
                m.instructions.insertBefore(n, exit);
            }
        }
        InsnList entry = new InsnList();
        if (name.equals("render.frame")) entry.add(call("DollyProfilerGpu", "begin", "()V"));
        if (!frame) { entry.add(new LdcInsnNode(symbol(name))); entry.add(new InsnNode(deepOnly ? ICONST_1 : ICONST_0)); }
        entry.add(call("DollyProfiler", frame ? "frameBegin" : "begin", frame ? "()J" : "(IZ)J"));
        entry.add(new VarInsnNode(LSTORE, token)); entry.add(begin);
        m.instructions.insert(entry);
        m.instructions.add(end); m.instructions.add(handler);
        m.instructions.add(new VarInsnNode(ASTORE, error));
        m.instructions.add(finish(name, token, frame));
        m.instructions.add(new VarInsnNode(ALOAD, error)); m.instructions.add(new InsnNode(ATHROW));
        m.tryCatchBlocks.add(new TryCatchBlockNode(begin, end, handler, null));
    }
    private static InsnList finish(String name, int token, boolean frame) {
        InsnList code = new InsnList();
        if (name.equals("render.frame")) code.add(call("DollyProfilerGpu", "end", "()V"));
        code.add(new VarInsnNode(LLOAD, token));
        code.add(call("DollyProfiler", frame ? "frameEnd" : "end", "(J)V"));
        return code;
    }
    private static void networkQueue(ClassNode cls) {
        MethodNode m = method(cls, "a", "(Lff;Lep;Lod;)V");
        int found = 0;
        for (AbstractInsnNode n : m.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.owner.equals("od") && c.name.equals("a") && c.desc.startsWith("(Ljava/lang/Runnable;)")) {
                m.instructions.insertBefore(n, list(new VarInsnNode(ALOAD, 0), new VarInsnNode(ALOAD, 2),
                        call("DollyProfilerNetwork", "queue", "(Ljava/lang/Runnable;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Runnable;")));
                found++;
            }
        }
        require(found == 1, "Missing packet scheduling hook");
    }
    private static void traffic(ClassNode cls) {
        boolean out = cls.name.equals("eo");
        MethodNode m = method(cls, out ? "a" : "decode", out
                ? "(Lio/netty/channel/ChannelHandlerContext;Lff;Lio/netty/buffer/ByteBuf;)V"
                : "(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Ljava/util/List;)V");
        int baseline = m.maxLocals++;
        int buffer = out ? 3 : 2;
        String index = out ? "writerIndex" : "readerIndex";
        m.instructions.insert(list(new VarInsnNode(ALOAD, buffer),
                new MethodInsnNode(INVOKEVIRTUAL, "io/netty/buffer/ByteBuf", index, "()I", false), new VarInsnNode(ISTORE, baseline)));
        beforeReturns(m, RETURN, () -> list(new InsnNode(out ? ICONST_1 : ICONST_0), new VarInsnNode(ALOAD, 0),
                new FieldInsnNode(GETFIELD, cls.name, "c", "Lfg;"), new VarInsnNode(ALOAD, buffer),
                new MethodInsnNode(INVOKEVIRTUAL, "io/netty/buffer/ByteBuf", index, "()I", false),
                new VarInsnNode(ILOAD, baseline), new InsnNode(ISUB), call("DollyProfilerNetwork", "traffic", "(ZLfg;I)V")));
    }

}
