import java.util.*;
import org.lwjgl.input.Keyboard;

/** Searchable native event list. Registry failures keep navigation and saved preferences available. */
public final class DollyAudioScreen extends axu {
    private final axu parent;
    private avw search;
    private int page;
    private String query = "", selected;
    private List<String> ids = Collections.emptyList(), filtered = Collections.emptyList();
    private final DollyOptionHelp tooltip = new DollyOptionHelp();

    public DollyAudioScreen(axu parent) { this.parent = parent; DollyAudio.error = DollyAudio.status = null; }
    private int rows() { return Math.max(1, (m - 154) / 26); }
    private int pages() { return Math.max(1, (filtered.size() + rows() - 1) / rows()); }
    private void filter() {
        List<String> matches = new ArrayList<String>();
        String needle = query.toLowerCase(Locale.ROOT).trim();
        for (String id : ids) if (id.toLowerCase(Locale.ROOT).contains(needle)
                || id.replace('.', ' ').replace('_', ' ').toLowerCase(Locale.ROOT).contains(needle)) matches.add(id);
        filtered = matches;
    }
    @Override public void b() {
        ids = DollyAudio.ids(); filter(); n.clear(); tooltip.reset();
        int width = Math.min(560, l - 20), left = (l - width) / 2;
        if (search != null) query = search.b();
        search = new avw(0, q, left, 44, width, 20); search.f(100); search.a(query);
        filter(); page = Math.max(0, Math.min(page, pages() - 1));
        int sliderWidth = Math.max(60, width / 3);
        for (int i = page * rows(); i < Math.min(filtered.size(), (page + 1) * rows()); i++) {
            int row = i - page * rows();
            n.add(new VolumeSlider(200 + row, left + width - sliderWidth, 74 + row * 26, sliderWidth, filtered.get(i)));
        }
        n.add(new DollyButton(0, left, m - 76, 60, 20, "Anterior")); n.get(n.size() - 1).l = page > 0;
        n.add(new DollyButton(1, left + width - 60, m - 76, 60, 20, "Próxima")); n.get(n.size() - 1).l = page + 1 < pages();
        n.add(new DollyButton(2, left, m - 50, (width - 8) / 2, 20, "Recarregar sons"));
        n.add(new DollyButton(3, left + (width + 8) / 2, m - 50, (width - 8) / 2, 20, "Recarregar texturas"));
        n.add(new DollyButton(4, left, m - 26, (width - 8) / 2, 20, "Restaurar volumes"));
        n.add(new DollyButton(5, left + (width + 8) / 2, m - 26, (width - 8) / 2, 20, "Concluir"));
    }
    @Override protected void a(avs button) {
        if (!button.l) return;
        if (button instanceof VolumeSlider) { selected = ((VolumeSlider) button).id; return; }
        if (button.k == 0) page--;
        if (button.k == 1) page++;
        if (button.k == 2) DollyAudio.reloadSounds(ave.A());
        if (button.k == 3) DollyAudio.reloadTextures(ave.A());
        if (button.k == 4) { DollyAudio.reset(); DollyAudio.commit(); }
        if (button.k == 5) { ave.A().a(parent); return; }
        b();
    }
    @Override protected void a(char character, int code) {
        if (code == Keyboard.KEY_ESCAPE) { ave.A().a(parent); return; }
        if (search.a(character, code)) {
            query = search.b(); filter(); page = 0; selected = null;
            // Preserve the input caret/focus while replacing only the visible rows.
            avw input = search; b(); search = input;
        } else if (selected != null && (code == Keyboard.KEY_LEFT || code == Keyboard.KEY_RIGHT || code == Keyboard.KEY_DELETE)) {
            int value = code == Keyboard.KEY_DELETE ? 100 : Math.max(0, Math.min(200,
                    DollyAudio.percent(selected) + (code == Keyboard.KEY_LEFT ? -5 : 5)));
            change(selected, value); DollyAudio.commit(); b();
        }
    }
    @Override protected void a(int x, int y, int button) {
        search.a(x, y, button); super.a(x, y, button);
    }
    private static void change(String id, int value) {
        try { DollyAudio.set(id, value); }
        catch (IllegalArgumentException e) { DollyAudio.error = e.getMessage(); }
    }
    @Override public void a(int mouseX, int mouseY, float partialTicks) {
        c(); int width = Math.min(560, l - 20), left = (l - width) / 2, sliderWidth = Math.max(60, width / 3);
        a(q, "Dolly · Sons individuais", l / 2, 12, 0xFFE8EEE9);
        String status = DollySettings.lastError != null ? DollySettings.lastError : DollyAudio.error;
        if (status == null) status = DollyAudio.status == null ? "Buscar som · arraste de 0% a 200%" : DollyAudio.status;
        a(q, q.a(status, l - 20), l / 2, 29, DollyAudio.error == null && DollySettings.lastError == null ? 0xFFAAAAAA : 0xFFFF7777);
        search.g();
        for (int i = page * rows(); i < Math.min(filtered.size(), (page + 1) * rows()); i++) {
            String id = filtered.get(i), label = id.startsWith("minecraft:") ? id.substring(10) : id;
            q.a(q.a(label, width - sliderWidth - 10), left, 80 + (i - page * rows()) * 26, 0xFFE8EEE9);
        }
        if (filtered.isEmpty()) a(q, "Nenhum som encontrado", l / 2, 80, 0xFFAAAAAA);
        a(q, (page + 1) + "/" + pages() + " · " + filtered.size() + " sons", l / 2, m - 70, 0xFFAAAAAA);
        super.a(mouseX, mouseY, partialTicks);
        avs hovered = DollyOptionHelp.hovered(n, mouseX, mouseY);
        String key = hovered == null ? null : hovered instanceof VolumeSlider ? "soundVolume"
                : hovered.k == 2 ? "reloadSounds" : hovered.k == 3 ? "reloadTextures" : hovered.k == 4 ? "resetVolumes" : null;
        String title = key == null ? "" : hovered instanceof VolumeSlider ? ((VolumeSlider) hovered).id : hovered.j;
        List<String> help = tooltip.lines(key, title, q, l, System.nanoTime());
        if (help != null) a(help, tooltip.anchorX(mouseX, l), mouseY);
    }
    @Override public void m() { DollyAudio.commit(); }

    private static final class VolumeSlider extends avs {
        final String id;
        boolean dragging;
        VolumeSlider(int button, int x, int y, int width, String id) {
            super(button, x, y, width, 20, DollyAudio.percent(id) + "%"); this.id = id;
        }
        private void move(int x) {
            int percent = Math.max(0, Math.min(200, Math.round((x - h - 4) * 200f / Math.max(1, f - 8))));
            if (percent != DollyAudio.percent(id)) change(id, percent);
            j = DollyAudio.percent(id) + "%";
        }
        @Override public boolean c(ave mc, int x, int y) {
            if (!super.c(mc, x, y)) return false;
            dragging = true; move(x); return true;
        }
        @Override protected void b(ave mc, int x, int y) {
            if (dragging) move(x);
            int thumb = h + 4 + Math.round((f - 8) * DollyAudio.percent(id) / 200f);
            avp.a(h + 4, i + g - 5, h + f - 4, i + g - 3, 0xFF68796D);
            avp.a(thumb - 2, i + 3, thumb + 2, i + g - 3, 0xFFBFEAD1);
        }
        @Override public void a(int x, int y) { if (dragging) { dragging = false; DollyAudio.commit(); } }
    }
}
