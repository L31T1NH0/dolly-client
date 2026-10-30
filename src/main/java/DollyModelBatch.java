import org.lwjgl.opengl.GL11;

/** Only batches the cube loop; OptiFine sprites and shader formats keep their native path. */
public final class DollyModelBatch {
    private static bfd buffer;
    private static boolean culling;

    public static int mode() {
        if (Config.isShaders()) return 0;
        return (DollySettings.config.batchModels ? 1 : 0) | (DollySettings.config.modelBackFaces ? 2 : 0);
    }
    public static void begin(bfd renderer) {
        int mode = mode();
        culling = (mode & 2) != 0;
        if (culling) {
            // These commands live inside the model display list, and restore the caller's culling state.
            GL11.glPushAttrib(GL11.GL_POLYGON_BIT | GL11.GL_ENABLE_BIT);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(GL11.GL_BACK);
        }
        if ((mode & 1) != 0) {
            buffer = renderer;
            renderer.a(GL11.GL_QUADS, bms.c);
        }
    }
    public static void end() {
        if (buffer != null) {
            try { bfx.a().b(); } finally { buffer = null; }
        }
        if (culling) { GL11.glPopAttrib(); culling = false; }
    }
    public static void quadBegin(bfd renderer, int mode, bmu format) {
        if (renderer != buffer) renderer.a(mode, format);
    }
    public static void quadEnd(bfx tessellator) {
        if (buffer == null || tessellator.c() != buffer) tessellator.b();
    }
}
