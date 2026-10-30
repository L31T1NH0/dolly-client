import dolly.tactile.core.SubstepModel;

/** Effective server profile consumed by the upstream movement engine. */
public final class DollyTactileConfig {
    public enum Mode { VANILLA, STEPPED, EXACT }
    public enum InputSource { AUTO, EVENTS, POLLER, FRAME }
    public enum Overlay { OFF, SIMPLE, DETAILED }
    public static final int MIN_STEP_RATE_HZ = 1, DEFAULT_STEP_RATE_HZ = 20;
    public static final int MAX_STEPS_PER_TICK = 96, MAX_CONFIG_RATE_HZ = 1920, DEFAULT_RATE_HZ = 960, MAX_POLL_RATE_HZ = 8000;
    public boolean enabled;
    public Mode selectedMode = Mode.EXACT, mode = Mode.VANILLA;
    public SubstepModel.Contacts contacts = SubstepModel.Contacts.GRID;
    public int physicsRateHz = DEFAULT_RATE_HZ, stepRateHz = DEFAULT_STEP_RATE_HZ, pollRateHz = 2000;
    public InputSource inputSource = InputSource.AUTO;
    public boolean pressLatch = true, chatMessages;
    public Overlay overlay = Overlay.OFF;
    public volatile int generation;
    public int stepsPerTick() { return Math.max(1, Math.min(MAX_STEPS_PER_TICK, Math.round(physicsRateHz / 20f))); }
}
