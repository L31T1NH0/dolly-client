import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Chat-only search and copy gestures; vanilla keeps command input and link handling. */
public final class DollyChatControls {
    private static awv owner;
    private static avw search;
    private static boolean searching;
    private static long copiedUntil;
    public static void open(awv screen) {
        String text = owner == screen && search != null ? search.b() : "";
        boolean restore = owner == screen && searching;
        owner = screen;
        search = new avw(1, ave.A().k, 6, 6, Math.max(40, screen.l - 12), 18);
        search.f(256); search.a(text); search.b(restore); searching = restore;
    }
    public static void close() {
        DollyChat chat = DollyChat.current();
        if (chat != null) chat.search("");
        owner = null; search = null; searching = false; DollyImagePreview.close();
    }
    private static boolean ctrl() { return DollyInput.physicalKey(Keyboard.KEY_LCONTROL) || DollyInput.physicalKey(Keyboard.KEY_RCONTROL); }
    public static boolean key(awv screen, char character, int code) {
        if (owner != screen) open(screen);
        if (ctrl() && code == Keyboard.KEY_F) {
            searching = !searching; search.b(searching);
            if (!searching && DollyChat.current() != null) DollyChat.current().search("");
            else if (DollyChat.current() != null) DollyChat.current().search(search.b());
            return true;
        }
        if (ctrl() && code == Keyboard.KEY_BACK) {
            DollyChatSending.queue.clear(); DollyChatSending.notice = "Fila cancelada"; return true;
        }
        if (!searching) return false;
        DollyChat chat = DollyChat.current();
        if (code == Keyboard.KEY_ESCAPE) {
            searching = false; search.b(false); if (chat != null) chat.search("");
        } else if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_NUMPADENTER) {
            if (chat != null) chat.b((axu.r() ? -1 : 1) * Math.max(1, chat.i() - 1));
        } else if (code == Keyboard.KEY_PRIOR || code == Keyboard.KEY_NEXT) {
            if (chat != null) chat.b((code == Keyboard.KEY_PRIOR ? 1 : -1) * Math.max(1, chat.i() - 1));
        } else {
            search.a(character, code); if (chat != null) chat.search(search.b());
        }
        return true;
    }
    public static boolean click(awv screen, int x, int y, int button) {
        if (owner != screen) open(screen);
        DollyChat chat = DollyChat.current();
        if (searching && y <= 42) { search.a(x, y, button); search.b(true); return true; }
        if (button == 0 && ctrl() && chat != null) {
            String message = chat.messageAt(Mouse.getX(), Mouse.getY());
            if (message != null) { axu.e(message); copiedUntil = DollyChat.now() + 1500; return true; }
        }
        if (button == 0 && chat != null && chat.unread() > 0 && DollyChat.now() >= copiedUntil
                && x >= 2 && x < statusRight(screen, status(chat)) && y >= screen.m - 29 && y < screen.m - 16) {
            chat.d(); return true;
        }
        return false;
    }
    public static void draw(awv screen, int mouseX, int mouseY) {
        if (owner != screen) open(screen);
        DollyChat chat = DollyChat.current();
        if (searching) {
            search.g();
            String hint = search.b().isEmpty() ? "Digite para buscar no histórico" : (chat == null ? 0 : chat.matches()) + " mensagens encontradas";
            ave.A().k.a(hint + " · Enter: mais · Esc: voltar", 6, 29, 0xFFFFFFFF);
        } else ave.A().k.a("Ctrl+F: buscar · Ctrl+clique: copiar", 6, 6, 0xFFFFFFFF);
        DollyChatActions.draw(screen, mouseX, mouseY);
        String sending = DollyChatSending.status();
        if (!sending.isEmpty()) ave.A().k.a(sending, 6, 44, 0xFFFFFF55);
        String status = status(chat);
        if (!status.isEmpty()) {
            avp.a(2, screen.m - 29, statusRight(screen, status), screen.m - 16, 0xDD17231C);
            ave.A().k.a(status, 5, screen.m - 27, 0xFFFFFFFF);
        }
    }
    private static String status(DollyChat chat) {
        return DollyChat.now() < copiedUntil ? "Mensagem copiada" : chat != null && chat.unread() > 0
                ? chat.unread() + (chat.unread() == 1 ? " nova mensagem" : " novas mensagens") + " · clique para descer" : "";
    }
    private static int statusRight(awv screen, String text) { return Math.min(screen.l - 2, ave.A().k.a(text) + 8); }
    public static void tick() { if (searching && search != null) search.a(); }
}
