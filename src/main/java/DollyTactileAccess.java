import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Version-bound reflection for private vanilla timers and movement bookkeeping. */
final class DollyTactileAccess {
    static Field findField(Class<?> type, String... names) {
        for (String name : names) try {
            Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
        } catch (NoSuchFieldException ignored) { }
        throw new IllegalStateException("Missing TACTILE field in " + type.getName());
    }
    static Method findMethod(Class<?> type, Object unused, String[] names, Class<?>... parameters) {
        for (String name : names) try {
            Method method = type.getDeclaredMethod(name, parameters); method.setAccessible(true); return method;
        } catch (NoSuchMethodException ignored) { }
        throw new IllegalStateException("Missing TACTILE method in " + type.getName());
    }
    @SuppressWarnings("unchecked") static <T> T getPrivateValue(Class<?> type, Object target, String... names) {
        try { return (T) findField(type, names).get(target); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
}
