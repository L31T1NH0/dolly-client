# CMClient como referência para o Dolly

## Resultado

O pacote contém mudanças reais de entrada, mira, animações e renderização. Os candidatos mais fortes para a sensação padrão de PvP são o bloqueio de clique após erro desativado, o cálculo corrigido de direção de mira, a recuperação de teclas ao retornar ao jogo e Toggle Sprint. Animações antigas também são candidatas se tiverem sido ativadas pelo usuário. A análise demonstra esses mecanismos no bytecode; não demonstra qual deles causou a experiência pessoal do usuário.

O [aprofundamento dos padrões](padroes.md) corrigiu uma distinção desta primeira análise: opções de Old Animations começam com valores verdadeiros, mas o módulo começa desligado e bloqueia sua aplicação. Valor inicial de uma opção não significa recurso ativo.

O [levantamento de mecanismos técnicos](mecanismos-tecnicos.md) aprofunda entrada, cache de texto/HUD, atualização de jogadores, carregamento, screenshots e resolução de endereços, incluindo diferenças confirmadas na base do Dolly.

## Alvo e método

- Arquivo: `references/cmpack_1.8.8_2.8.11-beta.zip`.
- Versão declarada em `info.txt`: CMClient 2.8.11, Minecraft **1.8.8**, build 10/9/2025. A base pretendida do Dolly é **1.8.9**.
- SHA-256 do ZIP: `43e45120f1ffaf8793b692427a9b6eea376aadc56b054c608861b548a51722cc`.
- O ZIP contém um bootstrap, JSON de versão, `CMPack_app.jar` e instruções. O JSON inicia `pl.cmpack.Main`, declara Java 8 e LWJGL 2.
- `CMPack_app.jar`: 7.642 entradas, 3.366 classes JVM; nomes majoritariamente ofuscados. Contém recursos OptiFine e identificador `OptiFine_1.8.8_HD_U_H8`; isso identifica o pacote, sem verificar equivalência integral com a distribuição oficial.
- REA confirmou a identidade/inventário do ZIP. Evidência da inspeção: `ev_1c87bdd2fa0d5775996f493429d745d67b8aa6bcbfba749804b2bdfce9a16e48`; inventário: `ev_6d9c8c615ff0fc9ddfccf081b3b156f4f4b796d9d64bb7e64f902f811a443de8`.
- O aprofundamento foi feito em bytecode JVM, com resolução do constant pool e das constantes inteiras ofuscadas dos pools de strings, e inspeção de classes selecionadas com `java -m jdk.jdeps/com.sun.tools.javap.Main`.
- Nenhum código do client foi executado. Não foi necessário iniciar sessão, entrar em servidor ou fornecer credenciais.
- As localizações abaixo são **classe, assinatura e offset de bytecode**, não linhas de código-fonte recuperado. Evidências e hashes estão em [evidence/manifest.json](evidence/manifest.json). Os trechos `.bytecode.txt` preservam offsets e referências simbólicas; as constantes continuam como no binário.

## Achados ligados à jogabilidade

### 1. Click delay fix: candidato mais direto para a interrupção de combos

**Observado:** `D.<init>()V`, offsets 426–477, cria a opção `hitDelayFix` / “Click delay fix” com valor inicial verdadeiro (`D.aN`). Em `ir.gr()V`, o ataque é impedido quando o contador `ir.Fl` é positivo. Nos caminhos de ausência de alvo/erro, a atribuição de **10** ao contador só ocorre quando o fix está desligado e o modo do jogador permite essa penalidade. O loop de atualização decrementa esse contador.

Locais: `ir.gr`, offsets 0–4, 50–69 e 207–236; `ir.hC()V`, offsets 725–739. Ver [ir.bytecode.txt](evidence/ir.bytecode.txt) e [D.bytecode.txt](evidence/D.bytecode.txt).

**Interpretação:** ao errar um ataque, esse fix evita uma penalidade local que pode atrasar a próxima tentativa. Dez ticks correspondem nominalmente a cerca de meio segundo em 20 TPS. É uma explicação plausível para o relato de parar de bater durante um combo.

**Limite:** não recupera eventos que o sistema operacional nunca entregou nem garante que o servidor aceite um golpe. O bloqueio também tem outros usos, como transições de telas; a implementação no Dolly deverá tratar a causa específica, sem zerar indiscriminadamente o contador a cada frame.

### 2. Mouse delay fix: direção da mira, separado de raw input

**Observado:** opção `mouseDelayFix` (`D.bl`) começa verdadeira. `zX.at(F)LQt;`, offsets 0–27, identifica o jogador local (`jp`) e, com o fix ligado, chama a implementação de `zI.at(F)LQt;`. O caminho normal da subclasse usa os campos de rotação `aRU/aSA`; o caminho da base usa `aQb/aPF`. A interpolação de pitch permanece. O vetor retornado participa do traçado de mira em `zI.q(DF)LPY;`.

Ver [zX.bytecode.txt](evidence/zX.bytecode.txt) e [zI.bytecode.txt](evidence/zI.bytecode.txt).

**Interpretação:** a troca evita usar uma orientação alternativa do corpo/cabeça no cálculo da direção do jogador local. Pode tornar a seleção de alvo mais coerente com a rotação de mira durante movimentos rápidos.

**Limite:** não foi medida latência de mouse. Isso não demonstra raw input, aumento de alcance ou qualquer ganho de registro no servidor. Na base do Dolly com OptiFine M5, `bew` herda de `bet`, cujo `d(float)` já usa diretamente pitch/yaw atuais (`z/y`). Esse override não chamava `pr.d(float)`, onde o patch Dolly havia sido aplicado. O Dolly agora encaminha apenas o jogador local de `bet.d(float)` para a implementação herdada e aplica o mesmo seletor do CMPack em `pr.d(float)`: ON chama `pk.d(float)` (yaw do jogador), OFF mantém yaw da cabeça. Ambos preservam a interpolação de pitch/yaw. Os demais jogadores mantêm o caminho OptiFine. Uma regressão executa ambos os estados com rotações distintas e três valores de interpolação. A seleção de alvo ainda precisa de validação visual no jogo.

### 3. Modern Keybind Handling: relação direta com o inventário

**Observado:** `modernKeybindHandling` (`D.bD`) começa verdadeiro. `ir.gy()V`, offsets 0–35, ao recuperar o foco do jogo, testa a opção e chama `wn.vh()V`, sob uma condição adicional de ambiente (`ir.Ga`). `wn.vh()` percorre os atalhos e atualiza seu estado com `Keyboard.isKeyDown` para códigos entre 0 e 255.

Ver [wn.bytecode.txt](evidence/wn.bytecode.txt) e [ir.bytecode.txt](evidence/ir.bytecode.txt).

**Interpretação:** recupera teclas que já estão seguradas, em vez de esperar outro evento de pressionamento. É o mecanismo que queremos para voltar do inventário segurando W/A/S/D ou teclas remapeadas.

**Limite:** ainda falta reproduzir a transição de inventário e perda de foco no Linux. A rotina do CMClient atualiza todos os atalhos de teclado suportados, não apenas movimento. Para Dolly, testar separadamente movimento, ações de uso único e atalhos de mouse.

### 4. Old Animations: feedback de espada e bloqueio

**Observado:** `au` define separadamente animações de bloquear, bater com espada, comer, atacar, cor de dano de armadura e corações com valores iniciais verdadeiros. Posição antiga de itens segurados, arco e vara começa falsa. Entretanto, o módulo começa **desligado**: `M.bT` começa falso, `au` não o ativa e `au.bS=true` faz `bP.ak()` retornar falso enquanto o módulo está inativo. Há consumidores concretos no render: `qw` consulta espada, `tN` consulta bloqueio, `tE` consulta cor de dano e `jP` consulta corações.

**Interpretação:** movimento da espada e transição de bloqueio podem mudar bastante a percepção de ritmo e continuidade dos cliques quando o módulo é ativado. É um candidato para inspiração visual, mas não uma explicação da instalação limpa.

**Cuidado técnico concreto:** a opção “1.7 attacking” (`au.fM`) também altera lógica em `ng.mH()Z`, `ng.r(LOt;LPh;)Z` e `ir.c(Z)V`. Não é correto classificar o módulo inteiro como cosmético. Em `ng.r`, ela permite retorno antecipado quando o jogador está usando um item; em `ir.c`, altera uma condição que normalmente interrompe o processamento nesse estado.

Ver [ng.bytecode.txt](evidence/ng.bytecode.txt). Para Dolly, manter controles de animação independentes e revisar comportamento de ataque/uso antes de reproduzir qualquer parte funcional.

### 5. Raw Mouse Input existe, mas não explica o padrão por si só

**Observado:** `rawMouseInput` (`D.aS`) começa **falso**. Quando ligado, `afM` substitui a captura por `afN`, consulta um mouse via JInput e acumula deltas em uma thread separada. `afN.Vl()` transfere e zera os acumuladores. A seleção de dispositivos usa `DirectAndRawInputEnvironmentPlugin`.

Ver [afM.bytecode.txt](evidence/afM.bytecode.txt) e [afN.bytecode.txt](evidence/afN.bytecode.txt).

**Limite:** não há configuração pessoal no ZIP para saber se o usuário o ativou. A compatibilidade desse backend com Linux não foi demonstrada. Não transplantar esse caminho sem validar suporte, sincronização dos acumuladores e fallback.

### 6. InputFix não é o fix Linux de Shift+2/Shift+6

**Observado:** `an` é o módulo “InputFix”, inicialmente ativo. O consumidor examinado, `kB.kU()V`, usa `Keyboard.getEventCharacter`, `getEventKey`, `getEventKeyState` e `Character.isDefined` para despachar caracteres em telas mesmo em um caso de keycode zero.

Ver [an.bytecode.txt](evidence/an.bytecode.txt) e [kB.bytecode.txt](evidence/kB.bytecode.txt).

**Conclusão limitada ao fluxo:** trata entrada de texto em GUI. Não comprova a correção das combinações Linux discutidas anteriormente. Essas continuam um requisito separado do Dolly.

## Desempenho, câmera e recursos para inspiração

| Recurso | Evidência estática | Relevância para Dolly |
| --- | --- | --- |
| Cache de largura de texto | `D.aE` começa verdadeiro; `jr.N(String)` usa `Map.computeIfAbsent`, limpa acima de 5.000 entradas e tem caminho sem cache | Útil quando HUD/chat repetem textos; verificar invalidação ao mudar fonte/recursos |
| Cache de HUD | `afH` usa framebuffer, redesenha HUD quando invalidado e apresenta a textura entre atualizações; scheduler `afI`/`afK`; limite inicial de 60 Hz | Pode economizar render com muitos overlays, mas também limitar atualização visual do HUD |
| Culling de entidades | `D.bs` começa verdadeiro; worker `afC.run` atualiza estados `afD` consumidos pelo render | Candidato para cenas lotadas; algoritmo completo e correção de visibilidade ainda precisam de testes |
| Atualização de jogadores otimizada | `D.bv` começa verdadeiro; `jo` retorna antes de cálculos adicionais após atualizar posição/rotação | Existe mudança real, mas falta medir economia e efeitos visuais |
| Balanço mínimo da câmera | `D.bu` começa **falso**; `pM` condiciona a chamada de bobbing | Bom controle opcional; não atribuir a sensação padrão a ele |
| Tremor ao tomar dano | `D.aM` (“Show hurt camera”) começa **verdadeiro**; `pM.O(F)` testa o valor | Permitir ajuste; a remoção não é o padrão encontrado |
| Zoom | `aS` ativo, nível inicial 4; suavidade de zoom/câmera e scroll inicialmente ligados; interpolação de FOV em `aS.t(FF)` | Feature bem definida e independente do combate |

Trechos de câmera, animações e desempenho estão em [features.bytecode.txt](evidence/features.bytecode.txt), com hashes das classes e indicação das constantes normalizadas.

O cache de HUD começa conforme suporte a framebuffer (`qy.pE()`), não simplesmente sempre ativo. Nenhum ganho de FPS ou frametime foi medido. O label “Smooth Camera Movement” pertence ao zoom; não comprova suavização permanente da mira.

Outros módulos identificados, **sem reconstrução completa de comportamento**: crosshair, block overlay, damage tint, hit color, hitbox, partículas, perspective, waypoints, TNT timer, chat, tab, nametags, horário/clima e glint. Eles são um inventário de possibilidades, não explicações demonstradas para a jogabilidade.

## Prioridades propostas para o Dolly

1. Corrigir o ciclo de clique após erro e validar a direção de mira. São os candidatos mais diretos para a sensação de resposta em combate.
2. Recuperar teclas ao fechar inventário e manter o fix Linux de Shift+2/Shift+6 como trabalho separado.
3. Oferecer animações de espada/bloqueio configuráveis, separando apresentação de lógica de uso/ataque.
4. Adicionar zoom suave, câmera configurável e HUD simples. Usar cache de texto onde houver repetição medida.
5. Só adicionar cache de HUD/culling após medir necessidade e validar atualização visual/visibilidade.

## Como identificar o que você sente na prática

Comparar o próprio CMClient com cada opção ligada e desligada, uma por vez, mantendo mundo/servidor, sensibilidade, FOV, resource pack, limite de FPS e condições de conexão. Guardar a configuração usada antes do teste.

- Clique: errar no ar e imediatamente voltar ao alvo, comparando intervalos entre cliques e ataques processados.
- Mira: mover rapidamente a câmera atravessando um alvo e comparar a direção/seleção de alvo, com mouse delay fix ligado e desligado.
- Teclado: segurar movimento ao fechar inventário, pressionar dentro dele, soltar dentro dele e alternar foco da janela.
- Animações: comparar espada/bloqueio sem mudar as opções funcionais de ataque ao mesmo tempo.
- Desempenho: medir distribuição de tempo por frame na mesma cena, além de FPS médio; comparar HUD/culling separadamente.

Esses testes **não foram executados** nesta análise. Também não foram fornecidos configs pessoais, logs de sessão ou um vanilla de referência para comparar integralmente os métodos. A contribuição subjetiva de cada mudança permanece uma hipótese testável.

## Cobertura

| Pergunta | Estado |
| --- | --- |
| Qual artefato e versão foram enviados? | Respondida pelo pacote e hashes |
| Há fixes reais de clique, mira e recuperação de teclas? | Respondida estaticamente nos caminhos descritos |
| Há animações/otimizações reais além dos labels? | Respondida nos caminhos selecionados; inventário restante é parcial |
| Qual opção estava ativa na sua instalação? | Não resolvida; só defaults de construção disponíveis |
| Qual mecanismo explica sua preferência? | Parcial: candidatos identificados; faltam testes comparativos |
| O que funciona igual no Linux e na 1.8.9? | Não validado em execução |

Nenhuma implementação do Dolly foi alterada; este material é uma referência de investigação.
