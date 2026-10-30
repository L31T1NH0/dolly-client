import org.objectweb.asm.tree.*;

/** Exact 1.8.9/M5 integration for the first performance group. */
final class GroupOnePatches extends PatchSupport {
    static void font(ClassNode cls) {
        cls.interfaces.add("DollyFontAccess");
        MethodNode original = method(cls, "a", "(Ljava/lang/String;Z)V");
        original.name = "dolly$drawText"; original.access = ACC_PUBLIC;
        MethodNode wrapper = new MethodNode(ACC_PRIVATE, "a", "(Ljava/lang/String;Z)V", null, null);
        wrapper.instructions.add(list(new VarInsnNode(ALOAD, 0), new VarInsnNode(ALOAD, 1), new VarInsnNode(ILOAD, 2),
                call("DollyRenderedText", "draw", "(Lavn;Ljava/lang/String;Z)V"), new InsnNode(RETURN)));
        cls.methods.add(wrapper);
        String[] names = {"i", "j", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "k", "l", "offsetBold"};
        MethodNode get = new MethodNode(ACC_PUBLIC, "dolly$fontState", "()[F", null, null);
        MethodNode read = new MethodNode(ACC_PUBLIC, "dolly$readFontState", "([F)V", null, null);
        MethodNode set = new MethodNode(ACC_PUBLIC, "dolly$fontState", "([F)V", null, null);
        get.instructions.add(new IntInsnNode(BIPUSH, names.length)); get.instructions.add(new IntInsnNode(NEWARRAY, T_FLOAT));
        for (int i = 0; i < names.length; i++) {
            FieldNode field = null;
            for (FieldNode f : cls.fields) if (f.name.equals(names[i])) field = f;
            require(field != null, "Missing font state " + names[i]);
            boolean integral = !field.desc.equals("F");
            get.instructions.add(list(new InsnNode(DUP), new IntInsnNode(BIPUSH, i), new VarInsnNode(ALOAD, 0),
                    new FieldInsnNode(GETFIELD, cls.name, field.name, field.desc)));
            if (integral) get.instructions.add(new InsnNode(I2F));
            get.instructions.add(new InsnNode(FASTORE));
            read.instructions.add(list(new VarInsnNode(ALOAD, 1), new IntInsnNode(BIPUSH, i), new VarInsnNode(ALOAD, 0),
                    new FieldInsnNode(GETFIELD, cls.name, field.name, field.desc)));
            if (integral) read.instructions.add(new InsnNode(I2F));
            read.instructions.add(new InsnNode(FASTORE));
            set.instructions.add(list(new VarInsnNode(ALOAD, 0), new VarInsnNode(ALOAD, 1), new IntInsnNode(BIPUSH, i), new InsnNode(FALOAD)));
            if (integral) set.instructions.add(new InsnNode(F2I));
            set.instructions.add(new FieldInsnNode(PUTFIELD, cls.name, field.name, field.desc));
        }
        get.instructions.add(new InsnNode(ARETURN)); set.instructions.add(new InsnNode(RETURN));
        read.instructions.add(new InsnNode(RETURN));
        cls.methods.add(get); cls.methods.add(set); cls.methods.add(read);
        for (MethodNode m : new MethodNode[] {method(cls, "a", "(Lbni;)V"), method(cls, "a", "(Z)V"), method(cls, "b", "(Z)V")})
            m.instructions.insert(list(new VarInsnNode(ALOAD, 0), call("DollyRenderedText", "invalidate", "(Lavn;)V")));
    }
    static void model(ClassNode cls) {
        cls.fields.add(new FieldNode(ACC_PRIVATE, "dolly$modelMode", "I", null, null));
        for (String name : new String[] {"a", "b", "c"}) {
            LabelNode same = new LabelNode();
            method(cls, name, "(F)V").instructions.insert(list(new VarInsnNode(ALOAD, 0),
                    new FieldInsnNode(GETFIELD, cls.name, "dolly$modelMode", "I"), call("DollyModelBatch", "mode", "()I"),
                    new JumpInsnNode(IF_ICMPEQ, same), new VarInsnNode(ALOAD, 0), new InsnNode(ICONST_0),
                    new FieldInsnNode(PUTFIELD, cls.name, "t", "Z"), new VarInsnNode(ALOAD, 0),
                    call("DollyModelBatch", "mode", "()I"), new FieldInsnNode(PUTFIELD, cls.name, "dolly$modelMode", "I"), same));
        }
        MethodNode compile = method(cls, "d", "(F)V");
        int begins = 0, ends = 0;
        for (AbstractInsnNode n : compile.instructions.toArray()) {
            if (n instanceof MethodInsnNode) {
                MethodInsnNode c = (MethodInsnNode) n;
                if (c.owner.equals("bfx") && c.name.equals("c") && c.desc.equals("()Lbfd;")) {
                    compile.instructions.insert(n, list(new InsnNode(DUP), call("DollyModelBatch", "begin", "(Lbfd;)V"))); begins++;
                }
            }
            if (n instanceof JumpInsnNode && n.getOpcode() == IF_ICMPGE && ends == 0) {
                compile.instructions.insert(((JumpInsnNode) n).label, call("DollyModelBatch", "end", "()V")); ends++;
            }
        }
        require(begins == 1 && ends == 1, "Expected cube loop before OptiFine sprites");
    }
    static void quad(ClassNode cls) {
        int begins = 0, ends = 0;
        for (AbstractInsnNode n : method(cls, "a", "(Lbfd;F)V").instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.owner.equals("bfd") && c.name.equals("a") && c.desc.equals("(ILbmu;)V")) {
                method(cls, "a", "(Lbfd;F)V").instructions.set(n, call("DollyModelBatch", "quadBegin", "(Lbfd;ILbmu;)V")); begins++;
            } else if (c.owner.equals("bfx") && c.name.equals("b") && c.desc.equals("()V")) {
                method(cls, "a", "(Lbfd;F)V").instructions.set(n, call("DollyModelBatch", "quadEnd", "(Lbfx;)V")); ends++;
            }
        }
        require(begins == 2 && ends == 1, "Expected M5 native/shader quad paths");
    }
    static void particle(ClassNode cls) {
        MethodNode render = method(cls, "a", "(Lbfd;Lpk;FFFFFF)V"); int count = 0;
        for (AbstractInsnNode n : render.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.owner.equals("beb") && c.name.equals("b") && c.desc.equals("(F)I")) {
                render.instructions.set(n, call("DollyParticles", "brightness", "(Lbeb;F)I")); count++;
            }
        }
        require(count == 1, "Expected one base particle light lookup");
    }
    static void pack(ClassNode cls) {
        beforeReturns(method(cls, "a", "()Ljava/awt/image/BufferedImage;"), ARETURN,
                () -> list(call("DollyPackIcons", "resize", "(Ljava/awt/image/BufferedImage;)Ljava/awt/image/BufferedImage;")));
    }
    static void node(ClassNode cls) {
        method(cls, "a", "()V").instructions.insert(list(new VarInsnNode(ALOAD, 0), new InsnNode(ACONST_NULL),
                new FieldInsnNode(PUTFIELD, cls.name, "a", "Ladq;")));
    }
    static void torch(ClassNode cls) {
        int count = 0;
        for (AbstractInsnNode n : method(cls, "<clinit>", "()V").instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.owner.equals("com/google/common/collect/Maps") && c.name.equals("newHashMap")) {
                method(cls, "<clinit>", "()V").instructions.insertBefore(n, list(new TypeInsnNode(NEW, "java/util/WeakHashMap"),
                        new InsnNode(DUP), new MethodInsnNode(INVOKESPECIAL, "java/util/WeakHashMap", "<init>", "()V", false)));
                method(cls, "<clinit>", "()V").instructions.remove(n); count++;
            }
        }
        require(count == 1, "Expected one redstone world map");
    }
    static void enchantments(ClassNode cls) {
        cleanup(cls, "a", "([Lzx;Low;)I", "b", "ack$e", new String[] {"b"}, new String[] {"Low;"});
        cleanup(cls, "a", "(Lzx;Lpw;)F", "c", "ack$d", new String[] {"b"}, new String[] {"Lpw;"});
        cleanup(cls, "a", "(Lpr;Lpk;)V", "d", "ack$b", new String[] {"a", "b"}, new String[] {"Lpr;", "Lpk;"});
        cleanup(cls, "b", "(Lpr;Lpk;)V", "e", "ack$a", new String[] {"a", "b"}, new String[] {"Lpr;", "Lpk;"});
    }
    private static void cleanup(ClassNode cls, String name, String desc, String holder, String type, String[] fields, String[] types) {
        MethodNode m = method(cls, name, desc);
        java.util.function.Supplier<InsnList> clear = () -> {
            InsnList code = new InsnList();
            for (int i = 0; i < fields.length; i++) code.add(list(new FieldInsnNode(GETSTATIC, cls.name, holder, "L" + type + ";"),
                    new InsnNode(ACONST_NULL), new FieldInsnNode(PUTFIELD, type, fields[i], types[i])));
            return code;
        };
        int returns = desc.endsWith("I") ? IRETURN : desc.endsWith("F") ? FRETURN : RETURN;
        beforeReturns(m, returns, clear);
        LabelNode start = new LabelNode(), end = new LabelNode(), handler = new LabelNode();
        m.instructions.insert(start); m.instructions.add(end); m.instructions.add(handler);
        m.instructions.add(clear.get()); m.instructions.add(new InsnNode(ATHROW));
        m.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, null));
    }
}
