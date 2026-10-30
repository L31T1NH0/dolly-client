# Grupo 9: correções e manutenção

## Controles e padrões

| Controle | Local | Padrão | Efeito no jogo |
| --- | --- | --- | --- |
| FPS simples no F3 | Controles | ON | Retira o segundo valor de FPS acrescentado pelo OptiFine, mantendo o FPS principal e as demais informações de renderização. Também vale para o contador independente do OptiFine. |
| Gráfico original Alt+F3 | Controles | ON | Substitui o Lagometer do M5 pelo gráfico no estilo vanilla usado pelo Patcher, com tempos dos quadros e referências de 30/60 FPS. |
| Corrigir caixa do vazio | Visual | ON | Pula a geometria da caixa preta desenhada abaixo do horizonte. O caminho com shaders é preservado. |
| Corrigir fim da náusea | Visual | ON | Ao remover náusea do jogador local, zera os dois valores visuais compartilhados com o efeito de portal, evitando o efeito residual. |
| Limpar logs antigos | Desempenho | ON | Limpa arquivos antigos de log em segundo plano ao iniciar o cliente. |
| Guardar logs (dias) | Desempenho | 30 | Idade de retenção, ajustável por barra/teclado entre 1 e 3.650 dias. Mudanças valem na próxima inicialização. |

As opções podem ser desligadas individualmente. Configurações existentes preservam os campos já salvos; os novos campos recebem os padrões da tabela. O Mouse Delay Fix permanece como estava, conforme a decisão de mantê-lo.

## Gráfico de desempenho

O método original do gráfico de debug está vazio no M5 integrado. A implementação restaura a apresentação de tempos no estilo vanilla/Patcher, em vez de apenas esconder o gráfico do OptiFine. Cada barra mostra o intervalo de um quadro; barras mais altas indicam maior demora. O gráfico mantém uma janela de até 240 amostras, estatísticas mínima/média/máxima em milissegundos e marcas de 30/60 FPS. Ele acompanha o comando Alt+F3 e a opção de Lagometer do OptiFine quando a tela de debug está visível.

Em um mundo local, um segundo gráfico usa os tempos de tick já medidos pelo servidor integrado, com referência de 20 TPS. Não se inventa TPS para servidores remotos: o cliente não recebe esses tempos pelo protocolo normal.

As amostras são independentes da gravação do profiler Dolly e reiniciam ao trocar de mundo. O desenho usa geometria agrupada e restaura profundidade, iluminação, blend e textura. Desligar a opção devolve a chamada ao Lagometer nativo. A aparência gráfica ainda precisa de confirmação no jogo.

## Vazio e náusea

A correção do vazio atua somente no ramo da caixa sob o horizonte do renderizador do céu. Não altera posição, colisão, queda, dano ou renderização dos jogadores. Com shaders ativos, esse caminho mantém o comportamento do M5 para preservar a integração do shader.

A correção de náusea mantém a remoção original da poção. Outros efeitos e seus temporizadores não são alterados; a correção não remove a náusea enquanto ela ainda está ativa, não desativa portais reais nem altera teleporte.

## Resource packs

A proteção é permanente e ocorre no início do tratamento do pedido de pack, antes de o caminho nativo consultar arquivos:

- Servidores remotos podem continuar usando URLs HTTP/HTTPS, com a política nativa de aceitar/recusar packs preservada.
- Pedidos remotos `level://` são recusados antes de qualquer consulta ao disco. Um servidor remoto não precisa obter acesso aos packs de mundos locais para fornecer seu próprio pack.
- Pedidos de um mundo local podem acessar somente `<mundo>/resources.zip`, dentro de `saves`. A validação decodifica o caminho, exige o nome esperado e verifica o destino real, incluindo links simbólicos.
- Protocolos diferentes, caminhos absolutos, travessia de diretórios, barras invertidas, codificações ambíguas e URLs malformadas são recusados. HTTP/HTTPS exigem host e não aceitam credenciais embutidas ou fragmento.
- Uma recusa devolve `FAILED_DOWNLOAD` ao servidor e um aviso genérico no log, sem enviar o caminho do computador.

A restrição de `level://` remoto é mais forte que apenas procurar `..` no texto do caminho. Não há opção para reabrir essa sondagem de arquivos. Packs HTTP/HTTPS legítimos continuam pelo carregador original do jogo.

## Limpeza de logs

A limpeza roda uma vez por inicialização, fora da thread do jogo. Considera somente arquivos de `logs/` com nomes datados no formato `AAAA-MM-DD-N.log` ou `.log.gz` e data de modificação anterior ao período escolhido. Não apaga `latest.log`, `debug.log`, arquivos arbitrários, pastas, links simbólicos ou logs do profiler em `dolly/logs`. Uma pasta `logs` que seja link simbólico é ignorada. Falhas de acesso são registradas e não impedem o cliente de abrir.

## Atualizações visuais do ambiente

A opção **Menos animações ambiente**, já implementada no grupo 1 e mantida em Desempenho, reduz de 1.000 para 100 as amostras visuais locais por atualização. Desligar restaura 1.000. Não há um segundo patch sobre o mesmo loop e não se alteram ticks do servidor, redstone, crescimento, combate ou duração de poções. O efeito visível depende também das opções de partículas.

## Integração e validação

`MaintenancePatches` conecta o pacote de resource pack, o fim da náusea, a caixa do céu, as duas apresentações de FPS e a chamada ao gráfico. `DollyResourcePacks` usa `dolly.security.ResourcePackPolicy`. `DollyDebug` e `dolly.debug.FrameHistory` cuidam do gráfico e das políticas visuais. `DollyMaintenance` executa `dolly.maintenance.LogCleanup` após carregar a configuração.

`DollyMaintenancePolicyTest` cobre URLs normais/malformadas, codificação de espaços, travessia simples/codificada, protocolos indevidos, links simbólicos, recusa de `level://` remoto sem acesso ao disco, retenção de logs e proteção de arquivos atuais, ordem/limites das amostras e escala do gráfico. `DollyGroupNineTest` executa o handler realmente modificado para conferir o status de recusa, remove uma poção pelo método nativo, verifica opções, persistência/reset, redução ambiente existente e todos os hooks no JAR final. Os testes de interface percorrem as 89 opções atuais sem repetição, omissão ou sobreposição de controles.

Falta validar em uma execução gráfica real a aparência do Alt+F3, o céu abaixo do horizonte e a transição visual ao remover náusea. Os testes de packs não baixam conteúdo externo nem se conectam a um servidor real.

Referências primárias: [validação de packs no Patcher](https://github.com/Sk1erLLC/Patcher/blob/master/src/main/java/club/sk1er/patcher/hooks/NetHandlerPlayClientHook.java), [náusea](https://github.com/Sk1erLLC/Patcher/blob/master/src/main/java/club/sk1er/patcher/mixins/features/EntityPlayerSPMixin_NauseaEffect.java), [caixa do vazio](https://github.com/Sk1erLLC/Patcher/blob/master/src/main/java/club/sk1er/patcher/asm/render/world/RenderGlobalTransformer.java), [gráfico de métricas](https://github.com/Sk1erLLC/Patcher/blob/master/src/main/java/club/sk1er/patcher/screen/render/overlay/metrics/MetricsRenderer.java) e [contador de FPS](https://github.com/Sk1erLLC/Patcher/blob/master/src/main/java/club/sk1er/patcher/mixins/features/optifine/ConfigMixin_SimplifyFpsCounter.java).
