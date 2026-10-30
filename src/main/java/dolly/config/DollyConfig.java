package dolly.config;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Dolly-only preferences; vanilla controls and resource packs stay in options-dolly.txt. */
public final class DollyConfig {
    public boolean vbo = true, renderRegions = true, fastRender = true;
    public boolean ambientParticles = false;
    public boolean clickFix = true, mouseFix = true, keyFix = true, linuxShiftFix = true;
    public boolean mouseBindFix = true, inputFix = true, rawMouseInput = false;
    public boolean stringWidthCaching = true, optimizedPlayerUpdating = true;
    public boolean asyncScreenshot = true, fastLanguageSwitch = false, fastWorldLoading = true;
    public boolean hudCaching = true, memoryCleanup = false;
    public boolean optimizedText = true, batchModels = true, modelBackFaces = true;
    public boolean optimizedItems = true, staticParticleColor = true, smallPackIcons = true, lowAnimationTick = true;
    public boolean alternateDns = true, fallbackDns = false;
    public int lazyChunkLoading = 1, hudCacheFps = 24;
    public boolean scrollLock = true;
    public boolean entityCulling = true, mergeTerrain = true;
    public boolean hideEnchantmentBooks = true, singleGroundItem = true, staticGroundItems = true;
    public boolean oldAnimations = true, toggleSprint = true, sneakAnimation = true, zoom = true;
    public boolean perspective = true, fullBright = false, timeChanger = false, tntTimer = false, pingDisplay = false;
    public boolean zoomScroll = true, smoothZoom = true, smoothScrollZoom = true, toggleZoom = false;
    public boolean directZoomCamera = false, dynamicZoomSensitivity = false, renderHandZoom = false;
    public boolean smartFullbright = true, parallaxFix = true, betterCamera = true, customSkyFix = true;
    public boolean hideFireResistance = true, removeWaterOverlay = true, cleanView = true;
    public boolean hideBreakingParticles = true, hideLightning = true, cleanProjectiles = true;
    public int fireHeight = 100, fireOpacity = 100, waterFogDensity = 100, horseOpacity = 100;
    public boolean separateResourceLoading = true;
    public String soundVolumes = "";
    public float zoomSensitivity = 1f;
    public int zoomEasing = 0;
    public int backgroundFps = 30, menuFps = 0, loadingFps = 30, particleLimit = 1000, timeOfDay = 6000;
    public float smoothLight = 0.25f, zoomFactor = 4f;
    public int menuKey = 54, zoomKey = 46, perspectiveKey = 56;
    public boolean slotProfile = false;
    public int blockedSlots = 0, slotProfileKey = 0;
    public boolean simpleDebugFps = true, vanillaMetrics = true, voidBoxFix = true, nauseaFix = true;
    public boolean cleanOldLogs = true;
    public int logRetentionDays = 30;
    public boolean screenshotPreview = true, screenshotAutoCopy = false, screenshotCompact = true;
    public boolean confirmQuit = true, reconnectMenu = true;
    public int lanButton = 2;
    public boolean chatGroup = true, chatMentions = true, chatMentionSound = false, chatKeepHistory = true;
    public boolean chatGroupConsecutive = false, chatSendQueue = false, chatInspectActions = true;
    public boolean chatClickedHistory = true, chatImagePreview = true, tabPing = true;
    public int chatGroupSeconds = 120;
    public float chatSendInterval = 1f;
    public boolean chatFilter = false, chatHideNotices = false, chatBackground = true;
    public String chatHiddenWords = "", chatNoticeWords = "", chatMentionWords = "";
    public int chatHistory = 1000, chatWidth = 320, chatHeight = 180, chatSpacing = 0;
    public int chatOpacity = 100, chatBackgroundOpacity = 50, chatDuration = 10, chatFade = 1;
    public float chatScale = 1f;
    public boolean scoreboardVisible = true, scoreboardNumbers = true, scoreboardBackground = true;
    public int scoreboardX = 100, scoreboardY = 50, scoreboardOpacity = 35, scoreboardKey = 0;
    public float scoreboardScale = 1f;
    public String hudLayout = "";
    public String tactileProfiles = "";
    public boolean hudSnap = true;
    private final Path file;

    public DollyConfig(Path file) { this.file = file; }

    public static DollyConfig load(Path file) throws IOException {
        DollyConfig config = new DollyConfig(file);
        if (!Files.exists(file)) return config;
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) { properties.load(reader); }
        catch (IllegalArgumentException e) { throw new IOException("Malformed Dolly properties", e); }
        for (java.lang.reflect.Field field : DollyConfig.class.getFields()) {
            String value = properties.getProperty(field.getName());
            if (value == null) continue;
            try {
                if (field.getType() == boolean.class) {
                    if (!value.equals("true") && !value.equals("false")) throw new IllegalArgumentException("expected true/false");
                    field.setBoolean(config, Boolean.parseBoolean(value));
                } else if (field.getType() == String.class) field.set(config, value);
                else if (field.getType() == int.class) field.setInt(config, Integer.parseInt(value));
                else field.setFloat(config, Float.parseFloat(value));
            } catch (ReflectiveOperationException | IllegalArgumentException e) {
                throw new IOException("Invalid Dolly setting: " + field.getName(), e);
            }
        }
        config.validate();
        return config;
    }

    private void validate() throws IOException {
        dolly.hud.HudLayout.parse(hudLayout);
        dolly.tactile.ServerProfiles.parse(tactileProfiles);
        dolly.audio.SoundVolumes.parse(soundVolumes);
        if (logRetentionDays < 1 || logRetentionDays > 3650
                || lanButton < 0 || lanButton > 2
                || fireHeight < 0 || fireHeight > 100 || fireOpacity < 0 || fireOpacity > 100
                || waterFogDensity < 0 || waterFogDensity > 100 || horseOpacity < 0 || horseOpacity > 100
                || backgroundFps < 5 || backgroundFps > 240 || menuFps < 0 || menuFps > 1000
                || lazyChunkLoading < 0 || lazyChunkLoading > 5 || hudCacheFps < 15 || hudCacheFps > 240
                || loadingFps < 5 || loadingFps > 240
                || particleLimit < 1 || particleLimit > 10000 || timeOfDay < 0 || timeOfDay >= 24000
                || !Float.isFinite(smoothLight) || smoothLight < 0 || smoothLight > 1
                || !Float.isFinite(zoomFactor) || zoomFactor < 1 || zoomFactor > 600
                || !Float.isFinite(zoomSensitivity) || zoomSensitivity < 0.05f || zoomSensitivity > 1
                || zoomEasing < 0 || zoomEasing > 2
                || menuKey < 1 || menuKey > 255 || zoomKey < 1 || zoomKey > 255
                || perspectiveKey < 1 || perspectiveKey > 255
                || blockedSlots < 0 || blockedSlots >= 511 || slotProfileKey < -100 || slotProfileKey > 255
                || chatGroupSeconds < 1 || chatGroupSeconds > 600
                || !Float.isFinite(chatSendInterval) || chatSendInterval < 0.1f || chatSendInterval > 60f
                || chatHistory < 100 || chatHistory > 5000 || chatWidth < 80 || chatWidth > 640
                || chatHeight < 40 || chatHeight > 320 || chatSpacing < 0 || chatSpacing > 6
                || chatOpacity < 10 || chatOpacity > 100 || chatBackgroundOpacity < 0 || chatBackgroundOpacity > 100
                || chatDuration < 1 || chatDuration > 120 || chatFade < 0 || chatFade > 10
                || !Float.isFinite(chatScale) || chatScale < 0.5f || chatScale > 2f
                || chatHiddenWords == null || chatNoticeWords == null || chatMentionWords == null
                || chatHiddenWords.length() > 1000 || chatNoticeWords.length() > 1000 || chatMentionWords.length() > 1000
                || scoreboardX < 0 || scoreboardX > 100 || scoreboardY < 0 || scoreboardY > 100
                || scoreboardOpacity < 0 || scoreboardOpacity > 100 || scoreboardKey < -100 || scoreboardKey > 255
                || !Float.isFinite(scoreboardScale) || scoreboardScale < 0.5f || scoreboardScale > 2f)
            throw new IOException("Dolly setting outside supported range");
    }

    public void save() throws IOException {
        validate();
        Files.createDirectories(file.toAbsolutePath().getParent());
        Properties properties = new Properties();
        for (java.lang.reflect.Field field : DollyConfig.class.getFields()) {
            try { properties.setProperty(field.getName(), String.valueOf(field.get(this))); }
            catch (IllegalAccessException e) { throw new AssertionError(e); }
        }
        Path temp = Files.createTempFile(file.toAbsolutePath().getParent(), "dolly-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp)) { properties.store(writer, "Dolly Client 1.8.9"); }
            try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    public DollyConfig reset() throws IOException { DollyConfig defaults = new DollyConfig(file); defaults.save(); return defaults; }
}
