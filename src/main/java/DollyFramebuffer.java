import java.lang.reflect.Field;

/** Framebuffer lifecycle when Fast Render changes; all calls stay on the render thread. */
public final class DollyFramebuffer {
    private static final Field LOADING_BUFFER;
    static {
        try {
            LOADING_BUFFER = avi.class.getDeclaredField("g");
            if (LOADING_BUFFER.getType() != bfw.class) throw new NoSuchFieldException("Loading framebuffer type changed");
            LOADING_BUFFER.setAccessible(true);
        } catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }
    private static bfw loading(ave mc) {
        if (mc.n == null) return null;
        try { return (bfw) LOADING_BUFFER.get(mc.n); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
    public static void replaceLoading(ave mc, avi next) {
        bfw old = loading(mc);
        if (old != null) old.a();
        mc.n = next;
    }
    public static void releaseBeforeChange() {
        DollyHudCache.release();
        if (!bqs.i()) return;
        ave mc = ave.A();
        if (mc.o != null) mc.o.b();
        if (mc.b() != null) mc.b().a();
        bfw loading = loading(mc);
        if (loading != null) loading.a();
    }
    public static void recreate() {
        DollyHudCache.release();
        ave mc = ave.A();
        if (mc.b() != null) mc.b().a(mc.d, mc.e);
        bfw loading = loading(mc);
        if (loading != null) loading.a(mc.d, mc.e);
        if (mc.o != null) mc.o.a(mc.d, mc.e);
    }
}
