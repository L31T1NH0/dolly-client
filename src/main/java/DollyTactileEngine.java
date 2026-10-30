// Adapted from TACTILE (fallingsword), commit 66892f1005b3e249240c89a4cf3521c5f73e6cd8. MIT; see licenses/TACTILE.txt in the client JAR.

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import dolly.tactile.client.input.AsyncKeyPoller;
import dolly.tactile.client.input.KeySource;
import dolly.tactile.client.input.RawKeyInput;
import dolly.tactile.client.input.VkMap;
import dolly.tactile.client.input.Win32;
import dolly.tactile.core.Box;
import dolly.tactile.core.InputTimeline;
import dolly.tactile.core.PlayerState;
import dolly.tactile.core.SubstepModel;
import dolly.tactile.core.TickInput;


/**
 * Live high-resolution movement for the local player.
 *
 * <p><b>Time.</b> Player time tau is measured in vanilla ticks, counted from activation. At the
 * START of each vanilla tick the player is advanced exactly to the next integer boundary, so packets
 * and tick bookkeeping always see a boundary state. Between ticks, each render frame runs every
 * substep that has fully ended by "now". "Now" is mapped through vanilla's avl, re-anchored every
 * frame (measured frame-to-frame consistency p99 at most 1.2 ms). Substep j covers [j/N, (j+1)/N).
 *
 * <p><b>Vanilla tick.</b> The vanilla tick still runs: timers, sprint rules, packets. The redirected
 * move call ({@link DollyTactileHooks}) restores the engine's boundary state, which undoes the tick's own
 * jump/sprint/cutoff mutations, and skips vanilla's move. DollyTactileMovement gives the tick the sampled
 * sneak state for packets and suppresses vanilla's jump.
 *
 * <p><b>Input.</b> Sample-and-hold timeline with press latch. Sources: Raw Input key events or a background poller
 * (Windows), per-frame state after a fresh message pump, or a script (validation). Yaw is a separate
 * held track sampled once per frame.
 *
 * <p><b>Authority.</b> Before each batch, the entity is compared with what the engine last wrote. Any
 * difference (S08 teleport/correction, S12 velocity, entity push) is adopted, counted and logged.
 */
public final class DollyTactileEngine implements DollyTactileHooks.MoveHandler
{
    private final ave mc = ave.A();
    private final DollyTactileConfig config;
    private final boolean tickDriven = "1".equals(System.getenv("HRMOVE_TICKDRIVEN"));
    private final boolean scripted = "SCRIPTED".equalsIgnoreCase(System.getenv("HRMOVE_INPUT"));

    // reflection
    private final Field timerField = DollyTactileAccess.findField(ave.class, "timer", "Y");
    private final Field lastHRTimeField = DollyTactileAccess.findField(avl.class, "lastHRTime", "f");
    private final Field syncAdjField = DollyTactileAccess.findField(avl.class, "timeSyncAdjustment", "j");
    private final Field jumpTicksField = DollyTactileAccess.findField(pr.class, "jumpTicks", "bn");
    private final Field sprintToggleField = DollyTactileAccess.findField(bew.class, "sprintToggleTimer", "d");
    private final Field flyToggleField = DollyTactileAccess.findField(wn.class, "flyToggleTimer", "bm");
    private final Field nextStepField =DollyTactileAccess.findField(pk.class, "nextStepDistance", "h");
    private final Field inWebField = DollyTactileAccess.findField(pk.class, "isInWeb", "H");
    private final Method playStepSound = DollyTactileAccess.findMethod(pk.class, null,
        new String[] {"a"}, cj.class, afh.class);

    // engine state
    private bew player;
    private adm world;
    private DollyTactileWorld collision;
    private SubstepModel model;
    private PlayerState mirror;
    private PlayerState snapshot;      // boundary state restored inside the vanilla tick
    private PlayerState lastWritten;   // what the entity should hold, for external-change detection
    private boolean active;
    private boolean activeThisTick;
    private long nextBoundary;         // tick boundary the next vanilla tick START advances to
    private long substepIndex;         // next substep to run
    private double prevBoundaryX, prevBoundaryZ;
    private String fallbackReason = "not started";
    private final DollyTactileMovement hrInput = new DollyTactileMovement();

    // time anchor
    private double anchorTau;
    private long anchorNanos;
    private double ticksPerNano = 20.0D / 1e9D;
    private double lastFrameTau;

    // input
    private final InputTimeline keys = new InputTimeline();
    private final InputTimeline yaw = new InputTimeline(); // only yaw/pitch of samples are used
    private TickInput currentKeys = TickInput.fromKeyString("-------", 0.0F, 0.0F);
    private KeySource keySource;
    private int[] pollerVks;
    private boolean lastFocused;
    private String inputDescription = "none";
    private double scriptEndTau = Double.NEGATIVE_INFINITY;

    // diagnostics
    public final Diagnostics diag = new Diagnostics();

    public DollyTactileEngine(DollyTactileConfig config)
    {
        this.config = config;
    }

    public boolean isTickDriven()
    {
        return tickDriven;
    }

    public boolean isActive()
    {
        return active;
    }

    public String fallbackReason()
    {
        return fallbackReason;
    }

    public String inputDescription()
    {
        return inputDescription;
    }

    public int stepsPerTick()
    {
        return model == null ? config.stepsPerTick() : model.stepsPerTick;
    }

    // ================================================================== tick boundary

    // ================================================================== fail-safe

    /**
     * Set by the first unexpected error inside the engine. From then on the engine stays off for the session and
     * movement is vanilla's: an error in TACTILE must never crash the game or leave the player unable to move.
     */
    private volatile boolean faulted;
    private String faultDetail;

    /** Test only (HRMOVE_FAULTTEST=tick|frame|move): throw once in that entry point, 40 active ticks after start. */
    private static final String FAULT_TEST = System.getenv("HRMOVE_FAULTTEST");
    private int faultTestCountdown = 40;

    private void faultTest(String where)
    {
        if (FAULT_TEST != null && FAULT_TEST.equals(where) && active && --faultTestCountdown == 0)
            throw new IllegalStateException("HRMOVE_FAULTTEST: forced error in " + where);
    }

    public boolean isFaulted()
    {
        return faulted;
    }

    private void fault(String where, Throwable t)
    {
        if (faulted) return;
        faulted = true;
        faultDetail = where + ": " + t;
        System.out.println("[tactile] internal error in " + where + "; movement is vanilla for the rest of this session. Please report this log.");
        t.printStackTrace(System.out);
        try
        {
            restoreRenderSwap();
            if (active) deactivate("internal error");
            stopInput();
        }
        catch (Throwable t2)
        {
            System.out.println("[tactile] while switching to vanilla after the error: " + t2);
            active = false;
            activeThisTick = false;
        }
        try
        {
            if (player != null)
                player.a(new fa(a.m + "[TACTILE] " + a.v
                    + "An internal error occurred. Movement is vanilla until you restart the game. Please report the log file."));
        }
        catch (Throwable ignored)
        {
        }
    }

    public void onClientTick(boolean start)
    {
        if (faulted) return;
        try
        {
            if (start) faultTest("tick");
            onClientTickImpl(start);
        }
        catch (Throwable t)
        {
            fault("tick", t);
        }
    }

    private void onClientTickImpl(boolean start)
    {
        if (!start)
        {
            activeThisTick = false;
            return;
        }
        bew p = mc.h;
        if (p == null || mc.f == null)
        {
            if (active) deactivate("no world");
            player = null;
            return;
        }
        if (p != player || mc.f != world)
        {
            if (active) deactivate("player/world changed");
            player = p;
            world = mc.f;
            haveLastDisplay = false; // nothing shown yet for this player: no hand-over blend
            nextBoundary = 0; // new clock for a new player/world; timelines are rebuilt on activation
            keys.clear();
            yaw.clear();
            scriptEndTau = Double.NEGATIVE_INFINITY;
        }
        if (mc.V())
        {
            if (active) deactivate("paused");
            return;
        }
        // One continuous clock: the boundary advances on every player tick, active or not, so input
        // samples and preloaded scripts stay valid across fallback and reactivation.
        final long boundary = nextBoundary;

        String reason = null;
        if (config.mode == DollyTactileConfig.Mode.VANILLA) reason = "mode VANILLA";
        else reason = DollyTactileSupport.unsupportedReason(p, mc.f, config.contacts == dolly.tactile.core.SubstepModel.Contacts.GRID);
        if (reason == null && TOGGLE_TEST > 0 && boundary % TOGGLE_TEST == 0) reason = "toggle test"; // test only: rapid hand-overs

        if (active)
        {
            long t0 = System.nanoTime();
            adoptExternalChanges("tick");
            readMovementAttributes(mirror); // potions, server speed changes, food: vanilla reads them every tick
            double pending = pendingTau();
            sampleFrameInputs(); // also refreshes focus state
            if (pending < (double)boundary && waitForPending()) sampleFrameInputs();
            runSubstepsUntil((double)boundary);
            if (sentState != null && CLICK_SYNC)
            {
                // Between here and the player update, ave.runTick handles clicks (block placement, attacks), and
                // the client checks them against the player's box. Vanilla's box at this point is the position the
                // server last received. The engine is already at this boundary, up to a tick ahead, so the client
                // would allow placements the server refuses (a block appears, then vanishes). Show the server's view
                // until the move hook installs the boundary state.
                writeState(sentState);
                lastWritten = sentState.copy();
            }
            else writeToEntity();
            diag.recordBatch(System.nanoTime() - t0, 0);
            if (reason == null)
            {
                boolean media = config.contacts == dolly.tactile.core.SubstepModel.Contacts.GRID;
                if (creativeFlightToggle(boundary) && !media) reason = "flying";
                mirror.flying = player.bA.b; // a double tap toggled flight: the engine flies too (GRID)
            }
            if (reason != null || model.stepsPerTick != config.stepsPerTick() || stepped != (config.mode == DollyTactileConfig.Mode.STEPPED)
                || stepped && stepTicks != 20.0D / config.stepRateHz
                || activeGeneration != config.generation)
            {
                deactivate(reason != null ? reason : "settings changed"); // without a reason it restarts right below
            }
        }
        announce(reason);
        if (!active)
        {
            if (reason != null)
            {
                fallbackReason = reason;
                nextBoundary = boundary + 1; // vanilla runs this tick; the clock still advances
                announce(reason);
                return;
            }
            activate(boundary);
            announce(null);
        }

        // This vanilla tick runs under engine control.
        snapshot = mirror.copy();
        hrInput.c(inputAt((double)boundary - 1e-9D, (double)boundary));
        nextBoundary = boundary + 1;
        activeThisTick = true;
        diag.ticks++;
    }

    /**
     * Vanilla's double-tap-jump flight toggle (bew.onLivingUpdate). Vanilla cannot see jump
     * presses while the engine controls the player (DollyTactileMovement reports jump up), so the engine
     * applies the same rule to its own timeline: a press edge since the last boundary either starts the
     * 7-tick window or, inside it, toggles flight. Returns true if flight was switched on.
     */
    private boolean creativeFlightToggle(long boundary)
    {
        if (!player.bA.c || player.v()) return false;
        if (!keys.jumpPressedIn((double)boundary - 1.0D, (double)boundary)) return false;
        try
        {
            if (flyToggleField.getInt(player) == 0)
            {
                flyToggleField.setInt(player, 7);
                return false;
            }
            player.bA.b = !player.bA.b;
            player.t();
            flyToggleField.setInt(player, 0);
            return player.bA.b;
        }
        catch (IllegalAccessException e)
        {
            return false;
        }
    }

    /** Config generation the running model and input source were built from. */
    private int activeGeneration;

    /** Mode the engine is actually running (VANILLA when inactive). */
    public DollyTactileConfig.Mode runningMode()
    {
        if (!active || model == null) return DollyTactileConfig.Mode.VANILLA;
        return stepped ? DollyTactileConfig.Mode.STEPPED : DollyTactileConfig.Mode.EXACT;
    }

    /**
     * STEPPED: key changes take effect only at multiples of {@link #stepTicks} on the engine clock (the physics is
     * EXACT's). EXACT: false.
     */
    private boolean stepped;
    private double stepTicks = 1.0D;

    private String modeLabel()
    {
        return stepped ? "STEPPED " + Math.round(20.0D / stepTicks) + " Hz steps" : "EXACT";
    }

    /** Overlay status: what is in control right now, and why when it differs from the setting. */
    public String statusLine()
    {
        String tag = a.h + "[TACTILE] ";
        if (faulted) return tag + a.m + "internal error: vanilla movement until restart (see log)";
        if (config.mode == DollyTactileConfig.Mode.VANILLA)
            return tag + a.p + (config.enabled ? "VANILLA (20 Hz)" : "OFF (vanilla movement)");
        if (active && model != null)
            return tag + a.k + modeLabel() + a.h + " active, physics " + model.rateHz() + " Hz, input " + inputDescription;
        return tag + a.o + config.mode + a.h
            + " paused, vanilla movement: " + (fallbackReason != null ? fallbackReason : "starting");
    }

    private String announced = "";
    private long announcedAt;

    /** Tells the player in chat what actually controls movement whenever that changes (rate-limited). */
    private void announce(String reason)
    {
        if (config.mode == DollyTactileConfig.Mode.VANILLA || player == null || !config.chatMessages)
        {
            announced = "";
            return;
        }
        if (reason == null && !active) return; // about to activate; announced after activation
        String state = reason == null ? "active " + (model != null ? modeLabel() : config.mode.name()) + ", input " + inputDescription
            : "vanilla fallback: " + reason;
        long now = System.currentTimeMillis();
        if (state.equals(announced) || now - announcedAt < 4000L) return;
        announced = state;
        announcedAt = now;
        player.a(new fa("[TACTILE] " + state));
    }

    private void activate(long boundary)
    {
        activeGeneration = config.generation;
        velocityQueue.clear();
        model = new SubstepModel(config.stepsPerTick(), config.contacts, true); // EXACT physics; STEPPED only steps the keys
        stepped = config.mode == DollyTactileConfig.Mode.STEPPED;
        stepTicks = 20.0D / Math.max(DollyTactileConfig.MIN_STEP_RATE_HZ, config.stepRateHz);
        collision = new DollyTactileWorld(mc.f, player);
        mirror = readEntity();
        beu mi = player.b;
        mirror.prevJump = mi != null && mi.c;
        mirror.prevSneak = mi != null && mi.d;
        mirror.prevForwardHigh = mi != null && mi.b >= 0.8F;
        substepIndex = boundary * model.stepsPerTick;
        anchorTau = (double)boundary;
        anchorNanos = System.nanoTime();
        lastFrameTau = (double)boundary;
        prevBoundaryX = player.s;
        prevBoundaryZ = player.u;
        renderReset = true;
        startInput();
        if (scriptActive((double)boundary))
        {
            // Keep the preloaded script; drop only samples the vanilla ticks already covered.
            keys.pruneBefore((double)boundary - 1.0D);
            yaw.pruneBefore((double)boundary - 1.0D);
        }
        else
        {
            keys.clear();
            yaw.clear();
            currentKeys = readKeysNow();
            keys.add((double)boundary - 1.0D, currentKeys);
            yaw.add((double)boundary - 1.0D, TickInput.fromKeyString("-------", player.y, player.z));
        }
        player.b = hrInput;
        lastWritten = mirror.copy();
        active = true;
        fallbackReason = null;
        diag.activations++;
        log("high-resolution movement active: " + model.rateHz() + " Hz, input " + inputDescription + (tickDriven ? ", tick-driven" : ""));
    }

    public void shutdown() {
        restoreRenderSwap();
        if (active) deactivate("shutdown");
        else stopInput();
    }

    private void deactivate(String reason)
    {
        active = false;
        activeThisTick = false;
        sentState = null;
        sentMirror = null;
        fallbackReason = reason;
        diag.fallbacks++;
        diag.lastFallback = reason;
        if (player != null && player.b == hrInput)
        {
            // Vanilla detects jump and sneak presses as edges against the previous tick's movementInput. A fresh
            // input object starts with both up: a held space bar would read as a new press, and with a recent
            // press (jumping onto a ladder) as a double tap that toggles creative flight. Start from the keys'
            // real state instead.
            beu mi = new bev(mc.t);
            if (!"1".equals(System.getenv("HRMOVE_OLDHANDOVER"))) // A/B only: the previous behavior
            {
                mi.c = mc.t.ac.d();
                mi.d = mc.t.ad.d();
            }
            player.b = mi;
        }
        stopInput();
        log("high-resolution movement inactive: " + reason);
    }

    // ================================================================== move hook

    @Override
    public boolean handleMove(pr entity, float strafe, float forward)
    {
        if (faulted) return false;
        try
        {
            if (activeThisTick && entity == player) faultTest("move");
            return handleMoveImpl(entity, strafe, forward);
        }
        catch (Throwable t)
        {
            fault("move", t);
            return false; // vanilla's own move runs for this tick
        }
    }

    private boolean handleMoveImpl(pr entity, float strafe, float forward)
    {
        if (!activeThisTick || entity != player) return false;
        replayIfBlocked();
        // Item use (eating, drawing a bow, blocking) was started or stopped by this tick's click handling, just before
        // this move: it applies to the tick that starts here, as in vanilla. Set before saving the replay point.
        mirror.usingItem = player.bS();
        // Undo the vanilla tick's own mutations (sprint rules, timers, cutoff) by restoring the boundary state.
        writeState(snapshot);
        lastWritten = snapshot.copy();
        sentState = snapshot.copy(); // what onUpdateWalkingPlayer sends to the server after this move
        sentMirror = mirror.copy();
        sentModel = model.save();
        sentSubstepIndex = substepIndex;
        // Animation bookkeeping vanilla does at the end of moveEntityWithHeading, measured over the last tick.
        player.p = prevBoundaryX;
        player.r = prevBoundaryZ;
        player.aA = player.aB;
        double dx = player.s - prevBoundaryX, dz = player.u - prevBoundaryZ;
        float f7 = ns.a(dx * dx + dz * dz) * 4.0F;
        if (f7 > 1.0F) f7 = 1.0F;
        player.aB += (f7 - player.aB) * 0.4F;
        player.aC += player.aB;
        prevBoundaryX = player.s;
        prevBoundaryZ = player.u;
        return true;
    }

    // ================================================================== frames

    /** Test only (HRMOVE_TOGGLETEST=n): hand control to vanilla for one tick every n ticks. */
    private static final int TOGGLE_TEST = System.getenv("HRMOVE_TOGGLETEST") == null ? 0 : Integer.parseInt(System.getenv("HRMOVE_TOGGLETEST").trim());

    /** State the server last received (the last engine-controlled boundary); null when unknown. */
    private PlayerState sentState;
    /** Full engine state at that boundary (mirror, model, substep index), for replaying the tick since then. */
    private PlayerState sentMirror;
    private SubstepModel.Saved sentModel;
    private long sentSubstepIndex;
    private boolean replaying;

    /**
     * A click handled this c (block placement) was checked against the server's view, the last boundary,
     * as in vanilla. The engine had already simulated the tick since then without the new block. If the block
     * now overlaps the player, the server would refuse that position and pull the player back (bridging stalls).
     * Vanilla simulates this tick after the click, so it collides with the block. Do the same: rewind to the
     * last boundary and replay the tick against the current world.
     */
    private void replayIfBlocked()
    {
        if (sentMirror == null || sentModel == null || sentSubstepIndex >= substepIndex) return;
        Box b = snapshot.box;
        double e = 1e-7D;
        if (collision.getCollidingBoxes(new Box(b.minX + e, b.minY + e, b.minZ + e, b.maxX - e, b.maxY - e, b.maxZ - e)).isEmpty()) return;
        long end = substepIndex;
        mirror = sentMirror.copy();
        model.restore(sentModel);
        substepIndex = sentSubstepIndex;
        replaying = true;
        try
        {
            runSubstepsUntil((double)end / model.stepsPerTick);
        }
        finally
        {
            replaying = false;
        }
        snapshot = mirror.copy();
        renderReset = true;
        diag.replays++;
    }
    /** A/B measurement only: HRMOVE_CLICKSYNC=0 leaves the engine's boundary state on the entity during click handling. */
    private static final boolean CLICK_SYNC = !"0".equals(System.getenv("HRMOVE_CLICKSYNC"));

    /** A/B measurement only: HRMOVE_NOINTERP=1 restores the previous (uninterpolated) camera. */
    private static final boolean NO_INTERP = "1".equals(System.getenv("HRMOVE_NOINTERP"));

    private double renderPrevX, renderPrevY, renderPrevZ;
    /** pos, prevPos, lastTickPos (x, y, z each) saved while a render position is swapped in. */
    private final double[] savedPos = new double[9];
    private boolean renderSwapped, renderReset = true, walkedSwapped;
    private float renderPrevWalked, actualWalked, actualPrevWalked;

    /**
     * Hand-over between the engine's camera and vanilla's. Vanilla draws the player interpolated between its last
     * two ticks, about one tick behind the simulation. The engine draws the present (EXACT) or one substep behind
     * (HIGH_RES). Where control changes hands (ladder, water, fallback, mode switch) the camera would jump by up to
     * one tick of motion. Instead the jump is turned into an offset that decays exponentially (time constant
     * BLEND_NANOS), without ever moving the camera back against the direction of travel.
     */
    /** Time constant of the exponential fade (80 ms: 85 % gone after 150 ms). HRMOVE_NOBLEND=1 disables it (A/B only). */
    private static final long BLEND_NANOS = "1".equals(System.getenv("HRMOVE_NOBLEND")) ? 1L : 80_000_000L;
    private boolean haveLastDisplay, lastDisplayEngine;
    private double lastDispX, lastDispY, lastDispZ, blendX, blendY, blendZ, dispX, dispY, dispZ, lastSrcX, lastSrcY, lastSrcZ, prevDispX, prevDispY, prevDispZ;
    private long blendStart;
    /** EXACT: view position at exactly the frame time (see {@link #provisional}). */
    private double provX, provY, provZ;

    public void onRenderTick(boolean start, float partial)
    {
        if (faulted)
        {
            if (!start) restoreRenderSwap();
            return;
        }
        try
        {
            if (start) faultTest("frame");
            onRenderTickImpl(start, partial);
        }
        catch (Throwable t)
        {
            fault("frame", t);
        }
    }

    /** Puts back the simulated position and walked distance swapped in for the frame being drawn. */
    private void restoreRenderSwap()
    {
        if (renderSwapped && player != null)
        {
            player.s = savedPos[0];
            player.t = savedPos[1];
            player.u = savedPos[2];
            player.p = savedPos[3];
            player.q = savedPos[4];
            player.r = savedPos[5];
            player.P = savedPos[6];
            player.Q = savedPos[7];
            player.R = savedPos[8];
        }
        if (walkedSwapped && player != null)
        {
            player.M = actualWalked;
            player.L = actualPrevWalked;
        }
        renderSwapped = false;
        walkedSwapped = false;
    }

    private void onRenderTickImpl(boolean start, float partial)
    {
        if (!start)
        {
            // Restore the simulated position after the frame; game logic must never see the render position.
            if (renderSwapped && player != null)
            {
                player.s = savedPos[0];
                player.t = savedPos[1];
                player.u = savedPos[2];
                player.p = savedPos[3];
                player.q = savedPos[4];
                player.r = savedPos[5];
                player.P = savedPos[6];
                player.Q = savedPos[7];
                player.R = savedPos[8];
            }
            if (walkedSwapped && player != null)
            {
                player.M = actualWalked;
                player.L = actualPrevWalked;
            }
            renderSwapped = false;
            walkedSwapped = false;
            return;
        }
        if (!start || player == null || player != mc.h || mc.f == null || mc.V()) return;
        if (!active)
        {
            vanillaFrame(partial);
            return;
        }
        long t0 = System.nanoTime();
        diag.recordFrame(t0);
        updateAnchor(partial);
        double now = Math.min(tauAt(System.nanoTime()), (double)nextBoundary);
        // Yaw set by last frame's mouse handling; it took effect at roughly last frame's time.
        if (!scriptActive(now)) yaw.add(Math.max(yaw.lastTau(), lastFrameTau), TickInput.fromKeyString("-------", player.y, player.z));
        lastFrameTau = now;
        adoptExternalChanges("frame");
        double pending = pendingTau(); // read before draining: an event handed over after the drain was pending here
        sampleFrameInputs();
        long before = substepIndex;
        if (!tickDriven) runSubstepsUntil(Math.min(now, pending));
        writeToEntity();

        // The view position (SubstepModel.updateView) differs from the box only inside a grid tick at contacts.
        double h = 1.0D / model.stepsPerTick;
        double lastEnd = (double)substepIndex / model.stepsPerTick;
        double rx, ry, rz;
        float rw;
        actualWalked = player.M;
        actualPrevWalked = player.L;
        if (!tickDriven && !NO_INTERP)
        {
            // The state at exactly this frame's time, from a throwaway continuation of the last substep (EXACT: split
            // at key timestamps, or at step boundaries in STEPPED). No render delay at any rate. Measured in 0.7.3: drawing
            // the previous substep instead (the old HIGH_RES) added 50 ms at 20 Hz.
            provisional(lastEnd, now);
            rx = provX;
            ry = provY;
            rz = provZ;
            double dx = provX - mirror.viewX(), dz = provZ - mirror.viewZ();
            rw = (float)(actualWalked + Math.sqrt(dx * dx + dz * dz) * 0.6D);
        }
        else
        {
            // Tick-driven, A/B (HRMOVE_NOINTERP) or HRMOVE_HIGHRES_DELAYED: (now - h), interpolated between the last two
            // substep states. Showing the latest substep
            // as-is advanced the camera by 1 or 2 substeps on alternate frames (~2 cm at a sprint): visible shake.
            double frac = Math.max(0.0D, Math.min(1.0D, (now - lastEnd) / h));
            if (substepIndex == 0 || renderReset || NO_INTERP) frac = 1.0D;
            rx = renderPrevX + (mirror.viewX() - renderPrevX) * frac;
            ry = renderPrevY + (mirror.viewY() - renderPrevY) * frac;
            rz = renderPrevZ + (mirror.viewZ() - renderPrevZ) * frac;
            rw = (float)(renderPrevWalked + (actualWalked - renderPrevWalked) * frac);
        }
        renderReset = false;
        blend(true, rx, ry, rz);
        // Positions are swapped for the frame only and restored at RenderTickEvent END.
        swapRenderPosition(dispX, dispY, dispZ);
        // View bobbing: vanilla adds (distanceWalked - prevDistanceWalked) * partialTicks. With distanceWalked
        // advanced live, that term grew within each tick and snapped back at the tick boundary: a 20 Hz bob
        // jerk (measured CV 192 % vs vanilla 9 %). The bob distance follows the rendered position and is
        // swapped in for the frame, with prev == current so the partial-tick term is zero.
        if (!NO_INTERP)
        {
            player.M = rw;
            player.L = rw;
            walkedSwapped = true;
        }
        // Crouch (eye height, pose) follows the sneak key at frame rate rather than at the next tick. Vanilla's tick
        // sets it back to the tick's value for packets and game logic.
        if (!tickDriven && keys.size() > 0)
        {
            try
            {
                hrInput.d = keys.at(now).sneak;
            }
            catch (IllegalStateException e)
            {
                // no sample yet at this time
            }
        }
        diag.recordBatch(System.nanoTime() - t0, (int)(substepIndex - before));
        keys.pruneBefore((double)substepIndex / model.stepsPerTick - 1.0D - (stepped ? 2.0D * stepTicks : 0.0D)); // STEPPED reads up to two steps back
        yaw.pruneBefore((double)substepIndex / model.stepsPerTick - 1.0D);
    }

    /** EXACT: advances a copy of the state from the last substep end to {@code to}; leaves the result in prov*. */
    private void provisional(double from, double to)
    {
        provX = mirror.viewX();
        provY = mirror.viewY();
        provZ = mirror.viewZ();
        if (to - from <= 1e-9D) return;
        PlayerState keep = mirror;
        SubstepModel.Saved saved = model.save();
        int jumps = model.jumpImpulses;
        mirror = keep.copy();
        inProvisional = true;
        try
        {
            boolean boundary = substepIndex % model.stepsPerTick == 0;
            if (model.exact) substepExact(from, to, boundary, to - from);
            else
            {
                double n = model.stepsPerTick;
                TickInput in = inputAt((substepIndex - 1) / n, substepIndex / n); // what this substep will use
                model.substep(mirror, collision, new TickInput[] {in}, new double[] {0.0D}, 1, boundary, to - from);
            }
            provX = mirror.viewX();
            provY = mirror.viewY();
            provZ = mirror.viewZ();
        }
        finally
        {
            inProvisional = false;
            mirror = keep;
            model.restore(saved);
            model.jumpImpulses = jumps;
        }
    }

    /**
     * Display position for this frame (dispX/Y/Z) from the source's own position. When the source changes (engine to
     * vanilla or back), the camera starts where it was last shown and the difference fades out (smoothstep).
     * Differences of 2 blocks or more (teleports) are not blended.
     */
    private void blend(boolean engine, double x, double y, double z)
    {
        long now = System.nanoTime();
        if (haveLastDisplay && engine != lastDisplayEngine)
        {
            // continue from where the camera was, carrying on with its last step (no one-frame stop)
            double ox = 2.0D * lastDispX - prevDispX - x, oy = 2.0D * lastDispY - prevDispY - y, oz = 2.0D * lastDispZ - prevDispZ - z;
            boolean small = ox * ox + oy * oy + oz * oz < 4.0D;
            blendX = small ? ox : 0.0D;
            blendY = small ? oy : 0.0D;
            blendZ = small ? oz : 0.0D;
        }
        else if (haveLastDisplay && (blendX != 0.0D || blendY != 0.0D || blendZ != 0.0D))
        {
            // The remaining offset decays exponentially (no end point to snap at). Where the camera is
            // ahead of the source along its motion (all three axes), the offset may shrink by at most 70 % of the source's own step:
            // the camera keeps moving forward (at 30 % of the source speed or more) instead of stopping or backing up
            // when the source slows sharply right after a hand-over.
            double k = BLEND_NANOS <= 1L ? 0.0D : Math.exp(-(now - blendStart) / (double)BLEND_NANOS);
            double mx = x - lastSrcX, my = y - lastSrcY, mz = z - lastSrcZ, m = Math.sqrt(mx * mx + my * my + mz * mz);
            double nx = blendX * k, ny = blendY * k, nz = blendZ * k;
            if (m > 1e-9D)
            {
                double ux = mx / m, uy = my / m, uz = mz / m;
                double par = blendX * ux + blendY * uy + blendZ * uz, newPar = nx * ux + ny * uy + nz * uz;
                if (par > 0.0D && par - newPar > 0.7D * m)
                {
                    double keepPar = par - 0.7D * m;
                    nx += (keepPar - newPar) * ux;
                    ny += (keepPar - newPar) * uy;
                    nz += (keepPar - newPar) * uz;
                }
            }
            blendX = nx;
            blendY = ny;
            blendZ = nz;
        }
        blendStart = now; // time of the previous frame
        dispX = x + blendX;
        dispY = y + blendY;
        dispZ = z + blendZ;
        if (blendX * blendX + blendY * blendY + blendZ * blendZ < 1e-8D) blendX = blendY = blendZ = 0.0D;
        prevDispX = haveLastDisplay ? lastDispX : dispX;
        prevDispY = haveLastDisplay ? lastDispY : dispY;
        prevDispZ = haveLastDisplay ? lastDispZ : dispZ;
        lastSrcX = x;
        lastSrcY = y;
        lastSrcZ = z;
        lastDispX = dispX;
        lastDispY = dispY;
        lastDispZ = dispZ;
        lastDisplayEngine = engine;
        haveLastDisplay = true;
    }

    /** Frames while vanilla controls the player: only the hand-over blend changes what vanilla would draw. */
    private void vanillaFrame(float partialTicks)
    {
        double vx = player.P + (player.s - player.P) * partialTicks;
        double vy = player.Q + (player.t - player.Q) * partialTicks;
        double vz = player.R + (player.u - player.R) * partialTicks;
        blend(false, vx, vy, vz);
        if (blendX != 0.0D || blendY != 0.0D || blendZ != 0.0D) swapRenderPosition(dispX, dispY, dispZ);
    }

    private void swapRenderPosition(double x, double y, double z)
    {
        savedPos[0] = player.s;
        savedPos[1] = player.t;
        savedPos[2] = player.u;
        savedPos[3] = player.p;
        savedPos[4] = player.q;
        savedPos[5] = player.r;
        savedPos[6] = player.P;
        savedPos[7] = player.Q;
        savedPos[8] = player.R;
        renderSwapped = true;
        player.s = player.p = player.P = x;
        player.t = player.q = player.Q = y;
        player.u = player.r = player.R = z;
    }

    private void updateAnchor(float renderPartialTicks)
    {
        try
        {
            avl timer = (avl)timerField.get(mc);
            double lastHR = lastHRTimeField.getDouble(timer);
            double adj = syncAdjField.getDouble(timer);
            anchorNanos = Math.round(lastHR * 1000.0D) * 1_000_000L; // avl's own ms-quantized nanoTime base
            anchorTau = (double)(nextBoundary - 1) + renderPartialTicks;
            ticksPerNano = 20.0D * timer.d * adj / 1e9D;
        }
        catch (IllegalAccessException e)
        {
            anchorNanos = System.nanoTime();
            anchorTau = (double)(nextBoundary - 1) + renderPartialTicks;
        }
    }

    private double tauAt(long nanos)
    {
        return anchorTau + (nanos - anchorNanos) * ticksPerNano;
    }

    // ================================================================== substeps

    private void runSubstepsUntil(double tau)
    {
        int n = model.stepsPerTick;
        int maxPerCall = n * 11; // vanilla caps catch-up at 10 ticks per frame
        int run = 0;
        while ((double)(substepIndex + 1) / n <= tau + 1e-12 && run < maxPerCall)
        {
            long j = substepIndex;
            double tj = (double)j / n;
            double tPrev = (double)(j - 1) / n;
            TickInput in = inputAt(tPrev, tj);
            boolean groundBefore = mirror.onGround;
            double x0 = mirror.viewX(), y0 = mirror.viewY(), z0 = mirror.viewZ();
            renderPrevX = mirror.viewX();
            renderPrevY = mirror.viewY();
            renderPrevZ = mirror.viewZ();
            renderPrevWalked = player.M;
            int jumpsBefore = model.jumpImpulses;
            long q0 = collision.queries;
            long s0 = System.nanoTime();
            if (model.exact) substepExact(tj, (double)(j + 1) / n, j % n == 0, model.h);
            else model.substep(mirror, collision, in, j % n == 0);
            diag.substepNanos += System.nanoTime() - s0;
            diag.queries += collision.queries - q0;
            if (model.jumpImpulses != jumpsBefore) lastJumpTau = (double)(j / n) + model.lastJumpPhase; // tick start + phase
            if (!replaying)
            {
                // a replay re-runs time whose jump and step side effects (stats, sounds, events) already happened
                if (model.jumpImpulses != jumpsBefore) onJump();
                stepSideEffects(x0, y0, z0, groundBefore && in.sneak);
            }
            ++substepIndex;
            ++run;
            diag.substeps++;
        }
        if (run >= maxPerCall && (double)(substepIndex + 1) / n <= tau) diag.backlogDrops++;
    }

    private final double[][] keyTimes = {new double[8]}, yawTimes = {new double[8]};
    private double[] pieceOffsets = new double[16];
    private TickInput[] pieceInputs = new TickInput[16];

    /**
     * EXACT mode: substep [tau, end) split at every key sample (poller timestamps) and every yaw sample (frame
     * times) inside it; each piece uses the key and yaw state from its own start.
     */
    private void substepExact(double tau, double end, boolean tickBoundary, double span)
    {
        int nk = stepped ? stepsInside(tau, end) : keys.samplesInside(tau, end, keyTimes);
        int ny = yaw.samplesInside(tau, end, yawTimes);
        // outside velocities (knockback) due in this stretch: one at or before its start applies at the start
        int nv = 0;
        for (double[] v : velocityQueue) if (v[0] < end - 1e-12D) ++nv;
        int need = nk + ny + nv + 1;
        if (pieceOffsets.length < need)
        {
            pieceOffsets = new double[2 * need];
            pieceInputs = new TickInput[2 * need];
            pieceVelocities = new double[2 * need][];
        }
        int count = 0, i = 0, k = 0, q = 0;
        double t = tau;
        boolean anyVelocity = false;
        while (true)
        {
            pieceOffsets[count] = t - tau;
            pieceInputs[count] = combined(t);
            pieceVelocities[count] = null;
            while (q < nv && velocityQueue.get(q)[0] <= t + 1e-12D)
            {
                double[] v = velocityQueue.get(q++);
                pieceVelocities[count] = new double[] {v[1], v[2], v[3]}; // the latest one due here wins, as in vanilla
                anyVelocity = true;
            }
            ++count;
            double nextKey = i < nk ? keyTimes[0][i] : Double.POSITIVE_INFINITY;
            double nextYaw = k < ny ? yawTimes[0][k] : Double.POSITIVE_INFINITY;
            double nextVel = q < nv ? velocityQueue.get(q)[0] : Double.POSITIVE_INFINITY;
            double next = Math.min(nextKey, Math.min(nextYaw, nextVel));
            if (next == Double.POSITIVE_INFINITY) break;
            if (nextKey <= next) ++i;
            if (nextYaw <= next) ++k;
            t = next;
        }
        model.substep(mirror, collision, pieceInputs, pieceOffsets, count, tickBoundary, span, anyVelocity ? pieceVelocities : null);
        if (!inProvisional) velocityQueue.subList(0, nv).clear(); // applied (the frame prediction only looks ahead)
    }

    private double[][] pieceVelocities = new double[16][];
    /** Outside velocities (server velocity packets: knockback) not yet simulated: {engine time of arrival, vx, vy, vz}. */
    private final List<double[]> velocityQueue = new ArrayList<double[]>();
    private boolean inProvisional;

    /** STEPPED: step boundaries strictly inside (tau, end), written to keyTimes as piece starts. */
    private int stepsInside(double tau, double end)
    {
        return InputTimeline.stepsInside(tau, end, stepTicks, keyTimes);
    }

    private TickInput combined(double tau)
    {
        // STEPPED: the keys at the last step boundary, presses since the one before latched (InputTimeline.stepped)
        TickInput k = stepped ? keys.stepped(tau, stepTicks, config.pressLatch) : keys.at(tau);
        TickInput y = yaw.at(tau);
        return new TickInput(k.forward, k.back, k.left, k.right, k.jump, k.sneak, k.sprint, y.yaw, y.pitch);
    }

    private TickInput inputAt(double prevTau, double tau)
    {
        TickInput k = config.pressLatch ? keys.atLatched(prevTau, tau) : keys.at(tau);
        TickInput y = yaw.at(tau);
        return new TickInput(k.forward, k.back, k.left, k.right, k.jump, k.sneak, k.sprint, y.yaw, y.pitch);
    }

    /** Jump side effects vanilla attaches to EntityPlayer.jump: stat, exhaustion, LivingJumpEvent. */
    private void onJump()
    {
        writeToEntity();
        player.ai = true;
        player.b(na.u);
        player.a(mirror.sprinting ? 0.8F : 0.2F);
        mirror.motionX = player.v; // a LivingJumpEvent handler may change motion
        mirror.motionY = player.w;
        mirror.motionZ = player.x;
        diag.jumps++;
    }

    /** Distance-walked and step-sound bookkeeping of Entity.moveEntity, applied per substep. */
    private void stepSideEffects(double x0, double y0, double z0, boolean sneakEdge)
    {
        if (sneakEdge) return;
        // walked distance (view bob, step sounds) follows the displayed path, so it has no stall-and-release jerk
        double dx = mirror.viewX() - x0, dy = mirror.viewY() - y0, dz = mirror.viewZ() - z0;
        cj pos = new cj(ns.c(mirror.posX()), ns.c(mirror.posY() - 0.20000000298023224D),
            ns.c(mirror.posZ()));
        afh block = world.p(pos).c();
        if (block.t() == arm.a)
        {
            afh below = world.p(pos.b()).c();
            if (below instanceof agt || below instanceof akl || below instanceof agu)
            {
                block = below;
                pos = pos.b();
            }
        }
        if (block != afi.au) dy = 0.0D;
        player.M = (float)((double)player.M + (double)ns.a(dx * dx + dz * dz) * 0.6D);
        player.N = (float)((double)player.N
            + (double)ns.a(dx * dx + dy * dy + dz * dz) * 0.6D);
        try
        {
            int next = nextStepField.getInt(player);
            if (player.N > (float)next && block.t() != arm.a)
            {
                nextStepField.setInt(player, (int)player.N + 1);
                playStepSound.invoke(player, pos, block);
            }
        }
        catch (Exception e)
        {
            // sound bookkeeping is cosmetic; never let it break movement
        }
    }

    // ================================================================== state transfer

    private static final qd SPRINT_MODIFIER =
        DollyTactileAccess.getPrivateValue(pr.class, null, "sprintingSpeedBoostModifier", "b");

    /**
     * Movement inputs vanilla reads from the entity each tick: the movement-speed attribute without and with the
     * sprint modifier (so speed/slowness potions and server-set speeds are included, computed by vanilla's own
     * attribute code), the jump boost amplifier, and the sprint conditions (food level, blindness).
     */
    private void readMovementAttributes(PlayerState s)
    {
        qc a = player.a(vy.d);
        boolean had = a.a(SPRINT_MODIFIER.a()) != null;
        try {
            if (had) a.c(SPRINT_MODIFIER);
            s.walkSpeed = (float)a.e();
            a.b(SPRINT_MODIFIER);
            s.sprintSpeed = (float)a.e();
        } finally {
            boolean present = a.a(SPRINT_MODIFIER.a()) != null;
            if (had && !present) a.b(SPRINT_MODIFIER);
            else if (!had && present) a.c(SPRINT_MODIFIER);
        }
        pf jump = player.b(pe.j);
        s.jumpBoost = jump != null ? jump.c() : -1;
        s.foodAllowsSprint = (float)player.cl().a() > 6.0F || player.bA.c;
        s.blind = player.a(pe.q);
        s.depthStrider = ack.b(player);
        s.flying = player.bA.b;
        s.flySpeed = player.bA.a();
    }

    private PlayerState readEntity()
    {
        PlayerState s = new PlayerState();
        aug b = player.aR();
        s.box = new Box(b.a, b.b, b.c, b.d, b.e, b.f);
        s.motionX = player.v;
        s.motionY = player.w;
        s.motionZ = player.x;
        s.onGround = player.C;
        s.collidedHorizontally = player.D;
        s.collidedVertically = player.E;
        s.sprinting = player.aw();
        s.sprintingTicksLeft = player.e;
        s.jumpMovementFactor = player.aM;
        s.yaw = player.y;
        s.usingItem = player.bS();
        readMovementAttributes(s);
        try
        {
            s.jumpTicks = jumpTicksField.getInt(player);
            s.inWeb = inWebField.getBoolean(player);
            s.sprintToggleTimer = sprintToggleField.getInt(player);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
        return s;
    }

    private void writeToEntity()
    {
        writeState(mirror);
        lastWritten = mirror.copy();
    }

    private void writeState(PlayerState s)
    {
        Box coreBox = s.box;
        player.a(new aug(coreBox.minX, coreBox.minY, coreBox.minZ, coreBox.maxX, coreBox.maxY, coreBox.maxZ));
        player.s = s.posX();
        player.t = s.posY();
        player.u = s.posZ();
        player.v = s.motionX;
        player.w = s.motionY;
        player.x = s.motionZ;
        player.C = s.onGround;
        player.D = s.collidedHorizontally;
        player.E = s.collidedVertically;
        player.F = s.collidedHorizontally || s.collidedVertically;
        if (player.aw() != s.sprinting) player.d(s.sprinting);
        if (player.bA.b != s.flying)
        {
            // the engine ended the flight (landing) or picked up a toggle: tell the server, as vanilla does
            player.bA.b = s.flying;
            player.t();
        }
        player.e = s.sprintingTicksLeft;
        player.aM = s.jumpMovementFactor;
        try
        {
            jumpTicksField.setInt(player, (int)Math.ceil(s.jumpTicks - 1e-9D));
            sprintToggleField.setInt(player, (int)Math.ceil(s.sprintToggleTimer - 1e-9D));
            inWebField.setBoolean(player, s.inWeb);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    /** Adopts any change made to the entity by something other than the engine (server, other entities). */
    private void adoptExternalChanges(String where)
    {
        // Flight switched by someone else (server abilities packet, /fly, gamemode): adopt it, never write it back.
        if (player.bA.b != lastWritten.flying)
        {
            mirror.flying = player.bA.b;
            lastWritten.flying = mirror.flying;
        }
        aug b = player.aR();
        Box w = lastWritten.box;
        boolean moved = b.a != w.minX || b.b != w.minY || b.c != w.minZ;
        boolean velocity = player.v != lastWritten.motionX || player.w != lastWritten.motionY || player.x != lastWritten.motionZ;
        if (!moved && !velocity) return;
        if (!moved && model != null && model.contacts == dolly.tactile.core.SubstepModel.Contacts.GRID)
        {
            // A velocity set from outside (knockback): vanilla moves with it from its next tick, with that tick's friction.
            // The engine applies it at its arrival, starting a full tick there (SubstepModel.overrideVelocity), so the
            // knockback is vanilla's, started when it arrived. Never earlier than what is already simulated.
            velocityQueue.add(new double[] {Math.max(tauAt(System.nanoTime()), (double)substepIndex / model.stepsPerTick),
                player.v, player.w, player.x});
            lastWritten.motionX = player.v;
            lastWritten.motionY = player.w;
            lastWritten.motionZ = player.x;
            diag.velocityOverrides++;
            return;
        }
        velocityQueue.clear(); // a position change replaces the state; pending velocities are part of what it replaced
        double dx = b.a - w.minX, dy = b.b - w.minY, dz = b.c - w.minZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        boolean motionZeroed = player.v == 0.0D && player.w == 0.0D && player.x == 0.0D;
        PlayerState adopted = readEntity();
        adopted.prevJump = mirror.prevJump;
        adopted.prevSneak = mirror.prevSneak;
        adopted.prevForwardHigh = mirror.prevForwardHigh;
        adopted.jumpTicks = Math.max(adopted.jumpTicks, Math.min(mirror.jumpTicks, adopted.jumpTicks + 1.0D));
        mirror = adopted;
        lastWritten = mirror.copy();
        renderReset = true; // do not interpolate across a teleport or correction
        if (moved)
        {
            sentState = null; // the server placed us; its view is the adopted state, not the last one we sent
            sentMirror = null;
            diag.corrections++;
            diag.lastCorrection = dist;
            diag.maxCorrection = Math.max(diag.maxCorrection, dist);
            if (dist > 1e-6) log(String.format("external position change (%s): %.4f blocks (dx %.4f dy %.4f dz %.4f, motion zeroed %s)",
                where, dist, dx, dy, dz, motionZeroed));
        }
        else
        {
            diag.velocityOverrides++;
        }
    }

    // ================================================================== input sources

    private boolean scriptActive(double tau)
    {
        return scripted && tau <= scriptEndTau;
    }

    /**
     * Validation: preloads a per-tick script. Row i is the input vanilla would use for the move of the
     * i-th tick from now. With {@code lead} 0 (tick-driven) it covers the interval ending at this
     * START's boundary. With lead 1 it starts one tick later, because frames may already have simulated
     * part of the current interval.
     */
    public void loadScript(List<TickInput> script, int lead)
    {
        if (!active) return;
        double start = (double)nextBoundary - 1.0D + lead;
        keys.truncateAfter(start - 1e-9D);
        yaw.truncateAfter(start - 1e-9D);
        for (int i = 0; i < script.size(); ++i)
        {
            TickInput in = script.get(i);
            keys.add(start + i, in);
            yaw.add(start + i, in);
        }
        TickInput last = script.get(script.size() - 1);
        TickInput idle = TickInput.fromKeyString("-------", last.yaw, last.pitch);
        keys.add(start + script.size(), idle);
        yaw.add(start + script.size(), idle);
        scriptEndTau = start + script.size();
        currentKeys = idle;
    }

    private void startInput()
    {
        stopInput();
        if (scripted)
        {
            inputDescription = "scripted";
            return;
        }
        DollyTactileConfig.InputSource source = config.inputSource;
        Win32.User32 u = source != DollyTactileConfig.InputSource.FRAME ? Win32.get() : null;
        if (u != null)
        {
            avb[] kb = movementBindings();
            int[] vks = new int[kb.length];
            boolean ok = true;
            for (int i = 0; i < kb.length; ++i)
            {
                vks[i] = VkMap.toVk(kb[i].i());
                if (vks[i] == 0) ok = false;
            }
            if (ok)
            {
                pollerVks = vks;
                String eventsUnavailable = null;
                if (source != DollyTactileConfig.InputSource.POLLER)
                {
                    if (RawKeyInput.canServe(vks))
                    {
                        RawKeyInput events = new RawKeyInput(u, vks);
                        events.setFocused(isFocused());
                        events.start();
                        if (events.failure() == null)
                        {
                            keySource = events;
                            inputDescription = "key events";
                            return;
                        }
                        eventsUnavailable = events.failure();
                    }
                    else
                    {
                        eventsUnavailable = "a movement key is a mouse button";
                    }
                }
                long period = 1_000_000_000L / config.pollRateHz;
                AsyncKeyPoller poller = new AsyncKeyPoller(u, vks, period, AsyncKeyPoller.spinFor(period));
                poller.setFocused(isFocused());
                poller.start();
                keySource = poller;
                inputDescription = "poller " + config.pollRateHz + " Hz"
                    + (source == DollyTactileConfig.InputSource.EVENTS ? " (key events unavailable: " + eventsUnavailable + ")" : "");
                return;
            }
            inputDescription = "frame (a movement key cannot be polled asynchronously)";
            return;
        }
        inputDescription = source == DollyTactileConfig.InputSource.POLLER || source == DollyTactileConfig.InputSource.EVENTS ? "frame (Windows input unavailable: " + Win32.failureReason() + ")" : "frame";
    }

    private void stopInput()
    {
        if (keySource != null)
        {
            keySource.stop();
            keySource = null;
        }
    }

    private avb[] movementBindings()
    {
        return new avb[] {mc.t.Y, mc.t.aa, mc.t.Z,
            mc.t.ab, mc.t.ac, mc.t.ad, mc.t.ae};
    }

    private boolean isFocused()
    {
        return Display.isActive() && mc.w && mc.m == null;
    }

    /** Current physical state of the movement keys, read on the game thread (all up when unfocused). */
    private TickInput readKeysNow()
    {
        if (!isFocused()) return TickInput.fromKeyString("-------", 0.0F, 0.0F);
        avb[] kb = movementBindings();
        boolean[] d = new boolean[kb.length];
        Win32.User32 u = keySource != null ? Win32.get() : null;
        for (int i = 0; i < kb.length; ++i)
        {
            int code = kb[i].i();
            if (u != null) d[i] = Win32.isDown(u, pollerVks[i]);
            else if (code < 0) d[i] = Mouse.isButtonDown(code + 100);
            else d[i] = code > 0 && code < 256 && Keyboard.isKeyDown(code);
        }
        return new TickInput(d[0], d[1], d[2], d[3], d[4], d[5], d[6] || DollyInput.sprintLatched(), 0.0F, 0.0F);
    }

    /** Pushes input samples: drains the key-event source or poller, or samples the keyboard after a fresh pump. */
    private void sampleFrameInputs()
    {
        if (scripted) return;
        long now = System.nanoTime();
        double nowTau = tauAt(now);
        double floorTau = (double)substepIndex / model.stepsPerTick; // never rewrite input already consumed
        boolean focused = isFocused();
        if (keySource != null && !keySource.isAlive())
        {
            // the key thread stopped (an error): read keys once per frame from now on rather than see no keys at all
            log("key reading stopped (" + inputDescription + "); reading keys once per frame");
            keySource = null;
            inputDescription = "frame (key reading stopped)";
        }
        if (keySource != null) keySource.setFocused(focused);

        if (focused != lastFocused)
        {
            lastFocused = focused;
            pushKeys(Math.max(nowTau, floorTau), readKeysNow()); // focus change: all released, or current state on regain
            if (keySource != null) keySource.drain(new KeySource.Sink() { public void onKey(long n, int k, boolean d, boolean f) { } });
            return;
        }
        if (!focused) return;

        if (keySource != null)
        {
            final double floor = floorTau;
            prevDrainNanos = lastDrainNanos;
            lastDrainNanos = System.nanoTime();
            keySource.drain(new KeySource.Sink()
            {
                public void onKey(long nanos, int key, boolean down, boolean f)
                {
                    if (!f) return;
                    boolean[] d = toArray(currentKeys);
                    d[key] = down;
                    double tau = tauAt(nanos);
                    if (key == 4 && down)
                        jumpKeyEvent = new double[] {nanos, System.nanoTime(), tau, Math.max(Math.max(tau, floor), keys.lastTau()),
                            keySource instanceof RawKeyInput ? ((RawKeyInput)keySource).publishNanos(4) : Double.NaN, prevDrainNanos,
                            keySource instanceof RawKeyInput ? ((RawKeyInput)keySource).stageNanos(4)[0] : Double.NaN,
                            keySource instanceof RawKeyInput ? ((RawKeyInput)keySource).stageNanos(4)[1] : Double.NaN,
                            keySource instanceof RawKeyInput && ((RawKeyInput)keySource).isTimeCritical() ? 1.0D : 0.0D};
                    pushKeys(Math.max(tau, floor), new TickInput(d[0], d[1], d[2], d[3], d[4], d[5], d[6] || DollyInput.sprintLatched(), 0.0F, 0.0F));
                }
            });
        }
        else
        {
            Display.processMessages(); // measured: removes the frame-limiter staleness (p50 21 -> 7.7 ms at 60 FPS)
            pushKeys(Math.max(nowTau, floorTau), readKeysNow());
        }
    }

    /** Diagnostics: the engine's clock and internal state. */
    public String debugState()
    {
        if (!active || model == null || mirror == null) return "inactive";
        return String.format("rate %d, substep %d (tau %.3f), next boundary %d, now %.3f, mirror y %.4f my %.4f g %b, grid %s elapsed %.3f",
            model.rateHz(), substepIndex, (double)substepIndex / model.stepsPerTick, nextBoundary, tauAt(System.nanoTime()),
            mirror.posY(), mirror.motionY, mirror.onGround, mirror.gridStartBox == null ? "unset" : "set", mirror.gridElapsed);
    }

    /**
     * Test only: the most recent jump-key press from the key source: {received (nanoTime, key thread), collected
     * (nanoTime, game thread), its engine time, the engine time it entered the input timeline at, handed to the buffer
     * (nanoTime, key thread; key events only), start of the previous drain (nanoTime)}.
     */
    private double[] jumpKeyEvent;
    /** Test only: start of the latest and of the previous key-source drain (nanoTime). */
    private double lastDrainNanos = Double.NaN, prevDrainNanos = Double.NaN;

    public double[] lastJumpKeyEvent()
    {
        return jumpKeyEvent;
    }

    /** Test only: when the most recent jump happened (engine time, ticks). */
    private double lastJumpTau = Double.NaN;

    public double lastJumpTau()
    {
        return lastJumpTau;
    }

    /** Engine time of a key event the source has received but not handed over yet, or +infinity. */
    private double pendingTau()
    {
        long p = keySource != null ? keySource.pendingNanos() : 0L;
        return p != 0L ? tauAt(p) : Double.POSITIVE_INFINITY;
    }

    private static final long PENDING_WAIT_NANOS = 25_000_000L;

    /**
     * At a tick boundary the tick's movement is committed (sent to the server), so a key event from before the boundary
     * that the source is still reading is waited for, up to 25 ms. Measured: such a read takes ~10 ms in rare cases.
     * Returns true if it arrived.
     */
    private boolean waitForPending()
    {
        long t0 = System.nanoTime();
        while (keySource != null && keySource.pendingNanos() != 0L)
        {
            if (System.nanoTime() - t0 > PENDING_WAIT_NANOS) return false;
            Thread.yield();
        }
        diag.pendingWaits++;
        diag.pendingWaitNanos += System.nanoTime() - t0;
        return true;
    }

    /** Test only: the engine clock now (ticks), or NaN while inactive. */
    public double tauNow()
    {
        return active && model != null ? tauAt(System.nanoTime()) : Double.NaN;
    }

    /**
     * Test only: a key-state change observed now, entered exactly as a poller event would be (timestamped at the
     * current time, never before the last simulated substep).
     */
    public void injectKeys(TickInput k)
    {
        if (!active || model == null) return;
        pushKeys(Math.max(tauAt(System.nanoTime()), (double)substepIndex / model.stepsPerTick), k);
    }

    /** Test only: engine time (ticks) of the last forward-key press and release that entered the timeline. */
    private double forwardDownTau = Double.NaN, forwardUpTau = Double.NaN;

    public double[] lastForwardTap()
    {
        return new double[] {forwardDownTau, forwardUpTau};
    }

    private void pushKeys(double tau, TickInput k)
    {
        if (Arrays.equals(toArray(k), toArray(currentKeys))) return;
        double at = Math.max(tau, keys.lastTau());
        if (k.forward && !currentKeys.forward) forwardDownTau = at;
        if (!k.forward && currentKeys.forward) forwardUpTau = at;
        currentKeys = k;
        keys.add(Math.max(tau, keys.lastTau()), k);
        diag.inputSamples++;
    }

    private static boolean[] toArray(TickInput t)
    {
        return new boolean[] {t.forward, t.back, t.left, t.right, t.jump, t.sneak, t.sprint};
    }

    // ================================================================== diagnostics

    public static final class Diagnostics
    {
        public long ticks, substeps, queries, jumps, inputSamples, corrections, velocityOverrides, fallbacks, activations, backlogDrops, replays;
        /** Tick boundaries that waited for a key event still being read, and the total wait. */
        public long pendingWaits, pendingWaitNanos;
        public long substepNanos;
        public double lastCorrection, maxCorrection;
        public String lastFallback = "-";
        private final long[] batchNanos = new long[4096];
        private final int[] batchSteps = new int[4096];
        private int batchCount;
        private final long[] frameIntervals = new long[4096];
        private int frameCount;
        private long lastFrame;

        void recordFrame(long now)
        {
            if (lastFrame != 0) frameIntervals[frameCount++ % frameIntervals.length] = now - lastFrame;
            lastFrame = now;
        }

        /** {p50, p99, max} frame interval in ms over the recent window. */
        public double[] frameMillis()
        {
            int n = Math.min(frameCount, frameIntervals.length);
            if (n == 0) return new double[3];
            long[] c = Arrays.copyOf(frameIntervals, n);
            Arrays.sort(c);
            return new double[] {c[n / 2] / 1e6, c[(int)(n * 0.99)] / 1e6, c[n - 1] / 1e6};
        }

        public void reset()
        {
            ticks = substeps = queries = jumps = inputSamples = corrections = velocityOverrides = backlogDrops = replays = 0;
            substepNanos = 0;
            batchCount = frameCount = 0;
            lastFrame = 0;
            maxCorrection = lastCorrection = 0;
        }

        void recordBatch(long nanos, int steps)
        {
            batchNanos[batchCount % batchNanos.length] = nanos;
            batchSteps[batchCount % batchSteps.length] = steps;
            ++batchCount;
        }

        /** {p50, p95, p99, max} of per-batch engine time in microseconds over the recent window. */
        public double[] batchMicros()
        {
            int n = Math.min(batchCount, batchNanos.length);
            if (n == 0) return new double[4];
            long[] c = Arrays.copyOf(batchNanos, n);
            Arrays.sort(c);
            return new double[] {c[n / 2] / 1e3, c[(int)(n * 0.95)] / 1e3, c[(int)(n * 0.99)] / 1e3, c[n - 1] / 1e3};
        }

        public double meanStepsPerFrame()
        {
            int n = Math.min(batchCount, batchSteps.length);
            long s = 0;
            for (int i = 0; i < n; ++i) s += batchSteps[i];
            return n == 0 ? 0 : (double)s / n;
        }

        public List<String> lines(DollyTactileEngine e)
        {
            List<String> l = new ArrayList<String>();
            double[] b = batchMicros();
            if (e.tickDriven) l.add("[TACTILE] tick-driven (validation)");
            l.add("[TACTILE] input samples " + inputSamples + (e.runningMode() == DollyTactileConfig.Mode.STEPPED ? ", press latch " + e.config.pressLatch : ""));
            l.add(String.format("[TACTILE] engine us/batch p50 %.0f p95 %.0f p99 %.0f max %.0f, %.1f substeps/frame", b[0], b[1], b[2], b[3], meanStepsPerFrame()));
            double[] f = frameMillis();
            l.add(String.format("[TACTILE] substeps %d (%.2f us each, %.1f queries each), jumps %d, backlog drops %d",
                substeps, substeps == 0 ? 0 : substepNanos / 1e3 / substeps, substeps == 0 ? 0 : (double)queries / substeps, jumps, backlogDrops));
            l.add(String.format("[TACTILE] frame ms p50 %.2f p99 %.2f max %.2f", f[0], f[1], f[2]));
            l.add(String.format("[TACTILE] corrections %d (last %.3f, max %.3f), velocity overrides %d, replays %d, fallbacks %d (last: %s)",
                corrections, lastCorrection, maxCorrection, velocityOverrides, replays, fallbacks, lastFallback));
            l.add(String.format("[TACTILE] ticks that waited for a key event being read: %d (%.1f ms in total)", pendingWaits, pendingWaitNanos / 1e6));
            return l;
        }
    }

    private static void log(String s)
    {
        System.out.println("[tactile] " + s);
    }
}
