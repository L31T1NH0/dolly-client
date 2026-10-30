# CMPack: mecanismos técnicos além dos módulos visuais

Investigação do CMPack 2.8.11 beta / Minecraft 1.8.8 fornecido no projeto, para identificar correções e alterações internas úteis ao Dolly 1.8.9. Este documento registra a investigação estática. A implementação posterior está descrita na [integração técnica](integracao-tecnica.md).

## Escopo e evidência

O alvo é `references/cmpack_1.8.8_2.8.11-beta.zip`, SHA-256 `43e45120f1ffaf8793b692427a9b6eea376aadc56b054c608861b548a51722cc`. O membro `cmpack_1.8.8/CMPack_app.jar` tem SHA-256 `464347bee470967072e3b7a2536d95e1f87645c1a02d2f8b0f120fcc9ebc4a80`, conferido novamente nesta investigação.

A análise usa bytecode do binário, e não apenas os nomes dos botões. As ferramentas REA não estavam disponíveis nesta sessão; foram usados inventário ZIP e `java -m jdk.jdeps/com.sun.tools.javap.Main -c -p`, sem executar o client ou consultar serviços externos. As evidências novas são disassemblies locais, não novos Evidence IDs do REA.

Foram usados como ponto de partida os [58 ajustes gerais](defaults-gerais.json), os relatórios anteriores e os consumidores relacionados. Isto é uma investigação dirigida dos mecanismos abaixo, não uma comparação exaustiva das 3.366 classes com vanilla nem garantia de exclusividade do CMPack. Valores padrão são de construção; configuração pessoal pode alterá-los. Não houve benchmark, medição de latência ou validação gráfica.

Evidências com hashes de classes, métodos e offsets:

- [Entrada](evidence/technical-input.bytecode.txt).
- [Texto, jogadores e geometria dos itens](evidence/technical-render.bytecode.txt).
- [Carregamento e screenshot](evidence/technical-loading.bytecode.txt).
- [HUD, culling e resolução de endereços](evidence/technical-runtime.bytecode.txt).

## O que merece atenção primeiro

| Mecanismo | O que muda de fato | Avaliação para o Dolly |
| --- | --- | --- |
| Mouse bind fix de inventário | Permite usar botão extra associado à hotbar para trocar o item sob o cursor com aquele slot | Lacuna confirmada na base integrada; candidato direto |
| InputFix de texto | Aceita certos eventos de caracteres sem keycode físico | Lacuna confirmada; separado do fix Linux de Shift |
| Screenshot assíncrona | Move conversão de imagem e gravação PNG para executor | Candidato para reduzir parte da pausa de F2; leitura da GPU permanece no chamador |
| Troca rápida de idioma | Recarrega traduções sem fazer a recarga ampla de recursos naquele caminho | Candidato para melhorar uma operação específica |
| Cache de largura de texto | Reutiliza medidas de strings | Ideia útil, mas corrigir invalidação de fonte antes de adaptar |
| Atualização otimizada de jogadores | Pula cálculos de animação depois de atualizar posição/rotação | Precisa verificar efeitos visuais; não reduz tráfego de rede |
| HUD em cache | Reutiliza textura do HUD e atualiza em frequência própria | Pode economizar render, mas também atrasar sua atualização visual |
| Raw mouse | Captura deltas por JInput em worker separado | Exige adaptação de plataforma, sincronização e fallback; cópia literal tem problemas |
| Lazy Chunk Loading | Introduz pausas no worker de construção de chunks | É uma troca de velocidade de construção por menor pressão de trabalho |
| Fast world loading | Evita uma chamada explícita a `System.gc()` | Escopo pequeno: pode evitar pausa, não acelera recepção dos chunks |
| Limpeza ao desconectar | Remove um subconjunto de texturas por política temporal | Precisa validar ciclo de vida dos recursos |
| Item Stitching Fix | Altera geometria/UV para tratar frestas nos itens | Correção visual técnica dependente dos modelos e texturas |
| Entity culling | Evita renderização de objetos julgados ocultos por testes na CPU | Dolly já tem implementação própria; não duplicar sem comparação |
| Resolução SRV e cache de endereço | Usa resolvedor alternativo, fallback opcional e cache de um minuto | Atua na conexão ao servidor, não no ping durante a partida |

## Entrada: correções pequenas com efeito concreto

### Mouse bind fix

Começa ligado (`D.bG`). Em `lE.q(III)`, offsets 538–570, botões de mouse maiores que 2 chegam a `bp(button - 100)`. O helper `lE.bp(I)`, 0–76, procura a combinação nos nove atalhos de hotbar. Se há slot sob o cursor e nenhum item carregado no cursor, chama a operação de inventário tipo 2 para trocar esse item com o slot associado.

Exemplo: atribuir o slot 5 a um botão lateral e pressioná-lo sobre uma poção no inventário troca a poção com o conteúdo do slot 5. Não é apenas selecionar o slot atual da hotbar.

No Dolly, `ayl` já possui o helper `b(int)`, mas o caminho examinado de `a(III)` não encaminha esse código do mouse. O suporte de `DollyInput.physicalKey` a botões em atalhos gerais não resolve essa operação de inventário. Uma adaptação deve testar item no cursor, slot vazio e interação com o perfil de slots do Dolly.

**Correção da evidência antiga:** `default-consumers.bytecode.txt` exibia `CONST 156` no offset 563; o binário contém `bipush -100`. A evidência nova preserva o sinal original.

### InputFix de texto

O módulo `an` começa ativo. Em `kB.kU()`, 0–79, o caminho corrigido despacha o evento quando há keydown **ou** quando `keycode == 0 && Character.isDefined(character)`. O caminho original só aceita keydown.

Isso trata caracteres que chegam sem uma tecla física correspondente. O `axu.l()` da base integrada do Dolly ainda só aceita keydown no caminho examinado. É distinto da normalização de Shift+2/Shift+6 do Linux; a inspeção não demonstra cobertura completa de IME, acentos ou todos os layouts.

### Recuperação de teclas

`ir.gy()` e `wn.vh()` releem `Keyboard.isKeyDown` ao recuperar foco, sob condições de janela/plataforma. `wn.q(IZ)` atualiza o estado pressionado sem incrementar o contador de novos pressionamentos. A finalidade é retomar comandos contínuos, sem inventar cliques.

O Dolly já recupera movimento/sprint e consulta estado físico de mouse. Essa escolha é mais restrita que percorrer todos os atalhos do CMPack. Não é necessário tratar os dois mecanismos como completamente ausentes nem presumir que a cópia integral seja melhor.

### Raw mouse

Começa desligado (`D.aS`). `PV.Vl()` normalmente usa `Mouse.getDX/getDY`; `afM.dj(Z)` troca o capturador por `afN`. O worker de `afM.auF()` consulta JInput e acumula X/Y quando não há tela aberta, dormindo 1 ms entre iterações. `afN.Vl()` transfere os deltas, inverte Y e zera os acumuladores.

A seleção usa `DirectAndRawInputEnvironmentPlugin`. O suporte no Linux não foi demonstrado. Os campos compartilhados `afM.dcQ/dcR` são `int` comuns e os caminhos de escrita/leitura/zeragem examinados não usam sincronização, o que permite perda de deltas por concorrência. Não foi encontrado fallback automático para LWJGL nesse worker. Portanto existe um backend alternativo real, mas não uma prova de menor latência nem uma implementação para copiar literalmente.

## Renderização e processamento

### Cache de largura de texto

Começa ligado (`D.aE`). `jr.N(String)` guarda medidas em mapa via `computeIfAbsent`; acima de 5.000 entradas limpa o cache. Alternar a opção também limpa o mapa.

A recarga de fonte em `jr.q(vj)` e os setters de Unicode/bidi examinados não limpam esse mapa. Assim, uma string já medida pode continuar com largura antiga após mudar os recursos da fonte. Não foi reproduzido em uma janela, mas falta uma invalidação no fluxo observado. O `avn` da base OptiFine M5 do Dolly não contém esse mapa. Uma adaptação precisa tratar recarga e modos da fonte, além de medir se o cache compensa no HUD/chat do Dolly.

### Atualização otimizada de jogadores remotos

Começa ligada (`D.bv`). `jo.iM()` mantém a interpolação de posição/yaw/pitch e a atualização do swing, depois retorna antes de cálculos adicionais de velocidade/ângulo e dos campos de animação `bge/aSs`.

É redução de cálculos de apresentação. Não é aumento de TPS, mudança do intervalo de pacotes ou atualização mais rápida dos adversários. Campos desse grupo também participam do render de capas/cosméticos; omitir sua atualização pode mudar a aparência. O `bex.m()` do OptiFine M5 integrado ao Dolly ainda executa esses cálculos; a diferença existe, mas o ganho não foi medido e falta comparação visual.

### Cache do HUD

O estado inicial depende de suporte a framebuffer (`D.aU`), com limite próprio de 60 Hz (`D.be`). `afH.q(pM,jP,float)`, 397–448, apresenta a textura do HUD; 451–599 redesenha o framebuffer quando ausente ou invalidado. `afI.jZ(I)` agenda invalidação periódica e `afK.run()` chama `afH.bx()` quando há jogador.

O executor agenda a invalidação; não há evidência de que o OpenGL seja transferido para uma thread de render separada. Se framebuffer não está disponível, há caminho de desenho normal. Mira e alguns overlays têm tratamento separado no método; não é correto dizer que absolutamente tudo fica congelado a 60 Hz.

É possível desenhar o mundo acima de 60 FPS e reutilizar partes do HUD a 60 Hz. Isso troca custo de render por atualização visual independente e exige validar chat, animações, escalas, resize e composição com shaders/Fast Render. O Dolly já prepara parte de seu HUD por tick; isso é diferente de armazenar o HUD inteiro numa textura.

### Entity culling

Começa ligado (`D.bs`). `afE` inicia um worker; `afC.run()` dorme 10 ms entre verificações e só trabalha com mundo/jogador válidos e após a condição inicial de ticks. O worker consulta caixas de entidades/objetos de bloco por um motor de visibilidade na CPU (`cy`) e atualiza `afD`. Os renderers `sV.q(...)` e `uL.q(...)` pulam desenho quando o estado indica ocultação, respeitando uma janela de liberação.

`afB` define alcance inicial 128 e tamanho máximo de caixa 50. `afD.aus()` concede 1.000 ms de liberação, e `afD.aut()` verifica esse prazo. O worker também evita repetir a varredura quando a posição de referência não mudou e a flag de atualização não foi marcada. Isso significa que invalidação e movimento da câmera precisam ser entendidos antes de reproduzir a lógica.

O Dolly tem culling próprio com orçamento por quadro, consultas na thread de render e exclusão de jogadores. É um desenho diferente. Não há medição que sustente substituir esse mecanismo pelo worker do CMPack; seria necessário validar visibilidade, mudanças de mundo, geometria e acesso concorrente ao mundo.

### Item Stitching Fix

A opção começa desativada e oferece Outline e Unlerp (`D`, catálogo de defaults). O objetivo é tratar frestas nas faces de itens extrudados de texturas. Os dois modos alteram mecanismos diferentes: construção de geometria nas bordas e coordenadas UV. Não mudam entrada, combate ou colisão. Em `rl.w(int,String,um)`, o modo 1 encaminha a construção para `ce.q`, que examina pixels/transparência e gera laterais nas bordas; o modo 2 altera UVs via `ce.q(List,float)` e `cd.q`. A fórmula observada expande UV em relação ao centro: `(uv - s * centro) / (1 - s)`. `F.q(Object)` força recarga de recursos ao mudar o modo. A deduplicação de modelos do Dolly é outra operação; equivalência integral com eventuais correções já existentes no M5 não foi examinada.

## Carregamento, recursos e conexão

### Screenshot assíncrona

Começa desligada. Em `Qi.q(IIwo)`, a captura ainda executa `glReadPixels` ou `glGetTexImage`, cópia e inversão dos pixels no chamador; a criação da imagem e a gravação PNG são encaminhadas a executor nos offsets 303–342. O caminho de supersampling continua síncrono. Assim, a proposta reduz parte do trabalho de CPU/disco no caminho da screenshot, mas não elimina a possível sincronização com a GPU.

É candidata a adaptação isolada. É necessário preservar resultado/erro da gravação e a posse do buffer capturado. O worker examinado usa o array estático compartilhado `bVi`, sem capturar uma cópia dos pixels; capturas sobrepostas exigem tratamento próprio. Não explica fluidez contínua com F2 sem uso.

### Fast world loading

Começa desligada (`D.aQ`). Em `ir.q(nm,String)`, offsets 325–334, evita uma chamada explícita a `System.gc()`. Pode evitar a pausa causada por essa chamada; não é um protocolo mais rápido de download nem geração mais rápida de chunks. Verificar primeiro se o caminho equivalente ainda existe na base atual do Dolly.

### Fast language switch

Começa desligada (`D.bc`). Em `jX.q(IZII)`, 61–94, escolhe a atualização de traduções por `vn.q(vj)` em vez de chamar a recarga ampla `ir.hs()`. O benefício esperado é restringir o trabalho ao mudar de idioma. Exige validar fonte, Unicode e componentes que dependem do evento de recarga; o nome não prova equivalência em todos os resource packs.

### Lazy Chunk Loading

Começa em Off/Vanilla (`D.br`). No worker `rM.rx()`, o seletor pode inserir `Thread.sleep` de 15, 50, 110, 150 ou 200 ms antes de `BlockingQueue.take`, condicionado também a `acQ.bTY`.

É uma desaceleração deliberada do worker de chunks. Pode reduzir competição por CPU, mas faz a geometria aparecer mais devagar. A flag `acQ.bTY` é marcada verdadeira ao fim da inicialização `acQ.q(k)`, offset 1966; não foi demonstrado significado adicional para ela. O efeito do seletor precisa de medição na plataforma alvo. É diferente do limite de FPS na tela de carregamento já existente no Dolly.

### Limpeza de memória ao desconectar

Começa desligada (`D.aC`). Após a desconexão em `ny.u(Pu)`, um evento chega a `acQ.q(aex)` e agenda `asp` para 1.000 ms depois. O worker seleciona texturas de quatro namespaces cujos identificadores têm mais de 30 minutos de criação. A remoção do mapa e a exclusão da textura GL são agendadas na thread Minecraft por `E`. Os nomes desses quatro namespaces não foram decodificados nesta investigação. Não é uma liberação geral de toda a memória do Minecraft nem simplesmente forçar o coletor de lixo. A evidência de loading registra os critérios e o agendamento.

O timestamp `Qg.bUZ` é final e recebe `currentTimeMillis` no construtor: é idade de criação, não tempo desde o último uso. Antes de adaptar, é necessário verificar quem ainda pode manter referências às texturas; idade por si só não prova que estejam sem uso. O nome da opção sozinho não demonstra vazamento corrigido nem economia de RAM.

### Resolução de endereço e cache

O resolvedor alternativo começa ligado (`D.bp`); o fallback começa desligado (`D.aX`). `nh.ae(String)` consulta SRV `_minecraft._tcp` com JNDI e escolhe `dns://1.1.1.1` ou o provedor padrão. Em caso de falha, há um caminho opcional por `agB`, com espera de até cinco segundos por um `CompletableFuture`; o serviço não foi consultado nesta investigação.

`nh.af(String)` reaproveita endereços já resolvidos. O cache é criado com `expireAfterWrite(1, MINUTES)`, e a consulta SRV ocorre no caminho em que a porta interpretada é 25565. Isso atua na resolução do servidor/porta ao conectar; não acelera os pacotes da partida nem demonstra redução do ping. Não foi identificado aqui um conjunto de otimizações de protocolo próprio do CMPack equivalente às adaptações de rede já feitas no Dolly.

## Próximo passo proposto

Para um lote pequeno e verificável, os candidatos mais claros são o mouse bind fix do inventário e o InputFix de texto: ambos têm consumidores pequenos e uma lacuna confirmada nos métodos correspondentes do Dolly. Depois, screenshot assíncrona e recarga restrita de idioma podem ser avaliadas como operações isoladas.

Cache de texto merece protótipo com invalidação correta. Atualização reduzida de jogadores, cache de HUD, culling e raw input exigem testes mais amplos. Nenhum ganho numérico foi inferido do nome das opções. Click delay fix, mouse delay fix, recuperação de movimento, limites de FPS e políticas de partículas já têm trabalho correspondente no Dolly; não são funcionalidades novas por aparecerem nesta referência.
