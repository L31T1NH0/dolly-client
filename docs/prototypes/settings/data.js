window.DOLLY_DATA = {
  "settings": {
    "clickFix": {
      "name": "Corrigir cliques",
      "value": true,
      "description": "Evita a penalidade de cliques após atacar sem acertar um alvo. Não cria cliques automáticos.",
      "impact": "Custo mínimo: verificações na entrada.",
      "color": "neutral"
    },
    "mouseFix": {
      "name": "Corrigir mouse",
      "value": true,
      "description": "Corrige a direção usada para mirar com a rotação do jogador e sua interpolação.",
      "impact": "Custo mínimo: cálculo da direção da mira.",
      "color": "neutral"
    },
    "keyFix": {
      "name": "Recuperar teclas",
      "value": true,
      "description": "Recupera teclas de movimento presas ao perder foco ou trocar de tela.",
      "impact": "Custo mínimo: consulta poucas teclas por tick.",
      "color": "neutral"
    },
    "linuxShiftFix": {
      "name": "Shift no Linux",
      "value": true,
      "description": "Corrige combinações de Shift e teclas numéricas em layouts afetados no Linux.",
      "impact": "Custo mínimo: apenas em eventos de teclado.",
      "color": "neutral"
    },
    "oldAnimations": {
      "name": "Old Animations",
      "value": true,
      "description": "Usa animações antigas de bloquear, comer, dano e corações, mantendo o controle normal dos ataques.",
      "impact": "Custo baixo: ajustes no desenho existente.",
      "color": "neutral"
    },
    "toggleSprint": {
      "name": "Toggle Sprint",
      "value": true,
      "description": "Um toque no atalho de correr mantém a corrida; outro toque desativa.",
      "impact": "Custo mínimo: estado das teclas.",
      "color": "neutral"
    },
    "sneakAnimation": {
      "name": "Agachar suave",
      "value": true,
      "description": "Suaviza a mudança de altura da câmera ao agachar e levantar.",
      "impact": "Custo baixo: interpolação por frame.",
      "color": "neutral"
    },
    "zoom": {
      "name": "Zoom",
      "value": true,
      "description": "Aproxima a visão com o atalho de zoom. O fator e a sensibilidade ficam nesta seção.",
      "impact": "Custo baixo: ajuste do campo de visão.",
      "color": "neutral"
    },
    "perspective": {
      "name": "Perspective",
      "value": true,
      "description": "Permite olhar ao redor com o atalho de perspectiva sem alterar a direção do jogador.",
      "impact": "Custo baixo: controle local da câmera.",
      "color": "neutral"
    },
    "fullBright": {
      "name": "Full Bright",
      "value": false,
      "description": "Mantém a iluminação visual do mundo no máximo. Não muda a iluminação do servidor.",
      "impact": "Impacto variável: muda o caminho de iluminação e pode dispensar atualizações do lightmap.",
      "color": "variable"
    },
    "timeChanger": {
      "name": "Time Changer",
      "value": false,
      "description": "Escolhe o horário mostrado no seu mundo, sem mudar o horário do servidor.",
      "impact": "Custo mínimo: substitui leituras do horário visual.",
      "color": "neutral"
    },
    "tntTimer": {
      "name": "TNT Timer",
      "value": false,
      "description": "Mostra contagens de explosão de até oito TNTs próximas do jogador.",
      "impact": "Custo de CPU: procura TNTs entre as entidades a cada tick; cresce com a cena.",
      "color": "cost"
    },
    "pingDisplay": {
      "name": "Exibir ping",
      "value": false,
      "description": "Exibe no HUD o ping informado pelo servidor para o seu jogador.",
      "impact": "Custo baixo: texto adicional; não faz consultas extras ao servidor.",
      "color": "neutral"
    },
    "ambientParticles": {
      "name": "Partículas de ambiente",
      "value": false,
      "description": "Mostra partículas e efeitos visuais de ambiente próximos do jogador.",
      "impact": "Custo de CPU/GPU: depende dos blocos próximos e da quantidade de efeitos.",
      "color": "cost"
    },
    "scrollLock": {
      "name": "Bloquear scroll",
      "value": true,
      "description": "Impede trocar o slot da hotbar pela roda. Menus e ajuste do zoom continuam rolando.",
      "impact": "Custo mínimo: filtro dos eventos da roda.",
      "color": "neutral"
    },
    "backgroundFps": {
      "name": "FPS sem foco",
      "value": 30,
      "description": "Limita os frames quando a janela perde o foco. Respeita um limite nativo menor.",
      "impact": "Economia de CPU/GPU: valores menores reduzem o trabalho em segundo plano.",
      "color": "saving",
      "range": [
        5,
        240,
        1
      ]
    },
    "menuFps": {
      "name": "FPS no menu",
      "value": 0,
      "description": "Limita os frames sem mundo carregado. Zero usa a frequência do monitor, ou 60 quando indisponível.",
      "impact": "Mais FPS aumentam a carga no menu. Não controla os FPS durante uma partida.",
      "color": "cost",
      "range": [
        0,
        1000,
        1
      ]
    },
    "particleLimit": {
      "name": "Limite de partículas",
      "value": 1000,
      "description": "Limita a quantidade de partículas nas listas do renderizador.",
      "impact": "Mais partículas podem consumir mais CPU/GPU. Um limite menor corta efeitos excedentes.",
      "color": "cost",
      "range": [
        1,
        10000,
        1
      ]
    },
    "zoomFactor": {
      "name": "Fator do zoom",
      "value": 4.0,
      "description": "Define a aproximação do zoom: 1x não aproxima; valores maiores estreitam a visão.",
      "impact": "Custo baixo: altera a câmera, sem aumentar a resolução da imagem.",
      "color": "neutral",
      "range": [
        1,
        600,
        1
      ]
    },
    "timeOfDay": {
      "name": "Horário visual",
      "value": 6000,
      "description": "Escolhe a hora exibida quando Horário visual está ligado.",
      "impact": "Custo mínimo: não muda ticks ou regras do servidor.",
      "color": "neutral",
      "range": [
        0,
        23999,
        1
      ]
    },
    "entityCulling": {
      "name": "Ocultar objetos cobertos",
      "value": true,
      "description": "Tenta deixar de desenhar objetos totalmente cobertos por blocos opacos. Jogadores nunca são ocultados por esta opção.",
      "impact": "Troca CPU por menos desenho: consultas limitadas por frame; o ganho depende dos objetos ocultos.",
      "color": "variable"
    },
    "mergeTerrain": {
      "name": "Mesclar terreno",
      "value": true,
      "description": "Une intervalos contíguos da geometria do terreno, preservando a ordem das transparências.",
      "impact": "Impacto variável: percorre buffers na CPU para reduzir intervalos enviados à GPU.",
      "color": "variable"
    },
    "loadingFps": {
      "name": "FPS ao carregar",
      "value": 30,
      "description": "Limita os frames na tela de download de terreno, quando essa tela é exibida.",
      "impact": "Economia durante essa tela: não reduz os ticks nem o tráfego de carregamento.",
      "color": "saving",
      "range": [
        5,
        240,
        1
      ]
    },
    "mouseBindFix": {
      "name": "Mouse no inventário",
      "value": true,
      "description": "Permite usar botões extras do mouse nos atalhos de troca do inventário.",
      "impact": "Custo mínimo: tratamento de cliques do inventário.",
      "color": "neutral"
    },
    "inputFix": {
      "name": "Corrigir texto",
      "value": true,
      "description": "Aceita eventos de caracteres que alguns layouts enviam sem um código de tecla comum.",
      "impact": "Custo mínimo: apenas ao digitar em campos de texto.",
      "color": "neutral"
    },
    "rawMouseInput": {
      "name": "Raw Mouse Input",
      "value": false,
      "description": "Lê o movimento relativo por JInput. Usa a entrada normal se o dispositivo não estiver disponível.",
      "impact": "Custo de CPU: uma thread consulta o mouse com espera de 1 ms enquanto você joga.",
      "color": "variable"
    },
    "stringWidthCaching": {
      "name": "Medidas de texto",
      "value": true,
      "description": "Reutiliza a largura calculada dos textos. Atualiza os resultados quando a fonte muda.",
      "impact": "Pode economizar CPU em textos repetidos; usa um cache limitado em memória.",
      "color": "saving"
    },
    "optimizedPlayerUpdating": {
      "name": "Otimizar jogadores",
      "value": true,
      "description": "Reutiliza cálculos de velocidade quando os valores dos jogadores remotos não mudam. Mantém as animações.",
      "impact": "Impacto variável: usa um cache por jogador; movimentos diferentes exigem novos cálculos.",
      "color": "variable"
    },
    "asyncScreenshot": {
      "name": "Screenshot assíncrona",
      "value": true,
      "description": "Grava o PNG em segundo plano depois de ler os pixels na thread gráfica.",
      "impact": "Custo pontual: captura pixels, usa memória e grava em disco. Pode haver pausa na leitura.",
      "color": "cost"
    },
    "fastLanguageSwitch": {
      "name": "Idioma rápido",
      "value": false,
      "description": "Restringe a recarga ao necessário para trocar de idioma.",
      "impact": "Pode reduzir trabalho na troca de idioma; sem custo contínuo na partida.",
      "color": "saving"
    },
    "fastWorldLoading": {
      "name": "Carregar mundo rápido",
      "value": true,
      "description": "Evita a coleta de lixo explícita e telas intermediárias na troca de mundo. O mundo ainda precisa chegar do servidor.",
      "impact": "Afeta transições: reduz trabalho explícito; o coletor automático continua ativo.",
      "color": "variable"
    },
    "lazyChunkLoading": {
      "name": "Atraso de chunks",
      "value": 1,
      "description": "Adiciona uma espera antes de o worker buscar outra tarefa de chunk. OFF remove essa espera.",
      "impact": "Mais atraso pode aliviar rajadas de CPU, mas faz o terreno demorar mais para atualizar.",
      "color": "variable",
      "choices": [
        "Sem espera",
        "15 ms",
        "50 ms",
        "110 ms",
        "150 ms",
        "200 ms"
      ]
    },
    "hudCaching": {
      "name": "Cache do HUD",
      "value": true,
      "description": "Reutiliza a imagem do HUD quando há frames suficientes para compensar. Fast Render e shaders impedem esse caminho.",
      "impact": "Troca memória e composição na GPU por menos desenho da interface. Pode usar desenho direto.",
      "color": "variable"
    },
    "hudCacheFps": {
      "name": "Hz do HUD",
      "value": 24,
      "description": "Define a frequência máxima de renovação da imagem do HUD quando o cache está disponível.",
      "impact": "Valores maiores refazem mais o HUD; valores menores podem atrasar atualizações visuais.",
      "color": "cost",
      "range": [
        15,
        240,
        1
      ]
    },
    "memoryCleanup": {
      "name": "Limpar ao desconectar",
      "value": false,
      "description": "Ao desconectar, tenta liberar texturas estáticas recarregáveis sem uso recente. Não remove skins ou texturas dinâmicas.",
      "impact": "Custo na desconexão: verifica texturas e pode recarregar recursos; sem varredura contínua na partida.",
      "color": "cost"
    },
    "itemStitching": {
      "name": "Frestas dos itens",
      "value": 0,
      "description": "Escolhe a correção das frestas nos modelos de itens. Pode exigir reiniciar o jogo para aplicar aos modelos existentes.",
      "impact": "Custo na preparação dos modelos e uso de memória; não reconstrói itens a cada frame.",
      "color": "cost",
      "choices": [
        "Desligado",
        "Outline",
        "Unlerp"
      ]
    },
    "alternateDns": {
      "name": "DNS alternativo",
      "value": true,
      "description": "Usa a consulta DNS alternativa para localizar o destino SRV de um servidor.",
      "impact": "Custo na conexão: consulta de rede com cache; não melhora o ping durante a partida.",
      "color": "neutral"
    },
    "fallbackDns": {
      "name": "DNS de fallback",
      "value": false,
      "description": "Se a consulta alternativa falhar, tenta a resolução DNS padrão antes de manter o endereço direto.",
      "impact": "Pode aumentar a espera para conectar quando a primeira consulta falha.",
      "color": "cost"
    },
    "optimizedText": {
      "name": "Desenho de textos",
      "value": true,
      "description": "Reutiliza o desenho de textos repetidos. A primeira ocorrência usa desenho normal; textos ofuscados continuam nativos.",
      "impact": "Impacto variável: reduz desenho repetido, mas usa memória gráfica e consultas de estado.",
      "color": "variable"
    },
    "batchModels": {
      "name": "Agrupar modelos",
      "value": true,
      "description": "Agrupa faces de cubos ao preparar os modelos. Preserva o caminho nativo para shaders e sprites especiais.",
      "impact": "Pode reduzir submissões de geometria. Alterar a opção recompila modelos no próximo uso.",
      "color": "saving"
    },
    "modelBackFaces": {
      "name": "Só faces externas",
      "value": true,
      "description": "Não desenha o verso das faces dos modelos. Pode deixar superfícies invisíveis quando a câmera entra no modelo.",
      "impact": "Pode reduzir trabalho da GPU; muda a visão de dentro dos modelos.",
      "color": "saving"
    },
    "optimizedItems": {
      "name": "Modelos de itens",
      "value": true,
      "description": "Gera contornos mesclados para modelos de itens, preservando buracos, transparência e frames animados.",
      "impact": "Pode reduzir geometria. Alterar a opção recarrega recursos e pode causar uma pausa.",
      "color": "saving"
    },
    "staticParticleColor": {
      "name": "Luz fixa de partículas",
      "value": true,
      "description": "Usa iluminação fixa no renderizador base de partículas, em vez de consultar a luz do mundo para cada uma.",
      "impact": "Reduz consultas de luz na CPU; a iluminação visual das partículas fica constante.",
      "color": "saving"
    },
    "smallPackIcons": {
      "name": "Ícones leves dos packs",
      "value": true,
      "description": "Reduz ícones grandes dos pacotes para até 64x64. Não altera as texturas usadas no mundo.",
      "impact": "Pode economizar memória dos ícones; redimensiona somente no carregamento.",
      "color": "saving"
    },
    "lowAnimationTick": {
      "name": "Menos animações ambiente",
      "value": true,
      "description": "Reduz a amostragem visual de ambiente de 1000 para 100 tentativas por chamada.",
      "impact": "Reduz trabalho de ambiente na CPU; sem efeito com Partículas ambiente desligado.",
      "color": "saving"
    },
    "hideEnchantmentBooks": {
      "name": "Ocultar livros de encanto",
      "value": true,
      "description": "Oculta o livro animado das mesas de encantamento. A mesa continua funcionando.",
      "impact": "Evita o desenho desses livros quando estão na cena.",
      "color": "saving"
    },
    "singleGroundItem": {
      "name": "Item único por pilha",
      "value": true,
      "description": "Desenha uma cópia por pilha de itens no chão. A quantidade real e a coleta continuam iguais.",
      "impact": "Pode reduzir desenho de itens, principalmente com muitas pilhas na cena.",
      "color": "saving"
    },
    "staticGroundItems": {
      "name": "Itens sem balanço",
      "value": true,
      "description": "Retira o balanço vertical dos itens no chão. Mantém quantidade, rotação, física e coleta.",
      "impact": "Custo baixo: altera a apresentação, sem eliminar o desenho do item.",
      "color": "neutral"
    },
    "zoomScroll": {
      "name": "Zoom pela roda",
      "value": true,
      "description": "Permite ajustar a aproximação com a roda enquanto usa o zoom.",
      "impact": "Custo mínimo: eventos da roda durante o zoom.",
      "color": "neutral"
    },
    "smoothZoom": {
      "name": "Transição do zoom",
      "value": true,
      "description": "Suaviza a entrada e a saída do zoom em vez de mudar a aproximação de uma vez.",
      "impact": "Custo baixo: interpolação por frame durante a transição.",
      "color": "neutral"
    },
    "smoothScrollZoom": {
      "name": "Transição pela roda",
      "value": true,
      "description": "Suaviza as mudanças de aproximação feitas pela roda do mouse.",
      "impact": "Custo baixo: interpolação por frame durante a transição.",
      "color": "neutral"
    },
    "toggleZoom": {
      "name": "Zoom por alternância",
      "value": false,
      "description": "Um toque ativa o zoom e outro desativa. Desligado, é preciso manter o atalho pressionado.",
      "impact": "Custo mínimo: estado do atalho.",
      "color": "neutral"
    },
    "directZoomCamera": {
      "name": "Mouse direto no zoom",
      "value": false,
      "description": "Usa o mouse direto no zoom, sem a suavização cinematográfica adicionada pelo zoom.",
      "impact": "Custo baixo: muda a sensação do mouse; não altera o polling do dispositivo.",
      "color": "neutral"
    },
    "dynamicZoomSensitivity": {
      "name": "Sensibilidade dinâmica",
      "value": false,
      "description": "Adapta a sensibilidade à aproximação atual, além do multiplicador configurado para o zoom.",
      "impact": "Custo mínimo: ajuste do movimento da câmera.",
      "color": "neutral"
    },
    "renderHandZoom": {
      "name": "Mão durante zoom",
      "value": false,
      "description": "Aplica a aproximação também ao campo de visão da mão durante o zoom.",
      "impact": "Custo baixo: muda o campo de visão da mão.",
      "color": "neutral"
    },
    "smartFullbright": {
      "name": "Fullbright inteligente",
      "value": true,
      "description": "Suspende o brilho máximo do Dolly enquanto shaders estão ativos, preservando o caminho de iluminação deles.",
      "impact": "Custo mínimo: verificação de compatibilidade com shaders.",
      "color": "neutral"
    },
    "parallaxFix": {
      "name": "Parallax Fix",
      "value": false,
      "description": "Ajusta o deslocamento próximo da câmera para reduzir diferenças de perspectiva. Não muda a seleção de blocos.",
      "impact": "Custo mínimo: ajuste da câmera.",
      "color": "neutral"
    },
    "betterCamera": {
      "name": "Better Camera",
      "value": true,
      "description": "Permite à câmera de terceira pessoa atravessar vidro e grades. Não altera colisões do jogador ou a mira.",
      "impact": "Custo de CPU na consulta de obstáculos da câmera em terceira pessoa.",
      "color": "cost"
    },
    "customSkyFix": {
      "name": "Corrigir céu OptiFine",
      "value": true,
      "description": "Ajusta a geometria e o caminho de desenho do céu para corrigir problemas do céu personalizado no OptiFine.",
      "impact": "Impacto de GPU depende do pack e do céu; alternar a opção recarrega o renderer.",
      "color": "variable"
    },
    "zoomSensitivity": {
      "name": "Sensibilidade do zoom",
      "value": 1.0,
      "description": "Multiplica a sensibilidade durante o zoom. Valores menores deixam a mira mais lenta.",
      "impact": "Custo mínimo: multiplicação do movimento do mouse.",
      "color": "neutral",
      "range": [
        0.05,
        1,
        0.01
      ]
    },
    "zoomEasing": {
      "name": "Curva do zoom",
      "value": 0,
      "description": "Escolhe a curva de aceleração e desaceleração das transições suaves do zoom.",
      "impact": "Custo baixo: cálculo durante a transição; não muda o tempo do servidor.",
      "color": "neutral",
      "choices": [
        "Quadrática",
        "Circular",
        "Quíntica"
      ]
    },
    "fireHeight": {
      "name": "Altura do fogo",
      "value": 100,
      "description": "Ajusta a altura do fogo na tela. Zero oculta essa sobreposição.",
      "impact": "Custo baixo; zero evita o desenho dessa camada, sem remover o fogo do mundo.",
      "color": "neutral",
      "range": [
        0,
        100,
        1
      ]
    },
    "fireOpacity": {
      "name": "Opacidade do fogo",
      "value": 100,
      "description": "Ajusta a transparência do fogo na tela. Zero oculta essa sobreposição.",
      "impact": "Custo baixo; transparência parcial continua exigindo desenho.",
      "color": "neutral",
      "range": [
        0,
        100,
        1
      ]
    },
    "hideFireResistance": {
      "name": "Ocultar fogo protegido",
      "value": true,
      "description": "Oculta o fogo da tela com resistência ativa e mostra um aviso visual ao se aproximar do fim do efeito.",
      "impact": "Custo baixo: consulta o efeito e evita o desenho enquanto o fogo está oculto.",
      "color": "neutral"
    },
    "removeWaterOverlay": {
      "name": "Remover camada da água",
      "value": true,
      "description": "Remove a camada visual da água sobre a tela. Não remove a água ou a névoa subaquática.",
      "impact": "Evita o desenho dessa sobreposição quando submerso.",
      "color": "saving"
    },
    "waterFogDensity": {
      "name": "Névoa subaquática",
      "value": 100,
      "description": "Ajusta a densidade da névoa debaixo d'água. Não muda a distância de renderização.",
      "impact": "Custo baixo: parâmetro da névoa; não reduz a geometria carregada.",
      "color": "neutral",
      "range": [
        0,
        100,
        1
      ]
    },
    "cleanView": {
      "name": "Ocultar partículas próprias",
      "value": true,
      "description": "Oculta as partículas de poção emitidas pelo seu jogador, mantendo os efeitos e suas durações.",
      "impact": "Evita criar e desenhar essas partículas locais.",
      "color": "saving"
    },
    "hideBreakingParticles": {
      "name": "Ocultar fragmentos",
      "value": true,
      "description": "Oculta partículas ao atingir ou quebrar blocos. A interação com os blocos continua normal.",
      "impact": "Evita criar e desenhar esses fragmentos.",
      "color": "saving"
    },
    "hideLightning": {
      "name": "Ocultar raios",
      "value": true,
      "description": "Oculta o desenho dos raios. Os eventos e efeitos do servidor continuam existindo.",
      "impact": "Evita a geometria dos raios quando aparecem.",
      "color": "saving"
    },
    "cleanProjectiles": {
      "name": "Ocultação de projéteis",
      "value": true,
      "description": "Oculta alguns projéteis nos seus dois primeiros ticks no cliente, inclusive os de outros jogadores.",
      "impact": "Custo mínimo: verifica a idade; pode atrasar brevemente a percepção desses projéteis.",
      "color": "neutral"
    },
    "horseOpacity": {
      "name": "Opacidade do cavalo",
      "value": 100,
      "description": "Ajusta a transparência do modelo do cavalo que você está montando. Outros cavalos continuam normais.",
      "impact": "Transparência parcial acrescenta mistura e consultas de estado; 100% usa o desenho normal.",
      "color": "cost",
      "range": [
        0,
        100,
        1
      ]
    },
    "separateResourceLoading": {
      "name": "Separar recargas",
      "value": true,
      "description": "F3+S recarrega sons; F3+T recarrega recursos visuais preservando o áudio.",
      "impact": "Evita recarregar áudio desnecessariamente. As recargas ainda podem causar pausas.",
      "color": "saving"
    },
    "lanButton": {
      "name": "Botão LAN",
      "value": 2,
      "description": "Escolhe se o botão LAN original aparece, fica oculto ou abre a lista de servidores.",
      "impact": "Sem custo contínuo: altera apenas a navegação do menu.",
      "color": "neutral",
      "choices": [
        "Original",
        "Oculto",
        "Servidores"
      ]
    },
    "reconnectMenu": {
      "name": "Menu de reconexão",
      "value": true,
      "description": "Mostra a opção de reconectar nos menus suportados quando há um servidor disponível.",
      "impact": "Sem custo contínuo: atua ao navegar ou reconectar.",
      "color": "neutral"
    },
    "confirmQuit": {
      "name": "Confirmar saída",
      "value": true,
      "description": "Pede confirmação antes de sair do jogo pelo menu.",
      "impact": "Sem custo contínuo: abre uma tela de confirmação.",
      "color": "neutral"
    },
    "screenshotPreview": {
      "name": "Prévia da captura",
      "value": true,
      "description": "Mostra uma miniatura local após salvar uma captura.",
      "impact": "Custo pontual de memória e GPU para preparar e desenhar a miniatura.",
      "color": "cost"
    },
    "screenshotAutoCopy": {
      "name": "Copiar captura ao salvar",
      "value": false,
      "description": "Tenta copiar a imagem para a área de transferência depois de salvar a captura.",
      "impact": "Custo pontual: transfere a imagem ao sistema; depende do suporte ao clipboard.",
      "color": "cost"
    },
    "screenshotCompact": {
      "name": "Aviso compacto de captura",
      "value": true,
      "description": "Usa um aviso compacto após salvar a captura, mantendo os controles de gerenciamento.",
      "impact": "Custo baixo: muda a apresentação do aviso.",
      "color": "neutral"
    },
    "simpleDebugFps": {
      "name": "FPS simples no F3",
      "value": true,
      "description": "Simplifica o contador de FPS no F3, removendo a parte extra do contador do OptiFine.",
      "impact": "Custo baixo: formata o texto de depuração.",
      "color": "neutral"
    },
    "vanillaMetrics": {
      "name": "Gráfico original Alt+F3",
      "value": true,
      "description": "Usa o gráfico de tempos do Alt+F3 no lugar do gráfico do OptiFine.",
      "impact": "Exibir o gráfico acrescenta desenho de barras e textos; o histórico é limitado.",
      "color": "cost"
    },
    "voidBoxFix": {
      "name": "Corrigir caixa do vazio",
      "value": true,
      "description": "Corrige a caixa preta do céu no vazio, preservando o caminho de shaders.",
      "impact": "Custo baixo: altera uma condição do desenho do céu.",
      "color": "neutral"
    },
    "nauseaFix": {
      "name": "Corrigir fim da náusea",
      "value": true,
      "description": "Limpa a distorção residual quando o efeito de náusea é removido.",
      "impact": "Custo mínimo: atua na remoção do efeito.",
      "color": "neutral"
    },
    "cleanOldLogs": {
      "name": "Limpar logs antigos",
      "value": true,
      "description": "Limpa logs datados antigos uma vez ao iniciar, respeitando o período de retenção.",
      "impact": "Custo pontual de disco em segundo plano; não faz limpeza contínua durante a partida.",
      "color": "cost"
    },
    "logRetentionDays": {
      "name": "Guardar logs (dias)",
      "value": 30,
      "description": "Define por quantos dias guardar os logs datados quando a limpeza está ligada. O log atual é preservado.",
      "impact": "Sem custo por frame: muda apenas a seleção de arquivos na limpeza inicial.",
      "color": "neutral",
      "range": [
        1,
        3650,
        1
      ]
    },
    "chatGroup": {
      "name": "Agrupar repetidas",
      "value": true,
      "description": "Junta mensagens repetidas e mostra um contador, respeitando o período de agrupamento.",
      "impact": "Impacto variável: procura repetições recentes e reduz as linhas duplicadas.",
      "color": "variable"
    },
    "chatKeepHistory": {
      "name": "Manter entre servidores",
      "value": true,
      "description": "Mantém as mensagens desta sessão ao desconectar ou trocar de servidor. Encerrar o jogo limpa esse histórico.",
      "impact": "Usa memória até o limite do histórico; mensagens antigas continuam disponíveis na busca.",
      "color": "cost"
    },
    "chatMentions": {
      "name": "Destacar menções",
      "value": true,
      "description": "Destaca mensagens que citam seu nome ou as palavras de destaque configuradas.",
      "impact": "Custo de CPU ao preparar mensagens; depende do texto e das palavras configuradas.",
      "color": "cost"
    },
    "chatMentionSound": {
      "name": "Som de menção",
      "value": false,
      "description": "Toca um aviso ao receber uma menção reconhecida pelo chat.",
      "impact": "Custo pontual: reproduz um som por menção reconhecida.",
      "color": "neutral"
    },
    "chatFilter": {
      "name": "Filtro de palavras",
      "value": false,
      "description": "Oculta mensagens que contenham alguma palavra da lista de palavras ocultadas.",
      "impact": "Custo de CPU ao filtrar o histórico; listas maiores exigem mais verificações.",
      "color": "cost"
    },
    "chatHiddenWords": {
      "name": "Palavras ocultadas",
      "value": "",
      "description": "Edite palavras ou frases para ocultar. Separe entradas com ponto e vírgula; a busca ignora maiúsculas.",
      "impact": "Listas extensas aumentam o trabalho do filtro quando ele está ligado.",
      "color": "cost"
    },
    "chatHideNotices": {
      "name": "Ocultar avisos",
      "value": false,
      "description": "Oculta mensagens que correspondem à lista de frases de avisos.",
      "impact": "Custo de CPU ao filtrar; depende da quantidade de frases e mensagens.",
      "color": "cost"
    },
    "chatNoticeWords": {
      "name": "Frases de avisos",
      "value": "",
      "description": "Edite frases de avisos para ocultar. Separe entradas com ponto e vírgula.",
      "impact": "Mais frases aumentam as verificações quando Ocultar avisos está ligado.",
      "color": "cost"
    },
    "chatMentionWords": {
      "name": "Palavras destacadas",
      "value": "",
      "description": "Edite palavras ou frases que devem contar como menção, além do seu nome.",
      "impact": "Mais entradas aumentam o trabalho de reconhecimento das menções.",
      "color": "cost"
    },
    "chatHistory": {
      "name": "Histórico",
      "value": 1000,
      "description": "Define o número máximo de mensagens mantidas na sessão. Reduzir o limite descarta as mais antigas.",
      "impact": "Mais histórico usa memória e aumenta o trabalho de busca e preparação.",
      "color": "cost",
      "range": [
        100.0,
        5000.0,
        100.0
      ]
    },
    "chatWidth": {
      "name": "Largura",
      "value": 320,
      "description": "Define a largura disponível para as linhas do chat, limitada pelo tamanho da tela.",
      "impact": "Mudar a largura refaz a quebra de linhas do histórico; o custo é pontual.",
      "color": "cost",
      "range": [
        80.0,
        640.0,
        40.0
      ]
    },
    "chatHeight": {
      "name": "Altura",
      "value": 180,
      "description": "Define a altura visível do chat. O histórico pode ser maior e continua acessível pela rolagem.",
      "impact": "Mais linhas visíveis exigem mais desenho de texto e fundo.",
      "color": "cost",
      "range": [
        40.0,
        320.0,
        20.0
      ]
    },
    "chatScale": {
      "name": "Escala",
      "value": 1.0,
      "description": "Ajusta o tamanho visual do chat e a largura usada para quebrar suas mensagens.",
      "impact": "Alterar a escala refaz a quebra de linhas; o custo de desenho depende da área e das linhas visíveis.",
      "color": "cost",
      "range": [
        0.5,
        2.0,
        0.25
      ]
    },
    "chatSpacing": {
      "name": "Espaçamento",
      "value": 0,
      "description": "Acrescenta espaço vertical entre as linhas do chat.",
      "impact": "Custo baixo: muda a disposição e a quantidade de linhas que cabem na área.",
      "color": "neutral",
      "range": [
        0.0,
        6.0,
        1.0
      ]
    },
    "chatOpacity": {
      "name": "Opacidade do texto",
      "value": 100,
      "description": "Define a opacidade máxima dos textos do chat, além do desaparecimento por tempo.",
      "impact": "Custo baixo: textos semitransparentes continuam sendo desenhados.",
      "color": "neutral",
      "range": [
        10.0,
        100.0,
        10.0
      ]
    },
    "chatBackground": {
      "name": "Fundo",
      "value": true,
      "description": "Mostra um fundo atrás das linhas visíveis do chat.",
      "impact": "Acrescenta retângulos com transparência atrás das mensagens.",
      "color": "cost"
    },
    "chatBackgroundOpacity": {
      "name": "Opacidade do fundo",
      "value": 50,
      "description": "Ajusta a opacidade do fundo quando Fundo está ligado.",
      "impact": "Custo baixo: reduzir a opacidade não elimina necessariamente o desenho do fundo.",
      "color": "neutral",
      "range": [
        0.0,
        100.0,
        10.0
      ]
    },
    "chatDuration": {
      "name": "Exibição (segundos)",
      "value": 10,
      "description": "Define por quantos segundos uma mensagem fica visível com o chat fechado.",
      "impact": "Mais tempo pode manter mais mensagens sendo desenhadas.",
      "color": "cost",
      "range": [
        1.0,
        120.0,
        1.0
      ]
    },
    "chatFade": {
      "name": "Fade (segundos)",
      "value": 1,
      "description": "Define a duração do desaparecimento gradual das mensagens. Zero remove a transição.",
      "impact": "Custo baixo: calcula o alpha das linhas visíveis.",
      "color": "neutral",
      "range": [
        0.0,
        10.0,
        1.0
      ]
    },
    "chatGroupConsecutive": {
      "name": "Só consecutivas",
      "value": false,
      "description": "Agrupa apenas repetições consecutivas. Desligado, também procura repetições recentes entre outras mensagens.",
      "impact": "Ligado restringe a procura de repetições e pode reduzir o trabalho de CPU.",
      "color": "saving"
    },
    "chatGroupSeconds": {
      "name": "Agrupar por (s)",
      "value": 120,
      "description": "Define por quanto tempo uma mensagem anterior pode receber uma repetição no mesmo grupo.",
      "impact": "Custo limitado: a procura continua restrita às mensagens recentes.",
      "color": "neutral",
      "range": [
        1.0,
        600.0,
        1.0
      ]
    },
    "chatSendQueue": {
      "name": "Fila de envio",
      "value": false,
      "description": "Espaça o envio de suas mensagens. Ctrl+Backspace cancela a fila; desconectar também a cancela.",
      "impact": "Custo baixo: uma fila limitada, consultada por tick. Não contorna regras do servidor.",
      "color": "neutral"
    },
    "chatSendInterval": {
      "name": "Intervalo de envio (s)",
      "value": 1.0,
      "description": "Define a espera entre mensagens quando a fila de envio está ligada.",
      "impact": "Custo mínimo: muda o intervalo, sem criar novas mensagens.",
      "color": "neutral",
      "range": [
        0.1,
        60.0,
        0.1
      ]
    },
    "chatInspectActions": {
      "name": "Mostrar ações dos links",
      "value": true,
      "description": "Mostra a ação associada a mensagens clicáveis, mantendo o clique original.",
      "impact": "Custo baixo: examina o componente sob o mouse com o chat aberto.",
      "color": "neutral"
    },
    "chatClickedHistory": {
      "name": "Histórico dos cliques",
      "value": true,
      "description": "Guarda comandos clicados no histórico de envio para facilitar sua reutilização.",
      "impact": "Custo pontual: apenas ao clicar em uma ação de comando.",
      "color": "neutral"
    },
    "chatImagePreview": {
      "name": "Prévia de imagens",
      "value": true,
      "description": "Mostra prévias de links diretos de imagens ao passar o mouse. O download ocorre fora da thread gráfica.",
      "impact": "Usa rede, memória e GPU para imagens; mantém cache e fila limitados.",
      "color": "variable"
    },
    "tabPing": {
      "name": "Ping numérico no TAB",
      "value": true,
      "description": "Substitui as barras de conexão no TAB por um número de ping.",
      "impact": "Acrescenta desenho de texto por jogador enquanto o TAB está aberto.",
      "color": "cost"
    },
    "scoreboardVisible": {
      "name": "Exibir scoreboard",
      "value": true,
      "description": "Mostra o objetivo lateral enviado pelo servidor.",
      "impact": "Desenha os textos e o fundo do placar; a preparação é reutilizada quando possível.",
      "color": "cost"
    },
    "scoreboardNumbers": {
      "name": "Números laterais",
      "value": true,
      "description": "Mostra os números laterais das pontuações no placar.",
      "impact": "Acrescenta até um texto numérico por linha visível.",
      "color": "cost"
    },
    "scoreboardX": {
      "name": "Posição horizontal (%)",
      "value": 100,
      "description": "Move o placar horizontalmente dentro da área disponível da tela.",
      "impact": "Custo mínimo: muda a posição do desenho.",
      "color": "neutral",
      "range": [
        0.0,
        100.0,
        5.0
      ]
    },
    "scoreboardY": {
      "name": "Posição vertical (%)",
      "value": 50,
      "description": "Move o placar verticalmente dentro da área disponível da tela.",
      "impact": "Custo mínimo: muda a posição do desenho.",
      "color": "neutral",
      "range": [
        0.0,
        100.0,
        5.0
      ]
    },
    "scoreboardScale": {
      "name": "Escala",
      "value": 1.0,
      "description": "Ajusta o tamanho do placar, respeitando o espaço da tela.",
      "impact": "Custo baixo: transforma o desenho; tamanho maior ocupa mais pixels.",
      "color": "neutral",
      "range": [
        0.5,
        2.0,
        0.25
      ]
    },
    "scoreboardBackground": {
      "name": "Fundo",
      "value": true,
      "description": "Mostra um fundo atrás do placar lateral.",
      "impact": "Acrescenta um retângulo com transparência.",
      "color": "cost"
    },
    "scoreboardOpacity": {
      "name": "Opacidade do fundo",
      "value": 35,
      "description": "Define a opacidade do fundo do placar.",
      "impact": "Custo baixo: reduzir a opacidade não elimina o retângulo.",
      "color": "neutral",
      "range": [
        0.0,
        100.0,
        5.0
      ]
    },
    "scoreboardKey": {
      "name": "Atalho de visibilidade",
      "value": 0,
      "description": "Define um atalho para mostrar ou ocultar o placar. Delete remove; Esc cancela a alteração.",
      "impact": "Custo mínimo: tratamento do atalho.",
      "color": "neutral"
    },
    "slotProfile": {
      "name": "Perfil de slots",
      "value": false,
      "description": "Liga o perfil que impede selecionar os slots marcados. Não impede organizar o inventário.",
      "impact": "Custo mínimo: verificação ao trocar de slot.",
      "color": "neutral"
    },
    "slotProfileKey": {
      "name": "Atalho do perfil",
      "value": 0,
      "description": "Define o atalho para ligar ou desligar o perfil sem perder os slots marcados. Delete remove o atalho.",
      "impact": "Custo mínimo: tratamento do atalho.",
      "color": "neutral"
    }
  },
  "modules": [
    {
      "key": "zoom",
      "name": "Zoom",
      "icon": "zoom",
      "options": [
        "zoomFactor",
        "zoomScroll",
        "toggleZoom",
        "zoomSensitivity",
        "dynamicZoomSensitivity",
        "directZoomCamera",
        "renderHandZoom",
        "smoothZoom",
        "smoothScrollZoom",
        "zoomEasing"
      ],
      "hud": null
    },
    {
      "key": "perspective",
      "name": "Perspective",
      "icon": "camera",
      "options": [],
      "hud": null
    },
    {
      "key": "toggleSprint",
      "name": "Toggle Sprint",
      "icon": "run",
      "options": [],
      "hud": "sprint"
    },
    {
      "key": "scrollLock",
      "name": "Bloquear scroll",
      "icon": "mouse",
      "options": [],
      "hud": null
    },
    {
      "key": "slotProfile",
      "name": "Perfil de slots",
      "icon": "slots",
      "options": [],
      "hud": "slots"
    },
    {
      "key": "pingDisplay",
      "name": "Ping",
      "icon": "signal",
      "options": [],
      "hud": "ping"
    },
    {
      "key": "tntTimer",
      "name": "TNT Timer",
      "icon": "timer",
      "options": [],
      "hud": "tnt"
    },
    {
      "key": "scoreboardVisible",
      "name": "Scoreboard",
      "icon": "board",
      "options": [
        "scoreboardNumbers",
        "scoreboardBackground",
        "scoreboardOpacity",
        "scoreboardScale",
        "scoreboardKey"
      ],
      "hud": "scoreboard"
    },
    {
      "key": "oldAnimations",
      "name": "Old Animations",
      "icon": "sword",
      "options": [],
      "hud": null
    },
    {
      "key": "fullBright",
      "name": "Fullbright",
      "icon": "sun",
      "options": [
        "smartFullbright"
      ],
      "hud": null
    },
    {
      "key": "timeChanger",
      "name": "Horário visual",
      "icon": "clock",
      "options": [
        "timeOfDay"
      ],
      "hud": null
    },
    {
      "key": "sneakAnimation",
      "name": "Agachar suave",
      "icon": "run",
      "options": [],
      "hud": null
    },
    {
      "key": "cleanView",
      "name": "Ocultar partículas próprias",
      "icon": "sparkles",
      "options": [],
      "hud": null
    },
    {
      "key": "hideBreakingParticles",
      "name": "Ocultar fragmentos",
      "icon": "cube",
      "options": [],
      "hud": null
    },
    {
      "key": "hideLightning",
      "name": "Ocultar raios",
      "icon": "bolt",
      "options": [],
      "hud": null
    },
    {
      "key": "cleanProjectiles",
      "name": "Ocultação inicial de projéteis",
      "icon": "arrow",
      "options": [],
      "hud": null
    },
    {
      "key": "hideEnchantmentBooks",
      "name": "Ocultar livros de encanto",
      "icon": "book",
      "options": [],
      "hud": null
    },
    {
      "key": "singleGroundItem",
      "name": "Item único por pilha",
      "icon": "layers",
      "options": [],
      "hud": null
    },
    {
      "key": "staticGroundItems",
      "name": "Itens sem balanço",
      "icon": "cube",
      "options": [],
      "hud": null
    },
    {
      "key": "removeWaterOverlay",
      "name": "Remover camada da água",
      "icon": "water",
      "options": [],
      "hud": null
    },
    {
      "key": "hideFireResistance",
      "name": "Ocultar fogo protegido",
      "icon": "flame",
      "options": [],
      "hud": null
    }
  ],
  "groups": [
    {
      "name": "Entrada e câmera",
      "icon": "mouse",
      "sections": [
        {
          "name": "Teclado e mouse",
          "keys": [
            "clickFix",
            "mouseFix",
            "keyFix",
            "linuxShiftFix",
            "mouseBindFix",
            "inputFix",
            "rawMouseInput"
          ]
        },
        {
          "name": "Câmera e enquadramento",
          "keys": [
            "parallaxFix",
            "betterCamera"
          ]
        }
      ]
    },
    {
      "name": "Vídeo e desempenho",
      "icon": "monitor",
      "sections": [
        {
          "name": "Limites de FPS",
          "keys": [
            "backgroundFps",
            "menuFps",
            "loadingFps"
          ]
        },
        {
          "name": "Renderização",
          "keys": [
            "entityCulling",
            "mergeTerrain",
            "batchModels",
            "modelBackFaces",
            "optimizedItems",
            "customSkyFix",
            "voidBoxFix"
          ]
        },
        {
          "name": "Texto e HUD",
          "keys": [
            "optimizedText",
            "stringWidthCaching",
            "hudCaching",
            "hudCacheFps"
          ]
        },
        {
          "name": "Partículas e iluminação",
          "keys": [
            "ambientParticles",
            "particleLimit",
            "staticParticleColor",
            "lowAnimationTick"
          ]
        }
      ]
    },
    {
      "name": "Carregamento",
      "icon": "layers",
      "sections": [
        {
          "name": "Mundo e memória",
          "keys": [
            "fastWorldLoading",
            "lazyChunkLoading",
            "optimizedPlayerUpdating",
            "memoryCleanup"
          ]
        },
        {
          "name": "Pacotes de recursos",
          "keys": [
            "smallPackIcons",
            "fastLanguageSwitch",
            "itemStitching"
          ]
        }
      ]
    },
    {
      "name": "Chat",
      "icon": "chat",
      "sections": [
        {
          "name": "Histórico e repetições",
          "keys": [
            "chatGroup",
            "chatKeepHistory",
            "chatHistory",
            "chatGroupConsecutive",
            "chatGroupSeconds"
          ]
        },
        {
          "name": "Menções e filtros",
          "keys": [
            "chatMentions",
            "chatMentionSound",
            "chatMentionWords",
            "chatFilter",
            "chatHiddenWords",
            "chatHideNotices",
            "chatNoticeWords"
          ]
        },
        {
          "name": "Aparência e leitura",
          "keys": [
            "chatWidth",
            "chatHeight",
            "chatScale",
            "chatSpacing",
            "chatOpacity",
            "chatBackground",
            "chatBackgroundOpacity",
            "chatDuration",
            "chatFade"
          ]
        },
        {
          "name": "Envio e links",
          "keys": [
            "chatSendQueue",
            "chatSendInterval",
            "chatInspectActions",
            "chatClickedHistory",
            "chatImagePreview",
            "tabPing"
          ]
        }
      ]
    },
    {
      "name": "Efeitos visuais",
      "icon": "sparkles",
      "sections": [
        {
          "name": "Fogo, água e montaria",
          "keys": [
            "fireHeight",
            "fireOpacity",
            "waterFogDensity",
            "horseOpacity"
          ]
        },
        {
          "name": "Correções visuais",
          "keys": [
            "nauseaFix"
          ]
        }
      ]
    },
    {
      "name": "Capturas e áudio",
      "icon": "image",
      "sections": [
        {
          "name": "Capturas de tela",
          "keys": [
            "asyncScreenshot",
            "screenshotPreview",
            "screenshotAutoCopy",
            "screenshotCompact"
          ]
        },
        {
          "name": "Recarga de recursos",
          "keys": [
            "separateResourceLoading"
          ]
        }
      ]
    },
    {
      "name": "Menus e conexão",
      "icon": "network",
      "sections": [
        {
          "name": "Menus",
          "keys": [
            "lanButton",
            "reconnectMenu",
            "confirmQuit"
          ]
        },
        {
          "name": "Resolução de servidores",
          "keys": [
            "alternateDns",
            "fallbackDns"
          ]
        }
      ]
    },
    {
      "name": "Manutenção e F3",
      "icon": "settings",
      "sections": [
        {
          "name": "Depuração visual",
          "keys": [
            "simpleDebugFps",
            "vanillaMetrics"
          ]
        },
        {
          "name": "Logs locais",
          "keys": [
            "cleanOldLogs",
            "logRetentionDays"
          ]
        }
      ]
    }
  ]
};
