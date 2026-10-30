# EntityCulling 1.5.0 para Minecraft 1.8.9

## O que vale aproveitar

O candidato mais interessante é evitar desenhar entidades e tile entities que estão completamente escondidas por blocos opacos. Isso pode reduzir trabalho de renderização em lobbies, bases e ambientes fechados com muitos objetos. O ganho depende da cena e do gargalo; esta análise estática não mediu FPS nem tempo de frame.

A ideia complementa o trabalho de VBO/Render Regions: o código examinado decide se um objeto chega ao renderizador, enquanto as opções de geometria do terreno têm outra responsabilidade. O Dolly ainda não tem um módulo próprio equivalente nos adaptadores e patches examinados. Compatibilidade efetiva com todos os caminhos do OptiFine M5 permanece por validar.

## Artefato e cobertura

- Entrada: `references/modrinth-mods.zip!/entityculling-forge-mc1.8.9-1.5.0.jar`.
- SHA-256 do JAR: `36c82bfedcc8d34744006fe794bfec9e951db3589b264444507b07c5f331fd6d`.
- Inventário: 2.475 classes, das quais 13 em `dev/tr7zw/entityculling` e sete em `com/logisticscraft/occlusionculling`. Os demais grupos incluem Mixin, Google e Apache; não representam 2.475 classes de lógica de culling.
- Classes próprias e de oclusão têm major version 52, compatível com Java 8.
- Examinados: bytecode das 20 classes do mod/biblioteca de oclusão, anotações de três mixins, manifesto e refmap. Nenhum código do mod ou do jogo foi executado.
- Evidências: [bytecode](evidence/entityculling.bytecode.txt), [hooks e anotações](evidence/entityculling-hooks.verbose.txt), [metadados e mapeamentos](evidence/entityculling-metadata.txt). As posições citadas abaixo são offsets de bytecode dentro de cada método, não linhas do fonte original.

## Mecanismo confirmado no bytecode

`EntityCullingModBase.onInitialize`, offsets 105–185, cria uma `OcclusionCullingInstance`, uma `CullTask` e inicia a thread `CullThread`. `clientTick` e `worldTick` solicitam novos cálculos; a thread também considera mudança de posição (`CullTask.run`, 100–158). A espera configurável limita a frequência de sondagem, mas não garante um orçamento fixo de CPU por passagem.

`CullTask.run` percorre tile entities e entidades carregadas. Para tile entities próximas, usa uma caixa unitária na posição do bloco (329–447); para entidades usa seu AABB (607–760). `OcclusionCullingInstance.isAABBVisible` expande os limites em 0,5 bloco, percorre voxels relevantes e consulta visibilidade por raios. `isVoxelVisible` seleciona pontos próximos aos cantos do voxel (0,05/0,95); `isVisible` prepara os raios; `stepRay` atravessa a grade e interrompe o raio ao encontrar bloco opaco (`stepRay`, 0–176).

`Provider.isOpaqueFullCube`, 0–29, consulta o estado do bloco e `Block.func_149662_c()` (`isOpaqueCube`). Trata-se de consulta de opacidade do mundo pela CPU. Não é uma consulta de oclusão à GPU e não equivale a desligar toda entidade fora da tela.

O cache codifica quatro posições por byte, com dois bits por posição (`ArrayOcclusionCache`). A fórmula do construtor é `(2 * reach)^3 / 4`: com o alcance padrão 128, são **4.194.304 bytes**, aproximadamente 4 MiB, só para esse array. `CullTask.run`, 186–190, limpa o cache a cada passagem. A memória e o custo de limpar aumentam de forma cúbica com o alcance.

## Comportamentos que merecem ser preservados

| Proteção/padrão | Evidência |
| --- | --- |
| Módulo global começa ligado | `EntityCullingModBase.<clinit>`, 10–11 |
| Entidade visível fica protegida por 1 segundo contra ocultação imediata | `CullableMixin.setCulled(false)`, 5–10; `setTimeout`, 0–8; hooks consultam `isForcedVisible` |
| Nomes elegíveis continuam renderizados quando o corpo é ocultado | `WorldRendererMixin.doRenderEntity`, 36–67; configuração `renderNametagsThroughWalls=true` |
| Beacon fica na whitelist | `Config.<init>`, 14–34, valor `tile.beacon` |
| Alcance de entidades 128; caixas com dimensão maior que 50 não são ocultadas | `Config.<init>`, 37–58; `CullTask.run`, 563–682 |
| Tile entities são examinadas a menos de aproximadamente 64 blocos | `CullTask.run`, 336–357, distância ao quadrado `< 4096` |
| Armor stands marcadores são excluídos do culling por padrão | `Config.<init>`, 61–63; `isSkippableArmorstand`; `run`, 538–560 |
| Espectador e terceira pessoa passam a visíveis | `CullTask.run`, 193–224, 313–326 e 538–560 |
| Aguarda mais de dez ticks de vida do jogador | `CullTask.run`, 41–53 |
| `sleepDelay=10` ms; debug desligado | `Config.<init>`, 44–52 |

`skipMarkerArmorStands` significa pular o **teste de ocultação**, preservando o render do marcador. Não significa apagar armor stands do jogo. O tratamento de nomes consulta a regra original `shouldShowName`; não força nomes que o renderizador normalmente esconderia.

## Integração possível no Dolly

Os mixins injetam no início dos métodos de render e cancelam a chamada quando o resultado é oculto. O refmap fornece os alvos 1.8.9 ofuscados:

- `RenderManager`: `biu.a(Lpk;DDDFFZ)Z`, equivalente a `doRenderEntity`.
- `TileEntityRendererDispatcher`: `bhc.a(Lakw;DDDFI)V`, equivalente a `renderTileEntityAt`.
- `CullableMixin` aplica estado a `Entity` e `TileEntity`.

O Dolly já aplica ASM durante o build em `tools/java/PatchClient.java`, portanto uma adaptação poderia criar hooks próprios nesses pontos sem adicionar Forge ou Mixin ao runtime. Isso é uma proposta arquitetural; os métodos finais no JAR combinado com OptiFine precisam ser conferidos antes de implementar. O manifesto do mod original usa MixinTweaker e eventos Forge, então simplesmente incorporar o JAR não resolve a integração.

Uma primeira implementação deve preservar as regras de nomes e beacon, favorecer visibilidade quando o resultado estiver ausente/velho e ter alternância para comparação. O contador de objetos renderizados/ignorados e tempo da última passagem em `DebugHudMixin` também é útil para validar se o custo da thread compensa o render evitado.

## Cuidados concretos antes de portar

1. **Perspective e câmera:** `DollyCamera.updateFreeLook` ativa terceira pessoa. O mod de referência libera o culling nessa situação; `getCameraPos` retorna a posição dos olhos da entidade nos dois ramos. Manter a exclusão inicialmente é coerente. Para otimizar Perspective futuramente, calcular a posição efetiva da câmera, incluindo afastamento e colisão.
2. **Concorrência:** a thread lê mundo/listas vivos e grava campos como `culled` e `requestCull`, declarados sem `volatile` nesse artefato. `run` captura `ConcurrentModificationException`/`NullPointerException` durante iteração. Capturar essas exceções não demonstra segurança de publicação entre threads. Para Dolly, definir snapshots/publicação de resultados e descarte ao trocar de mundo, em vez de transplantar esse modelo sem revisão.
3. **Custo:** o provider aloca `BlockPos` em cada consulta de opacidade. Cache, vetores reutilizados e intervalo de atualização são ideias úteis, mas alcance maior pode custar mais do que renderizar. Medir em cenas abertas e fechadas, com objetos visíveis e ocultos.
4. **Correção visual:** testar aproximação de cantos, destruição de paredes, TNT, placas, baús duplos, nomes, teleporte e troca de mundo. A caixa unitária usada para tile entities merece atenção quando a geometria renderizada ultrapassa o bloco. A biblioteca retorna visível em caso de exceção (`isAABBVisible`, 613–621); a integração deve preservar uma política conservadora equivalente.
5. **Render separado de simulação:** os hooks observados cancelam desenho. Não há base aqui para remover ticks, entidades da lista, colisão, ataques ou processamento de rede.

Prioridade sugerida: protótipo conservador de oclusão com medição de tempo de frame e contadores, depois expansão de cobertura. Aproveitar a biblioteca pura e hooks mínimos é mais adequado à base atual do que carregar toda a infraestrutura incluída no JAR.

O JAR contém `LICENSE` MIT com copyright LogisticsCraft e `LICENSE.txt` MIT; também inclui licenças/notice de bibliotecas empacotadas. Esses arquivos são evidência local da distribuição recebida, não uma auditoria de autoria de cada classe. Ao incorporar código, preservar os avisos correspondentes e limitar dependências ao que for realmente usado.
