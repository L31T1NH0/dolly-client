import dolly.config.DollyConfig;
import net.optifine.shaders.Shaders;

/** Maps shared preferences to OptiFine's renderer; no second render backend. */
public final class DollyOptiFine {
    public static void apply(avh settings) {
        settings.u = DollySettings.config.vbo;
        settings.ofRenderRegions = DollySettings.config.renderRegions;
        settings.ofFastRender = DollySettings.config.fastRender;
        settings.ofAoLevel = DollySettings.config.smoothLight;
        disableZoom(settings);
    }

    // Keep KeyBinding's global lookup in sync with the disabled OptiFine binding.
    public static void disableZoom(avh settings) {
        if (settings.ofKeyBindZoom != null) {
            boolean changed = settings.ofKeyBindZoom.i() != 0;
            settings.ofKeyBindZoom.b(0);
            for (int i = 0; i < settings.ax.length; i++) if (settings.ax[i] == settings.ofKeyBindZoom) {
                avb[] bindings = new avb[settings.ax.length - 1];
                System.arraycopy(settings.ax, 0, bindings, 0, i);
                System.arraycopy(settings.ax, i + 1, bindings, i, bindings.length - i);
                settings.ax = bindings;
                changed = true;
                break;
            }
            if (changed) avb.b();
        }
    }

    public static void capture(avh settings) {
        DollySettings.config.vbo = settings.u;
        DollySettings.config.renderRegions = settings.ofRenderRegions;
        DollySettings.config.fastRender = settings.ofFastRender;
        DollySettings.config.smoothLight = settings.ofAoLevel;
    }

    public static void resetVideo(avh settings) {
        DollyConfig defaults = new DollyConfig(new java.io.File("config/dolly.properties").toPath());
        DollySettings.config.vbo = defaults.vbo;
        DollySettings.config.renderRegions = defaults.renderRegions;
        DollySettings.config.fastRender = defaults.fastRender;
        DollySettings.config.smoothLight = defaults.smoothLight;
        DollySettings.applyVideoDefaults(settings);
    }

    public static boolean regionsActive() { return bqs.f() && Config.isRenderRegions(); }

    public static void disableShaders() {
        if (!Config.isShaders()) return;
        Shaders.setShaderPack("OFF");
        Shaders.uninit();
        Shaders.storeConfig();
    }
}
