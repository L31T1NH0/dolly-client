# Otimizações integradas ao Dolly

Esta implementação seleciona mecanismos de FoamFix, Krypton, EntityCulling e MCOpt Linux para a base Minecraft 1.8.9/Java 8 com OptiFine M5. Usa código próprio e patches no build; os JARs de referência, Forge, Fabric, Sodium e bibliotecas nativas novas não entram no runtime.

## Recursos e padrões

| Origem da ideia | Implementação | Padrão e controle |
| --- | --- | --- |
| FoamFix | Compartilha arrays de vértices iguais entre quads estáticos após o bake dos modelos | Automático em cada recarga de recursos |
| Krypton | Decoder sem arrays temporários de cabeçalho, fatias retidas do buffer em vez de cópias e escrita/tamanho de VarInts otimizados | Automático; preserva protocolo e Netty 4.0.23 |
| EntityCulling | Evita desenho de mobs/suportes de armadura/itens/TNT e tile entities selecionadas quando uma barreira opaca cobre a caixa inteira | Ligado; `entityCulling`, botão **Ocultar objetos cobertos** |
| MCOpt | Combina faixas de quads consecutivas e contíguas no multi-draw do Render Regions | Ligado; `mergeTerrain`, botão **Unir faixas do terreno** |
| MCOpt | Limita a tela de download do terreno a 30 FPS, inclusive quando o mundo já foi criado | `loadingFps=30`, botão **FPS ao carregar** |

Os controles ficam no menu Dolly e são salvos em `config/dolly.properties`. Configurações antigas recebem os novos padrões; restaurar padrões volta a ligar culling/fusão e retorna carregamento a 30 FPS. O limite de carregamento respeita um limite nativo menor e deixa de valer imediatamente ao sair da tela de download de terreno. Menus comuns durante a partida continuam usando o limite de gameplay.

## Jogadores e correção visual

**Jogadores locais e remotos nunca são candidatos ao culling do Dolly.** O filtro usa a classe-base de jogador antes de consultar mundo, nome ou caixa; nomes e corpos mantêm o caminho original do renderizador. Nada muda em ticks, colisão, mira, ataques ou pacotes.

O culling permite entidades nomeadas, incluindo mobs, itens e TNT, e suportes de armadura usados para hologramas; preserva montarias, passageiros e mobs com laço. Entidades com render especial fora dessa seleção não são ocultadas. Nomes de não jogadores podem desaparecer junto do objeto ocultado. Veja os controles do [grupo 2](grupo-2.md). Para tile entities, a seleção inclui baús, ender chests, placas, crânios, banners e mesas de encantamento, com caixa ampliada. Beacons, portais, pistões móveis e tipos desconhecidos mantêm seu caminho original.

Terceira pessoa/Perspective, espectador, sono, view bobbing, câmera de debug, outra entidade observada e passes de sombra dos shaders liberam a visibilidade. Isso evita assumir uma origem de câmera que esses modos deslocam.

## Culling conservador e limitado

Em vez de transportar a thread que lê listas/mundo vivos da referência, a implementação consulta blocos na thread de renderização. Um teste só oculta quando encontra uma seção plana de cubos opacos cobrindo **toda a projeção da caixa**; falha aberta em buracos, dados não carregados, caixas grandes/inválidas e orçamento esgotado. A caixa das entidades inclui uma margem de interpolação e apresentação. A projeção também engloba uma margem de 0,25 bloco em torno dos olhos para pequenos deslocamentos da câmera em primeira pessoa.

O orçamento é compartilhado por frame: até 512 consultas de blocos e prazo cooperativo de 0,75 ms a partir da primeira consulta. O prazo é conferido entre consultas; uma consulta individual ainda pode ultrapassá-lo. A distância de teste é de 8 a 64 blocos. Não há resultado de visibilidade reaproveitado de um frame anterior: mover a câmera, quebrar paredes, teleportar ou trocar mundo não mantém uma decisão antiga em cache.

Este algoritmo é deliberadamente mais conservador que a biblioteca da referência: pode deixar desenhados objetos escondidos por vários obstáculos que não formam uma barreira completa. Também não transporta a proteção temporal de um segundo, pois não mantém resultados entre frames. O ganho de FPS depende da cena e do custo economizado.

Arquivos: `DollyCulling.java` e `dolly/render/Occlusion.java`. Hooks: `biu.a(Lpk;DDDFFZ)Z`, `bhc.a(Lakw;DDDFI)V` e início do frame em `bfk.a(FJ)V`.

## Memória de modelos

`DollyModels` visita modelos cuja classe exata é `bow` e quads cuja classe exata é `bgg`. Subclasses e implementações dinâmicas ficam intactas. Compara todo o conteúdo do array, inclusive colisões de hash; compartilha apenas dados iguais após sua construção. O pool e o conjunto de visitas são locais à recarga e não retêm o resource pack antigo.

O ponto de integração é o retorno de `bou.a(bni)`, após atualizar o registro e os estados dos modelos. O caminho de quebra (`bgn`) copia os vértices antes de alterar UVs; os caminhos examinados de connected/natural textures também trabalham com cópias. A deduplicação não compartilha listas mutáveis, objetos de quad ou metadados de sprite. `savedBytes` é uma estimativa dos arrays dispensados na última recarga, não uma medição do heap.

**Limite:** essa premissa se refere ao runtime fechado com OptiFine M5 examinado. Acrescentar um consumidor que escreva no array de um quad estático exige copiar antes de escrever ou revisar a deduplicação. Trocas de formato de vértice por shaders podem desfazer parte do compartilhamento sem alterar o conteúdo visual.

Não foram transportados registros FML, cache LaunchWrapper, mapper compacto de estados ou loader Forge de itens sob demanda. Essas estruturas não são o alvo desta adaptação.

## Rede e terreno

`DollyNetwork` substitui o corpo de `eq.decode` e dois métodos de `em`. Aguarda cabeçalho/corpo completos sem consumir um frame parcial, mantém o limite de três bytes do cabeçalho e retém a fatia entregue ao próximo decoder. O receptor libera essa referência pelo ciclo normal do Netty. VarInts preservam zero, negativos, limites de int e ordem dos bytes, inclusive buffers little-endian.

Não há consolidação de flushes, alteração de ordem/frequência dos pacotes, compressão/cifra nativa, troca de Netty ou promessa de melhora de ping/hit registration.

`DollyTerrain` intercepta o multi-draw do `VboRegion`. Só une `GL_QUADS`, faixas completas e adjacentes na sequência original e na memória, respeitando os limites de int. Preserva a ordem de transparências, não reorganiza geometria e restaura os limites dos buffers após o desenho. Com shaders, mantém as faixas originais para preservar também semânticas dependentes de cada subdraw. VBO simples e display lists continuam nos seus caminhos nativos. Não foi acrescentado método A/B do MCOpt.

## Verificação e teste em jogo

O build verifica hooks e liga as classes alteradas sob `-Xverify:all` no Java 8. Os testes cobrem:

- VarInts contra encoder de referência, valores de fronteira, negativos, aleatórios e ambas as ordens de buffer.
- Frames vazios, pequenos/grandes, fragmentação de cabeçalho/corpo, vários frames na mesma entrada, cabeçalho inválido e vida útil das fatias após fechar o canal.
- Equivalência dos vértices e ordem antes/depois da fusão em casos aleatórios, faixas não contíguas e primitivas incompatíveis.
- Paredes completas, buracos, barreira atrás do objeto, coordenadas negativas, três eixos e orçamento interrompido no teste de oclusão.
- Exclusão de jogadores locais/remotos, deduplicação de modelos e isolamento do overlay de quebra, persistência/reset e saída do limite de carregamento.

Essas verificações não executam um driver OpenGL nem demonstram ganho de FPS/RAM na máquina do usuário. Validar em jogo com jogadores atrás de paredes, mobs e itens próximos a cantos, placas/baús duplos, paredes quebradas/colocadas, Perspective, troca de servidor, resource packs, connected/natural textures e shaders. Ficar atento a desaparecimentos e regularidade dos frames, além do FPS médio.
