package dolly.chat;

import java.util.ArrayDeque;
import java.util.function.Consumer;

/** One server connection owns a queue; delayed ticks never cause a burst. */
public final class MessageQueue {
    private final ArrayDeque<String> pending = new ArrayDeque<String>();
    private Object connection;
    private long next;
    public int size() { return pending.size(); }
    public void clear() { pending.clear(); }
    public void context(Object current) { if (connection != current) { clear(); next = 0; connection = current; } }
    public boolean offer(String message) {
        if (pending.size() >= 100) return false;
        pending.addLast(message); return true;
    }
    public void tick(long now, long interval, Consumer<String> sender) {
        if (!pending.isEmpty() && now >= next) {
            String message = pending.removeFirst();
            next = now + interval;
            sender.accept(message);
        }
    }
}
