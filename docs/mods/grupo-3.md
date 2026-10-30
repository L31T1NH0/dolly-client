# Grupo 3: câmera, zoom, iluminação e horário

Adapta os comportamentos selecionados de Patcher/PolyPatcher à base 1.8.9 com OptiFine M5. Não instala carregador Forge nem mantém dois sistemas de zoom simultâneos.

## Zoom — aba Câmera

| Controle | Padrão | Comportamento |
| --- | --- | --- |
| Zoom | ON | Segurar o atalho aproxima; o atalho continua remapeável nos controles nativos. |
| Fator do zoom | 4× | Intervalo de 1× a 600×. |
| Zoom pela roda | ON | Enquanto o zoom está ativo, a roda ajusta a aproximação em passos proporcionais (+25% / −25%), sem trocar o slot. |
| Transição do zoom | ON | Entrada e saída em aproximadamente 200 ms, independente do FPS. |
| Transição pela roda | ON | Suaviza mudanças de aproximação durante o zoom. |
| Zoom por alternância | OFF | Um toque liga; outro desliga. Menus, perda de foco e troca de contexto cancelam o estado. |
| Mouse direto no zoom | OFF | Permite remover a câmera suave do zoom sem alterar a preferência nativa salva de câmera cinematográfica. |
| Sensibilidade do zoom | 100% | Multiplicador de sensibilidade usado somente na câmera durante o zoom. |
| Sensibilidade dinâmica | OFF | Diminui a sensibilidade proporcionalmente à aproximação escolhida. |
| Mão durante zoom | OFF | Permite manter a mão visível com seu campo de visão nativo. |
| Curva do zoom | Quadrática | Quadrática, circular ou quíntica. |

A sensibilidade é consultada nos caminhos de mouse por quadro e de suavização por tick, sem modificar o valor nativo salvo. O Perspective usa o mesmo ajuste quando combinado com zoom. Mudanças na aproximação invalidam a visibilidade do cenário para evitar chunks ausentes ao afastar a visão. O cenário e as interações continuam usando a câmera original do jogador para o gameplay.

## Fullbright — aba Visual

O Fullbright continua desligado por padrão; passa a usar luz máxima nas consultas visuais do mundo cliente e dos chunks, em vez de elevar o gamma. A imagem da luz fica branca e sua reconstrução repetida é dispensada enquanto o estado é válido. Ativar/desativar recarrega o desenho dos chunks. Mudanças de mundo, recursos e reset invalidam a imagem reaproveitada.

**Fullbright inteligente** começa ligado e desativa o efeito com shaders. O mundo do servidor integrado nunca é candidato: seus dados de luz e regras de gameplay permanecem nativos. A preferência de brilho/gamma do usuário não é substituída pelo Fullbright.

## Parallax — aba Câmera

**Parallax Fix** começa ligado desde 0.1.15 e pode ser alternado. Ajusta o pequeno deslocamento da câmera em primeira pessoa. O desenho nativo dos eixos do F3 usa seu deslocamento original, evitando que os eixos sejam cortados pela câmera corrigida. O estado temporário é limpo também se esse desenho falhar. Não muda alcance ou posição real do jogador.

## Better Camera — aba Câmera

Começa ligado. A consulta da câmera em terceira pessoa ignora vegetação sem colisão e os tipos de vidro, painéis de vidro e grades usados pelo PolyPatcher. A exceção para blocos transparentes é restrita ao traçado da câmera por estado local à thread, restaurado em `finally`. A seleção de blocos, ataques e demais traçados continuam usando suas colisões normais.

## Céu do OptiFine — aba Visual

**Corrigir céu OptiFine** começa ligado e pode ser desativado. Amplia a geometria do céu e usa display lists nos caminhos de construção e desenho do céu de forma consistente. Isso evita misturar uma lista com um buffer VBO inexistente. A escolha de VBOs/Render Regions do terreno permanece independente. A troca do controle reconstrói os renderizadores; a aparência do céu normal também pode mudar.

## Horário — aba Visual

**Time Changer** continua desligado por padrão. Ligue-o e selecione **Horário visual**. O controle existente percorre manhã, meio-dia, fim de tarde e meia-noite.

A falha anterior era o hook apenas no getter de horário `World.L()`: o cálculo do ângulo do céu lia diretamente `WorldInfo.getWorldTime()`, ignorando esse getter. Agora as leituras diretas na classe World recebem o ajuste somente quando o mundo é cliente. Preserva-se o contador de dias; os dados recebidos do servidor e o relógio total não são sobrescritos. Desligar restaura a leitura original.

## Integração e validação

`DollyZoom` concentra alternância, roda, curvas e sensibilidade; `DollyCamera` mantém a transição por quadro e o Perspective. `DollyCameraTrace` restringe as exceções de colisão e fornece a política visual de Parallax/céu. `DollyLighting` trata luz máxima e invalidação; `DollyWorldTime` continua responsável pelo horário apresentado. `CameraLightingPatches` conecta os pontos exatos do M5; os formatos críticos são conferidos no build.

Os testes verificam estados de zoom, passos/limites, monotonicidade das curvas, sensibilidade sem alteração da preferência salva, leitura real do ângulo celestial com WorldInfo inalterado, exclusão do servidor integrado, luz dos chunks, shaders, traçado restrito à câmera (inclusive com erro), eixos do F3 e hooks do JAR final. Os testes anteriores exercitam foco, entrada, configurações, reset e equivalência temporal em diferentes frequências de quadros.

Ainda é necessário conferir no jogo o céu dos resource packs, shaders, mão no zoom, mudanças de iluminação e Parallax. O ambiente não disponibiliza um display OpenGL para validação visual; não há ganho de desempenho medido.

## Referências

- [Zoom do PolyPatcher](https://github.com/Polyfrost/PolyPatcher/blob/main/src/main/java/club/sk1er/patcher/hooks/ZoomHook.java).
- [Parallax](https://github.com/Polyfrost/PolyPatcher/blob/main/src/main/java/club/sk1er/patcher/mixins/features/EntityRendererMixin_ParallaxFix.java).
- [Better Camera](https://github.com/Polyfrost/PolyPatcher/blob/main/src/main/java/club/sk1er/patcher/mixins/features/EntityRendererMixin_CameraPerspective.java).
- [Blocos transparentes](https://github.com/Polyfrost/PolyPatcher/blob/main/src/main/java/club/sk1er/patcher/mixins/features/WorldMixin_CameraPerspective.java).
- [Fullbright](https://github.com/Polyfrost/PolyPatcher/blob/main/src/main/java/club/sk1er/patcher/mixins/features/fullbright/WorldMixin_Fullbright.java).
- [Céu do OptiFine](https://github.com/Polyfrost/PolyPatcher/blob/main/src/main/java/club/sk1er/patcher/mixins/bugfixes/optifine/RenderGlobalMixin_FixSkyVBOs.java).
