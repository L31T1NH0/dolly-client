import java.util.*;
import org.lwjgl.input.Mouse;

public final class DollyChatActions {
    static String describe(eu component) {
        et action = component == null ? null : component.b().h();
        if (action == null) return null;
        String kind = action.a().b();
        if (kind.equals("run_command")) kind = "Executar comando";
        else if (kind.equals("suggest_command")) kind = "Preencher comando";
        else if (kind.equals("open_url")) kind = "Abrir endereço";
        else if (kind.equals("open_file")) kind = "Abrir arquivo";
        return kind + ": " + action.b();
    }
    public static void draw(awv screen, int x, int y) {
        DollyChat chat = DollyChat.current();
        eu component = chat == null ? null : chat.a(Mouse.getX(), Mouse.getY());
        et action = component == null ? null : component.b().h();
        String description = DollySettings.config.chatInspectActions ? describe(component) : null;
        int height = 0;
        if (description != null) {
            List<String> lines = new ArrayList<String>();
            String rest = description;
            int width = Math.max(40, Math.min(400, screen.l - 40));
            while (!rest.isEmpty() && lines.size() < 32) {
                String line = ave.A().k.a(rest, width);
                if (line.isEmpty()) break;
                lines.add(line); rest = rest.substring(line.length());
            }
            height = lines.size() * 10 + 12;
            screen.a(lines, x, y);
        }
        String link = action != null && action.a() == et.a.a ? action.b() : "";
        DollyImagePreview.draw(link, x + 12, y + height + 12, screen.l, screen.m);
    }
}
