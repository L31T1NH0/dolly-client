import dolly.chat.MessageQueue;

/** Outbound chat only; keepalive, movement and combat packets remain native. */
public final class DollyChatSending {
    static final MessageQueue queue = new MessageQueue();
    private static boolean sending;
    private static Object connection;
    static String notice = "";
    public static boolean intercept(bew player, String message) {
        if (player != ave.A().h) return false;
        if (DollyScreenshotManager.command(message)) return true;
        if (sending) return false;
        context();
        if (!DollySettings.config.chatSendQueue) { queue.clear(); return false; }
        if (!queue.offer(message)) notice = "Fila cheia (100). Mensagem não enviada.";
        else { notice = ""; tick(); }
        return true;
    }
    private static void context() {
        ave mc = ave.A(); Object current = mc.h == null || mc.f == null ? null : mc.h.a;
        if (connection != current) { notice = ""; connection = current; }
        queue.context(current);
    }
    public static void tick() {
        context();
        if (!DollySettings.config.chatSendQueue || connection == null) { queue.clear(); return; }
        queue.tick(System.nanoTime() / 1000000L, Math.round(DollySettings.config.chatSendInterval * 1000d), message -> {
            sending = true;
            try { ave.A().h.e(message); }
            finally { sending = false; }
        });
    }
    public static String status() { return !notice.isEmpty() ? notice : queue.size() == 0 ? "" : "Fila: " + queue.size() + " · Ctrl+Backspace cancela"; }
    public static boolean history(boolean nativeHistory, String message) {
        if (message.startsWith("/dolly-screenshot ")) return false;
        return nativeHistory || DollySettings.config.chatClickedHistory && message.startsWith("/");
    }
}
