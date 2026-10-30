# MCOpt Linux: avaliação para PvP multiplayer no Dolly

Posteriormente foram integradas adaptações de fusão de faixas e limite de carregamento, sem o método A/B. Consulte [integração](integracao.md); este relatório preserva a avaliação que precedeu a implementação.

## Decisão

**Não adicionar o MCOpt Linux inteiro ao Dolly.** A cópia local contém ideias aproveitáveis, mas foi construída para Minecraft 26.3, Java 25 e Sodium 0.9.3. O Dolly é 1.8.9/Java 8 com OptiFine M5. A alternativa útil é selecionar mudanças pequenas quando houver um custo equivalente comprovado na nossa base.

Para PvP multiplayer, a ordem deve ser estabilidade do tempo de frame, apresentação correta de jogadores e atualização rápida do cenário. Otimizações da geração do mundo, gravação em disco, POIs e random ticks do servidor não aceleram o servidor remoto quando instaladas somente no client.

## Alvo realmente examinado

- Pasta indicada pelo usuário: `/home/leite/Documents/GitHub/mcopt-linux`.
- HEAD local: `b0253f4f7a5251e22dbed491691993b59e6c1485`.
- A árvore contém muitas alterações locais, inclusive arquivos novos e remoção do renderer Apple. As conclusões referem-se aos arquivos presentes, **não apenas ao commit HEAD** e não ao estado atual do upstream no GitHub.
- O fork local é Linux/OpenGL/Vulkan, não o renderer Metal do MCOpt upstream. `fabric.mod.json` ainda usa o ID `mcopt-metal`, mas o nome é `mcopt Linux`.
- Lidos: metadados/build, profile Linux e seleção dos mixins; fontes dos mecanismos listados abaixo; verificações existentes de desempenho/equivalência; pontos correspondentes do Dolly e bytecode do OptiFine integrado.
- Não foi executado build do MCOpt, mod, jogo ou benchmark. Não foi alterado o checkout do MCOpt. Relatório antigo `AUDITORIA-TECNICA.md` descreve um estado anterior com Metal e 155 fontes; a árvore atual tem 83 arquivos Java em `metal/src/main/java`. Ele não foi tratado como prova do comportamento atual.
- Hashes e recortes comparativos estão em [evidence/mcopt-manifest.json](evidence/mcopt-manifest.json) e [evidence/mcopt-dolly-overlap.bytecode.txt](evidence/mcopt-dolly-overlap.bytecode.txt).

## Avaliação por mecanismo

| Mecanismo observado | Relevância no multiplayer | Decisão para o Dolly |
| --- | --- | --- |
| Unir faixas contíguas de terreno (`DrawMerge`) | Pode reduzir trabalho de subdraws quando o gargalo está no terreno visível | Melhor candidato para protótipo, adaptado ao Render Regions, após medir contiguidade e custo |
| Limitar FPS na tela de carregamento | Pode liberar CPU durante entrada/troca de servidor, não durante o combate | Pequena melhoria possível; verificar a tela e o loop 1.8.9 antes de portar |
| Modelos com laço indexado (`ModelLoopMixin`) | Seria útil se o render de entidades criasse iteradores repetidamente | O caminho equivalente de compilação do OptiFine já usa índice; não adicionar patch duplicado |
| Reutilizar caixa de visibilidade (`EntityBox`) | Evita calcular duas vezes a mesma caixa, sem esconder jogadores | Resolve uma duplicação Sodium + vanilla; não foi demonstrado o mesmo par de chamadas no Dolly |
| Cache da última região (`ListRegionCacheMixin`) | Reduz buscas por hash durante a construção das listas visíveis | Acoplado às listas/regiões do Sodium; exigir perfil e equivalente no OptiFine |
| Recuperar/reutilizar passes de culling (`CullReuse`) | Pode economizar CPU ou evitar atualizações visuais perdidas | Não portar o scheduler Sodium; invalidação errada é especialmente ruim em PvP |
| Arrays AIR, limites de slice, biomas e callbacks reutilizados | Podem reduzir custo de reconstrução de chunks recebidos do servidor | Ideias válidas, implementações dependem de estruturas modernas/Sodium ausentes no Dolly |
| Visibilidade por máscaras (`VisSweep`) | Pode acelerar o cálculo de visibilidade dos chunks | Não substitui diretamente o flood fill 1.8.9; semântica direcional precisa de prova separada |
| Limpeza por prefixo do cache de snapshots | Evita varrer entradas ainda válidas | Específica ao cache LRU de clones do Sodium; sem alvo equivalente comprovado no Dolly |
| Cache do offset vertical do chunk | Reduz cadeia de consultas de altura em versões modernas | A base 1.8.9 examinada já indexa a seção por `y >> 4`; não tem a cadeia-alvo |
| Mapas de iluminação copy-on-write | Pode diminuir cópias de snapshots de luz no client moderno | Não é o mesmo motor de luz da 1.8.9; trazer o mapa isoladamente não instala a otimização |
| Rust para geração de terreno | Atua em geração executada localmente/servidor instalado | Não priorizar para client conectado a servidor externo |
| POI, random ticks, gravação de regiões e codecs de paleta | Principalmente servidor/mundo local; as estruturas também mudaram | Excluir desta seleção PvP do Dolly |
| Narrador, cache de shapes e fade do startup | Inicialização/interface, sem ganho de combate demonstrado | Sem prioridade; narrador moderno e cache de shapes não são alvos equivalentes na 1.8.9 |
| Ferramentas A/B e verificações de equivalência | Úteis para distinguir economia real de regressão visual | Aproveitar o método usando o profiler já existente no Dolly |

## 1. Candidato principal: faixas contíguas de terreno

`cpu/DrawMerge.java`, métodos `merge` e `mergeVulkan`, preserva a ordem dos comandos e só une faixas contíguas de quads completos. No caminho OpenGL, exige ponteiro de índices zero e base seguinte igual à base atual acrescida dos vértices anteriores; respeita o limite do index buffer. `mixin/cpu/DrawMergeMixin.java` intercepta `GLDrawBatch.draw` do Sodium e usa o resultado apenas quando há fusão.

Isso **já atua dentro de um multi-draw**: não é correto vender a mudança como a introdução de batching em um renderer que não o tinha. A economia é de faixas/subdraws e trabalho associado; não necessariamente do número de chamadas Java/OpenGL.

Na nossa base, `VboRegion.drawArrays`, offsets 64–87, coloca posição e quantidade nos buffers; `finishDraw`, 24–36, entrega ambos a `bfl.glMultiDrawArrays`. O OptiFine já agrupa render por região. O recorte examinado não combina faixas adjacentes antes dessa entrega, portanto existe uma pergunta concreta para um protótipo: quantas faixas consecutivas também são contíguas na memória e quanto custa uni-las?

Uma adaptação precisaria trabalhar com `glMultiDrawArrays`/vértices do OptiFine, e não copiar o código de índices/baseVertex do Sodium ou Vulkan. Deve manter modo de primitiva, ordem, região, alinhamento, limites e estados gráficos. Não ordenar os comandos de transparência para aumentar fusões.

**Aceitar somente com:** imagem equivalente, mesmos vértices na mesma ordem, testes de faixas separadas/contíguas/vazias e ganho de tempo de frame sob carga representativa. Poucas faixas ou quase nenhuma adjacência podem fazer a varredura custar mais do que economiza. Não há percentual de ganho medido no Dolly.

## 2. Limite de FPS durante carregamento

O profile Linux estabelece `mcopt.cpu.loadingFps=30`. `LoadingFpsMixin` limita `FramerateLimitTracker.getFramerateLimit` apenas em `LevelLoadingScreen`, mantendo o tick responsável por fechar a tela.

`DollyFrameLimiter.frameLimit` limita menus sem mundo e janela inativa. Com mundo presente, devolve o limite de gameplay; portanto uma tela de carregamento com mundo já criado merece investigação. Entretanto, não foi demonstrado que o loop moderno e a tela 1.8.9 passam pelo mesmo ponto durante toda a conexão.

Seria uma alteração pequena para evitar competição de CPU durante o carregamento, desde que identificada a tela exata e garantida a retomada imediata do limite de gameplay. Não atribuir ganho de FPS em duelo a esse limite.

## 3. Otimizações que parecem boas, mas não se transferem diretamente

### Modelos dos jogadores

`ModelLoopMixin` troca iteração de `ModelPart.cubes` por `for` indexado, mantendo as chamadas e sua ordem. É relevante procurar custos no desenho dos jogadores, que continuarão renderizados por exigência do usuário.

No OptiFine M5, porém, `bct.d(float)` já percorre os cubos por `List.size/get`, offsets 32–68, dentro de `glNewList/glEndList`. `bct.a(float)`, 14–27, só chama essa compilação quando o display list não está pronto. Trata-se de um desenho diferente do fluxo moderno: esse patch específico não elimina o iterador anunciado na nossa base. Outros custos de animação/render de jogadores permanecem possíveis, mas precisam ser encontrados no profiler.

### Caixa de entidade e culling

`EntityBox.put/take` guarda uma caixa de uso único vinculada a renderer, entidade, partial tick e thread. Os mixins fazem a ponte entre `SodiumWorldRenderer.isEntityVisible` e `EntityRenderer.shouldRender`. Isso não é ocultação adicional: reutiliza o mesmo resultado de cálculo.

O Dolly não usa Sodium; acrescentar o slot sem a duplicação original comprovada introduziria estado sem economia demonstrada. Se futuramente houver um teste próprio de visibilidade que repita o cálculo, a técnica pode ser reavaliada. Ela não exige ocultar jogadores.

`CullReuse` é outro mecanismo: trata árvores e scheduler assíncrono de chunks do Sodium, incluindo gerações do grafo, câmera, distância, vizinhança e tarefas pendentes. O profile ativa `cullRecover`, enquanto `cullReuse=true` não é ativado por esse profile. Não confundir recuperação de atualização cancelada com pular culling por padrão. Em arenas com blocos quebrados/colocados, qualquer cache adaptado precisa invalidar imediatamente o resultado relevante e não manter paredes obsoletas na imagem.

### Visibilidade direcional e chunks

`VisSweep.reach` calcula caminhos **monotônicos**, limitados a um conjunto de direções, para substituir `DirectionalVisGraph.resolveWithDirections` do Sodium. A existência de um bitset de 4096 blocos em outra versão não prova equivalência de conectividade. Não usar essa rotina como substituição direta de um flood fill geral.

`LevelSliceAirMixin`, `LevelSliceBoundsMixin`, `BlockRendererMixin` e `LevelBiomeSliceMixin` reduzem trabalho real de meshing no Sodium. Esse tipo de trabalho também importa em multiplayer quando chunks chegam ou blocos mudam; multiplayer não torna todo trabalho de chunks irrelevante. O que não se transfere é o patch pronto para estruturas como `LevelSlice`, `PlatformModelEmitter` e biomas modernos.

`ChunkAccessSectionMixin` elimina consultas modernas a altura mínima. No bytecode local 1.8.9, `amy.g(cj)`, 67–98, já verifica Y e acessa o array de seções usando deslocamento de quatro bits. `LightMapMixin` substitui o mapa de `DataLayerStorageMap`, uma estrutura do motor moderno; isso não otimiza automaticamente os arrays de iluminação da base antiga.

## 4. O que fica fora para multiplayer

`GenNoiseChunkMixin` redireciona o conjunto de samplers usado por `NoiseChunk`; `GenServerNoise` é a entrada para geração de terreno do servidor. A biblioteca Rust usa `java.lang.foreign`, incompatível com Java 8 sem outra ponte. Colocar geração nativa no client não acelera a geração que o servidor remoto executa nem a entrega dos chunks por sua conexão.

`ServerLevelTickMixin`, `AcquirePoiMixin` e `RegionFileStorageMixin` atendem a tarefas de simulação/armazenamento do mundo. Podem fazer sentido em outro projeto de servidor, mas não justificam aumentar o runtime do Dolly PvP. O próprio fork possui um artefato de servidor separado. Seus comentários e README deixam ganhos na máquina-alvo sem medição; os números de fixtures/Mac presentes em comentários não são benchmark deste client.

## Validação e recomendação prática

1. Usar o profiler atual para separar `render.entities`, terreno, rebuild de chunks, chamadas GL e pausas de memória. Priorizar p95/p99 de frame e regularidade de apresentação, além da média.
2. Se o terreno for o custo relevante, medir faixas de Render Regions e prototipar fusão apenas quando houver adjacência suficiente.
3. Verificar o limite durante conexão/troca de servidor como melhoria isolada de carregamento.
4. Se jogadores forem o gargalo, investigar animação e render reais da 1.8.9 mantendo-os visíveis; o patch de laço do MCOpt não resolve um custo já removido pelo OptiFine.
5. Comparar cenas de duelo, lobby com muitos jogadores, movimentação com chunks chegando e minigame com alterações de blocos. Mesma configuração/câmera, aquecimento e múltiplas passagens; perfil detalhado também tem overhead.

As ferramentas `PerformanceChecks.java` já contêm verificações de contrato de mapas, isolamento de snapshots, equivalência de comandos Vulkan e identidade da caixa reutilizada. Elas ajudam a entender os invariantes, mas não foram executadas e não validam a integração 1.8.9. O Dolly já possui coleta de tempos, contadores e percentis aproximados; não é necessário importar outro HUD/profiler inteiro.

O `NOTICE` local distingue Apache-2.0 de arquivos/trechos derivados do Sodium sob PolyForm Shield. Uma eventual reutilização deve preservar os avisos específicos; a seleção técnica acima não assume uma licença uniforme para todos os fontes.

**Conclusão:** usar o MCOpt Linux como referência seletiva. Hoje não há evidência que justifique incorporar o pacote, Rust, Sodium ou trocar o renderer. Há um candidato concreto para render do terreno e uma pequena melhoria possível de carregamento; o restante requer outro alvo, já tem equivalente ou não atende ao custo do client multiplayer. Nenhuma otimização foi integrada nesta avaliação.
