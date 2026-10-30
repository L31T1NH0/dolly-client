import java.util.*;

/** Local help, prepared only when the hovered setting changes. Colors describe impact, not benchmarks. */
public final class DollyOptionHelp {
    private static final Map<String, Tip> TIPS = new HashMap<String, Tip>();
    static final class Tip {
        final String description, impact;
        Tip(String description, String impact) { this.description = description; this.impact = impact; }
    }
    private static void add(String key, String description, String impact) { TIPS.put(key, new Tip(description, impact)); }
    static Tip tip(String key) { return TIPS.get(key); }
    static {
        add("hudScale", "Ajusta o tamanho do elemento selecionado. O desenho é limitado ao espaço da tela; chat e scoreboard compartilham a escala com suas configurações.", "§eTexto maior ocupa mais pixels; o custo depende do elemento e da quantidade de linhas.");
        add("hudBackground", "Ajusta o fundo do elemento selecionado. Zero remove o fundo; o valor não muda a opacidade do texto.", "§eUm fundo visível acrescenta um retângulo com transparência ao desenho.");
        add("hudVisible", "Mostra ou oculta apenas o elemento do HUD. Não desliga a corrida ou o perfil de slots. O editor mantém exemplos para posicionamento.", "§aOcultar evita o desenho desse elemento; mensagens do chat continuam sendo recebidas.");
        add("hudSnap", "Aproxima o elemento das bordas ou do centro enquanto você o arrasta. Desligue para posicionar livremente.", "§7Custo mínimo, somente durante a edição do HUD.");
        add("hudUndo", "Desfaz a última alteração do HUD. Mantém até trinta estados nesta edição; Ctrl+Z também desfaz.", "§7Custo pequeno e limitado de memória enquanto o editor está aberto.");
        add("hudReset", "Restaura as posições dos seis elementos, mantendo escala, fundo e visibilidade. É possível desfazer nesta edição.", "§7Sem custo contínuo: muda as posições e salva o layout.");
        add("resetSettings", "Restaura as opções do Dolly em todas as abas e os padrões de vídeo aplicados pelo client.", "§cCusto pontual: pode recarregar recursos ou o renderer ao aplicar os padrões.");
        add("clickFix", "Evita a penalidade de cliques após atacar sem acertar um alvo. Não cria cliques automáticos.", "§7Custo mínimo: verificações na entrada.");
        add("mouseFix", "Corrige a direção usada para mirar com a rotação do jogador e sua interpolação.", "§7Custo mínimo: cálculo da direção da mira.");
        add("keyFix", "Recupera teclas de movimento presas ao perder foco ou trocar de tela.", "§7Custo mínimo: consulta poucas teclas por tick.");
        add("linuxShiftFix", "Corrige combinações de Shift e teclas numéricas em layouts afetados no Linux.", "§7Custo mínimo: apenas em eventos de teclado.");
        add("oldAnimations", "Usa animações antigas de bloquear, comer, dano e corações, mantendo o controle normal dos ataques.", "§7Custo baixo: ajustes no desenho existente.");
        add("toggleSprint", "Um toque no atalho de correr mantém a corrida; outro toque desativa.", "§7Custo mínimo: estado das teclas.");
        add("sneakAnimation", "Suaviza a mudança de altura da câmera ao agachar e levantar.", "§7Custo baixo: interpolação por frame.");
        add("zoom", "Aproxima a visão com o atalho de zoom. O fator e a sensibilidade ficam nesta seção.", "§7Custo baixo: ajuste do campo de visão.");
        add("perspective", "Permite olhar ao redor com o atalho de perspectiva sem alterar a direção do jogador.", "§7Custo baixo: controle local da câmera.");
        add("fullBright", "Mantém a iluminação visual do mundo no máximo. Não muda a iluminação do servidor.", "§6Impacto variável: muda o caminho de iluminação e pode dispensar atualizações do lightmap.");
        add("timeChanger", "Escolhe o horário mostrado no seu mundo, sem mudar o horário do servidor.", "§7Custo mínimo: substitui leituras do horário visual.");
        add("tntTimer", "Mostra contagens de explosão de até oito TNTs próximas do jogador.", "§eCusto de CPU: procura TNTs entre as entidades a cada tick; cresce com a cena.");
        add("pingDisplay", "Exibe no HUD o ping informado pelo servidor para o seu jogador.", "§7Custo baixo: texto adicional; não faz consultas extras ao servidor.");
        add("ambientParticles", "Mostra partículas e efeitos visuais de ambiente próximos do jogador.", "§eCusto de CPU/GPU: depende dos blocos próximos e da quantidade de efeitos.");
        add("scrollLock", "Impede trocar o slot da hotbar pela roda. Menus e ajuste do zoom continuam rolando.", "§7Custo mínimo: filtro dos eventos da roda.");
        add("backgroundFps", "Limita os frames quando a janela perde o foco. Respeita um limite nativo menor.", "§aEconomia de CPU/GPU: valores menores reduzem o trabalho em segundo plano.");
        add("menuFps", "Limita os frames sem mundo carregado. Zero usa a frequência do monitor, ou 60 quando indisponível.", "§eMais FPS aumentam a carga no menu. Não controla os FPS durante uma partida.");
        add("particleLimit", "Limita a quantidade de partículas nas listas do renderizador.", "§eMais partículas podem consumir mais CPU/GPU. Um limite menor corta efeitos excedentes.");
        add("zoomFactor", "Define a aproximação do zoom: 1x não aproxima; valores maiores estreitam a visão.", "§7Custo baixo: altera a câmera, sem aumentar a resolução da imagem.");
        add("timeOfDay", "Escolhe a hora exibida quando Horário visual está ligado.", "§7Custo mínimo: não muda ticks ou regras do servidor.");
        add("entityCulling", "Tenta deixar de desenhar objetos totalmente cobertos por blocos opacos. Jogadores nunca são ocultados por esta opção.", "§6Troca CPU por menos desenho: consultas limitadas por frame; o ganho depende dos objetos ocultos.");
        add("mergeTerrain", "Une intervalos contíguos da geometria do terreno, preservando a ordem das transparências.", "§6Impacto variável: percorre buffers na CPU para reduzir intervalos enviados à GPU.");
        add("loadingFps", "Limita os frames na tela de download de terreno, quando essa tela é exibida.", "§aEconomia durante essa tela: não reduz os ticks nem o tráfego de carregamento.");
        add("chatSettings", "Abre aparência, histórico, filtros, menções e envio de mensagens do chat.", "§7O custo depende das opções escolhidas; cada uma tem sua própria dica.");
        add("scoreboardSettings", "Abre posição, escala, fundo, números e atalho do placar lateral.", "§7Custo baixo: usa o placar já enviado pelo servidor.");
        add("slotSettings", "Escolhe quais slots da hotbar não podem ser selecionados. O inventário continua disponível.", "§7Custo mínimo: verifica a seleção; mantém pelo menos um slot livre.");
        add("mouseBindFix", "Permite usar botões extras do mouse nos atalhos de troca do inventário.", "§7Custo mínimo: tratamento de cliques do inventário.");
        add("inputFix", "Aceita eventos de caracteres que alguns layouts enviam sem um código de tecla comum.", "§7Custo mínimo: apenas ao digitar em campos de texto.");
        add("rawMouseInput", "Lê o movimento relativo por JInput. Usa a entrada normal se o dispositivo não estiver disponível.", "§6Custo de CPU: uma thread consulta o mouse com espera de 1 ms enquanto você joga.");
        add("stringWidthCaching", "Reutiliza a largura calculada dos textos. Atualiza os resultados quando a fonte muda.", "§aPode economizar CPU em textos repetidos; usa um cache limitado em memória.");
        add("optimizedPlayerUpdating", "Reutiliza cálculos de velocidade quando os valores dos jogadores remotos não mudam. Mantém as animações.", "§6Impacto variável: usa um cache por jogador; movimentos diferentes exigem novos cálculos.");
        add("asyncScreenshot", "Grava o PNG em segundo plano depois de ler os pixels na thread gráfica.", "§eCusto pontual: captura pixels, usa memória e grava em disco. Pode haver pausa na leitura.");
        add("fastLanguageSwitch", "Restringe a recarga ao necessário para trocar de idioma.", "§aPode reduzir trabalho na troca de idioma; sem custo contínuo na partida.");
        add("fastWorldLoading", "Evita a coleta de lixo explícita e telas intermediárias na troca de mundo. O mundo ainda precisa chegar do servidor.", "§6Afeta transições: reduz trabalho explícito; o coletor automático continua ativo.");
        add("lazyChunkLoading", "Adiciona uma espera antes de o worker buscar outra tarefa de chunk. OFF remove essa espera.", "§6Mais atraso pode aliviar rajadas de CPU, mas faz o terreno demorar mais para atualizar.");
        add("hudCaching", "Reutiliza a imagem do HUD quando há frames suficientes para compensar. Fast Render e shaders impedem esse caminho.", "§6Troca memória e composição na GPU por menos desenho da interface. Pode usar desenho direto.");
        add("hudCacheFps", "Define a frequência máxima de renovação da imagem do HUD quando o cache está disponível.", "§eValores maiores refazem mais o HUD; valores menores podem atrasar atualizações visuais.");
        add("memoryCleanup", "Ao desconectar, tenta liberar texturas estáticas recarregáveis sem uso recente. Não remove skins ou texturas dinâmicas.", "§eCusto na desconexão: verifica texturas e pode recarregar recursos; sem varredura contínua na partida.");
        add("alternateDns", "Usa a consulta DNS alternativa para localizar o destino SRV de um servidor.", "§7Custo na conexão: consulta de rede com cache; não melhora o ping durante a partida.");
        add("fallbackDns", "Se a consulta alternativa falhar, tenta a resolução DNS padrão antes de manter o endereço direto.", "§ePode aumentar a espera para conectar quando a primeira consulta falha.");
        add("optimizedText", "Reutiliza o desenho de textos repetidos. A primeira ocorrência usa desenho normal; textos ofuscados continuam nativos.", "§6Impacto variável: reduz desenho repetido, mas usa memória gráfica e consultas de estado.");
        add("batchModels", "Agrupa faces de cubos ao preparar os modelos. Preserva o caminho nativo para shaders e sprites especiais.", "§aPode reduzir submissões de geometria. Alterar a opção recompila modelos no próximo uso.");
        add("modelBackFaces", "Não desenha o verso das faces dos modelos. Pode deixar superfícies invisíveis quando a câmera entra no modelo.", "§aPode reduzir trabalho da GPU; muda a visão de dentro dos modelos.");
        add("optimizedItems", "Gera contornos mesclados para modelos de itens, preservando buracos, transparência e frames animados.", "§aPode reduzir geometria. Alterar a opção recarrega recursos e pode causar uma pausa.");
        add("staticParticleColor", "Usa iluminação fixa no renderizador base de partículas, em vez de consultar a luz do mundo para cada uma.", "§aReduz consultas de luz na CPU; a iluminação visual das partículas fica constante.");
        add("smallPackIcons", "Reduz ícones grandes dos pacotes para até 64x64. Não altera as texturas usadas no mundo.", "§aPode economizar memória dos ícones; redimensiona somente no carregamento.");
        add("lowAnimationTick", "Reduz a amostragem visual de ambiente de 1000 para 100 tentativas por chamada.", "§aReduz trabalho de ambiente na CPU; sem efeito com Partículas ambiente desligado.");
        add("hideEnchantmentBooks", "Oculta o livro animado das mesas de encantamento. A mesa continua funcionando.", "§aEvita o desenho desses livros quando estão na cena.");
        add("singleGroundItem", "Desenha uma cópia por pilha de itens no chão. A quantidade real e a coleta continuam iguais.", "§aPode reduzir desenho de itens, principalmente com muitas pilhas na cena.");
        add("staticGroundItems", "Retira o balanço vertical dos itens no chão. Mantém quantidade, rotação, física e coleta.", "§7Custo baixo: altera a apresentação, sem eliminar o desenho do item.");
        add("zoomScroll", "Permite ajustar a aproximação com a roda enquanto usa o zoom.", "§7Custo mínimo: eventos da roda durante o zoom.");
        add("smoothZoom", "Suaviza a entrada e a saída do zoom em vez de mudar a aproximação de uma vez.", "§7Custo baixo: interpolação por frame durante a transição.");
        add("smoothScrollZoom", "Suaviza as mudanças de aproximação feitas pela roda do mouse.", "§7Custo baixo: interpolação por frame durante a transição.");
        add("toggleZoom", "Um toque ativa o zoom e outro desativa. Desligado, é preciso manter o atalho pressionado.", "§7Custo mínimo: estado do atalho.");
        add("directZoomCamera", "Usa o mouse direto no zoom, sem a suavização cinematográfica adicionada pelo zoom.", "§7Custo baixo: muda a sensação do mouse; não altera o polling do dispositivo.");
        add("dynamicZoomSensitivity", "Adapta a sensibilidade à aproximação atual, além do multiplicador configurado para o zoom.", "§7Custo mínimo: ajuste do movimento da câmera.");
        add("renderHandZoom", "Aplica a aproximação também ao campo de visão da mão durante o zoom.", "§7Custo baixo: muda o campo de visão da mão.");
        add("smartFullbright", "Suspende o brilho máximo do Dolly enquanto shaders estão ativos, preservando o caminho de iluminação deles.", "§7Custo mínimo: verificação de compatibilidade com shaders.");
        add("tactileSettings", "Configura movimento e leitura de teclas por servidor. Hypixel preserva física vanilla; EXACT e STEPPED ficam disponíveis nos outros perfis.", "§eA simulação entre ticks aumenta o uso de CPU; a compatibilidade depende do servidor.");
        add("parallaxFix", "Ajusta o deslocamento próximo da câmera para reduzir diferenças de perspectiva. Não muda a seleção de blocos.", "§7Custo mínimo: ajuste da câmera.");
        add("betterCamera", "Permite à câmera de terceira pessoa atravessar vidro e grades. Não altera colisões do jogador ou a mira.", "§eCusto de CPU na consulta de obstáculos da câmera em terceira pessoa.");
        add("customSkyFix", "Ajusta a geometria e o caminho de desenho do céu para corrigir problemas do céu personalizado no OptiFine.", "§6Impacto de GPU depende do pack e do céu; alternar a opção recarrega o renderer.");
        add("zoomSensitivity", "Multiplica a sensibilidade durante o zoom. Valores menores deixam a mira mais lenta.", "§7Custo mínimo: multiplicação do movimento do mouse.");
        add("zoomEasing", "Escolhe a curva de aceleração e desaceleração das transições suaves do zoom.", "§7Custo baixo: cálculo durante a transição; não muda o tempo do servidor.");
        add("fireHeight", "Ajusta a altura do fogo na tela. Zero oculta essa sobreposição.", "§7Custo baixo; zero evita o desenho dessa camada, sem remover o fogo do mundo.");
        add("fireOpacity", "Ajusta a transparência do fogo na tela. Zero oculta essa sobreposição.", "§7Custo baixo; transparência parcial continua exigindo desenho.");
        add("hideFireResistance", "Oculta o fogo da tela com resistência ativa e mostra um aviso visual ao se aproximar do fim do efeito.", "§7Custo baixo: consulta o efeito e evita o desenho enquanto o fogo está oculto.");
        add("removeWaterOverlay", "Remove a camada visual da água sobre a tela. Não remove a água ou a névoa subaquática.", "§aEvita o desenho dessa sobreposição quando submerso.");
        add("waterFogDensity", "Ajusta a densidade da névoa debaixo d'água. Não muda a distância de renderização.", "§7Custo baixo: parâmetro da névoa; não reduz a geometria carregada.");
        add("cleanView", "Oculta as partículas de poção emitidas pelo seu jogador, mantendo os efeitos e suas durações.", "§aEvita criar e desenhar essas partículas locais.");
        add("hideBreakingParticles", "Oculta partículas ao atingir ou quebrar blocos. A interação com os blocos continua normal.", "§aEvita criar e desenhar esses fragmentos.");
        add("hideLightning", "Oculta o desenho dos raios. Os eventos e efeitos do servidor continuam existindo.", "§aEvita a geometria dos raios quando aparecem.");
        add("cleanProjectiles", "Oculta alguns projéteis nos seus dois primeiros ticks no cliente, inclusive os de outros jogadores, por no máximo 100 ms desde o primeiro desenho.", "§7Custo mínimo: verifica a idade; pode atrasar brevemente a percepção desses projéteis.");
        add("horseOpacity", "Ajusta a transparência do modelo do cavalo que você está montando. Outros cavalos continuam normais.", "§eTransparência parcial acrescenta mistura e consultas de estado; 100% usa o desenho normal.");
        add("soundSettings", "Abre busca e volumes individuais dos sons. Os ajustes multiplicam o volume das categorias do jogo.", "§7O custo depende da reprodução; aumentar volume não cria mais fontes de som.");
        add("separateResourceLoading", "F3+S recarrega sons; F3+T recarrega recursos visuais preservando o áudio.", "§aEvita recarregar áudio desnecessariamente. As recargas ainda podem causar pausas.");
        add("screenshots", "Abre as capturas da sessão para ver prévias, copiar, favoritar, abrir a pasta ou excluir com confirmação.", "§ePrévias usam memória e texturas; ações de arquivo têm custo pontual de disco.");
        add("lanButton", "Escolhe se o botão LAN original aparece, fica oculto ou abre a lista de servidores.", "§7Sem custo contínuo: altera apenas a navegação do menu.");
        add("reconnectMenu", "Mostra a opção de reconectar nos menus suportados quando há um servidor disponível.", "§7Sem custo contínuo: atua ao navegar ou reconectar.");
        add("confirmQuit", "Pede confirmação antes de sair do jogo pelo menu.", "§7Sem custo contínuo: abre uma tela de confirmação.");
        add("screenshotPreview", "Mostra uma miniatura local após salvar uma captura.", "§eCusto pontual de memória e GPU para preparar e desenhar a miniatura.");
        add("screenshotAutoCopy", "Tenta copiar a imagem para a área de transferência depois de salvar a captura.", "§eCusto pontual: transfere a imagem ao sistema; depende do suporte ao clipboard.");
        add("screenshotCompact", "Usa um aviso compacto após salvar a captura, mantendo os controles de gerenciamento.", "§7Custo baixo: muda a apresentação do aviso.");
        add("simpleDebugFps", "Simplifica o contador de FPS no F3, removendo a parte extra do contador do OptiFine.", "§7Custo baixo: formata o texto de depuração.");
        add("vanillaMetrics", "Usa o gráfico de tempos do Alt+F3 no lugar do gráfico do OptiFine.", "§eExibir o gráfico acrescenta desenho de barras e textos; o histórico é limitado.");
        add("voidBoxFix", "Corrige a caixa preta do céu no vazio, preservando o caminho de shaders.", "§7Custo baixo: altera uma condição do desenho do céu.");
        add("nauseaFix", "Limpa a distorção residual quando o efeito de náusea é removido.", "§7Custo mínimo: atua na remoção do efeito.");
        add("cleanOldLogs", "Limpa logs datados antigos uma vez ao iniciar, respeitando o período de retenção.", "§eCusto pontual de disco em segundo plano; não faz limpeza contínua durante a partida.");
        add("logRetentionDays", "Define por quantos dias guardar os logs datados quando a limpeza está ligada. O log atual é preservado.", "§7Sem custo por frame: muda apenas a seleção de arquivos na limpeza inicial.");

        add("chatGroup", "Junta mensagens repetidas e mostra um contador, respeitando o período de agrupamento.", "§6Impacto variável: procura repetições recentes e reduz as linhas duplicadas.");
        add("chatKeepHistory", "Mantém as mensagens desta sessão ao desconectar ou trocar de servidor. Encerrar o jogo limpa esse histórico.", "§eUsa memória até o limite do histórico; mensagens antigas continuam disponíveis na busca.");
        add("chatMentions", "Destaca mensagens que citam seu nome ou as palavras de destaque configuradas.", "§eCusto de CPU ao preparar mensagens; depende do texto e das palavras configuradas.");
        add("chatMentionSound", "Toca um aviso ao receber uma menção reconhecida pelo chat.", "§7Custo pontual: reproduz um som por menção reconhecida.");
        add("chatFilter", "Oculta mensagens que contenham alguma palavra da lista de palavras ocultadas.", "§eCusto de CPU ao filtrar o histórico; listas maiores exigem mais verificações.");
        add("chatHiddenWords", "Edite palavras ou frases para ocultar. Separe entradas com ponto e vírgula; a busca ignora maiúsculas.", "§eListas extensas aumentam o trabalho do filtro quando ele está ligado.");
        add("chatHideNotices", "Oculta mensagens que correspondem à lista de frases de avisos.", "§eCusto de CPU ao filtrar; depende da quantidade de frases e mensagens.");
        add("chatNoticeWords", "Edite frases de avisos para ocultar. Separe entradas com ponto e vírgula.", "§eMais frases aumentam as verificações quando Ocultar avisos está ligado.");
        add("chatMentionWords", "Edite palavras ou frases que devem contar como menção, além do seu nome.", "§eMais entradas aumentam o trabalho de reconhecimento das menções.");
        add("chatHistory", "Define o número máximo de mensagens mantidas na sessão. Reduzir o limite descarta as mais antigas.", "§eMais histórico usa memória e aumenta o trabalho de busca e preparação.");
        add("chatWidth", "Define a largura disponível para as linhas do chat, limitada pelo tamanho da tela.", "§eMudar a largura refaz a quebra de linhas do histórico; o custo é pontual.");
        add("chatHeight", "Define a altura visível do chat. O histórico pode ser maior e continua acessível pela rolagem.", "§eMais linhas visíveis exigem mais desenho de texto e fundo.");
        add("chatScale", "Ajusta o tamanho visual do chat e a largura usada para quebrar suas mensagens.", "§eAlterar a escala refaz a quebra de linhas; o custo de desenho depende da área e das linhas visíveis.");
        add("chatSpacing", "Acrescenta espaço vertical entre as linhas do chat.", "§7Custo baixo: muda a disposição e a quantidade de linhas que cabem na área.");
        add("chatOpacity", "Define a opacidade máxima dos textos do chat, além do desaparecimento por tempo.", "§7Custo baixo: textos semitransparentes continuam sendo desenhados.");
        add("chatBackground", "Mostra um fundo atrás das linhas visíveis do chat.", "§eAcrescenta retângulos com transparência atrás das mensagens.");
        add("chatBackgroundOpacity", "Ajusta a opacidade do fundo quando Fundo está ligado.", "§7Custo baixo: reduzir a opacidade não elimina necessariamente o desenho do fundo.");
        add("chatDuration", "Define por quantos segundos uma mensagem fica visível com o chat fechado.", "§eMais tempo pode manter mais mensagens sendo desenhadas.");
        add("chatFade", "Define a duração do desaparecimento gradual das mensagens. Zero remove a transição.", "§7Custo baixo: calcula o alpha das linhas visíveis.");
        add("chatGroupConsecutive", "Agrupa apenas repetições consecutivas. Desligado, também procura repetições recentes entre outras mensagens.", "§aLigado restringe a procura de repetições e pode reduzir o trabalho de CPU.");
        add("chatGroupSeconds", "Define por quanto tempo uma mensagem anterior pode receber uma repetição no mesmo grupo.", "§7Custo limitado: a procura continua restrita às mensagens recentes.");
        add("chatSendQueue", "Espaça o envio de suas mensagens. Ctrl+Backspace cancela a fila; desconectar também a cancela.", "§7Custo baixo: uma fila limitada, consultada por tick. Não contorna regras do servidor.");
        add("chatSendInterval", "Define a espera entre mensagens quando a fila de envio está ligada.", "§7Custo mínimo: muda o intervalo, sem criar novas mensagens.");
        add("chatInspectActions", "Mostra a ação associada a mensagens clicáveis, mantendo o clique original.", "§7Custo baixo: examina o componente sob o mouse com o chat aberto.");
        add("chatClickedHistory", "Guarda comandos clicados no histórico de envio para facilitar sua reutilização.", "§7Custo pontual: apenas ao clicar em uma ação de comando.");
        add("chatImagePreview", "Mostra prévias de links diretos de imagens ao passar o mouse. O download ocorre fora da thread gráfica.", "§6Usa rede, memória e GPU para imagens; mantém cache e fila limitados.");
        add("tabPing", "Substitui as barras de conexão no TAB por um número de ping.", "§eAcrescenta desenho de texto por jogador enquanto o TAB está aberto.");
        add("scoreboardVisible", "Mostra o objetivo lateral enviado pelo servidor.", "§eDesenha os textos e o fundo do placar; a preparação é reutilizada quando possível.");
        add("scoreboardNumbers", "Mostra os números laterais das pontuações no placar.", "§eAcrescenta até um texto numérico por linha visível.");
        add("scoreboardX", "Move o placar horizontalmente dentro da área disponível da tela.", "§7Custo mínimo: muda a posição do desenho.");
        add("scoreboardY", "Move o placar verticalmente dentro da área disponível da tela.", "§7Custo mínimo: muda a posição do desenho.");
        add("scoreboardScale", "Ajusta o tamanho do placar, respeitando o espaço da tela.", "§7Custo baixo: transforma o desenho; tamanho maior ocupa mais pixels.");
        add("scoreboardBackground", "Mostra um fundo atrás do placar lateral.", "§eAcrescenta um retângulo com transparência.");
        add("scoreboardOpacity", "Define a opacidade do fundo do placar.", "§7Custo baixo: reduzir a opacidade não elimina o retângulo.");
        add("scoreboardKey", "Define um atalho para mostrar ou ocultar o placar. Delete remove; Esc cancela a alteração.", "§7Custo mínimo: tratamento do atalho.");
        add("slotProfile", "Liga o perfil que impede selecionar os slots marcados. Não impede organizar o inventário.", "§7Custo mínimo: verificação ao trocar de slot.");
        add("blockedSlots", "Marca este slot como livre ou bloqueado para seleção. Pelo menos um slot deve permanecer livre.", "§7Custo mínimo: consulta uma máscara de nove slots.");
        add("slotProfileKey", "Define o atalho para ligar ou desligar o perfil sem perder os slots marcados. Delete remove o atalho.", "§7Custo mínimo: tratamento do atalho.");
        add("soundVolume", "Multiplica o volume deste evento de som: 100% é o original e 0% silencia. As categorias do jogo ainda se aplicam.", "§7Custo baixo: ajusta o volume; 200% não duplica a quantidade de sons.");
        add("reloadSounds", "Recarrega o mecanismo e os recursos de áudio.", "§cCusto pontual elevado: pode pausar o jogo durante a recarga.");
        add("reloadTextures", "Recarrega os recursos visuais, preservando o áudio quando Separar recargas está ligado.", "§cCusto pontual elevado: leitura de recursos, preparação e envio de texturas à GPU.");
        add("resetVolumes", "Restaura os volumes individuais dos eventos de som para 100%.", "§7Custo pontual: salva as preferências, sem recarregar todos os recursos.");
    }

    private String key, title;
    private avn font;
    private int width, measuredWidth;
    private long since;
    private List<String> lines;
    public void reset() { key = title = null; font = null; lines = null; }
    public List<String> lines(String nextKey, String nextTitle, avn nextFont, int screenWidth, long now) {
        if (nextKey == null || nextFont == null || tip(nextKey) == null) { reset(); return null; }
        int nextWidth = Math.max(40, Math.min(280, screenWidth - 32));
        if (!nextKey.equals(key) || !nextTitle.equals(title) || nextFont != font || width != nextWidth) {
            key = nextKey; title = nextTitle; font = nextFont; width = nextWidth; since = now; lines = null;
        }
        if (now - since < 300_000_000L) return null;
        if (lines == null) {
            List<String> prepared = new ArrayList<String>();
            wrap(prepared, title, "§f");
            Tip tip = tip(key);
            wrap(prepared, tip.description, "§7");
            wrap(prepared, tip.impact.substring(2), tip.impact.substring(0, 2));
            measuredWidth = 0;
            for (String line : prepared) measuredWidth = Math.max(measuredWidth, font.a(line));
            lines = Collections.unmodifiableList(prepared);
        }
        return lines;
    }
    private void wrap(List<String> output, String text, String color) {
        String line = "";
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (font.a(next) > width && !line.isEmpty()) { output.add(color + line); line = ""; }
            while (font.a(word) > width) {
                String part = font.a(word, width);
                if (part.isEmpty()) break;
                output.add(color + part); word = word.substring(part.length());
            }
            line = line.isEmpty() ? word : line + " " + word;
        }
        if (!line.isEmpty()) output.add(color + line);
    }
    public int anchorX(int mouseX, int screenWidth) {
        return Math.max(0, Math.min(mouseX, screenWidth - measuredWidth - 20));
    }
    public static avs hovered(List<avs> buttons, int x, int y) {
        for (avs button : buttons) if (button.m && x >= button.h && x < button.h + button.b()
                && y >= button.i && y < button.i + button.g) return button;
        return null;
    }
}
