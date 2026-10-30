public final class DollyWorldTime {
    public static long displayTime(long original, adm world) {
        return DollySettings.config.timeChanger && world instanceof bdb
                ? Math.floorDiv(original, 24000L) * 24000L + DollySettings.config.timeOfDay : original;
    }
}
