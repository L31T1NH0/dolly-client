# Organização do código

As adaptações de desempenho ficam em `DollyModels` (pool temporário de vértices), `DollyNetwork` (protocolo/Netty), `DollyCulling` e `dolly.render.Occlusion` (visibilidade conservadora na thread de render), e `DollyTerrain` (faixas contíguas). `PerformancePatches` aplica os hooks durante o build; `DollyFrameLimiter` trata a tela de download de terreno. Veja [integração e limites](mods/integracao.md).

O projeto gera uma versão própria a partir do JAR original da 1.8.9. O build reconstrói o OptiFine M5 fornecido sobre essa base e aplica os patches Dolly depois. O jogo inclui o runtime do OptiFine, sem Forge, Launchwrapper, transformer ou ASM em runtime.

## Divisão de responsabilidades

| Arquivo/área | Responsabilidade |
| --- | --- |
| `src/main/java/dolly/config/DollyConfig.java` | Valores próprios do Dolly, leitura, validação e gravação atômica de propriedades |
| `DollySettings.java` | Aplicar, salvar e restaurar configurações no adaptador Minecraft |
| `DollyInput.java` | Atalhos, normalização do Shift no Linux, recuperação de teclas e estado de Toggle Sprint |
| `dolly/input/SlotProfile.java` | Política de seleção e navegação circular entre slots permitidos da hotbar |
| `DollySlots.java` | Aplicar o perfil ao jogador local, ativar/desativar e preservar um slot livre |
| `DollySlotScreen.java` | Configurar os nove slots e o atalho de ativação no menu Controles |
| `DollyCamera.java` | Zoom, câmera de Perspective e interpolação visual ao agachar |
| `DollyCombat.java` | Política de bloqueio após clique sem alvo |
| `DollyOldAnimations.java` | Apresentação de espada, comer, dano e corações; guards de uso de item, sem gerar cliques |
| `DollyLighting.java` | Luz máxima somente no mundo cliente e ciclo de vida do lightmap |
| `DollyOptiFine.java` | Sincronizar configurações compartilhadas, reset e integração dos recursos OptiFine |
| `DollyWorldTime.java` | Horário visual do mundo cliente |
| `DollyParticles.java` | Limite de partículas e despacho da ambientação |
| `DollyFramebuffer.java` | Destruir/recriar framebuffers ao alterar Fast Render; liberar o carregamento substituído no resize e no OptiFine |
| `DollyFrameLimiter.java` | Limites do menu e segundo plano |
| `DollyHud.java` | Exibir ping, estado de sprint e contadores de TNT próximos |
| `dolly/chat/ChatHistory.java` | Histórico limitado em memória, agrupamento, regras de palavras/menções e cálculo de fade |
| `DollyChat.java` | Adaptador de chat nativo com quebra de linhas, componentes clicáveis, busca, filtros e âncora de rolagem |
| `DollyChatControls.java` | Ctrl+F, campo de busca, Ctrl+clique e aviso de mensagens novas dentro do chat nativo |
| `DollyScoreboard.java` | Apresentação local do objetivo lateral recebido do servidor |
| `DollySocialScreen.java` | Opções paginadas de chat e scoreboard, listas de palavras e seleção de atalho |
| `DollyScreen.java` | Layout, páginas e eventos da tela de opções; delega alterações a DollySettings |
| `DollyMenu.java` | Entrada da tela do Dolly nos menus vanilla |
| `tools/java/InputPatches.java` | Pontos de integração de entrada e combate na 1.8.9 |
| `tools/java/SettingsPatches.java` | Pontos de integração de configuração, menus e HUD |
| `tools/java/ChatPatches.java` | Substituição do adaptador de chat no HUD, hooks da tela nativa e render do scoreboard |
| `tools/java/LegacyAnimationPatches.java` | Pontos de integração específicos das animações antigas, armadura, corações e uso de item |
| `tools/java/RenderPatches.java` | Pontos de integração de câmera, brilho, horário e partículas |
| `tools/java/OptiFinePatches.java` | Reset de vídeo e remoção do segundo caminho de zoom |
| `tools/optifine.py` | Reconstrução GDIFF, validação dos recursos e merge offline do OptiFine |
| `tools/package_prism.py` | Perfil Prism com JAR próprio, Java 8 e memória configurada |
| `tools/java/PatchClient.java` | Ler JAR, encaminhar cada classe ao patch adequado e empacotar o resultado |
| `tools/java/HierarchyWriter.java` | Resolver hierarquias pelos metadados, sem executar classes do jogo, para gerar stack frames |
| `tools/version-template.json` | Metadados completos da versão, bibliotecas e natives; sem herdar downloads de outro client JAR |
| `tools/libraries.json` | Dependências usadas exclusivamente pelas ferramentas de build |
| `tools/build.py` | Resolver arquivos locais, compilar, executar verificações e gerar o pacote |
| `src/test/java/` | Testes de configuração, transições, entrada/câmera/HUD, patches e ligação/verificação da JVM |

Os arquivos `Dolly*.java` que acessam diretamente o Minecraft ficam no pacote padrão porque a distribuição obfuscada da 1.8.9 usa esse pacote, e Java não permite importar essas classes de um pacote nomeado. A lógica independente já fica em pacotes `dolly.*`. Esse é um limite do adaptador para esta base, não uma razão para colocar lógica nova inteira no pacote padrão.

## Mecanismos técnicos adaptados

A [integração técnica do CMPack](cmpack/integracao-tecnica.md) documenta padrões, diferenças e testes.

- `DollyRawMouse` adapta JInput; `dolly.input.RawMouseInput` concentra a concorrência e a transferência atômica dos eixos. `TechnicalInputPatches` conecta GUI, inventário e captura.
- `DollyTextCache` invalida medidas por fonte; `DollyItemStitching` atua antes do bake; `TechnicalRenderPatches` liga esses pontos.
- `DollyHudCache` controla um framebuffer na thread gráfica. `TechnicalHudPatches` separa as camadas dinâmicas por `DollyHudLayers` e preserva a acumulação do alpha. `DollyFramebuffer` e as transições de recursos liberam esse buffer.
- `DollyScreenshots` mantém pixels e gravação por captura; `DollyLanguage` restringe a recarga de idioma; `DollyServerAddress` resolve SRV com cache e timeout. `TechnicalLoadingPatches` conecta as operações e o encerramento do gravador.
- `DollyResources` trata troca de mundo, coleta opcional e pacing dos workers; `DollyPlayers` reutiliza resultados de velocidade sem eliminar atualizações de animação; `DollyTextureCleanup` rastreia último uso de texturas recarregáveis. `TechnicalRuntimePatches` faz os encaminhamentos.

Os hooks continuam aplicados somente durante o build. O CMPack não é incluído no runtime e nenhuma dependência nova foi adicionada.

## Compatibilidade com o binário

A compilação aceita somente o SHA-1 oficial da 1.8.9: `3870888a6c3d349d3771a3e9d16c9bf5e076b908`. Os patches procuram assinaturas exatas e verificam padrões críticos, como as duas atribuições da penalidade de ataque. Mudança de versão exige revisar esses pontos; não deve funcionar silenciosamente sobre outro JAR.

Alguns nomes principais:

| Nome do binário | Papel |
| --- | --- |
| `ave` | Minecraft |
| `avh` / `avb` | GameSettings / KeyBinding |
| `pr` / `pk` / `bew` | EntityLivingBase / Entity / jogador local |
| `bfk` | EntityRenderer |
| `bqs` | OpenGlHelper |
| `bmt` / `bht` / `bfy` | VertexBuffer / RenderChunk / VboRenderList |
| `bdb` / `adm` | Mundo cliente / World |
| `bec` | EffectRenderer |
| `bfn` | ItemRenderer |
| `bgf$b` | Cálculo dos fatores de iluminação ambiente dos vértices |

O código de uma função fica em sua classe de responsabilidade; o patch apenas encaminha o ponto correspondente do Minecraft. Não adicionar comportamento de gameplay ao empacotador nem lógica de persistência à tela.

## Configuração e recursos

- `options-dolly.txt` guarda as opções/atalhos vanilla desta versão.
- `config/dolly.properties` guarda os recursos do Dolly.
- Restaurar padrões usa a mesma função de padrões de vídeo da primeira execução.
- Render Regions, Fast Render e intensidade de AO usam as implementações nativas do OptiFine. Não há segundo alocador ou segundo passe de AO no Dolly.
- A seleção efetiva do backend segue os gates do OptiFine; a tela Dolly mostra o backend ativo. Em M5, Regions sem suporte pode desabilitar VBO; desligar Regions nas opções permite testar VBO simples.
- `optionsof.txt` guarda as opções nativas do OptiFine. A ponte captura as opções compartilhadas ao salvar e reaplica no carregamento. VBOs e iluminação suave são controlados na interface nativa.
- `references/` mantém os binários locais fora do Git. O OptiFine M5 fornecido é uma entrada obrigatória do build; o CMClient é apenas referência.
- Nove tipos `net/minecraftforge/` fornecidos pelo próprio OptiFine são preservados por compatibilidade de assinaturas; não há carregador Forge.

## Transições e atualização

- Alterações completam a atualização do renderer e a recriação dos framebuffers antes do salvamento nativo. Falhas de escrita mantêm o estado aplicado e exibem o erro; uma nova tentativa pode persistir esse estado. O reset grava os padrões antes de alterar shaders ou recursos gráficos.
- O botão nativo de Fast Render libera recursos antes de mudar o gate do OptiFine. O reset nativo faz o mesmo antes de alterar seus padrões. Toda substituição do carregamento no Minecraft e em `Config.updateFramebufferSize()` libera o framebuffer anterior.
- Dolly desativa o zoom nativo também após `loadOfOptions()` e reconstrói o mapa global quando muda esse binding. O gate nativo do OptiFine decide a disponibilidade de pós-processamento.
- Menu e Toggle Sprint recebem eventos de teclado e mouse. Soltar a tecla mantém o comando de sprint quando alternado; repetições não alternam novamente. Perspective é consultado por quadro antes do mouse.
- Zoom e Smooth Sneak avançam uma vez por quadro: o zoom usa curvas configuráveis de 200 ms e o agachar mantém suavização exponencial baseada em tempo. Consultas de FOV/altura não alteram o estado; mudança de mundo ou entidade observada reinicia a interpolação.
- O HUD seleciona TNTs e prepara os textos por tick; a renderização usa o resultado somente para o mesmo mundo e jogador. O limite de menu vale sem mundo carregado; telas dentro do jogo preservam o limite de gameplay.
- `DollyRuntimeTest` roda em JVM isolada com dispositivos determinísticos em `src/test/stubs/` e substitutos dos recursos GL. `DollyPatchTest` verifica os pontos de integração no JAR final. Esses testes não exercitam um driver OpenGL real, e os dispositivos de teste não são empacotados no cliente.

## Grupo 1 de desempenho

`DollyRenderedText` reaproveita o desenho nativo de glifos por fonte; `DollyFontAccess` é implementado no build. `DollyModelBatch` limita agrupamento/culling ao loop de cubos de modelos, preservando sprites M5 e shaders. `DollyPackIcons` redimensiona apenas ícones. `GroupOnePatches` concentra os hooks e as limpezas de encantamentos/navegação; o registro de redstone passa a usar um mapa fraco de mundos. Veja [comportamento e validação](mods/grupo-1.md).

`DollyWorldObjects` e `WorldObjectPatches` tratam apenas cópias/balanço dos itens no chão e o desenho do livro de encantamento. A política de candidatos ao culling permanece em `DollyCulling`, com exclusão antecipada de todos os jogadores. Veja o [grupo 2](mods/grupo-2.md).

O [grupo 3](mods/grupo-3.md) adiciona `DollyZoom` (estado/roda/sensibilidade), `DollyCameraTrace` (consulta exclusiva da câmera e políticas de Parallax/céu) e `CameraLightingPatches`. O horário é aplicado nas leituras diretas de WorldInfo no mundo cliente, incluindo o cálculo celestial; o servidor não é modificado.

`DollyVisuals` concentra as políticas do [grupo 4](mods/grupo-4.md); `VisualCleanupPatches` conecta overlays de fogo/água, névoa subaquática, spawn visual de poções, fragmentos e os renderizadores de raios/projéteis/modelo da montaria. O estado gráfico da transparência é restaurado em `finally`, e as atualizações de poções/projéteis permanecem nativas.

O [grupo 5](mods/grupo-5.md) usa `dolly.audio.SoundVolumes` para preferências validadas, `DollyAudio` para multiplicadores/recargas e `DollyAudioScreen` para controles individuais. `AudioPatches` aplica o multiplicador ao volume normalizado, expõe os eventos registrados via `DollySoundRegistry` e separa os atalhos F3+S/F3+T. A recarga visual preserva o carregador de áudio com um escopo por thread restaurado em `finally`.

O [grupo 6](mods/grupo-6.md) amplia `ChatHistory` e adiciona a fila independente `dolly.chat.MessageQueue`. `DollyChatSending` intercepta somente o envio de chat do jogador local, usa a identidade da conexão e cancela ao descarregar o mundo. `DollyChatActions` inspeciona componentes sem alterar cliques; `DollyImagePreview` mantém download/decodificação fora da thread gráfica e upload/descarte das texturas nela. `DollyTabPing` substitui apenas o desenho das barras no TAB. Todos os hooks são verificados em `ChatPatches` e nos testes do JAR final.

Os [grupos 7](mods/grupo-7.md) e [8](mods/grupo-8.md) adicionam `DollyScreenshotManager`/`DollyScreenshotScreen` para controles locais por arquivo, `DollyPicture` para a miniatura com textura de ciclo explícito, `DollySlider`/`DollyNumeric` para controles numéricos compartilhados e `DollyNavigation`/`DollyChoiceScreen` para navegação e confirmação. `TechnicalLoadingPatches` conecta o retorno de sucesso do screenshot nativo e `MenuPatches` trata os quatro menus/construtores de navegação. Valores são aplicados durante o arraste e persistidos ao soltar/fechar; escolhas de modo permanecem botões.

O [grupo 9](mods/grupo-9.md) usa `MaintenancePatches` para os hooks de FPS/gráfico, céu, poção e pedidos de packs. `DollyResourcePacks` valida o pedido antes de qualquer consulta nativa a arquivos, com regras independentes em `dolly.security.ResourcePackPolicy`. `DollyDebug`/`FrameHistory` registram quadros fora da sessão do profiler e exibem métricas apenas no debug. `DollyMaintenance` inicia uma única limpeza de logs em segundo plano; `LogCleanup` restringe nomes, idade, diretório e tipo de arquivo. A redução ambiente existente e o Mouse Delay Fix não recebem patches duplicados.

## TACTILE e perfis por servidor

`dolly.tactile.core` e `dolly.tactile.client.input` preservam física e fontes de teclado do TACTILE. `DollyTactileEngine` adapta o motor ao binário; `DollyTactileWorld` consulta colisões vanilla e `DollyTactileSupport` determina fallbacks. `ServerProfiles` valida a persistência e restringe endereços Hypixel à física vanilla. `DollyTactile` gerencia as transições e `DollyTactileScreen` configura perfis. `TactilePatches` liga tick, movimento e render no build; o caminho vanilla permanece delegado para entidades não controladas. Veja [escopo e limites](mods/tactile.md).
