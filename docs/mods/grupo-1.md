# Grupo 1: desempenho e memória

Adaptação dos mecanismos selecionados de Patcher/PolyPatcher para o runtime fechado Minecraft 1.8.9 + OptiFine M5. Não instala Forge, Essential, OneConfig ou os mods de referência. Os hooks são aplicados na compilação.

## Recursos

| Controle em Desempenho | Padrão | Efeito e limites |
| --- | --- | --- |
| Textos otimizados | Ligado | Reaproveita o desenho de textos com display lists limitadas a 512 entradas por fonte, mantendo os glifos, cores, estilos e Unicode do OptiFine. Textos ofuscados, longos, subclasses de fonte, shaders e desenho dentro de outra display list mantêm o caminho nativo. |
| Cache do HUD / Hz do HUD | Ligado / 24 | Atualiza a imagem reaproveitada da interface até 24 vezes por segundo. Mudança de slot invalida a imagem; mira, vinheta, portal e abóbora permanecem no caminho dinâmico. Fast Render ou shaders podem tornar o cache indisponível; nesse caso o desenho é normal. |
| Agrupar modelos | Ligado | Agrupa as faces do loop de cubos na preparação das display lists dos modelos. O loop de sprites do OptiFine permanece separado. Shaders preservam o formato e caminho nativos. |
| Faces externas | Ligado | Ativa back-face culling apenas durante o desenho dos cubos dos modelos, restaurando o estado gráfico do chamador. Não oculta entidades inteiras ou nomes. Pode deixar superfícies invisíveis quando a câmera entra em um modelo, como esperado nessa opção. |
| Modelos de itens | Ligado | Usa os contornos mesclados já implementados no Dolly também no modo padrão, preservando buracos, transparências e bordas de cada frame animado; compensa o encolhimento de UV do M5. Os modos explícitos Outline e Unlerp continuam disponíveis. Alterar o controle recarrega os recursos. |
| Luz fixa de partículas | Ligado | Substitui a consulta de luz por iluminação constante no renderizador base de partículas. Subclasses com desenho próprio permanecem nativas. |
| Ícones leves dos packs | Ligado | Reduz imagens de packs maiores que 64×64 para 64×64, preservando alpha. Não muda as texturas do mundo. Ícones já carregados mudam quando forem recarregados. |
| Menos animações ambiente | Ligado | Limita a amostragem visual de ambiente de 1000 para 100 por chamada, conforme o código atual do PolyPatcher (a descrição antiga do README mencionava 500). Não altera os ticks de gameplay. Com Partículas ambiente desligado, o controle não tem efeito visível. |

Os controles ficam na aba Desempenho e são persistidos em `config/dolly.properties`. Novas instalações e restaurar padrões usam os valores acima; valores previamente salvos continuam sendo respeitados. Restaurar padrões libera os caches de texto. Alternar batch/culling recompila as display lists quando os modelos forem usados novamente.

## Memória

- Iteradores estáticos de encantamento liberam referências a entidades e fontes de dano na saída normal e excepcional, preservando o resultado dos cálculos.
- O processador de navegação libera o acesso ao mundo ao concluir a busca.
- O registro de mudanças de tochas de redstone usa chaves fracas para não manter mundos descarregados vivos.
- O OptiFine M5 já remove do gerenciador os registros de texturas excluídas; essa correção foi preservada, sem duplicação.
- As listas de texto são liberadas em recarga de fonte, mudança de Unicode/bidi, troca de mundo, reset, desligamento da opção e encerramento. O limite evita crescimento ilimitado por mensagens únicas.

As otimizações de descoberta/carregamento de mods e modelos do Forge não se aplicam. A redução de geometria do ItemLayerModel do Forge foi adaptada usando o gerador de contornos do Dolly, sem introduzir estruturas do Forge.

## Validação

O build executa regressões de preferências/reset, geometria de itens sólidos, vazados e animados, estado das fontes, tamanho/alpha dos ícones, iluminação nativa/fixa das partículas, relógio de 24 Hz do HUD, liberação de referências de encantamentos (inclusive com erro), descarte do acesso ao mundo e mapa fraco de redstone. Também verifica os hooks no JAR final e a ligação/verificação das classes em Java 8.

Não foi possível abrir o display `:0`. Os testes não medem FPS nem validam um driver OpenGL real. Conferir no jogo textos coloridos/Unicode/ofuscados, fontes de resource packs, HUD e slots, skins/armaduras/capas, shaders, sprites de modelos personalizados, itens animados e troca de packs.

## Referências

- [Patcher: fonte e cache de textos](https://github.com/Sk1erLLC/Patcher/blob/master/src/main/java/club/sk1er/patcher/hooks/FontRendererHook.java).
- [PolyPatcher: agrupamento de modelos](https://github.com/Polyfrost/PolyPatcher/blob/main/src/main/java/club/sk1er/patcher/mixins/performance/ModelRendererMixin_BatchDrawing.java).
- [PolyPatcher: HUD](https://github.com/Polyfrost/PolyPatcher/blob/main/src/main/java/club/sk1er/patcher/screen/render/caching/HUDCaching.java).
- [PolyPatcher: modelos de itens](https://github.com/Polyfrost/PolyPatcher/blob/main/src/main/java/club/sk1er/patcher/mixins/performance/forge/ItemLayerModelMixin_ReduceQuadCount.java).
- [PolyPatcher: encantamentos](https://github.com/Polyfrost/PolyPatcher/blob/main/src/main/java/club/sk1er/patcher/mixins/performance/EnchantmentHelperMixin_MemoryLeak.java).

## Correção de inicialização em 0.1.8

O log de uma execução real revelou que o LWJGL 2 exige ao menos 16 posições restantes no buffer de `glGetFloat`, inclusive para consultar uma cor com quatro componentes. O buffer do cache de textos passou de 4 para 16 posições. A mesma correção foi aplicada à preservação de cor na transparência do cavalo. `DollyGroupOneTest` verifica os dois buffers com o `BufferChecks` real do LWJGL e reproduz a rejeição de um buffer de quatro posições, sem precisar de contexto OpenGL. A reprodução gráfica completa ainda depende de abrir o cliente no jogo.
