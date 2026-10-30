import dolly.config.DollyConfig;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Map;

/** Owns loading, saving, resetting, and applying settings to the Minecraft adapter. */
public final class DollySettings {
    public static DollyConfig config = new DollyConfig(new File("config/dolly.properties").toPath());
    public static String lastError;

    public static void defaults(avh settings, Map<bpg, Float> sound, File gameDirectory) {
        try { config = DollyConfig.load(new File(gameDirectory, "config/dolly.properties").toPath()); }
        catch (IOException e) { throw new IllegalStateException("Cannot load Dolly configuration; original file preserved", e); }
        DollyAudio.sync();
        DollyMaintenance.start(gameDirectory);
        DollyProfiler.directory(gameDirectory);
        applyVideoDefaults(settings);
        sound.put(bpg.b, 0f);
        DollyInput.register(settings);
        DollyResources.sync();
    }

    public static void applyVideoDefaults(avh settings) {
        DollyOptiFine.apply(settings);
        settings.t = false;
        settings.d = false;
        settings.aJ = 1f;
        settings.j = 2;
    }

    public static void persist(avh settings) {
        DollyOptiFine.capture(settings);
        DollyInput.captureScoreboardKey();
        DollyInput.captureSlotProfileKey();
        try { config.save(); lastError = null; }
        catch (IOException e) { failed(e); }
    }

    public static void reset() {
        ave mc = ave.A();
        DollyOptiFine.capture(mc.t);
        DollyConfig defaults;
        try { defaults = config.reset(); }
        catch (IOException e) { failed(e); return; }
        DollyOptiFine.disableShaders();
        boolean itemModelsChanged = config.optimizedItems != defaults.optimizedItems;
        boolean framebufferChange = !config.fastRender;
        if (framebufferChange) DollyFramebuffer.releaseBeforeChange();
        try {
            config = defaults;
            DollyAudio.sync();
            DollyResources.sync();
            DollyRawMouse.reset();
            DollyTextCache.clear();
            DollyRenderedText.clear();
            DollyLighting.invalidate();
            DollyZoom.reset();
            DollyPlayers.clear();
            DollyTextureCleanup.clear();
            DollyHudCache.release();
            DollyInput.syncScoreboardKey();
            DollyInput.syncSlotProfileKey();
            applyVideoDefaults(mc.t);
            mc.t.a(bpg.b, 0f);
            org.lwjgl.opengl.Display.setVSyncEnabled(false);
            DollyInput.clear();
            if (itemModelsChanged) mc.e();
            else reloadRenderer();
        } finally { if (framebufferChange) DollyFramebuffer.recreate(); }
        mc.t.b();
    }

    public static void change(String key) {
        ave mc = ave.A();
        DollyOptiFine.capture(mc.t);
        if (key.equals("fastRender") && !config.fastRender && Config.isShaders()) {
            lastError = "Desative os shaders antes de ligar Fast Render";
            return;
        }
        boolean framebufferChange = key.equals("fastRender");
        if (framebufferChange) DollyFramebuffer.releaseBeforeChange();
        try {
            Field field = DollyConfig.class.getField(key);
            if (field.getType() == boolean.class) field.setBoolean(config, !field.getBoolean(config));
            else if (key.equals("lanButton")) config.lanButton = (config.lanButton + 1) % 3;
            else if (key.equals("backgroundFps")) config.backgroundFps = config.backgroundFps >= 120 ? 15 : config.backgroundFps + 15;
            else if (key.equals("loadingFps")) config.loadingFps = config.loadingFps >= 120 ? 15 : config.loadingFps + 15;
            else if (key.equals("menuFps")) config.menuFps = config.menuFps == 0 ? 60 : config.menuFps >= 240 ? 0 : config.menuFps + 30;
            else if (key.equals("particleLimit")) config.particleLimit = config.particleLimit >= 4000 ? 250 : config.particleLimit + 250;
            else if (key.equals("zoomFactor")) config.zoomFactor = config.zoomFactor >= 600 ? 1 : Math.min(600f, (float) Math.floor(config.zoomFactor) + 1f);
            else if (key.equals("fireHeight") || key.equals("fireOpacity") || key.equals("waterFogDensity") || key.equals("horseOpacity"))
                field.setInt(config, field.getInt(config) >= 100 ? 0 : Math.min(100, field.getInt(config) + 10));
            else if (key.equals("zoomSensitivity")) config.zoomSensitivity = config.zoomSensitivity >= .99f ? .05f : Math.min(1f, config.zoomSensitivity + .05f);
            else if (key.equals("zoomEasing")) config.zoomEasing = (config.zoomEasing + 1) % 3;
            else if (key.equals("lazyChunkLoading")) config.lazyChunkLoading = (config.lazyChunkLoading + 1) % 6;
            else if (key.equals("hudCacheFps")) config.hudCacheFps = config.hudCacheFps >= 240 ? 24 : Math.min(240, config.hudCacheFps + 1);
            else if (key.equals("timeOfDay")) config.timeOfDay = (config.timeOfDay + 6000) % 24000;
            DollyOptiFine.apply(mc.t);
            if (key.equals("renderRegions") || key.equals("fastRender") || key.equals("customSkyFix")) reloadRenderer();
            if (key.equals("fullBright") || key.equals("smartFullbright")) { DollyLighting.invalidate(); DollyLighting.sync(mc); }
            if (key.equals("toggleZoom") || key.equals("zoom")) DollyZoom.reset();
            DollyResources.sync();
            DollyHudCache.invalidate();
            if (key.equals("optimizedItems")) mc.e();
            if (key.equals("rawMouseInput")) DollyRawMouse.reset();
            if (key.equals("stringWidthCaching")) DollyTextCache.clear();
            if (key.equals("optimizedText")) DollyRenderedText.clear();
            if (key.equals("optimizedPlayerUpdating")) DollyPlayers.clear();
            if (key.equals("memoryCleanup")) DollyTextureCleanup.clear();
            if (key.equals("toggleSprint")) DollyInput.clear();
        } catch (ReflectiveOperationException e) { throw new IllegalArgumentException("Unknown Dolly option: " + key, e); }
        finally { if (framebufferChange) DollyFramebuffer.recreate(); }
        mc.t.b();
    }

    private static void reloadRenderer() {
        ave.A().g.a();
    }

    public static void optifineReset(avh settings) {
        DollyFramebuffer.releaseBeforeChange();
        try {
            DollyOptiFine.resetVideo(settings);
            settings.a(bpg.b, 0f);
        } finally { DollyFramebuffer.recreate(); }
    }

    private static void failed(IOException e) { lastError = e.getMessage(); System.err.println("[Dolly] Save failed: " + lastError); }
}
