import org.objectweb.asm.tree.*;

/** Camera-only geometry changes and client-world light/time reads for the verified M5 binary. */
final class CameraLightingPatches extends PatchSupport {
    static void renderer(ClassNode cls) {
        MethodNode orient = method(cls, "f", "(F)V"); int parallax = 0, traces = 0;
        for (AbstractInsnNode n : orient.instructions.toArray()) {
            if (n instanceof LdcInsnNode && Float.valueOf(-.1f).equals(((LdcInsnNode) n).cst)) {
                orient.instructions.insert(n, call("DollyCameraTrace", "parallax", "(F)F")); parallax++;
            }
            if (n instanceof MethodInsnNode) {
                MethodInsnNode c = (MethodInsnNode) n;
                if (c.owner.equals("bdb") && c.name.equals("a") && c.desc.equals("(Laui;Laui;)Lauh;")) {
                    orient.instructions.set(n, call("DollyCameraTrace", "trace", "(Lbdb;Laui;Laui;)Lauh;")); traces++;
                }
            }
        }
        require(parallax == 1 && traces == 1, "Expected M5 camera offset and obstruction trace");
        // The native F3 axes call orientCamera too: keep its vanilla offset, avoiding the clipping bug.
        MethodNode debug = method(cls, "h", "(F)V");
        debug.instructions.insert(list(new InsnNode(ICONST_1), new FieldInsnNode(PUTSTATIC, "DollyCamera", "debugDirections", "Z")));
        beforeReturns(debug, RETURN, () -> list(new InsnNode(ICONST_0), new FieldInsnNode(PUTSTATIC, "DollyCamera", "debugDirections", "Z")));
        LabelNode start = new LabelNode(), end = new LabelNode(), handler = new LabelNode();
        debug.instructions.insert(start); debug.instructions.add(end); debug.instructions.add(handler);
        debug.instructions.add(list(new InsnNode(ICONST_0), new FieldInsnNode(PUTSTATIC, "DollyCamera", "debugDirections", "Z"), new InsnNode(ATHROW)));
        debug.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, null));
        int sensitivity = 0;
        for (MethodNode camera : new MethodNode[] {method(cls, "a", "(FJ)V"), method(cls, "e", "()V")})
        for (AbstractInsnNode n : camera.instructions.toArray()) if (n instanceof FieldInsnNode) {
            FieldInsnNode f = (FieldInsnNode) n;
            if (f.getOpcode() == GETFIELD && f.owner.equals("avh") && f.name.equals("a") && f.desc.equals("F")) {
                camera.instructions.set(n, call("DollyZoom", "sensitivity", "(Lavh;)F")); sensitivity++;
            }
        }
        require(sensitivity == 2, "Expected native camera sensitivity read");
        MethodNode lightmap = method(cls, "g", "(F)V");
        lightmap.instructions.insert(handled(new InsnList(), "DollyLighting", "skipLightmap", "()Z"));
        int uploads = 0;
        for (AbstractInsnNode n : lightmap.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.owner.equals("blz") && c.name.equals("d") && c.desc.equals("()V")) {
                lightmap.instructions.insertBefore(n, list(new VarInsnNode(ALOAD, 0), new FieldInsnNode(GETFIELD, cls.name, "H", "[I"),
                        call("DollyLighting", "lightmap", "([I)V"))); uploads++;
            }
        }
        require(uploads == 2, "Expected M5 custom-color and native lightmap uploads");
    }
    static void hand(ClassNode cls) {
        method(cls, "a", "(F)V").instructions.insert(handled(new InsnList(), "DollyZoom", "hideHand", "()Z"));
    }
    static void world(ClassNode cls) {
        int times = 0, boxes = 0;
        for (MethodNode m : cls.methods) for (AbstractInsnNode n : m.instructions.toArray()) if (n instanceof MethodInsnNode) {
            MethodInsnNode c = (MethodInsnNode) n;
            if (c.owner.equals("ato") && c.name.equals("g") && c.desc.equals("()J")) {
                m.instructions.insert(n, list(new VarInsnNode(ALOAD, 0), call("DollyWorldTime", "displayTime", "(JLadm;)J"))); times++;
            }
            if (m.desc.equals("(Laui;Laui;ZZZ)Lauh;") && c.owner.equals("afh") && c.name.equals("a")
                    && c.desc.equals("(Ladm;Lcj;Lalz;)Laug;")) {
                m.instructions.set(n, call("DollyCameraTrace", "collision", "(Lafh;Ladm;Lcj;Lalz;)Laug;")); boxes++;
            }
        }
        require(times >= 4 && boxes == 2, "Expected direct celestial time reads and two trace collision checks");
        for (String[] target : new String[][] {{"k", "(Lcj;)I"}, {"l", "(Lcj;)I"}, {"c", "(Lcj;Z)I"},
                {"a", "(Lads;Lcj;)I"}, {"b", "(Lads;Lcj;)I"}}) light(method(cls, target[0], target[1]), cls.name, null);
        MethodNode update = method(cls, "c", "(Lads;Lcj;)Z");
        LabelNode nativeLight = new LabelNode();
        update.instructions.insert(list(new VarInsnNode(ALOAD, 0), call("DollyLighting", "active", "(Ladm;)Z"),
                new JumpInsnNode(IFEQ, nativeLight), new InsnNode(ICONST_1), new InsnNode(IRETURN), nativeLight));
    }
    static void chunk(ClassNode cls) {
        light(method(cls, "a", "(Lads;Lcj;)I"), cls.name, "i");
        light(method(cls, "a", "(Lcj;I)I"), cls.name, "i");
    }
    private static void light(MethodNode m, String owner, String worldField) {
        InsnList check = list(new VarInsnNode(ALOAD, 0));
        if (worldField != null) check.add(new FieldInsnNode(GETFIELD, owner, worldField, "Ladm;"));
        LabelNode normal = new LabelNode();
        check.add(call("DollyLighting", "active", "(Ladm;)Z")); check.add(new JumpInsnNode(IFEQ, normal));
        check.add(new IntInsnNode(BIPUSH, 15)); check.add(new InsnNode(IRETURN)); check.add(normal); m.instructions.insert(check);
    }
    static void sky(ClassNode cls) {
        int distance = 0, vbos = 0;
        for (MethodNode m : cls.methods) {
            boolean sky = m.desc.equals("(FI)V") && m.name.equals("a")
                    || m.desc.equals("()V") && (m.name.equals("o") || m.name.equals("p") || m.name.equals("q"));
            for (AbstractInsnNode n : m.instructions.toArray()) if (n instanceof FieldInsnNode && n.getOpcode() == GETFIELD) {
                FieldInsnNode f = (FieldInsnNode) n;
                if (m.desc.equals("(Lbfd;FZ)V") && f.owner.equals(cls.name) && f.name.equals("renderDistance")) {
                    m.instructions.insert(n, call("DollyCameraTrace", "skyDistance", "(I)I")); distance++;
                }
                if (sky && f.owner.equals(cls.name) && f.name.equals("Y") && f.desc.equals("Z")) {
                    m.instructions.insert(n, call("DollyCameraTrace", "skyVbo", "(Z)Z")); vbos++;
                }
            }
        }
        require(distance == 1 && vbos >= 4, "Expected M5 sky geometry/build/draw backends");
    }
}
