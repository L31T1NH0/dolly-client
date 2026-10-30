/** Only the language selection screen calls this; resource-pack changes retain full reloads. */
public final class DollyLanguage {
    public static void reload(ave mc) {
        if (!DollySettings.config.fastLanguageSwitch || mc.S() == null || mc.Q() == null) { mc.e(); return; }
        try { mc.S().a(mc.Q()); net.optifine.Lang.resourcesReloaded(); }
        catch (RuntimeException error) { mc.e(); }
        DollyTextCache.clear();
        DollyHudCache.invalidate();
        // The native screen updates Unicode/Bidi flags, translated button labels and saves immediately afterward.
    }
}
