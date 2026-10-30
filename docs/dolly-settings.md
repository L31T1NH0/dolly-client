# Dolly Settings e editor do HUD

Implementação nativa baseada no protótipo aprovado em `prototypes/settings/index.html`. A versão 0.1.14 corrige os botões largos e usa o HUD real para edição. O pacote Prism está em `dist/Dolly-Client-0.1.14-Prism.zip`; a validação gráfica dentro do jogo continua pendente.

## Navegação

O menu principal oferece **Módulos**, **Configurações** e **Editar HUD**.

**Módulos** é uma lista única com 21 recursos, ícones de itens do próprio Minecraft, busca por nome e filtros Todos/ON/OFF. Cada linha separa o controle de ativação da seleção do módulo. A roda e a scrollbar percorrem a lista, sem paginação. Em telas largas, selecionar mostra uma descrição e o botão de ajustes; em telas pequenas, abre diretamente os ajustes do módulo. Os detalhes também usam rolagem.

Recursos como Zoom, Perspective, Toggle Sprint, Ping, TNT Timer, perfil de slots, scoreboard e alterações visuais ficam nesta lista. Caches, correções de entrada, carregamento e opções técnicas ficam em **Configurações**, nos grupos:

- Entrada e câmera;
- Vídeo e desempenho;
- Carregamento;
- Chat;
- Efeitos visuais;
- Capturas e áudio;
- Menus e conexão;
- Manutenção e F3.

Os grupos e seus controles possuem scrollbars independentes. Botões largos esticam somente o miolo da textura nativa, mantendo bordas, texto e área clicável na mesma posição. As opções de Zoom ficam nos detalhes de Zoom; brilho inteligente fica em Fullbright; horário fica em Horário visual. O perfil e o scoreboard continuam oferecendo suas telas próprias. Chat e scoreboard mantêm os assuntos agrupados, com rolagem dentro do assunto e acesso ao editor do HUD.

Os identificadores e valores das opções existentes foram preservados. O catálogo expõe também os interruptores de perfil de slots e scoreboard na lista principal de módulos. Não há migração das preferências antigas.

## Editor do HUD

É possível selecionar, arrastar e configurar separadamente **Ping**, **TNT Timer**, **Sprint**, **Perfil de slots**, **Scoreboard** e **Chat**. O editor abre sobre o mundo, em tamanho real, e usa o conteúdo e as medidas renderizadas pelo jogo. Ele fica disponível quando há um mundo aberto e não pausa a partida. Elementos sem conteúdo ou ocultos recebem uma área identificada, sem valores de exemplo.

- Clique em um elemento ou use `<`/`>` para selecioná-lo.
- Arraste para mover. O alinhamento opcional aproxima bordas e centro.
- Com o elemento selecionado, setas movem 1 pixel; Shift + setas movem 10 pixels. Selecionar uma barra direciona as setas para ela.
- Use **Opções** ou clique com o botão direito no elemento para abrir escala, fundo e visibilidade. Ocultar um indicador não desativa a corrida ou o bloqueio de slots.
- Desfazer ou Ctrl+Z restaura até trinta estados desta edição.
- Restaurar posições mantém escala, fundo e visibilidade.
- Concluir ou Esc retorna diretamente ao jogo. Soltar o arraste/barra, usar o teclado ou fechar o editor salva as alterações. Não há escrita em disco a cada movimento do mouse.

As posições são percentuais do espaço livre, levando em conta o tamanho do elemento, para acompanhar resolução e escala da GUI. O desenho ajusta a escala efetiva ao espaço disponível. O chat mantém espaço para o campo de digitação e aplica o deslocamento também à busca do componente clicável, cópia e links. O scoreboard continua usando o objetivo recebido do servidor.

`hudLayout` guarda até seis entradas validadas, com posição, escala, visibilidade e fundo; `hudSnap` guarda a preferência de alinhamento. O layout é interpretado somente quando muda. Sem uma entrada personalizada, cada elemento conserva seu posicionamento anterior. Escala e fundo de chat/scoreboard continuam compartilhados com suas opções existentes. Restaurar todos os padrões do Dolly limpa a personalização do HUD.

## Dicas de configuração

Manter o mouse sobre uma configuração por 300 ms mostra seu nome completo, descrição e impacto:

- **Cinza:** custo mínimo/baixo ou ausência de trabalho contínuo.
- **Verde:** trabalho que pode ser evitado ou reduzido.
- **Amarelo:** custo adicional de CPU/GPU, memória, rede ou disco.
- **Dourado:** resultado variável ou uma troca entre custos.
- **Vermelho:** recargas e ações com custo pontual mais elevado.

Cada indicação também explica o impacto por texto; não há porcentagens de uso ou garantias de ganho. As linhas são preparadas apenas ao mudar a opção sob o mouse. As dicas incluem módulos, ajustes técnicos, chat, scoreboard, perfil de slots, áudio, restauração de padrões e controles do editor.

## Verificação

Foram preparadas regressões para cobertura única do catálogo, navegação por scroll, limites dos controles, filtros, dicas, coordenadas proporcionais, alinhamento, validação e persistência do layout, restauração dos padrões, desfazer, cobertura contínua da textura de botões largos (inclusive larguras ímpares), arraste nas medidas reais do HUD e preservação dos links após mover/ocultar o chat.

A inspeção estática cobre os tamanhos 320×180, 320×240, 427×240, 640×360 e 854×480. O build completo (`python3 tools/build.py`) passou, incluindo essas regressões. Ainda é necessário conferir o resultado dentro do jogo, inclusive com resource packs, escalas de GUI diferentes, chat aberto e conteúdo variável do scoreboard/TNT.
