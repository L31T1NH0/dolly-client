import dolly.config.DollyConfig;
import java.nio.file.*;
import java.util.*;

public final class DollyCoreTest {
    public static void main(String[] args) throws Exception {
        DollyChatHistoryTest.run();
        DollySoundVolumesTest.run();
        DollySlotProfileTest.run();
        DollyLayoutTest.run();
        DollyTactileProfilesTest.run();
        DollyTactilePhysicsTest.run();
        Path directory = Files.createTempDirectory("dolly-core-test-");
        Path file = directory.resolve("dolly.properties");
        try {
            DollyConfig c = DollyConfig.load(file);
            assert c.vbo && c.renderRegions && c.fastRender && c.smoothLight == 0.25f && !c.ambientParticles;
            assert c.scrollLock && c.mouseFix;
            assert c.hudLayout.isEmpty() && c.hudSnap;
            assert c.separateResourceLoading && c.soundVolumes.isEmpty();
            assert c.hideFireResistance && c.removeWaterOverlay && c.cleanView && c.hideBreakingParticles && c.hideLightning && c.cleanProjectiles;
            assert c.fireHeight == 100 && c.fireOpacity == 100 && c.waterFogDensity == 100 && c.horseOpacity == 100;
            assert c.zoomScroll && c.smoothZoom && c.smoothScrollZoom && !c.toggleZoom;
            assert c.smartFullbright && c.betterCamera && c.customSkyFix && c.parallaxFix;
            assert c.zoomSensitivity == 1f && c.zoomEasing == 0;
            assert c.hideEnchantmentBooks && c.singleGroundItem && c.staticGroundItems;
            assert c.mouseBindFix && c.inputFix && c.stringWidthCaching && c.optimizedPlayerUpdating;
            assert !c.rawMouseInput && c.asyncScreenshot && !c.fastLanguageSwitch && c.fastWorldLoading;
            assert c.hudCaching && c.hudCacheFps == 24 && !c.memoryCleanup && c.alternateDns && !c.fallbackDns;
            assert c.optimizedText && c.batchModels && c.modelBackFaces && c.optimizedItems;
            assert c.staticParticleColor && c.smallPackIcons && c.lowAnimationTick;
            assert c.lazyChunkLoading == 1;
            Files.write(file, Collections.singletonList("itemStitching=0"));
            DollyConfig.load(file); // Legacy choice is ignored; Unlerp is now unconditional.
            Files.delete(file);
            c.rawMouseInput = true; c.lazyChunkLoading = 5; c.hudCacheFps = 120;
            assert !c.slotProfile && c.blockedSlots == 0 && c.slotProfileKey == 0;
            c.slotProfile = true; c.blockedSlots = 42; c.slotProfileKey = -99;
            assert c.chatGroup && c.chatKeepHistory && c.chatHistory == 1000;
            assert c.scoreboardVisible && c.scoreboardNumbers && c.scoreboardKey == 0;
            c.chatHiddenWords = "anúncio;loja"; c.chatMentionWords = "cama;Dolly";
            c.chatScale = 1.25f; c.scoreboardX = 15; c.scoreboardNumbers = false;
            assert c.entityCulling && c.mergeTerrain && c.loadingFps == 30;
            c.backgroundFps = 45; c.fullBright = true; c.scrollLock = false;
            c.entityCulling = false; c.mergeTerrain = false; c.loadingFps = 45;
            c.optimizedText = false; c.batchModels = false; c.modelBackFaces = false; c.optimizedItems = false;
            c.staticParticleColor = false; c.smallPackIcons = false; c.lowAnimationTick = false;
            c.hideEnchantmentBooks = false; c.singleGroundItem = false; c.staticGroundItems = false;
            c.toggleZoom = true; c.directZoomCamera = true; c.dynamicZoomSensitivity = true; c.renderHandZoom = true;
            c.parallaxFix = true; c.betterCamera = false; c.customSkyFix = false; c.smartFullbright = false;
            c.zoomSensitivity = .35f; c.zoomEasing = 2;
            c.hideFireResistance = false; c.removeWaterOverlay = false; c.cleanView = false;
            c.hideBreakingParticles = false; c.hideLightning = false; c.cleanProjectiles = false;
            c.fireHeight = 50; c.fireOpacity = 40; c.waterFogDensity = 30; c.horseOpacity = 20;
            c.tactileProfiles = "*=EXACT,960,20,AUTO,2000,true,1";
            c.hudLayout = "ping:25.0:50.0:1.25:false:35"; c.hudSnap = false;
            c.separateResourceLoading = false; c.soundVolumes = "minecraft:random.click=0;pack:custom=175"; c.save();
            DollyConfig loaded = DollyConfig.load(file);
            assert loaded.hudLayout.equals(c.hudLayout) && !loaded.hudSnap;
            assert !loaded.hideFireResistance && !loaded.removeWaterOverlay && !loaded.cleanView && !loaded.hideBreakingParticles && !loaded.hideLightning && !loaded.cleanProjectiles;
            assert !loaded.separateResourceLoading && loaded.soundVolumes.equals("minecraft:random.click=0;pack:custom=175");
            assert loaded.fireHeight == 50 && loaded.fireOpacity == 40 && loaded.waterFogDensity == 30 && loaded.horseOpacity == 20;
            assert loaded.toggleZoom && loaded.directZoomCamera && loaded.dynamicZoomSensitivity && loaded.renderHandZoom;
            assert loaded.tactileProfiles.equals("*=EXACT,960,20,AUTO,2000,true,1");
            assert loaded.parallaxFix && !loaded.betterCamera && !loaded.customSkyFix && !loaded.smartFullbright;
            assert loaded.zoomSensitivity == .35f && loaded.zoomEasing == 2;
            assert !loaded.hideEnchantmentBooks && !loaded.singleGroundItem && !loaded.staticGroundItems;
            assert loaded.rawMouseInput && loaded.lazyChunkLoading == 5 && loaded.hudCacheFps == 120;
            assert !loaded.optimizedText && !loaded.batchModels && !loaded.modelBackFaces && !loaded.optimizedItems;
            assert !loaded.staticParticleColor && !loaded.smallPackIcons && !loaded.lowAnimationTick;
            assert loaded.backgroundFps == 45 && loaded.fullBright && !loaded.scrollLock;
            assert loaded.slotProfile && loaded.blockedSlots == 42 && loaded.slotProfileKey == -99;
            assert loaded.chatHiddenWords.equals("anúncio;loja") && loaded.chatMentionWords.equals("cama;Dolly");
            assert loaded.chatScale == 1.25f && loaded.scoreboardX == 15 && !loaded.scoreboardNumbers;
            assert !loaded.entityCulling && !loaded.mergeTerrain && loaded.loadingFps == 45;
            DollyConfig reset = loaded.reset();
            assert reset.hudLayout.isEmpty() && reset.hudSnap;
            assert reset.tactileProfiles.isEmpty();
            assert !reset.rawMouseInput && reset.lazyChunkLoading == 1 && reset.hudCacheFps == 24;
            assert reset.hideFireResistance && reset.removeWaterOverlay && reset.cleanView && reset.hideBreakingParticles && reset.hideLightning && reset.cleanProjectiles;
            assert reset.separateResourceLoading && reset.soundVolumes.isEmpty();
            assert reset.fireHeight == 100 && reset.fireOpacity == 100 && reset.waterFogDensity == 100 && reset.horseOpacity == 100;
            assert !reset.toggleZoom && !reset.directZoomCamera && !reset.dynamicZoomSensitivity && !reset.renderHandZoom;
            assert reset.parallaxFix && reset.betterCamera && reset.customSkyFix && reset.smartFullbright;
            assert reset.zoomSensitivity == 1f && reset.zoomEasing == 0;
            assert reset.hideEnchantmentBooks && reset.singleGroundItem && reset.staticGroundItems;
            assert reset.optimizedText && reset.batchModels && reset.modelBackFaces && reset.optimizedItems;
            assert reset.staticParticleColor && reset.smallPackIcons && reset.lowAnimationTick;
            assert reset.fastRender && reset.backgroundFps == 30 && !reset.fullBright && reset.scrollLock;
            assert reset.entityCulling && reset.mergeTerrain && reset.loadingFps == 30;
            assert !reset.slotProfile && reset.blockedSlots == 0 && reset.slotProfileKey == 0;
            assert reset.chatHiddenWords.isEmpty() && reset.chatScale == 1 && reset.scoreboardX == 100 && reset.scoreboardNumbers;
            for (String invalid : Arrays.asList("lazyChunkLoading=6", "lazyChunkLoading=-1", "hudCacheFps=0", "rawMouseInput=perhaps", "chatScale=NaN", "scoreboardScale=Infinity", "soundVolumes=minecraft:x=-1", "soundVolumes=minecraft:x=201", "soundVolumes=bad=5", "fireHeight=-1", "fireHeight=101", "fireOpacity=-1", "fireOpacity=101", "waterFogDensity=-1", "waterFogDensity=101", "horseOpacity=-1", "horseOpacity=101", "zoomSensitivity=NaN", "zoomSensitivity=0", "zoomSensitivity=1.1", "zoomEasing=3", "zoomFactor=0", "chatHistory=999999", "chatWidth=0",
                    "chatOpacity=0", "chatFade=-1", "scoreboardX=101", "scoreboardKey=-101",
                    "blockedSlots=-1", "blockedSlots=511", "slotProfileKey=256", "slotProfileKey=-101")) {
                Files.write(file, Arrays.asList(invalid));
                try { DollyConfig.load(file); throw new AssertionError("Invalid chat/sidebar option accepted: " + invalid); }
                catch (java.io.IOException expected) { }
            }
            Files.write(file, Arrays.asList("mouseFix=false", "clickFix=false"));
            assert DollyConfig.load(file).scrollLock && !DollyConfig.load(file).mouseFix;
            Files.write(file, Arrays.asList("smoothLight=NaN"));
            try { DollyConfig.load(file); throw new AssertionError("Invalid value accepted"); }
            catch (java.io.IOException expected) { assert Files.readAllLines(file).get(0).equals("smoothLight=NaN"); }
            Files.write(file, Arrays.asList("clickFix=perhaps"));
            try { DollyConfig.load(file); throw new AssertionError("Invalid boolean accepted"); }
            catch (java.io.IOException expected) { }
            Files.write(file, Arrays.asList("zoomFactor=" + "\\" + "uZZZZ"));
            try { DollyConfig.load(file); throw new AssertionError("Malformed escape accepted"); }
            catch (java.io.IOException expected) { assert expected.getCause() instanceof IllegalArgumentException; }
            reset.save();
            byte[] before = Files.readAllBytes(file);
            reset.zoomFactor = 600.5f;
            try { reset.save(); throw new AssertionError("Invalid zoom saved"); }
            catch (java.io.IOException expected) { assert Arrays.equals(before, Files.readAllBytes(file)); }
        } finally { Files.deleteIfExists(file); Files.delete(directory); }
        System.out.println("Core tests passed: preferences, reset and invalid values.");
    }
}
