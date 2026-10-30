# Profiling de performance

F8 inicia uma captura **NORMAL**. Shift+F8 inicia **DEEP**. F8 (com ou sem Shift) encerra a captura ativa. O atalho funciona também nos menus e ignora repetição enquanto a tecla está pressionada. O HUD exibe o modo durante a captura; falhas aparecem no HUD e no stderr do launcher.

Os arquivos ficam em `<diretório da instância>/dolly/logs/profile-<epoch>-<modo>-<id>.jsonl`. Cada linha é um objeto JSON independente. O nome único impede sobrescrever capturas. O arquivo é criado ao iniciar; as amostras e os agregados são gravados em segundo plano, com flush aproximadamente a cada segundo e ao encerrar. Durante a finalização, outro início é ignorado. O shutdown hook aguarda até três segundos pela gravação.

## NORMAL e DEEP

NORMAL mede cada frame completo do loop (ticks, apresentação e espera do limitador), ticks, renderização do frame, camadas de chunks, atualização/reconstrução de chunks, transparências, entidades, partículas e HUD. Mede também os principais hooks Dolly de tick, frame, render, entrada, mouse, mudanças de configuração e partículas ambientes. Helpers menores e caminhos muito frequentes recebem instrumentação detalhada somente em DEEP. Dados do jogo são lidos na thread principal, sem varrer entidades: quantidade carregada, entidades/tile entities renderizadas, chunks visíveis/pendentes e partículas ativas. Em menus, os contadores de cena são zero.

DEEP acrescenta escopos com IDs numéricos, início relativo, duração inclusiva/exclusiva e thread. Instrumenta os métodos de Minecraft, EntityRenderer, RenderGlobal, partículas, HUD, reconstrução e despacho de chunks, render manager, itens, tessellator/uploader, GlStateManager, adaptadores Dolly e pacotes `net/optifine/render` e `net/optifine/shaders`. `build/patches.txt` relaciona as classes modificadas; o JAR contém `dolly-profiler-symbols.txt` com os nomes usados pela instrumentação. Construtores, inicializadores estáticos, métodos nativos/abstratos e o próprio profiler não recebem escopos.

Para limitar a interferência, o padrão DEEP coleta chamadas detalhadas em **1 a cada 10 frames**, com orçamento de **512 escopos por thread por frame selecionado**. As etapas principais do NORMAL continuam medidas em todos os frames. A amostra é determinada no início do frame; workers acompanham a janela desse frame. Antes do primeiro frame, há uma janela inicial limitada pelo mesmo orçamento. Operações iniciadas em uma janela podem terminar fora dela.

Opções da JVM, lidas a cada início de captura:

```text
-Ddolly.profiler.deepEveryFrame=10
-Ddolly.profiler.maxScopesPerFrame=512
-Ddolly.profiler.deepCategories=all
```

Categorias: `gl`, `render`, `client`, `network`, `dolly`, separadas por vírgula, ou `all`. Para rastrear todas as janelas, use `deepEveryFrame=1`; isso aumenta o overhead e não remove o orçamento nem a capacidade dos buffers. Os valores efetivos são exportados. Comparações precisam usar o mesmo modo, schema, categorias, taxa e orçamento.

## Custo, threads e tempos exclusivos

Hooks usam IDs definidos no build e pilhas primitivas por thread, com profundidade máxima de 256. Agregados são acumulados por thread. Não existe lock compartilhado entre threads no encerramento de cada função e nenhum escopo monta strings JSON no jogo. Cada thread possui um ring numérico de 512 registros; o writer consome os registros por publicação/consumo volatile e faz toda a serialização. Se não houver espaço, o registro é descartado antes de formatar JSON. Agregados das chamadas efetivamente medidas e histogramas dos frames continuam preservados.

Há locks locais curtos para atualizar os dados de cada thread e para o writer obter amostras de rede aproximadamente uma vez por segundo ou finalizar a sessão. O cadastro inicial de uma thread usa um lock da sessão. O limite é de 64 threads por captura; chamadas além desse limite são contabilizadas como não rastreadas. Esses limites mantêm o consumo de memória controlado. Ao esgotar o orçamento, operações posteriores daquele frame ficam sem detalhe DEEP; use categorias específicas ou ajuste o orçamento para investigar outra parte do frame. O sistema contabiliza descartes, escopos omitidos por orçamento, overflow de pilha e chamadas sem thread registrada.

Tempo exclusivo é o tempo inclusivo menos os filhos instrumentados diretos na mesma thread. Funciona com recursão e saídas excepcionais. Se uma chamada GL lança uma exceção, o encerramento do método envolvente desfaz os escopos GL abertos, marcando-os como `unwound`. Filhos não selecionados pelo modo, categoria, orçamento ou limite de pilha permanecem no tempo exclusivo do pai. Portanto, exclusivo significa **tempo de parede sem os filhos observados**, não CPU pura; pode conter esperas, desagendamento e custo do próprio profiler. Não some tempos inclusivos de pais e filhos.

DEEP ainda acrescenta relógios, manutenção de pilha e eventos e pode alterar FPS e GC. O limite de coleta reduz e torna explícita essa interferência; não a elimina.

## OpenGL e GPU

Nos dois modos são contados os call sites diretos de funções `gl*` estáticas do LWJGL em métodos instrumentáveis do JAR, inclusive OptiFine/VBO/Render Regions. DEEP mede a latência na CPU dessas chamadas quando a janela/categoria/orçamento permitem. Draw calls são submissões `glDraw*` (exceto seleção de draw buffer), `glMultiDraw*`, `glCallList(s)` e `glBegin`. Multi-draw/display lists contam como uma submissão. Comandos internos do driver/display lists e bibliotecas externas não são contados.

Além desses tempos de CPU, há medição de **tempo decorrido na GPU durante `render.frame`** por pares de queries `GL_TIMESTAMP`, com suporte a OpenGL 3.3 ou `ARB_timer_query` e contadores válidos. As queries são realizadas apenas na thread de renderização. Pares de timestamp não alteram a query `GL_TIME_ELAPSED` que outro componente possa usar. O profiler não consulta `glFinish` e nunca solicita o resultado antes de ambas as queries estarem disponíveis.

Um pool de 16 pares é consultado em renderizações seguintes; cada resultado mantém o índice do frame original. Se o pool estiver cheio, a medição daquele frame é omitida. Em F8, são exportados os resultados já prontos e marcados os pendentes; os objetos GL são liberados sem esperar pela GPU. Se a sessão terminar por outra API, a próxima renderização faz a limpeza; no fechamento da JVM o contexto GL será destruído. Não há chamadas GL na thread de shutdown ou de escrita.

O registro `gpu` contém `frame_index`, `duration_ns` e `reason`: 0 = válido, 1 = sem suporte, 2 = pool cheio, 3 = falha/resultado inválido, 4 = ainda pendente ao parar. Falhas desabilitam a coleta GPU naquela sessão. O resumo informa medições emitidas/coletadas. Tempo GPU cobre o intervalo da renderização; não inclui apresentação/VSync fora desse intervalo, nem representa utilização percentual ou exclusivamente tempo ocupado da GPU. Não há subtração entre relógios absolutos da CPU e GPU.

## Garbage Collector

A amostragem acumulada de GC continua disponível. Adicionalmente, listeners dos coletores recebem notificações de coleta, quando suportadas pela JVM. Os registros `gc_event` contêm coletor, causa, ação, início, duração e memória antes/depois. Início/fim vêm do `GcInfo`, não do horário de entrega da notificação. O uptime da JVM é alinhado ao relógio monotônico da sessão, com resolução nominal de 1 ms; correlações próximas da borda de um frame têm essa incerteza.

`pause_ns` é preenchido para coletores Serial/Parallel conhecidos (`Copy`, `MarkSweepCompact`, `PS Scavenge`, `PS MarkSweep`). Para outros coletores, inclusive ciclos concorrentes, o valor é -1: o intervalo do evento não prova uma pausa contínua. G1/CMS podem exigir logs de GC ou ferramenta específica da JVM para detalhar cada fase STW. `gc_notifications` na sessão informa se houve listeners disponíveis; a fila de notificações é limitada a 256 e suas perdas entram em `dropped_records`. Memória antes/depois soma os pools informados pelo evento, sem afirmar que todos são heap.

O analisador relaciona intervalos de GC com frames. Para pausas conhecidas, calcula a união dos intervalos sobrepostos, evitando contar duas vezes o mesmo trecho. Sobreposição temporal não é, por si só, prova de causalidade.

## Processamento e filas de rede

O patch de `PacketThreadUtil` envolve o Runnable original que é agendado para o cliente, preservando ordenação, execução e propagação de exceções. Registra o instante de enfileiramento, o início da execução na thread principal e a duração do processamento. O wrapper mantém a sessão original: tarefas executadas depois do encerramento não contaminam uma nova captura. Tarefas ainda não executadas entram no backlog ao parar; não são removidas nem canceladas pelo profiler.

NORMAL agrega espera e processamento, mede encode/decode/recepção no Netty e exporta amostras acumuladas de tráfego/backlog aproximadamente a cada segundo, além do backlog por frame. DEEP acrescenta eventos por tipo de pacote nas janelas selecionadas, respeitando orçamento/buffer. Tipos de pacotes têm símbolos; não há conteúdo, endereço do servidor ou payload na captura.

Bytes são os bytes de protocolo consumidos pelo decoder/produzidos pelo encoder, incluindo ID do pacote, antes de compressão/criptografia e fora dos cabeçalhos TCP. Os filtros de direção evitam contar o lado servidor integrado como tráfego do cliente. Conexões locais em memória podem não atravessar esses codecs. Essa medição não fornece RTT da rede, atraso unilateral ou tempo do servidor: a espera medida é **fila local até a thread principal**, e o tempo do handler é trabalho local.

## JSONL, schema 2

- `session`: modo, horário epoch ms, JVM/SO, processadores/heap máximo, parâmetros DEEP e suporte a notificações GC.
- `symbol`: associação entre ID e nome. Eventos usam IDs; agregados também incluem nome. IDs só são comparáveis usando o dicionário da própria captura.
- `frame`: índice, início/duração ns, FPS instantâneo derivado, CPU da thread (-1 se indisponível), deltas GL/draws, tempos das etapas, contadores de cena e backlog de rede.
- `resources`, `gc`: recursos e contadores acumulados aproximadamente por segundo. CPU load vai de 0 a 1, ou -1 quando indisponível. Memória virtual comprometida não é RSS. Contadores de GC incluem o período anterior à sessão; use diferenças entre amostras.
- `scope`: ID, início, duração, exclusivo, thread e `unwound` (DEEP).
- `aggregate`: chamadas, `total_ns` inclusivo, `exclusive_ns`, `max_ns` e `exclusive_max_ns`. Apenas chamadas efetivamente medidas entram nesses totais; não há extrapolação da amostra.
- `gpu`, `gc_event`: resultados e intervalos descritos acima.
- `network`, `network_summary`: amostras e resumo de tráfego, filas e handlers; `packet` detalha a espera e execução em DEEP.
- `summary`: frames completos, tempos, buckets p50/p95/p99, GL/draws, cobertura/perdas e contagem de resultados GPU. Buckets têm resolução de 1 ms, com saturação em 999 ms; -1 significa ausência de frames. O analisador calcula percentis exatos dos frames exportados.

Durações usam `System.nanoTime()`. Somente chamadas concluídas na mesma sessão ativa entram nos agregados; chamadas/frames atravessando início/fim podem ser descartados. Chamadas GL de workers entram nos deltas do intervalo do frame. Erros de I/O desativam a captura, removem listeners e preservam o arquivo parcial. Encerramento forçado pode perder o buffer final: ausência de `summary` identifica captura incompleta.

## Análise, comparação e calibração

```sh
python3 tools/analyze_profile.py captura.jsonl --top 20
python3 tools/analyze_profile.py antes.jsonl depois.jsonl --sort exclusive
python3 tools/analyze_profile.py captura.jsonl --sort inclusive
python3 tools/benchmark_profiler.py
```

O analisador aceita capturas schema 1 e 2. Mostra percentis, tempos inclusivos/exclusivos, resultados GPU e eventos GC associados aos frames lentos, filas de rede e perdas de cobertura. A comparação avisa quando os parâmetros de coleta diferem.

O benchmark requer uma build atualizada. Executa a mesma carga sintética nos modos OFF/NORMAL/DEEP, com aquecimento e mediana de três medições. O custo por chamada é o tempo do loop inteiro amortizado; inclui etapas por frame, concorrência com o writer e custos de início da captura, sem isolar apenas o custo de um hook. Não abre display nem conexão e não mede custo do driver, sincronização real da GPU ou FPS de uma partida. Para validar overhead real, repita um percurso/cenário com profiler desligado, NORMAL e DEEP; mantenha JVM/heap, distância de renderização, limite de FPS, VSync, shaders e parâmetros DEEP iguais. FPS inclui a espera do limitador: observe também `render_ns`, ticks, CPU da thread e GPU.

A build verifica bytecode/JVM e testa concorrência, modos/categorias/orçamento, tempos exclusivos com recursão e exceções, saturação/reuso dos buffers, queries GPU com backend simulado, indisponibilidade/pool cheio/finalização, notificações GC e isolamento/erros das filas de rede. Os testes Python verificam o analisador e a união de pausas sobrepostas. Medições GPU com driver real e overhead em partida precisam de validação gráfica.
