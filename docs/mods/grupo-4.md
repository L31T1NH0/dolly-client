# Grupo 4: limpeza visual durante a partida

Os controles ficam na aba **Visual** e são aplicados imediatamente. Não modificam dano, efeitos de poção, colisões, movimento de projéteis ou montarias.

| Controle | Padrão | Comportamento |
| --- | --- | --- |
| Altura do fogo | 100% | 100% mantém a posição original; valores menores abaixam o fogo. 0% esconde o desenho. |
| Opacidade do fogo | 100% | Multiplica a transparência nativa. 0% esconde o fogo da tela. |
| Ocultar fogo protegido | ON | Esconde o fogo durante resistência ao fogo; nos últimos 100 ticks (5 s), a imagem pisca como aviso, respeitando a opacidade escolhida. |
| Remover camada da água | ON | Remove a textura que cobre a tela debaixo d'água. |
| Névoa subaquática | 100% | 100% mantém a névoa nativa. Valores menores reduzem a densidade, incluindo o caminho de respiração aquática. Não muda a névoa de lava ou de outros ambientes. |
| Clean View | ON | Omite somente a criação das partículas de poção do jogador local. Atualizações e duração dos efeitos continuam nativas; partículas de outros jogadores continuam disponíveis. |
| Ocultar fragmentos | ON | Omite partículas de acertar/quebrar blocos. A animação de dano no bloco e a quebra real não são alteradas. |
| Ocultar raios | ON | Omite o desenho da entidade do raio; som, clarão ambiental e efeitos do evento continuam nativos. |
| Clean Projectiles | ON | Omite os dois primeiros ticks do desenho de anzóis e projéteis do renderizador de itens arremessados (incluindo ovos, bolas de neve, pérolas e poções). A ocultação termina em até 100 ms desde a primeira consulta visual mesmo se a idade não avançar. Trajetória, pacotes e ticks não são alterados. Flechas usam outro renderizador e não recebem esse atraso. |
| Opacidade do cavalo | 100% | Afeta apenas o cavalo montado pelo jogador local. Outros cavalos, jogadores e nomes continuam no caminho nativo. 0% omite o modelo; 100% usa o desenho original. |

Os percentuais vão de 0 a 100. Desde o [grupo 8](grupo-8.md), usam barras de arrastar com passos de 1 e ajuste por teclado. Configurações salvas com valores intermediários são preservadas.

## Integração

`DollyVisuals` contém as políticas de apresentação. `VisualCleanupPatches` conecta os pontos exatos no M5 e verifica contagens/assinaturas durante o build:

- O fogo usa o alpha nativo, a translação vertical e a duração apenas para leitura. Quando invisível, retorna antes de tocar no estado gráfico.
- Água e partículas de blocos usam retornos antecipados em renderizadores/geradores exclusivamente visuais.
- Clean View redireciona somente a chamada de criação de partículas no atualizador de poções, sem retornar do restante do método.
- Névoa é ajustada depois do cálculo nativo (inclusive Respiration/Clear Water), somente nos dois caminhos de água.
- Clean Projectiles atua nos renderizadores e nunca nos ticks ou pacotes. O helper também exclui jogadores.
- O cavalo usa transparência apenas na chamada do modelo do renderizador de seres vivos. Blend, alpha test e cor são restaurados em `finally`. O desenho de nomes permanece fora dessa chamada. A transição não aplica invisibilidade à entidade.

As chaves são `fireHeight`, `fireOpacity`, `hideFireResistance`, `removeWaterOverlay`, `waterFogDensity`, `cleanView`, `hideBreakingParticles`, `hideLightning`, `cleanProjectiles` e `horseOpacity`. Configurações antigas recebem os valores padrão para chaves ausentes; valores previamente salvos são preservados. Restaurar padrões restabelece os valores da tabela.

## Validação

`DollyGroupFourTest` cobre alpha/altura do fogo, janela de aviso de resistência sem alterar o contador, partículas locais/remotas, modo nativo quando desligado, densidades de água, idade original dos projéteis, exclusão de jogadores, opacidade restrita à montaria e caminhos nativos com 100% de opacidade. Também executa os retornos reais de água, fogo, fragmentos, raios e projéteis sem acessar OpenGL, e confere os hooks do JAR final. Persistência, reset e rejeição de percentuais inválidos são testados no núcleo.

Não foi possível validar um driver OpenGL neste ambiente. Conferir no jogo transparência parcial do cavalo (incluindo dano, sela e armadura), shaders, packs com fogo personalizado, respiração aquática e as opções ligadas/desligadas. Não há ganho de FPS medido.

## Referências

- [Fogo no PolyPatcher](https://github.com/Polyfrost/PolyPatcher/blob/main/src/main/java/club/sk1er/patcher/mixins/features/render/item/ItemRendererMixin_FireRendering.java).
- [Água](https://github.com/Polyfrost/PolyPatcher/blob/main/src/main/java/club/sk1er/patcher/mixins/features/render/item/ItemRendererMixin_RemoveWaterOverlay.java).
- [Névoa](https://github.com/Polyfrost/PolyPatcher/blob/main/src/main/java/club/sk1er/patcher/mixins/features/EntityRendererMixin_Fog.java).
- [Clean View](https://github.com/Sk1erLLC/Patcher/blob/master/src/main/java/club/sk1er/patcher/mixins/features/EntityLivingBaseMixin_CleanView.java).
- [Fragmentos](https://github.com/Sk1erLLC/Patcher/blob/master/src/main/java/club/sk1er/patcher/mixins/features/EffectRendererMixin_BlockParticles.java).
- [Projéteis](https://github.com/Sk1erLLC/Patcher/blob/master/src/main/java/club/sk1er/patcher/mixins/features/RenderSnowballMixin_CleanProjectiles.java).
- [Cavalo montado](https://github.com/Sk1erLLC/Patcher/blob/master/src/main/java/club/sk1er/patcher/util/world/render/entity/EntityRendering.java).
