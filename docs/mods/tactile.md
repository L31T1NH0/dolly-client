# TACTILE no Dolly 0.1.16

O motor completo de movimento do [TACTILE](https://github.com/fallingsword/TACTILE/tree/66892f1005b3e249240c89a4cf3521c5f73e6cd8) foi adaptado à base sem Forge do Dolly. O núcleo de física, a linha temporal de teclas, o collider e as fontes de entrada vêm desse commit; a licença MIT é distribuída em `licenses/TACTILE.txt` no JAR. Não há Forge, coremod ou transformer em runtime, nem dependências novas.

## Configuração por servidor

Abra **Dolly → Configurações → Entrada e câmera → Configurar TACTILE**. O perfil começa em **Vanilla**. Dentro de um servidor, a tela edita esse endereço; **Editar padrão global** muda o padrão herdado por endereços sem perfil próprio. **Usar padrão global neste servidor** apaga a exceção desse endereço. Mundos locais têm uma entrada separada. As alterações são salvas e aplicadas no próximo tick.

| Modo | Comportamento |
| --- | --- |
| Vanilla | Movimento e câmera vanilla. O antecipador de eventos do Dolly continua disponível. |
| Entrada antecipada | Física e pacotes vanilla a 20 Hz; usa o bombeamento de eventos antecipado já integrado ao Dolly. Não inicia o motor de substeps ou as threads de teclado do TACTILE. |
| EXACT | Motor completo: cada mudança de tecla é aplicada no tempo observado, dividindo os substeps nos timestamps das entradas. |
| STEPPED | Mesmo motor de física, com a entrada aplicada nos passos da frequência escolhida. Preservar toques curtos mantém um toque menor que um passo. |

As opções avançadas incluem física (20–1920 Hz, padrão 960), passos de entrada (1–1920 Hz, padrão 20), fonte AUTO/EVENTS/POLLER/FRAME, leitura do poller (250–8000 Hz, padrão 2000), preservação de toques curtos e estado OFF/Simples/Detalhado. Controles sem efeito no modo escolhido ficam desativados. As listas rolam e as barras aceitam setas/Home/End.

AUTO tenta Raw Input no Windows, depois POLLER e finalmente leitura por quadro. Fora do Windows, EVENTS/POLLER voltam à leitura por quadro. Acima de 2000 Hz, POLLER pode ocupar um núcleo; a tela informa esse custo. O modo detalhado mostra tempos, correções e fallbacks. F8 permanece reservado ao profiler do Dolly; os atalhos F7/F8 do mod original não foram importados.

Os perfis ficam em `tactileProfiles`, no `config/dolly.properties` já existente. Endereços são normalizados em minúsculas, removendo o ponto final do domínio e a porta padrão 25565. Portas diferentes preservam perfis separados. Valores inválidos, duplicados e frequências fora dos limites são rejeitados. Uma conexão multiplayer sem endereço identificável permanece em Vanilla, sem herdar EXACT do perfil global. Restaurar padrões apaga os perfis e volta a Vanilla.

## O que foi avaliado para Hypixel

O próprio [TACTILE relata rubber-banding no Hypixel](https://github.com/fallingsword/TACTILE/tree/66892f1005b3e249240c89a4cf3521c5f73e6cd8#compatibility). O motor não muda apenas a consulta do teclado: substitui a movimentação local, calcula colisões entre ticks, aplica mudanças de sprint e knockback em momentos diferentes e apresenta uma posição de câmera prevista. O projeto também documenta diferenças pequenas de trajetória em alguns casos. Baixar a taxa para 20 Hz ou esconder correções não demonstra compatibilidade.

Por isso, endereços `hypixel.net` e seus subdomínios usam **física vanilla**. EXACT/STEPPED herdados de um padrão global viram **Entrada antecipada** nesse endereço. O motor não é sequer construído nesse perfil. O cliente mantém os ticks, o método de movimento e os pacotes nativos; não há filtro de correções, manipulação de velocidade, alteração de pacotes ou mecanismo para contornar anticheat.

Isso é uma restrição de implementação, **não uma garantia de aprovação ou ausência de punição**. Não houve acesso ou teste no Hypixel. A identificação usa o endereço informado ao cliente: aliases próprios e proxies não são identificáveis como Hypixel por essa regra; nesses endereços, mantenha Vanilla/Entrada antecipada manualmente. Os dois modos preservam movimento vanilla e diferem pela intenção apresentada no perfil; o bombeamento antecipado já é global no Dolly.

## Integração e diferenças do adaptador

- `dolly.tactile.core` preserva o núcleo upstream, mudando apenas o namespace. Os fixtures e relatórios de teste ficam fora do JAR de produção.
- `DollyTactileEngine`, `DollyTactileMovement`, `DollyTactileWorld`, `DollyTactileSupport` e `DollyTactileHooks` adaptam os tipos e membros MCP ao binário 1.8.9/M5. Os hooks exigem assinaturas e contagens exatas no build.
- `DollyTactile` escolhe o perfil, gerencia o motor e apresenta o diagnóstico. Jogadores remotos e o servidor integrado continuam delegando ao método vanilla.
- A chamada de movimento é substituída em `pr.m()`. O tick começa depois da recuperação de teclas do Dolly, e o render restaura as posições previstas tanto em retornos normais quanto em exceções.
- A adaptação usa as escadas/vinhas vanilla, sem as extensões de blocos e callbacks do Forge. Montaria, espectador, voo em escadas, estados de bloco incompatíveis e falhas devolvem o movimento ao vanilla. Uma falha interna mantém o motor desativado até reiniciar o cliente. A leitura temporária dos atributos de velocidade restaura o modificador de sprint em `finally`, inclusive se ocorrer uma falha.
- Toggle Sprint é incorporado à entrada do motor. Zoom e Perspective continuam nos caminhos existentes.
- O POLLER foi reforçado para consultar teclas apenas quando o jogo tem foco e sua janela está em primeiro plano; o timer nativo é fechado também ao ocorrer uma exceção. Raw Input mantém sua própria proteção por foco.

## Validação e limites

Os testes cobrem normalização/persistência/reset e precedência de perfis; a restrição Hypixel sem inicializar o motor; ativação real do adaptador, salto sem duplicação, delegação de outras entidades, restauração da posição gráfica, retorno ao vanilla e falha forçada de colisão. O poller usa uma API Win32 simulada para verificar a interrupção de consultas ao trocar de janela. O build verifica a JVM e os hooks no JAR final. A interface é verificada em resoluções pequenas e grandes usando os dispositivos determinísticos existentes.

O teste offline compara o modelo de um passo ao modelo vanilla portado nos cenários upstream e verifica toques curtos/independência da taxa no modo EXACT. Isso não substitui os registros de baseline do Minecraft real, que não são distribuídos pelo repositório original. Os harnesses upstream foram preservados em `src/test/java/dolly/tactile/` para comparações adicionais quando esses registros estiverem disponíveis.

Ainda falta validação gráfica e de jogo em servidores, incluindo líquidos, escadas, voo criativo, knockback, pontes, itens em uso e troca de mundos durante movimento. As fontes Win32 foram verificadas com um substituto da API; não foram exercitadas em hardware Windows. Não há medição de latência ou ganho de FPS desta adaptação.
