# CMClient: aprofundamento das configurações padrão

Análise estática adicional do mesmo CMClient 2.8.11 / Minecraft 1.8.8 fornecido. Nenhuma implementação do Dolly foi iniciada. O [relatório inicial](analise.md) identifica o artefato e seus hashes.

## O que conta como padrão

Há quatro estados diferentes: valor inicial de uma opção, estado do módulo, alterações posteriores na inicialização e valores carregados de arquivos pessoais. O botão de restaurar opções gráficas também tem valores diferentes dos da primeira execução.

Foi catalogado o construtor das **58 configurações gerais** de `D` em [defaults-gerais.json](defaults-gerais.json). O catálogo inclui campos, chaves, offsets e valores iniciais; não é um dump de uma sessão real. Índices de enumeração não são interruptores booleanos. Cores que exigem interpretação adicional ficam sem valor no catálogo.

## Correção importante: Old Animations não começa ativo

`M.w(Z)` escreve o estado `M.bT`, lido por `M.ad()`. Esse campo começa falso. `au` registra opções de animação 1.7 como verdadeiras, mas não liga o módulo. Além disso, `au.bS=true` faz `bP.ak()` devolver falso enquanto o módulo estiver inativo.

Assim, as animações antigas e a opção funcional “1.7 attacking” **não explicam sozinhas uma instalação limpa**. Elas podem explicar sua experiência se você as ativou ou importou um perfil. O relatório inicial foi corrigido para registrar essa diferença.

Já o módulo `O`, chamado “Animations”, é diferente: ele se ativa e habilita animações de GUI e de agachar. Não confundir os dois módulos.

## Padrões relevantes para jogar

| Configuração | Padrão observado | Significado e limite |
| --- | --- | --- |
| Click delay fix | Ligado | Evita aplicar a penalidade local de 10 ticks após erro; não garante registro no servidor |
| Mouse delay fix | Ligado | Troca a origem da rotação usada no vetor de mira do jogador local |
| Modern Keybind Handling | Ligado | Relê teclas ao recuperar foco; candidato direto para movimento após inventário |
| Toggle Sprint | Módulo e opção ligados | A tecla alterna o sprint; não começa correndo automaticamente |
| Toggle Sneak | Opção desligada | Não presume agachamento alternado por padrão |
| Raw Mouse Input | Desligado | Não atribuir a sensação padrão à captura alternativa |
| Old Animations | Módulo desligado | Preferências 1.7 só entram em efeito depois de ativá-lo |
| Animações de GUI/agachar | Ligadas no módulo Animations | Velocidades iniciais 3 e aproximadamente 8,5; são unidades da configuração |
| Animação de chunks | Desligada nesse módulo | Não explica a sensação visual padrão |
| Mouse bind fix | Ligado | Em tela de inventário, botões extras entram num caminho adicional de atalhos da hotbar; validar bindings reais antes de prometer compatibilidade geral |
| Hotbar por scroll | Ligada | O zoom pode consumir a roda antes da troca de slot; não tratar os dois caminhos como independentes |
| InputFix | Módulo ligado | Trata eventos de caracteres em telas; distinto do fix Shift+2/Shift+6 do Linux |
| Minimal View Bobbing | Desligado | Não há redução padrão por essa opção |
| Show hurt camera | Ligado | O tremor de dano permanece |
| Dynamic FOV | Ligado | Não é um preset de FOV completamente estático |

Toggle Sprint: `aO.<init>`, opção `hK` configurada em 359 e ativação do módulo em 443; Toggle Sneak `ht` em 400. A ativação de um módulo deve ser conferida nos consumidores: nem todo evento é automaticamente filtrado por `M.ad()`.

## Desempenho: os detalhes menos óbvios

| Configuração | Valor inicial | Comportamento confirmado |
| --- | --- | --- |
| Limite de partículas | 1.000 | `nM.r(Los;)V` verifica cada lista interna selecionada por camada/tipo de transparência. Ao atingir o limite, remove a primeira antes de adicionar outra. Não é um teto global de 1.000 partículas |
| Void fog/barrier particles | Render desligado | `ir.hC`, 2354–2397, só chama a rotina de ambientação `nm.k` quando habilitado |
| Rotina otimizada de void/barrier | Ligada | Se a rotina for chamada, `nm.k`, 83–100, escolhe 100 iterações em vez de 1.000. Com o render desligado, esse caminho normalmente é evitado antes |
| Cache de texto | Ligado | Guarda larguras repetidas; identificado na análise inicial |
| Entity culling | Ligado | Worker e consumidores reais; algoritmo/benefício precisam de validação em jogo |
| Atualização otimizada de jogadores | Ligada | Retorno antecipado comprovado em `jo`; não foi medida a economia |
| Cache de HUD | Conforme suporte a framebuffer | HUD tem limite inicial próprio de 60 Hz; o mundo pode ser apresentado em outro ritmo |
| FPS com janela inativa | 30 | `ir.gw`, 1553–1579, usa limite quando não há foco de jogo e a janela está inativa |
| FPS do menu | Taxa do primeiro monitor, fallback 60 | `D.O()` consulta AWT; `ir.gA()` usa esse valor sem mundo e com tela aberta. Não é garantia de usar o monitor atual |
| Partículas de menu | Desligadas | Interface não começa com esse efeito extra |
| Screenshot assíncrona | Desligada | Não explica fluidez contínua padrão |
| Fast world loading / fast language switch | Desligados | Não inferir que toda opção com “fast” esteja ativa |
| Limpeza de memória ao desconectar | Desligada | Não há ativação padrão dessa opção experimental |

## Gráficos: primeira execução e reset diferem

| Ajuste | Construtor / primeira execução sem opções pessoais | Reset gráfico `wj.uY()` |
| --- | --- | --- |
| Fast Render | Ligado | Desligado |
| VSync | Ligado | Desligado |

O limite de FPS começa temporariamente em 120, mas é sobrescrito **no mesmo construtor** para o máximo de `wm.aHC`, 260. O render usa esse máximo como sentinela de ilimitado. Isso **não significa FPS efetivamente ilimitado** com VSync ativo ou opções pessoais carregadas.

Outros valores encontrados em `wj`:

- Sensibilidade **0,5**, FOV **70**, distância **8 chunks**. A distância também é sobrescrita durante o construtor; não usar a atribuição transitória de -1 como padrão final.
- Smooth FPS **desligado**, Fast Math **desligado**, antialiasing **0**, filtragem anisotrópica **1**.
- View bobbing **ligado**, gamma inicial **0**, mipmaps **4**, sombras de entidades **ligadas**.
- Custom fonts/colors/sky/items e várias animações/partículas continuam ligadas. O pacote não começa simplesmente desabilitando todos os efeitos.
- VBO usa `ahe.awe()`: a expressão retorna verdadeiro para a classificação de renderer Intel. É dependente da identificação OpenGL e de sua ordem de inicialização; não é um critério universal a copiar para qualquer GPU.
- Smooth World e Lazy Chunk Loading do OptiFine consultam estado de CPU; a atribuição do contador e a ordem de binding impedem classificá-los aqui como um booleano universal. Isso é separado do seletor “Lazy Chunk Loading” de `D`, que começa em Off (Vanilla).

Não foi usado um vanilla separado como baseline: esses são valores deste pacote, sem comprovação de que cada um seja exclusivo do CMClient.

## Diferenças por sistema e conexão

- Borderless Fullscreen só recebe verdadeiro no construtor quando o sistema é classificado como **Windows** e a resolução fullscreen é “Default”. No Linux, esse critério inicial dá falso. O caminho existe em `ir.hA`, mas não foi executado.
- “Alternate DNS resolver” começa ligado. O fluxo de consulta **SRV** em `nh.ae(String)` usa `dns://1.1.1.1`, em vez do provedor DNS padrão nesse caminho. Isso diz respeito à resolução do endereço/porta de um servidor; não demonstra redução de ping durante a partida.
- “Fallback DNS resolver” começa desligado. O caminho alternativo existe, mas não foi acionado nem consultado durante a análise.
- O label “Anti Cheat” começa com opção verdadeira, mas a inicialização condiciona a criação do worker a `acQ.cRt`; fora da condição, oculta a opção e força falso. A condição examinada identifica Windows; esse backend não é iniciado no Linux. Não foi feita engenharia reversa completa desse subsistema.

## Arquivos pessoais e persistência

`wj` aponta para `options.txt`, `optionsof.txt` e `optionscp.dat`. A busca pelo campo de `optionscp.dat` só demonstrou sua atribuição, não uma leitura. `uO()` lê `options.txt` e depois chama `uV()` para as opções OptiFine. Se `options.txt` não existe, o retorno inicial impede essa segunda chamada pelo construtor, mesmo que `optionsof.txt` exista isolado.

Os módulos têm persistência separada: após construir/registrar os módulos, `z.m()` chama `x.A()`. Os leitores de configuração podem restaurar `enabled` e propriedades; chaves ausentes preservam os valores do construtor nos caminhos examinados. Isso explica por que uma instalação reutilizando a pasta/perfil pode se comportar de forma diferente do padrão novo.

## O que isso muda para o Dolly

A inspiração mais forte nos padrões continua sendo entrada/mira corrigidas e movimentação confortável, agora incluindo Toggle Sprint. As opções gráficas merecem escolhas explícitas: VSync ligado não é sinônimo de latência mínima, e cache de HUD a 60 Hz pode mudar a atualização percebida de overlays.

Animações antigas são uma opção útil, mas a análise aprofundada não sustenta dizer que vêm ativas de fábrica. Também não há evidência de sensibilidade secreta ou de que todos os recursos opcionais tenham causado a experiência do usuário.

Antes de escolher padrões definitivos para o Dolly, comparar no Linux: VSync/limite FPS, Fast Render com cache de HUD, animações de agachar, Toggle Sprint e os fixes de clique/mira, variando uma opção de cada vez.

## Evidência e limites

- [Catálogo de 58 configurações gerais](defaults-gerais.json).
- [Inventário dos 54 módulos e seus estados iniciais](modulos-padroes.md).
- [Trechos adicionais de configurações gerais e consumidores](evidence/default-consumers.bytecode.txt).
- [Ativação de módulos e persistência](evidence/module-defaults.bytecode.txt).
- [Gráficos, carregamento e reset](evidence/graphics-defaults.bytecode.txt).
- [Evidência da primeira análise](evidence/manifest.json).

Trechos de bytecode identificam classe, método, offset e SHA-256 da classe extraída do mesmo JAR. Constantes normalizadas são uma ajuda de leitura, não código-fonte original. Não houve execução do client, medição de FPS/latência, comparação integral com vanilla ou acesso a configurações pessoais. Portanto mecanismos/defaults estáticos estão demonstrados nos caminhos citados; o efeito na sua sessão permanece por testar.
