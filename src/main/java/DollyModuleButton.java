/** Open-details area and the separate ON/OFF control have independent click targets. */
public final class DollyModuleButton extends avs {
    private final DollyModules.Module module;
    private final boolean selected;
    DollyModuleButton(int id, int x, int y, int width, DollyModules.Module module, boolean selected) {
        super(id, x, y, width, 26, module.name); this.module = module; this.selected = selected;
    }
    @Override public void a(ave mc, int x, int y) {
        if (!m) return;
        n = x >= h && x < h + f && y >= i && y < i + g;
        avp.a(h, i, h + f, i + g, selected ? 0xFF2C3D32 : n ? 0xFF253329 : 0xFF1B251F);
        DollyIcons.draw(module.icon, h + 5, i + 5);
        mc.k.a(mc.k.a(j, Math.max(1, f - 32)), h + 28, i + 9, 0xFFE8EEE9);
    }
}
