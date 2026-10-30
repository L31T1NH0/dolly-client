import dolly.chat.ChatHistory;
import dolly.chat.ChatHistory.Entry;
import dolly.config.DollyConfig;
import java.util.*;

/** Extended native chat, retaining Minecraft components for links and hover actions. */
public final class DollyChat extends avt {
    private final ave mc;
    final ChatHistory<eu> history = new ChatHistory<eu>();
    private final List<Row> lines = new ArrayList<Row>();
    private final Map<Entry<eu>, List<Row>> wrapped = new IdentityHashMap<Entry<eu>, List<Row>>();
    private String query = "", server = "";
    private int layoutWidth = -1, layoutHistory;
    private float layoutScale;
    private boolean layoutFilter, layoutNotices, layoutMentions;
    private String layoutHidden, layoutNotice, layoutMention, layoutNickname;
    private int scroll, unread, matches;
    private boolean dirty = true, boundary;
    private static final class Row {
        final Entry<eu> entry;
        final eu component;
        final int part;
        final boolean mention;
        final String text;
        Row(Entry<eu> entry, eu component, int part, boolean mention) {
            this.entry = entry; this.component = component; this.part = part; this.mention = mention; this.text = component.d();
        }
    }
    public DollyChat(ave mc) { super(mc); this.mc = mc; }
    static DollyChat current() {
        ave mc = ave.A();
        return mc != null && mc.q != null && mc.q.d() instanceof DollyChat ? (DollyChat) mc.q.d() : null;
    }
    public static void worldChanging(bdb next) {
        DollyChat chat = current();
        if (chat != null && next == null && ave.A().f != null) {
            chat.boundary = true;
            if (!DollySettings.config.chatKeepHistory) chat.a();
        }
    }
    static long now() { return System.nanoTime() / 1000000L; }
    private String serverName() { return mc.D() == null ? "Mundo local" : mc.D().b; }
    private String nickname() { return mc.h == null ? "" : mc.h.e_(); }
    private boolean hidden(String text) {
        DollyConfig c = DollySettings.config;
        return (c.chatFilter && ChatHistory.contains(text, c.chatHiddenWords))
                || (c.chatHideNotices && ChatHistory.contains(text, c.chatNoticeWords));
    }
    @Override public void a(eu component, int id) {
        if (boundary || !server.equals(serverName())) {
            if (!DollySettings.config.chatKeepHistory) a();
            server = serverName(); boundary = false;
            if (!history.entries().isEmpty()) history.add(new fa("§8—— " + server + " ——"), server, 0, now(), false,
                    DollySettings.config.chatHistory, true);
        }
        String text = ChatHistory.plain(component.c());
        Entry<eu> entry = history.add(component.f(), text, id, now(), DollySettings.config.chatGroup,
                DollySettings.config.chatHistory, false, DollySettings.config.chatGroupConsecutive, DollySettings.config.chatGroupSeconds * 1000L);
        wrapped.remove(entry); dirty = true;
        if (!hidden(text)) {
            if (e() && scroll > 0) unread++;
            if (DollySettings.config.chatMentions && DollySettings.config.chatMentionSound
                    && ChatHistory.mentions(text, nickname(), DollySettings.config.chatMentionWords) && mc.h != null)
                mc.h.a("note.pling", 0.5f, 1.2f);
        }
        org.apache.logging.log4j.LogManager.getLogger().info("[CHAT] " + component.c());
    }
    @Override public void a() { history.clear(); wrapped.clear(); lines.clear(); super.a(); d(); dirty = true; }
    public static void leavingMenu(avt chat) {
        if (chat instanceof DollyChat && DollySettings.config.chatKeepHistory) {
            DollyChat dolly = (DollyChat) chat;
            dolly.boundary = true; dolly.search(""); dolly.d();
        } else chat.a();
    }
    @Override public void c(int id) { history.remove(id); dirty = true; }
    @Override public void b() { wrapped.clear(); dirty = true; }
    @Override public void d() { scroll = 0; unread = 0; }
    @Override public void b(int amount) {
        prepare(); scroll = Math.max(0, Math.min(Math.max(0, lines.size() - i()), scroll + amount));
        if (scroll == 0) unread = 0;
    }
    public void search(String text) {
        if (!query.equals(text)) { query = text; d(); dirty = true; }
    }
    public int matches() { prepare(); return matches; }
    public int unread() { return unread; }
    @Override public float h() { return DollySettings.config.chatScale; }
    @Override public int f() {
        int available = new avr(mc).a() - 8;
        if (DollyHudPositions.layout().get(5) != null) available -= (int) Math.ceil(4 * h());
        return Math.min(DollySettings.config.chatWidth, Math.max(40, available));
    }
    @Override public int g() {
        int available = new avr(mc).b() - (e() ? 72 : 50);
        return Math.max(lineHeight(), Math.min(DollySettings.config.chatHeight, (int) (available / h())));
    }
    private int lineHeight() { return 9 + DollySettings.config.chatSpacing; }
    @Override public int i() { return Math.max(1, g() / lineHeight()); }
    private void prepare() {
        DollyConfig c = DollySettings.config;
        int availableWidth = f();
        String nick = nickname();
        if (layoutWidth != availableWidth || layoutScale != h() || layoutHistory != c.chatHistory
                || layoutFilter != c.chatFilter || layoutNotices != c.chatHideNotices || layoutMentions != c.chatMentions
                || !Objects.equals(layoutHidden, c.chatHiddenWords) || !Objects.equals(layoutNotice, c.chatNoticeWords)
                || !Objects.equals(layoutMention, c.chatMentionWords) || !Objects.equals(layoutNickname, nick)) {
            layoutWidth = availableWidth; layoutScale = h(); layoutHistory = c.chatHistory;
            layoutFilter = c.chatFilter; layoutNotices = c.chatHideNotices; layoutMentions = c.chatMentions;
            layoutHidden = c.chatHiddenWords; layoutNotice = c.chatNoticeWords;
            layoutMention = c.chatMentionWords; layoutNickname = nick;
            wrapped.clear(); dirty = true;
        }
        if (!dirty) return;
        Row anchor = scroll > 0 && scroll < lines.size() ? lines.get(scroll) : null;
        history.trim(c.chatHistory);
        wrapped.keySet().retainAll(new HashSet<Entry<eu>>(history.entries()));
        lines.clear(); matches = 0;
        List<Entry<eu>> entries = history.entries();
        String needle = query.toLowerCase(Locale.ROOT);
        for (int index = entries.size() - 1; index >= 0; index--) {
            Entry<eu> entry = entries.get(index);
            if ((!entry.separator && hidden(entry.text))
                    || (!needle.isEmpty() && (entry.separator || !entry.text.toLowerCase(Locale.ROOT).contains(needle)))) continue;
            if (!entry.separator) matches++;
            List<Row> prepared = wrapped.get(entry);
            if (prepared == null) {
                eu display = entry.value.f();
                if (entry.repeats > 1) display.a(new fa(" §7×" + entry.repeats));
                List<eu> parts = avu.a(display, Math.max(20, (int) (availableWidth / h())), mc.k, false, false);
                boolean mention = c.chatMentions && !entry.separator && ChatHistory.mentions(entry.text, nick, c.chatMentionWords);
                prepared = new ArrayList<Row>(parts.size());
                for (int part = parts.size() - 1; part >= 0; part--) prepared.add(new Row(entry, parts.get(part), part, mention));
                wrapped.put(entry, prepared);
            }
            lines.addAll(prepared);
        }
        if (anchor != null) for (int index = 0; index < lines.size(); index++) {
            Row row = lines.get(index);
            if (row.entry == anchor.entry && row.part == anchor.part) { scroll = index; break; }
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, lines.size() - i())));
        dirty = false;
    }
    @Override public void a(int tick) {
        if (mc.t.m == wn.b.c || !DollyHudPositions.visible(5)) return;
        prepare();
        DollyConfig c = DollySettings.config;
        bfl.E();
        try {
            avr resolution = new avr(mc);
            float areaWidth = f() + 4 * h(), areaHeight = i() * lineHeight() * h();
            if (DollyHudPositions.editing()) DollyHudPositions.record(5, DollyHudPositions.chat(resolution, areaWidth, areaHeight));
            bfl.b(DollyHudPositions.chatOffsetX(resolution, areaWidth, areaHeight),
                    DollyHudPositions.chatOffsetY(resolution, areaWidth, areaHeight), 0);
            bfl.b(2f, 20f, 0f); bfl.a(h(), h(), 1f);
            int width = (int) (f() / h());
            int visibleLines = i();
            boolean open = e() || DollyHudPositions.editing();
            long time = now();
            for (int row = 0; row < visibleLines && row + scroll < lines.size(); row++) {
                Row line = lines.get(row + scroll);
                float fade = open ? 1f : ChatHistory.opacity(time - line.entry.received, c.chatDuration, c.chatFade);
                int alpha = (int) (255 * fade * c.chatOpacity / 100f);
                if (alpha <= 3) continue;
                int y = -row * lineHeight();
                if (c.chatBackground) avp.a(0, y - lineHeight(), width + 4, y,
                        (int) (alpha * c.chatBackgroundOpacity / 100f) << 24);
                if (line.mention || !query.isEmpty()) avp.a(0, y - lineHeight(), width + 4, y,
                        ((int) (alpha * 0.25f) << 24) | (line.mention ? 0xC69A32 : 0x47795B));
                bfl.l(); mc.k.a(line.text, 0f, y - 8f, (alpha << 24) | 0xFFFFFF);
                bfl.c(); bfl.k();
            }
            if (open && lines.size() > visibleLines) {
                int height = visibleLines * lineHeight(), thumb = Math.max(4, height * visibleLines / lines.size());
                int top = -height + (height - thumb) * (lines.size() - visibleLines - scroll) / Math.max(1, lines.size() - visibleLines);
                avp.a(-2, top, -1, top + thumb, 0xFFBFEAD1);
            }
        } finally { bfl.F(); }
    }
    private Row hit(int rawX, int rawY) {
        if (!e() || mc.t.m == wn.b.c || !DollyHudPositions.visible(5)) return null;
        prepare(); avr resolution = new avr(mc); int factor = resolution.e();
        float width = f() + 4 * h(), height = i() * lineHeight() * h();
        float x = (rawX / (float) factor - 2 - DollyHudPositions.chatOffsetX(resolution, width, height)) / h();
        float y = (rawY / (float) factor - 28 + DollyHudPositions.chatOffsetY(resolution, width, height)) / h();
        int row = (int) (y / lineHeight());
        return x >= 0 && x <= f() / h() && y >= 0 && row < i() && row + scroll < lines.size()
                ? lines.get(row + scroll) : null;
    }
    public String messageAt(int rawX, int rawY) { Row row = hit(rawX, rawY); return row == null ? null : row.entry.text; }
    @Override public eu a(int rawX, int rawY) {
        Row row = hit(rawX, rawY);
        if (row == null) return null;
        avr resolution = new avr(mc);
        float x = (rawX / (float) resolution.e() - 2
                - DollyHudPositions.chatOffsetX(resolution, f() + 4 * h(), i() * lineHeight() * h())) / h();
        int width = 0;
        for (eu part : row.component) {
            width += mc.k.a(part.b().k() + part.e());
            if (width > x) return part;
        }
        return null;
    }
}
