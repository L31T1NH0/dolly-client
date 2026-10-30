import dolly.tactile.core.*;

/** Offline invariants for the port; this is not a replay of real-client baseline recordings. */
public final class DollyTactilePhysicsTest {
    public static void run() {
        int scenarios = 0, ticks = 0;
        for (Scenario scenario : Scenario.all()) {
            PlayerState reference = new PlayerState();
            reference.box = Box.forPlayerAt(scenario.startX, scenario.startY, scenario.startZ);
            reference.yaw = scenario.startYaw; reference.flying = scenario.startFlying; reference.onGround = !scenario.startFlying;
            PlayerState substep = reference.copy();
            SubstepModel model = new SubstepModel(1, SubstepModel.Contacts.GRID);
            Course world = scenario.course(0, 0, 0);
            for (TickInput input : scenario.script()) {
                VanillaModel.tick(reference, input, world); model.tick(substep, input, world);
                assert reference.posX() == substep.posX() && reference.posY() == substep.posY() && reference.posZ() == substep.posZ()
                        && reference.motionX == substep.motionX && reference.motionY == substep.motionY && reference.motionZ == substep.motionZ
                        && reference.onGround == substep.onGround && reference.sprinting == substep.sprinting
                        : "One-step vanilla identity: " + scenario.name + " tick " + ticks;
                ticks++;
            }
            scenarios++;
        }
        InputTimeline timeline = new InputTimeline();
        timeline.add(-1, TickInput.fromKeyString("-------", 0, 0));
        timeline.add(.25, TickInput.fromKeyString("W---J--", 0, 0));
        timeline.add(.35, TickInput.fromKeyString("W------", 0, 0));
        Course floor = new Course().fill(-10, -1, -10, 10, -1, 50, Course.Shape.FULL);
        PlayerState first = null;
        for (int rate : new int[] {1, 3, 12, 48, 96}) {
            PlayerState state = new PlayerState(); state.box = Box.forPlayerAt(.5, 0, .5); state.onGround = true; state.motionY = -.0784000015258789;
            SubstepModel model = new SubstepModel(rate, SubstepModel.Contacts.GRID, true);
            for (int tick = 0; tick < 50; tick++) model.tickExact(state, timeline, tick, floor);
            assert model.jumpImpulses == 1 : "EXACT rate " + rate + ": expected one jump, got " + model.jumpImpulses;
            if (first == null) first = state;
            else assert Math.abs(first.posX() - state.posX()) < 1e-7 && Math.abs(first.posY() - state.posY()) < 1e-7
                    && Math.abs(first.posZ() - state.posZ()) < 1e-7 : "EXACT timing must not depend on the physics rate";
        }
        System.out.println("TACTILE offline physics passed: " + scenarios + " scenarios / " + ticks + " one-step identity ticks and exact short-tap/rate invariance.");
    }
}
