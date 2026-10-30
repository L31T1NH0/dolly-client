# Dolly Client — seleção e padrões

## Otimizações autorizadas posteriormente

Foram integradas adaptações de FoamFix (dados de modelos), Krypton (frames/VarInts), EntityCulling (mobs/objetos de blocos) e duas ideias do MCOpt Linux (faixas contíguas de terreno e limite de FPS durante carregamento), conforme o [escopo da integração](mods/integracao.md). Jogadores locais e remotos são excluídos do culling. Culling e fusão começam ligados; carregamento começa em 30 FPS. Não adicionar o método A/B do MCOpt.

Esta especificação registra as decisões do usuário e prevalece sobre os padrões de referência do CMClient. A versão atual usa Minecraft 1.8.9 como versão própria, sem Forge, com OptiFine HD U M5 integrado por decisão posterior ao primeiro teste. A primeira build foi gerada e verificada sem iniciar a janela do jogo. O [guia do primeiro teste](primeiro-teste.md) distingue o que está implementado e as partes ainda pendentes.

## Renderização, áudio e reset

| Configuração | Padrão Dolly | Ao restaurar padrões |
| --- | --- | --- |
| VBOs | Ligado, controle nativo do OptiFine | Ligado |
| Render Regions | Ligado, opção separada de VBOs | Ligado |
| Fast Render | Ligado | Ligado; o reset não deve desligá-lo |
| VSync | Desligado | Desligado |
| Balanço da câmera ao andar (View Bobbing) | Desligado | Desligado |
| Música do jogo | 0% | 0% |
| Intensidade da iluminação suave (Smooth Lighting) | 25%, controle nativo do OptiFine | 25% |
| Brilho normal do jogo | 100% | 100% |

Render Regions depende de VBO e das capacidades gráficas necessárias. O estado solicitado e o backend efetivamente disponível precisam ser tratados durante a implementação. Fast Render e Render Regions usam agora as implementações do OptiFine encapsulado no JAR. O backend regional e o ajuste próprio de AO do Dolly foram removidos. VBOs e intensidade de iluminação ficam nos controles nativos, sem opções duplicadas no menu Dolly.

Iluminação suave em 25% descreve sua intensidade, não um modo enumérico de iluminação nem gamma. Balanço ao andar é diferente de tremor ao receber dano: o usuário não alterou explicitamente o segundo nesta seleção. Música em 0% corresponde à categoria de música, não ao volume principal.

O controle normal de brilho a 100% e o módulo Full Bright são recursos distintos. O módulo permanece disponível conforme seleção abaixo.

## Entrada e combate

| Recurso | Decisão |
| --- | --- |
| Click delay fix | Incluído e ligado por padrão: evitar a penalidade local de 10 ticks após erro de ataque |
| Mouse delay fix | Incluído e ligado por padrão: como no CMPack, usa yaw do jogador em vez da cabeça na mira local, preservando interpolação |
| Modern Keybind Handling | Incluído e ligado por padrão: recuperar o estado físico das teclas ao retornar ao jogo |
| Fix de Shift no Linux | Incluído e ligado por padrão no Linux: corrigir Shift+2/Shift+6 preservando os atalhos |
| Movimento após inventário | Retomar as teclas que continuam fisicamente pressionadas, sem exigir novo pressionamento |
| Toggle Sprint | Mantido, com alternância de sprint disponível e ligada por padrão conforme referência |
| Animação de agachar | Mantida e ligada por padrão conforme referência |
| Old Animations | Incluído, com as animações antigas solicitadas ativas; não herdar o módulo desligado da instalação limpa do CMClient |
| Zoom | Mantido, com suavidade e ajuste pela roda conforme referência |

Os fixes serão integrados ao próprio client. Não dependem de mods Forge externos. InputFix de texto do CMClient não equivale à correção Linux do Shift e não foi selecionado como módulo adicional.

Na referência Old Animations, as opções de bloqueio, espada ao bater, comer, atacar, dano de armadura e corações armazenam verdadeiro; posições antigas de itens segurados, arco e vara armazenam falso. Usar isso como ponto de partida do módulo solicitado. A opção de ataque também afeta lógica de uso/ataque: revisar e validar esses caminhos separadamente das transformações visuais.

## Menu, segundo plano e partículas

Manter os controles examinados no CMClient. Como o usuário não indicou valores novos para esses limites, usar a referência como ponto de partida:

| Controle | Valor de referência mantido |
| --- | --- |
| FPS no menu | Taxa do monitor detectado; fallback de 60 FPS |
| FPS com janela inativa | 30 FPS |
| Limite de partículas | 1.000 por lista interna de renderização |
| Partículas ao quebrar blocos | Ligadas |
| Render de partículas de void fog/barrier | Desligado |
| Rotina otimizada de void fog/barrier | Ligada quando a rotina for utilizada |

“Partículas” foi selecionado como recurso; o usuário não especificou partículas extras forçadas nem novos multiplicadores. Não interpretar essa escolha como ativação automática de Always Critical ou Always Sharpness.

## Módulos opcionais selecionados nominalmente

- Old Animations.
- Perspective.
- Zoom.
- Full Bright.
- Time Changer.
- TNT Timer.
- Ping Display.

Toggle Sprint e animação de agachar também foram explicitamente mantidos e fazem parte do escopo, mesmo não repetidos na lista final de sete módulos. Manter os controles de partículas descritos acima.

Para Perspective, Full Bright, Time Changer, TNT Timer e Ping Display, a inclusão está confirmada; o usuário não especificou um novo estado inicial nesta mensagem. O catálogo mantém essa distinção, sem decidir automaticamente que todos devem iniciar ativos.

Por decisão posterior, as personalizações de chat e scoreboard também fazem parte do Dolly, conforme [escopo e controles](chat-scoreboard.md). O chat não deve acrescentar horário às mensagens; a busca usa Ctrl+F apenas enquanto o chat está aberto. Os demais módulos de personalização do CMClient continuam fora da seleção atual. As funções básicas do Minecraft permanecem disponíveis.

## Validação necessária durante a implementação

- Restaurar padrões deve produzir Fast Render ligado e VSync desligado, assim como a primeira execução.
- Verificar os valores de música, brilho, iluminação suave e balanço após criar configuração, salvar, reabrir e restaurar.
- Testar clique após erro, giro rápido da mira, teclas remapeadas, perda de foco e fechamento de inventário.
- Testar Shift+2/Shift+6 no Linux do usuário e preservar texto/atalhos.
- Verificar VBO/Regions separadamente, capacidades gráficas e interação de Fast Render com framebuffer e efeitos.
- Separar testes de animação visual de alterações na lógica de uso/ataque.

Configuração, reset, integração dos padrões e estrutura do JAR já têm verificações automatizadas. Os critérios de gameplay e benchmark continuam pendentes de execução gráfica.
