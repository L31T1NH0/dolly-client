import dolly.input.RawMouseInput;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import org.lwjgl.input.Keyboard;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;
import com.mojang.authlib.GameProfile;

/** Uses a fake raw backend; never initializes JInput or native input devices. */
public final class DollyTechnicalInputTest {
    public static final class Minecraft extends ave {
        int keyboardPasses;
        Minecraft() { super(null); }
        @Override public void Z() { keyboardPasses++; }
    }
    public static final class Screen extends axu {
        int typed;
        char character;
        void bind(ave minecraft) { j = minecraft; }
        @Override protected void a(char c, int key) { typed++; character = c; }
    }
    public static final class Container extends xi {
        @Override public boolean a(wn player) { return true; }
    }
    public static final class Inventory extends ayl {
        int swaps, slot, hotbar, mode;
        Inventory() { super(new Container()); }
        void bind(ave minecraft) { j = minecraft; }
        void click(int button) { a(0, 0, button); }
        @Override protected void a(float partial, int x, int y) { }
        @Override protected void a(yg slot, int number, int button, int mode) {
            swaps++; this.slot = number; hotbar = button; this.mode = mode;
        }
    }
    private static <T> T allocate(Class<T> target) throws Exception {
        Class<?> type = Class.forName("sun.misc.Unsafe");
        Field field = type.getDeclaredField("theUnsafe"); field.setAccessible(true);
        Method method = type.getMethod("allocateInstance", Class.class);
        return target.cast(method.invoke(field.get(null), target));
    }
    private static void integration() throws Exception {
        Minecraft minecraft = allocate(Minecraft.class);
        Screen screen = new Screen(); screen.bind(minecraft);
        Keyboard.event = 0; Keyboard.eventCharacter = 'é'; Keyboard.eventPressed = false;
        DollySettings.config.inputFix = true;
        screen.l();
        assert screen.typed == 1 && screen.character == 'é' && minecraft.keyboardPasses == 1
                : "Patched GUI must dispatch character-only events and retain global keyboard handling";
        DollySettings.config.inputFix = false; screen.l();
        assert screen.typed == 1 && minecraft.keyboardPasses == 2;
        Keyboard.event = 30; Keyboard.eventCharacter = 'a'; Keyboard.eventPressed = true; screen.l();
        assert screen.typed == 2 && screen.character == 'a';
        Keyboard.event = 0; Keyboard.eventCharacter = 0; Keyboard.eventPressed = false;

        minecraft.t = allocate(avh.class);
        minecraft.t.aw = new avb[9];
        for (int i = 0; i < 9; i++) minecraft.t.aw[i] = new avb("test.slot" + i, i + 2, "test");
        minecraft.t.aw[2].b(-97);
        minecraft.h = allocate(bew.class); minecraft.h.bi = new wm(minecraft.h);
        Inventory inventory = new Inventory(); inventory.bind(minecraft);
        yg hovered = new yg(minecraft.h.bi, 0, 0, 0); hovered.e = 12;
        Field field = ayl.class.getDeclaredField("u"); field.setAccessible(true); field.set(inventory, hovered);
        DollySettings.config.mouseBindFix = true;
        DollySettings.config.slotProfile = true; DollySettings.config.blockedSlots = 1 << 2;
        inventory.click(3);
        assert inventory.swaps == 1 && inventory.slot == 12 && inventory.hotbar == 2 && inventory.mode == 2
                : "Extra mouse button must swap the hovered slot with its hotbar binding, including blocked profile slots";
        opponentStats(minecraft);
        inventoryPosition(minecraft);
    }

    public static final class Connection extends bcy {
        Collection<bdc> players;
        GameProfile self;
        Connection() { super(null, null, null, null); }
        public Collection<bdc> d() { return players; }
        public GameProfile e() { return self; }
    }
    public static final class Player extends bew {
        int sends;
        String command;
        boolean potion;
        Player() { super(null, null, null, null); }
        public void e(String command) { sends++; this.command = command; }
        public Collection<pf> bl() { return potion ? Collections.singletonList(new pf(1, 200)) : Collections.<pf>emptyList(); }
    }
    public static final class PositionedInventory extends ayw {
        PositionedInventory() { super(new Container()); }
        void update(ave mc, int width) { j = mc; l = width; f = 176; a(); }
        int left() { return i; }
        protected void a(float partial, int x, int y) { }
    }
    private static void opponentStats(Minecraft mc) throws Exception {
        UUID id = UUID.randomUUID();
        bdc self = new bdc(new GameProfile(id, "Self"));
        bdc enemy = new bdc(new GameProfile(UUID.randomUUID(), "Enemy_2"));
        assert DollyOpponentStats.command(Arrays.asList(self, enemy), id).equals("/stats Enemy_2");
        assert DollyOpponentStats.command(Arrays.asList(enemy, self), id).equals("/stats Enemy_2");
        assert DollyOpponentStats.command(Collections.singletonList(self), id) == null;
        assert DollyOpponentStats.command(Arrays.asList(self, enemy, enemy), id) == null;
        assert DollyOpponentStats.command(Arrays.asList(self, enemy), UUID.randomUUID()) == null;
        bdc invalid = new bdc(new GameProfile(UUID.randomUUID(), "bad name"));
        assert DollyOpponentStats.command(Arrays.asList(self, invalid), id) == null;
        bdc spectator = new bdc(new GameProfile(UUID.randomUUID(), "Spectator")) {
            public adp.a b() { return adp.a.d; }
        };
        assert DollyOpponentStats.command(Arrays.asList(self, spectator), id) == null;
        Connection connection = allocate(Connection.class);
        connection.self = self.a(); connection.players = Arrays.asList(self, enemy);
        Player player = allocate(Player.class);
        Field field = bew.class.getDeclaredField("a"); field.setAccessible(true); field.set(player, connection);
        field = ave.class.getDeclaredField("S"); field.setAccessible(true); field.set(null, mc);
        mc.h = player; mc.f = allocate(bdb.class); mc.m = null; mc.w = true;
        Keyboard.event = 20; Keyboard.eventPressed = true;
        Keyboard.down[29] = true; Keyboard.down[54] = true;
        assert DollyInput.eventKey() == 0;
        assert player.sends == 1 && player.command.equals("/stats Enemy_2");
        assert DollyInput.eventKey() == 0 && player.sends == 1 : "Repeated key queries/events must not send twice";
        Keyboard.eventPressed = false; assert DollyInput.eventKey() == 20;
        connection.players = Collections.singletonList(self);
        Keyboard.eventPressed = true; assert DollyInput.eventKey() == 0 && player.sends == 1;
        Keyboard.eventPressed = false; DollyInput.eventKey();
        Keyboard.down[29] = false; Keyboard.down[54] = false;
        Keyboard.eventPressed = true; assert DollyInput.eventKey() == 20 && player.sends == 1;
        Keyboard.eventPressed = false; DollyInput.eventKey();
    }
    private static void inventoryPosition(Minecraft mc) {
        PositionedInventory inventory = new PositionedInventory();
        Player player = (Player) mc.h;
        for (int width : new int[] {320, 427, 854}) {
            player.potion = false; inventory.update(mc, width);
            assert inventory.left() == (width - 176) / 2;
            player.potion = true; inventory.update(mc, width);
            assert inventory.left() == (width - 176) / 2 : "Potion effects must not shift the inventory";
        }
    }
    private static final class Source implements RawMouseInput.Source {
        volatile float dx, dy;
        volatile int status = READY;
        final AtomicInteger polls = new AtomicInteger();
        public synchronized int poll(float[] axes) {
            axes[0] = dx; axes[1] = dy;
            dx = dy = 0;
            polls.incrementAndGet();
            return status;
        }
        synchronized void add(float x, float y) { dx += x; dy += y; }
    }
    private interface Check { boolean get(); }
    private static void await(Check check) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!check.get()) {
            if (System.nanoTime() > end) throw new AssertionError("Raw input worker timed out");
            Thread.sleep(1L);
        }
    }
    private static void sampled(Source source) throws Exception {
        int before = source.polls.get();
        await(() -> source.polls.get() > before);
    }
    private static void motion(RawMouseInput input, int expectedX, int expectedY) throws Exception {
        int[] total = new int[2], axes = new int[2];
        await(() -> {
            if (input.drain(axes)) { total[0] += axes[0]; total[1] += axes[1]; }
            return total[0] == expectedX && total[1] == expectedY;
        });
    }
    private static void rawDevices() {
        AtomicInteger lookups = new AtomicInteger(), polls = new AtomicInteger();
        float[] movement = {0f, 0f}; boolean[] connected = {true};
        Component[] components = new Component[2];
        for (int i = 0; i < components.length; i++) {
            final int axis = i;
            components[i] = (Component) Proxy.newProxyInstance(Component.class.getClassLoader(), new Class<?>[] {Component.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("isRelative")) return true;
                        if (method.getName().equals("getPollData")) return movement[axis];
                        throw new AssertionError("Unexpected axis call: " + method.getName());
                    });
        }
        Controller mouse = (Controller) Proxy.newProxyInstance(Controller.class.getClassLoader(), new Class<?>[] {Controller.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getType")) return Controller.Type.MOUSE;
                    if (method.getName().equals("poll")) { polls.incrementAndGet(); return connected[0]; }
                    if (method.getName().equals("getComponent")) {
                        lookups.incrementAndGet();
                        return args[0] == Component.Identifier.Axis.X ? components[0] : components[1];
                    }
                    throw new AssertionError("Unexpected device call: " + method.getName());
                });
        assert DollyRawMouse.open(new Controller[0]) == null;
        RawMouseInput.Source source = DollyRawMouse.open(new Controller[] {mouse});
        assert source != null && lookups.get() == 2;
        float[] axes = new float[2];
        assert source.poll(axes) == RawMouseInput.Source.WAITING;
        movement[0] = 2.25f; movement[1] = -3.75f;
        assert source.poll(axes) == RawMouseInput.Source.READY && axes[0] == 2.25f && axes[1] == -3.75f;
        for (int i = 0; i < 100; i++) assert source.poll(axes) == RawMouseInput.Source.READY;
        assert lookups.get() == 2 && polls.get() == 102 : "Polling rediscovered the axes";
        connected[0] = false;
        assert source.poll(axes) == RawMouseInput.Source.FAILED : "Disconnection must retain native fallback";
        assert lookups.get() == 2;
    }

    private static void rawInput() throws Exception {
        Source source = new Source();
        AtomicInteger opens = new AtomicInteger();
        RawMouseInput input = new RawMouseInput(() -> { opens.incrementAndGet(); return source; });
        int[] axes = new int[2];
        assert !input.drain(axes) && opens.get() == 0 : "Disabled raw input must not discover native devices";
        source.add(100f, 200f);
        input.setEnabled(true);
        await(() -> input.drain(axes));
        assert axes[0] == 0 && axes[1] == 0 : "Handoff must not replay motion already sampled by LWJGL";
        source.add(2.25f, -3.75f);
        motion(input, 2, -3);
        source.add(0.75f, -0.25f);
        motion(input, 1, -1);
        source.add(19f, 21f); sampled(source);
        input.setEnabled(false);
        assert !input.drain(axes) : "Disabling must discard pending motion";
        input.setEnabled(true);
        await(() -> input.drain(axes));
        assert axes[0] == 0 && axes[1] == 0 : "A new session must not replay old motion";
        int beforeFailure = source.polls.get();
        source.status = RawMouseInput.Source.FAILED;
        await(() -> source.polls.get() > beforeFailure);
        await(() -> !input.drain(axes));
        input.setEnabled(false);

        RawMouseInput unavailable = new RawMouseInput(() -> { throw new UnsatisfiedLinkError("No native backend"); });
        unavailable.setEnabled(true);
        Thread.sleep(10L);
        assert !unavailable.drain(axes) : "Unavailable backend must preserve LWJGL fallback";
        unavailable.setEnabled(false);

        CountDownLatch opening = new CountDownLatch(1), release = new CountDownLatch(1);
        RawMouseInput late = new RawMouseInput(() -> {
            opening.countDown();
            try { release.await(); } catch (InterruptedException ignored) { }
            return source;
        });
        late.setEnabled(true);
        assert opening.await(3, TimeUnit.SECONDS);
        late.setEnabled(false);
        release.countDown();
        assert !late.drain(axes) : "Late initialization must not revive a stopped session";

        CountDownLatch pending = new CountDownLatch(1), finish = new CountDownLatch(1);
        AtomicInteger starts = new AtomicInteger();
        source.status = RawMouseInput.Source.READY;
        RawMouseInput toggled = new RawMouseInput(() -> {
            if (starts.incrementAndGet() == 1) {
                pending.countDown();
                while (finish.getCount() > 0) {
                    try { finish.await(); } catch (InterruptedException ignored) { }
                }
            }
            return source;
        });
        toggled.setEnabled(true);
        assert pending.await(3, TimeUnit.SECONDS);
        for (int i = 0; i < 20; i++) { toggled.setEnabled(false); toggled.setEnabled(true); }
        assert starts.get() == 1 : "Focus changes must not multiply blocked device discovery workers";
        finish.countDown();
        await(() -> starts.get() == 2 && toggled.drain(axes));
        toggled.setEnabled(false);
    }
    public static void main(String[] args) throws Exception {
        DollySettings.config.inputFix = true;
        assert DollyInput.textEvent(true, 30, 'a');
        assert DollyInput.textEvent(false, 0, 'é');
        assert !DollyInput.textEvent(false, 30, 'a');
        assert !DollyInput.textEvent(false, 0, '\uFFFF');
        DollySettings.config.inputFix = false;
        assert !DollyInput.textEvent(false, 0, 'é');
        DollySettings.config.mouseBindFix = true;
        assert DollyInput.mouseInventoryKey(3) == -97 && DollyInput.mouseInventoryKey(4) == -96;
        assert DollyInput.mouseInventoryKey(0) == 0 && DollyInput.mouseInventoryKey(2) == 0;
        DollySettings.config.mouseBindFix = false;
        assert DollyInput.mouseInventoryKey(3) == 0;
        rawDevices(); rawInput();
        if (args.length > 0 && args[0].equals("--integration")) integration();
        System.out.println("Technical input passed: text-only events, inventory mouse bindings, raw lifecycle, atomic axes, fractional motion and native fallback.");
    }
}
