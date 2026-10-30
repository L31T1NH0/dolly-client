import dolly.input.SlotProfile;
import org.lwjgl.input.Keyboard;

/** One saved hotbar profile, with explicit per-slot state and a toggle shortcut. */
public final class DollySlotScreen extends axu {
    private final axu parent;
    private boolean binding;
    private String message;
    private final DollyOptionHelp tooltip = new DollyOptionHelp();
    public DollySlotScreen(axu parent) { this.parent = parent; }
    @Override public void b() {
        n.clear(); tooltip.reset();
        int width = Math.min(350, l - 20), left = (l - width) / 2, cell = (width - 8) / 3;
        n.add(new DollyButton(0, left, 36, width, 20, "Perfil de slots: " + (DollySettings.config.slotProfile ? "§aON" : "§cOFF")));
        for (int slot = 0; slot < 9; slot++) n.add(new DollyButton(10 + slot, left + (slot % 3) * (cell + 4),
                62 + (slot / 3) * 24, cell, 20, (slot + 1) + ": "
                + (SlotProfile.blocked(DollySettings.config.blockedSlots, slot) ? "§cBloqueado" : "§aLivre")));
        int key = DollySettings.config.slotProfileKey;
        String name = key == 0 ? "Nenhum" : key < 0 ? "Mouse " + (key + 101) : Keyboard.getKeyName(key);
        int half = (width - 8) / 2;
        n.add(new DollyButton(1, left, m - 28, half, 20, binding ? "Pressione uma tecla…" : "Atalho: " + name));
        n.add(new DollyButton(2, left + half + 8, m - 28, half, 20, "Concluir"));
    }
    @Override protected void a(avs button) {
        if (!button.l) return;
        if (button.k == 2) { ave.A().a(parent); return; }
        if (button.k == 1) { binding = true; message = "Delete remove · Esc cancela"; b(); return; }
        binding = false; message = null;
        if (button.k == 0) DollySlots.toggle();
        else if (button.k >= 10 && button.k < 19 && !DollySlots.toggleSlot(button.k - 10)) message = "Mantenha pelo menos um slot livre";
        b();
    }
    @Override protected void a(char character, int code) {
        if (binding) {
            if (code != Keyboard.KEY_ESCAPE) {
                DollySettings.config.slotProfileKey = code == Keyboard.KEY_DELETE || code == Keyboard.KEY_BACK ? 0 : code;
                DollyInput.syncSlotProfileKey(); ave.A().t.b();
            }
            binding = false; message = null; b();
        } else if (code == Keyboard.KEY_ESCAPE) ave.A().a(parent);
        else super.a(character, code);
    }
    @Override public void a(int x, int y, float partialTicks) {
        c();
        a(q, "Dolly · Perfil de slots", l / 2, 12, 0xFFE8EEE9);
        String hint = DollySettings.lastError != null ? "Erro ao salvar: " + DollySettings.lastError
                : message != null ? message : "Bloqueia seleção; inventário continua livre";
        a(q, q.a(hint, l - 20), l / 2, m - 44, 0xFFDDDDDD);
        super.a(x, y, partialTicks);
        avs hovered = DollyOptionHelp.hovered(n, x, y);
        String key = binding || hovered == null ? null : hovered.k == 0 ? "slotProfile"
                : hovered.k == 1 ? "slotProfileKey" : hovered.k >= 10 && hovered.k < 19 ? "blockedSlots" : null;
        String title = key == null ? "" : key.equals("blockedSlots") ? "Slot " + (hovered.k - 9)
                : key.equals("slotProfileKey") ? "Atalho do perfil" : "Perfil de slots";
        java.util.List<String> help = tooltip.lines(key, title, q, l, System.nanoTime());
        if (help != null) a(help, tooltip.anchorX(x, l), y);
    }
    @Override public void m() { DollySettings.persist(ave.A().t); }
}
