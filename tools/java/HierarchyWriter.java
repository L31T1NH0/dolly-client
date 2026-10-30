import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import java.io.*;
import java.util.*;

/** Resolves frame types from class metadata without loading or initializing game classes. */
final class HierarchyWriter extends ClassWriter {
    private final Map<String, ClassReader> metadata = new HashMap<String, ClassReader>();
    HierarchyWriter() { super(COMPUTE_FRAMES | COMPUTE_MAXS); }

    private ClassReader info(String name) {
        ClassReader cached = metadata.get(name);
        if (cached != null) return cached;
        try (InputStream input = HierarchyWriter.class.getClassLoader().getResourceAsStream(name + ".class")) {
            if (input == null) throw new IllegalStateException("Missing frame dependency: " + name);
            ClassReader reader = new ClassReader(input); metadata.put(name, reader); return reader;
        } catch (IOException e) { throw new IllegalStateException("Cannot read frame metadata: " + name, e); }
    }

    private boolean assignable(String target, String source) {
        if (target.equals(source) || target.equals("java/lang/Object")) return true;
        if (source.startsWith("[")) return target.equals("java/lang/Cloneable") || target.equals("java/io/Serializable");
        if (target.startsWith("[")) return false;
        ClassReader cls = info(source);
        if (cls.getSuperName() != null && assignable(target, cls.getSuperName())) return true;
        for (String face : cls.getInterfaces()) if (assignable(target, face)) return true;
        return false;
    }

    @Override protected String getCommonSuperClass(String a, String b) {
        if (a.equals(b)) return a;
        if (a.startsWith("[") && b.startsWith("[")) {
            String x = a.substring(1), y = b.substring(1);
            if ((!x.startsWith("L") && !x.startsWith("[")) || (!y.startsWith("L") && !y.startsWith("["))) return "java/lang/Object";
            String c = getCommonSuperClass(component(x), component(y));
            return c.startsWith("[") ? "[" + c : "[L" + c + ";";
        }
        if (assignable(a, b)) return a;
        if (assignable(b, a)) return b;
        if (a.startsWith("[") || b.startsWith("[")) return "java/lang/Object";
        if ((info(a).getAccess() & Opcodes.ACC_INTERFACE) != 0 || (info(b).getAccess() & Opcodes.ACC_INTERFACE) != 0) return "java/lang/Object";
        do { a = info(a).getSuperName(); } while (a != null && !assignable(a, b));
        return a == null ? "java/lang/Object" : a;
    }
    private String component(String descriptor) { return descriptor.startsWith("[") ? descriptor : descriptor.substring(1, descriptor.length() - 1); }
}
