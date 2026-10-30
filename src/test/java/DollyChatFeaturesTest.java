import dolly.chat.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

public final class DollyChatFeaturesTest {
    static void run() throws Exception {
        MessageQueue queue = new MessageQueue(); Object server = new Object(); queue.context(server);
        List<String> sent = new ArrayList<String>();
        assert queue.offer("one"); queue.tick(0, 1000, sent::add);
        assert queue.offer("two") && queue.offer("three");
        queue.tick(999, 1000, sent::add); assert sent.size() == 1;
        queue.tick(1000, 1000, sent::add); assert sent.equals(Arrays.asList("one", "two"));
        queue.tick(100000, 1000, sent::add); assert sent.size() == 3;
        queue.offer("four"); queue.offer("five"); queue.tick(200000, 1000, sent::add);
        assert sent.size() == 4 && queue.size() == 1 : "Delayed ticks must never burst";
        queue.context(server); assert queue.size() == 1;
        queue.clear(); queue.offer("after cancellation"); queue.tick(200001, 1000, sent::add);
        assert sent.size() == 4 : "Cancelling pending messages must retain the send cooldown";
        queue.context(new Object()); assert queue.size() == 0;
        for (int i = 0; i < 100; i++) assert queue.offer("m" + i);
        assert !queue.offer("overflow"); queue.clear(); assert queue.size() == 0;
        ChatHistory<String> chat = new ChatHistory<String>();
        ChatHistory.Entry<String> first = chat.add("a", "a", 0, 0, true, 100, false, false, 1000);
        chat.add("b", "b", 0, 1, true, 100, false, false, 1000);
        assert chat.add("a", "a", 0, 500, true, 100, false, false, 1000) == first;
        assert chat.entries().size() == 2 && chat.entries().get(1).repeats == 2;
        chat.add("a", "a", 0, 1501, true, 100, false, false, 1000);
        assert chat.entries().size() == 3 : "Expired repetitions must remain separate";
        chat.add("server", "server", 0, 1502, true, 100, true, false, 1000);
        chat.add("a", "a", 0, 1503, true, 100, false, false, 1000);
        assert chat.entries().size() == 5 : "Never merge across server boundaries";
        chat.add("-----", "-----", 0, 1504, true, 100, false, false, 1000);
        chat.add("-----", "-----", 0, 1505, true, 100, false, false, 1000);
        assert chat.entries().size() == 7 : "Dividers must not be compacted";
        assert LinkImage.compatible("https://example.com/a.PNG?size=2") != null;
        assert LinkImage.compatible("file:///tmp/a.png") == null;
        assert LinkImage.compatible("https://example.com/page") == null;
        assert LinkImage.compatible("https://user:pass@example.com/a.jpg") == null;
        try { LinkImage.fetch(new java.net.URI("http://127.0.0.1/a.png")); throw new AssertionError("Local address accepted"); }
        catch (IOException expected) { }
        BufferedImage image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0x80402010);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageIO.write(image, "png", bytes);
        BufferedImage scaled = LinkImage.decode(bytes.toByteArray());
        assert scaled.getWidth() == 320 && scaled.getHeight() == 180;
        try { LinkImage.decode(new byte[] {1, 2, 3}); throw new AssertionError("Invalid image accepted"); }
        catch (IOException expected) { }
        try { LinkImage.decode(new byte[4 * 1024 * 1024 + 1]); throw new AssertionError("Oversize image accepted"); }
        catch (IOException expected) { }
    }
}
