import java.nio.file.*;
import java.util.*;

/** Checks actual 1.8.9 fields and helpers without a display or player session. */
public final class DollySettingsTest {
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("dolly-settings-test-");
        try {
            Class.forName("DollyFramebuffer");
            avh settings = new avh();
            Map<bpg, Float> audio = new HashMap<bpg, Float>();
            DollySettings.defaults(settings, audio, dir.toFile());
            assert settings.u && !settings.t && !settings.d && settings.aJ == 1f && settings.j == 2;
            assert audio.get(bpg.b) == 0f;
            assert DollySettings.config.fastRender && DollySettings.config.renderRegions;
            assert settings.ofFastRender && settings.ofRenderRegions && settings.ofAoLevel == 0.25f;
            settings.u = false; settings.ofFastRender = false; settings.ofRenderRegions = false; settings.ofAoLevel = 0.7f;
            DollyOptiFine.capture(settings);
            assert !DollySettings.config.vbo && !DollySettings.config.fastRender && !DollySettings.config.renderRegions;
            assert DollySettings.config.smoothLight == 0.7f;
            DollyOptiFine.resetVideo(settings);
            assert settings.u && settings.ofFastRender && settings.ofRenderRegions && settings.ofAoLevel == 0.25f;
            settings.ofKeyBindZoom = new avb("of.key.zoom", 46, "key.categories.misc");
            settings.ax = new avb[] {settings.ofKeyBindZoom, settings.Y};
            DollyOptiFine.apply(settings);
            assert settings.ofKeyBindZoom.i() == 0 && settings.ax.length == 1 && settings.ax[0] == settings.Y;
            avb original = new avb("audit.original", 33, "key.categories.misc");
            settings.ofKeyBindZoom = new avb("of.key.zoom", 33, "key.categories.misc");
            avb.b();
            DollyOptiFine.apply(settings);
            avb.a(33, true);
            assert original.d() && !settings.ofKeyBindZoom.d() : "Disabled zoom kept the old key map entry";
            original.b(-99);
            avb.b();
            avb.a(-99, true);
            assert original.i() == -99 && original.d();
            avb.a(-99, false);
            assert !original.d();
            assert DollyCombat.missDelay() == 0;
            assert DollySettings.config.scrollLock;
            assert DollyInput.wheel(120, false, false) == 0;
            assert DollyInput.wheel(-120, false, false) == 0;
            assert DollyInput.wheel(120, true, false) == 120;
            assert DollyInput.wheel(-120, true, true) == -120;
            float zoomFactor = DollySettings.config.zoomFactor;
            assert DollyInput.wheel(120, false, true) == 0;
            assert DollySettings.config.zoomFactor == zoomFactor * 1.25f;
            assert DollyInput.wheel(-120, false, true) == 0;
            assert DollySettings.config.zoomFactor == zoomFactor * 1.25f * .75f;
            DollySettings.config.scrollLock = false;
            assert DollyInput.wheel(120, false, false) == 120;
            assert DollyInput.wheel(-120, false, false) == -120;
            assert DollyInput.wheel(120, false, true) == 0;
            DollySettings.config.zoomFactor = zoomFactor;
            DollySettings.config.scrollLock = true;
            DollySettings.config.clickFix = false;
            assert DollyCombat.missDelay() == 10;
            assert DollyOldAnimations.enabled() && !DollyOldAnimations.previousHearts(true) && !DollyOldAnimations.preventSwing(true);
            DollySettings.config.oldAnimations = false;
            assert DollyOldAnimations.previousHearts(true) && DollyOldAnimations.preventSwing(true);
            assert DollyOldAnimations.blockEquip(0.5f) == 0.5f && DollyOldAnimations.blockSwing(0.5f) == 0;
            DollySettings.config.oldAnimations = true;
            assert DollyOldAnimations.blockEquip(0.5f) == -0.1f && DollyOldAnimations.blockSwing(0.5f) == 0.5f;
            settings.t = true; settings.d = true; settings.aJ = 0f;
            DollySettings.applyVideoDefaults(settings);
            assert !settings.t && !settings.d && settings.aJ == 1f;
            DollySettings.config.save();
            assert Files.isRegularFile(dir.resolve("config/dolly.properties"));
        } finally {
            Files.deleteIfExists(dir.resolve("config/dolly.properties"));
            Files.deleteIfExists(dir.resolve("config"));
            Files.deleteIfExists(dir);
        }
        System.out.println("Minecraft/OptiFine settings integration passed: shared settings, native-menu capture, reset, zoom binding and click penalty.");
    }
}
