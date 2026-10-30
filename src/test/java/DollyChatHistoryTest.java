import dolly.chat.ChatHistory;
import dolly.chat.ChatHistory.Entry;

public final class DollyChatHistoryTest {
    public static void run() {
        ChatHistory<String> history = new ChatHistory<String>();
        Entry<String> first = history.add("hello", "hello", 0, 10, true, 100, false);
        assert history.add("hello", "hello", 0, 20, true, 100, false) == first;
        assert history.entries().size() == 1 && first.repeats == 2 && first.received == 20;
        history.add("other", "other", 0, 30, true, 100, false);
        history.add("hello", "hello", 0, 40, true, 100, false);
        assert history.entries().size() == 3 : "Only consecutive identical messages group";
        history.add("update", "update", 5, 50, true, 100, false);
        history.add("updated", "updated", 5, 60, true, 100, false);
        assert history.entries().size() == 4 && history.entries().get(3).text.equals("updated");
        history.remove(5); assert history.entries().size() == 3;
        history.add("server", "server", 0, 70, true, 100, true);
        history.add("server", "server", 0, 80, true, 100, false);
        assert history.entries().size() == 5 : "A separator never groups with a message";
        history.trim(2); assert history.entries().size() == 2 && history.entries().get(0).separator;
        for (int i = 0; i < 200; i++) history.add("m" + i, "m" + i, 0, i, false, 100, false);
        assert history.entries().size() == 100 && history.entries().get(0).text.equals("m100");
        history.clear(); assert history.entries().isEmpty();
        assert ChatHistory.contains("§aPROMOção aqui", " ; outro ; promoção ;");
        assert !ChatHistory.contains("normal", " ; ; ");
        assert ChatHistory.mentions("Oi, Dolly!", "dolly", "");
        assert !ChatHistory.mentions("Dolly123 e SuperDolly", "Dolly", "");
        assert !ChatHistory.mentions("Dolly_2", "Dolly", "");
        assert ChatHistory.mentions("§cBED DESTRUÍDA", "", "bed destruída");
        assert ChatHistory.opacity(0, 10, 1) == 1;
        assert ChatHistory.opacity(9500, 10, 1) == 0.5f;
        assert ChatHistory.opacity(10000, 10, 1) == 0;
        assert ChatHistory.opacity(9999, 10, 0) == 1;
        assert ChatHistory.opacity(10000, 10, 0) == 0;
        assert ChatHistory.opacity(0, 1, 10) == 1 : "Fade longer than duration must still start fully visible";
    }
}
