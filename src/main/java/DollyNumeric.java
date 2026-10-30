import dolly.config.DollyConfig;
import java.lang.reflect.Field;
import java.util.function.Supplier;

/** Ranges are the same as the persisted configuration contract; enums keep buttons. */
public final class DollyNumeric {
    public static double[] range(String key) {
        if (key == null) return null;
        switch (key) {
            case "logRetentionDays": return new double[] {1, 3650, 1};
            case "backgroundFps": case "loadingFps": return new double[] {5, 240, 1};
            case "menuFps": return new double[] {0, 1000, 1};
            case "particleLimit": return new double[] {1, 10000, 1};
            case "zoomFactor": return new double[] {1, 600, 1};
            case "zoomSensitivity": return new double[] {.05, 1, .01};
            case "hudCacheFps": return new double[] {15, 240, 1};
            case "timeOfDay": return new double[] {0, 23999, 1};
            case "fireHeight": case "fireOpacity": case "waterFogDensity": case "horseOpacity": return new double[] {0, 100, 1};
            default: return null;
        }
    }
    public static DollySlider slider(int id, int x, int y, int width, String key, double min, double max, double step, Supplier<String> label) {
        try {
            Field field = DollyConfig.class.getField(key);
            return new DollySlider(id, x, y, width, min, max, step, () -> {
                try { return ((Number) field.get(DollySettings.config)).doubleValue(); }
                catch (IllegalAccessException e) { throw new AssertionError(e); }
            }, value -> {
                try {
                    if (field.getType() == int.class) field.setInt(DollySettings.config, (int) Math.round(value));
                    else field.setFloat(DollySettings.config, (float) value);
                    DollyHudPositions.syncOption(key);
                    DollyHudCache.invalidate();
                } catch (IllegalAccessException e) { throw new AssertionError(e); }
            }, label, () -> DollySettings.persist(ave.A().t));
        } catch (NoSuchFieldException e) { throw new IllegalArgumentException(key, e); }
    }
}
