# Dolly Client

Client PvP privado para Minecraft Java 1.8.9, instalado como versão própria no launcher, sem Forge, com OptiFine 1.8.9 HD U M5 integrado ao JAR.

## Primeira build

A base compila para Java 8 e gera um pacote de versão local. Já inclui menu próprio, configurações, fixes de entrada, controles de câmera e os primeiros recursos de renderização. A cobertura e as limitações estão no [guia do primeiro teste](docs/primeiro-teste.md).

```sh
python3 tools/build.py
```

Para o PrismLauncher, importe `dist/Dolly-Client-0.1.16-Prism.zip` em **Adicionar instância → Importar**. O build também gera `dist/dolly-1.8.9.zip` para launchers que aceitam versões locais. Usa o JAR original e bibliotecas do cache local; não altera a instalação Minecraft existente.

Para atualizar uma instância Dolly já importada, feche o jogo e a instância no Prism e execute:

```sh
python3 tools/update_prism.py "/caminho/PrismLauncher/instances/Dolly-Client"
```

Informe a pasta que contém `instance.cfg`, não `.minecraft`. O script usa o pacote da versão atual em `dist/`; para escolher outro, acrescente `--package /caminho/pacote-Prism.zip`. Ele atualiza o JAR, o patch Dolly e a versão do componente, preservando os demais componentes, o nome da instância, configurações, mundos, servidores e packs. Os arquivos substituídos ficam em um ZIP em `dolly-update-backups/` na instância; falhas de escrita tentam restaurar os originais automaticamente. Para restaurar manualmente, com a instância fechada, extraia o backup sobre a pasta da instância. O script não inicia o Prism nem o jogo. Testes do atualizador: `python3 tools/test_update_prism.py`.

A primeira execução usa VBOs, Render Regions e Fast Render ligados; VSync e balanço da câmera desligados; música 0%, iluminação suave 25% e brilho 100%. O reset preserva esses padrões. VBOs e iluminação suave são ajustados nas opções nativas do OptiFine, sem controles duplicados no menu Dolly.

A versão **0.1.12** reduz trabalho repetido de textos, scoreboard, chat, ocultação de objetos e submissão de terreno. O cache do HUD informa quando está indisponível com Fast Render e evita uma passagem extra quando sua taxa de atualização é igual ou superior ao limite de FPS. Detalhes e limites: [otimizações de renderização](docs/otimizacoes-renderizacao.md).

Abra **Dolly Client** nos menus ou pressione **Right Shift** dentro do jogo. Zoom: **C**, com roda para ajustar a aproximação; a aba Câmera permite configurar alternância, suavização e sensibilidade. Perspective: **Alt esquerdo**. Os atalhos podem ser remapeados nos controles.

O módulo **Bloquear scroll** vem ligado: selecione os itens da hotbar pelas teclas **1–9**, sem trocar o slot pela roda do mouse. A rolagem dos menus e o ajuste do zoom continuam disponíveis. Para liberar a troca pela roda, desligue o módulo no menu Dolly.

Em **Controles → Perfil de slots**, marque quais posições da hotbar devem ficar inacessíveis. O perfil pode ser ligado/desligado pelo menu ou por um atalho configurável, sem perder os slots marcados. Começa desligado e mantém pelo menos um slot livre. Com o perfil ativo, teclas de slots bloqueados não trocam a seleção; quando o scroll está liberado, a roda pula os slots bloqueados. O inventário continua disponível para organizar itens. Consulte [perfil de slots](docs/perfil-de-slots.md).

Inclui deduplicação dos vértices de modelos, otimizações de pacotes/VarInts, culling conservador de mobs e objetos de blocos, fusão de faixas do Render Regions e limite de 30 FPS ao baixar terreno. **Jogadores locais e remotos ficam excluídos do culling.** Os controles de culling, fusão e FPS de carregamento estão no menu Dolly. Consulte [escopo, testes e limites das adaptações](docs/mods/integracao.md).

Ainda falta validação gráfica: este ambiente não abriu o display `:0`. Old Animations e a integração gráfica com o OptiFine precisam de conferência visual e de comportamento dentro do jogo.

F8 inicia/encerra o profiler NORMAL; Shift+F8 inicia DEEP. As capturas JSONL ficam em `dolly/logs/` na instância. Consulte [profiling e comparação de performance](docs/profiling.md).

A versão **0.1.14** inclui [Dolly Settings e editor do HUD](docs/dolly-settings.md), com **Módulos** em lista única com scrollbar, **Configurações** técnicas agrupadas e **Editar HUD** para mover e ajustar ping, TNT, sprint, perfil de slots, scoreboard e chat. As opções incluem dicas e impacto de desempenho em cores. **Restaurar padrões** redefine as opções do Dolly e o layout do HUD.

Na aba **Visual**, **Configurar chat** permite ajustar histórico, filtros, menções, fundo, escala, dimensões, espaçamento e desaparecimento. Com o chat aberto, **Ctrl+F** busca nas mensagens recebidas; **Esc** sai da busca e **Ctrl+clique** copia a mensagem inteira. Mensagens repetidas dentro do período configurado recebem um contador; a opção **Só consecutivas** restringe o agrupamento. O histórico permanece apenas na memória da sessão, inclusive entre servidores quando essa opção está ligada; **F3+D** continua limpando o chat. Não há horário nas mensagens.

**Configurar scoreboard**, também em **Visual**, controla posição, escala, fundo, opacidade e números laterais. O atalho de visibilidade começa sem tecla atribuída e pode ser definido nessa tela ou nos controles do jogo. Veja [opções e teste de chat/scoreboard](docs/chat-scoreboard.md).

Os [mecanismos técnicos adaptados do CMPack](docs/cmpack/integracao-tecnica.md) incluem correções de mouse no inventário e texto, raw input opcional, caches, captura assíncrona, opções de carregamento, correção de frestas e resolução SRV. As opções ficam nas abas Controles, Visual e Desempenho.

A versão **0.1.15** adiciona **Ctrl+Shift+T** para enviar `/stats <adversário>` quando o TAB contém exatamente você e outro jogador, sem espectadores. O servidor precisa oferecer `/stats`; o cliente não consegue distinguir uma partida de um lobby que também tenha só dois jogadores. O inventário fica centralizado mesmo com efeitos de poção, Unlerp passa a ser permanente (sem seletor), e Parallax vem ligado em configurações novas e no reset. A ocultação inicial de projéteis termina após dois ticks ou, no máximo, 100 ms desde o primeiro desenho, mesmo se a idade da entidade ficar parada.

A leitura de eventos do sistema agora ocorre antes dos ticks de cada quadro, adaptando o mecanismo de antecipação do Input Optimizer ao LWJGL 2. Isso não incorpora os backends nativos GLFW do projeto original nem mede ganhos de latência. Veja [entrada e correções 0.1.15](docs/entrada-e-correcoes.md).

A versão **0.1.16** integra o [TACTILE com perfis por servidor](docs/mods/tactile.md), disponível em **Configurações → Entrada e câmera → Configurar TACTILE**. Começa em Vanilla; EXACT e STEPPED habilitam o motor completo nos perfis escolhidos. Hypixel e seus subdomínios preservam física vanilla mesmo quando o padrão global usa EXACT. Isso não constitui garantia de compatibilidade com anticheat; a adaptação ainda precisa de testes dentro do jogo.

## Organização

- `src/main/java/`: adaptadores do Minecraft e lógica por responsabilidade.
- `src/main/java/dolly/`: configuração independente do jogo.
- `src/test/java/`: testes automatizados.
- `tools/`: compilação e patches aplicados durante o build.
- `docs/`: especificação, arquitetura, pesquisas e instruções.
- `references/`: binários locais ignorados pelo Git; o OptiFine fornecido é uma entrada do build.
- `build/` e `dist/`: resultados gerados, ignorados pelo Git.

Consulte a [arquitetura](docs/arquitetura.md) antes de adicionar funções: interface, persistência, entrada, câmera, HUD e renderização têm arquivos separados.

## Especificação e pesquisas

- [Seleção e padrões do Dolly](docs/dolly-especificacao.md).
- [Catálogo do CMClient e decisões recebidas](docs/cmpack/catalogo-para-escolha.md).
- [Mecanismos técnicos do CMPack além dos módulos visuais](docs/cmpack/mecanismos-tecnicos.md).
- [Investigação do CMClient](docs/cmpack/analise.md) e [padrões encontrados](docs/cmpack/padroes.md).
- [VBO vanilla e Render Regions](docs/optifine/vbos.md).
- [FoamFix, EntityCulling e Krypton: oportunidades para o Dolly](docs/mods/analise.md).
- [MCOpt Linux: avaliação para PvP multiplayer](docs/mods/mcopt.md).
- [Referências locais](references/README.md).

A opção `fastWorldLoading`, na aba **Desempenho**, evita a coleta de lixo explícita e as telas intermediárias de progresso/terreno na troca de mundos. Vem ligada em configurações novas; configurações salvas preservam o valor anterior. Desligá-la restaura o comportamento original. O recebimento do mundo pelo servidor continua necessário.

O [grupo 1 de desempenho e memória](docs/mods/grupo-1.md) adiciona desenho de textos reaproveitado, HUD com padrão de 24 Hz, agrupamento e faces externas de modelos, itens com contornos mesclados, luz fixa de partículas, ícones leves dos packs e correções de referências retidas. Os novos controles ficam em **Desempenho**.

O [grupo 2 de jogadores e objetos](docs/mods/grupo-2.md) mantém jogadores fora da ocultação e permite otimizar mobs nomeados e hologramas cobertos. Na aba **Visual**, controle livros de encantamento, uma cópia por pilha no chão e itens sem balanço. As três opções começam ligadas e não alteram quantidades ou interações.

O [grupo 3 de câmera e iluminação](docs/mods/grupo-3.md) inclui zoom configurável de 1× a 600×, Fullbright com luz máxima e proteção para shaders, Parallax ligado por padrão, Better Camera, correção opcional do céu do OptiFine e correção do horário visual. Para escolher o horário, ligue **Time Changer** na aba **Visual**.

O [grupo 4 de limpeza visual](docs/mods/grupo-4.md) adiciona controles de fogo, água/névoa, partículas próprias de poção, fragmentos, raios, projéteis recém-lançados e opacidade do cavalo montado. Os controles ficam em **Visual**; fogo, névoa e cavalo começam em 100%, com ajuste percentual.

O [grupo 5 de áudio](docs/mods/grupo-5.md) adiciona **Visual → Configurar sons…**, com busca e barras individuais de 0% a 200%. A recarga separada vem ligada: **F3+S** recarrega sons e **F3+T** recarrega recursos visuais preservando o áudio. O menu apresenta falhas de lista/recarga e mantém a navegação disponível.

O [grupo 6 de chat e TAB](docs/mods/grupo-6.md) amplia o agrupamento de repetições com período configurável, mostra as ações de mensagens clicáveis e prévias de links diretos de imagens, guarda comandos clicados no histórico e exibe ping numérico no TAB. Em **Visual → Configurar chat**, ative **Fila de envio** e escolha o intervalo do servidor; **Ctrl+Backspace** cancela a fila.

O [grupo 7 de screenshots](docs/mods/grupo-7.md) combina a captura segura do Dolly com prévia, copiar imagem, favoritos, abrir pasta e exclusão com confirmação. **Visual → Gerenciar capturas…** mostra até 32 capturas da sessão. As funções são exclusivamente locais, sem upload.

O [grupo 8 de configurações e menus](docs/mods/grupo-8.md) adiciona barras aos valores numéricos do Dolly, chat e scoreboard, com ajuste por setas e Home/End. Em **Controles**, configure o botão LAN (padrão: Servidores), as opções de reconexão e a confirmação de saída. Reconectar e confirmar saída vêm ligados.

O [grupo 9 de correções e manutenção](docs/mods/grupo-9.md) simplifica o contador do F3, restaura o gráfico de tempos do Alt+F3, corrige a caixa preta no vazio e o efeito residual ao remover náusea, protege os pedidos de resource packs e limpa logs datados antigos. A retenção padrão é de **30 dias**, configurável em Desempenho. A redução de atualizações visuais do ambiente já integrada permanece no grupo 1; o Mouse Delay Fix atual foi mantido.
