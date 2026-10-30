/** User-facing features are separate from renderer, input and loading implementation switches. */
public final class DollyModules {
    public static final class Module {
        final int option, icon, hud;
        final String name;
        final int[] settings;
        Module(int option, String name, int icon, int hud, int... settings) {
            this.option = option; this.name = name; this.icon = icon; this.hud = hud; this.settings = settings;
        }
        int[] options() {
            int[] all = new int[settings.length + 1]; all[0] = option;
            System.arraycopy(settings, 0, all, 1, settings.length); return all;
        }
    }
    static final Module[] ALL = {
            new Module(7, "Zoom", 0, -1, 18, 51, 54, 62, 56, 55, 57, 52, 53, 63),
            new Module(8, "Perspective", 1, -1),
            new Module(5, "Toggle Sprint", 2, 2),
            new Module(14, "Bloquear scroll", 3, -1),
            new Module(89, "Perfil de slots", 4, 3, 25),
            new Module(12, "Ping", 5, 0),
            new Module(11, "TNT Timer", 6, 1),
            new Module(90, "Scoreboard", 7, 4, 24),
            new Module(4, "Old Animations", 8, -1),
            new Module(9, "Fullbright", 9, -1, 58),
            new Module(10, "Horário visual", 10, -1, 19),
            new Module(6, "Agachar suave", 2, -1),
            new Module(69, "Ocultar partículas próprias", 11, -1),
            new Module(70, "Ocultar fragmentos", 12, -1),
            new Module(71, "Ocultar raios", 13, -1),
            new Module(72, "Ocultação inicial de projéteis", 14, -1),
            new Module(48, "Ocultar livros de encanto", 15, -1),
            new Module(49, "Item único por pilha", 16, -1),
            new Module(50, "Itens sem balanço", 12, -1),
            new Module(67, "Remover camada da água", 17, -1),
            new Module(66, "Ocultar fogo protegido", 18, -1)
    };
    static final String[] GROUPS = {"Entrada e câmera", "Vídeo e desempenho", "Carregamento", "Chat", "Efeitos visuais", "Capturas e áudio", "Menus e conexão", "Manutenção e F3"};
    static final int[][] SETTINGS = {
            {0, 1, 2, 3, 26, 27, 28, 59, 60, 91},
            {15, 16, 22, 20, 21, 42, 43, 44, 61, 85, 41, 29, 35, 36, 13, 17, 45, 47},
            {33, 34, 30, 37, 46, 32}, {23}, {64, 65, 68, 73, 86},
            {76, 31, 80, 81, 82, 74, 75}, {77, 78, 79, 39, 40}, {83, 84, 87, 88}
    };
    static int[] matching(String query, int filter) {
        String needle = query.toLowerCase(java.util.Locale.ROOT).trim();
        int count = 0;
        for (Module module : ALL) if (matches(module, needle, filter)) count++;
        int[] indices = new int[count]; int index = 0;
        for (int i = 0; i < ALL.length; i++) if (matches(ALL[i], needle, filter)) indices[index++] = i;
        return indices;
    }
    private static boolean matches(Module module, String query, int filter) {
        return module.name.toLowerCase(java.util.Locale.ROOT).contains(query)
                && (filter == 0 || DollyScreen.enabled(module.option) == (filter == 1));
    }
}
