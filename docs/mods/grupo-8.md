# Grupo 8: controles de arrastar e menus

## Valores numéricos

As opções numéricas adequadas agora usam barras de arrastar. Clique na barra e arraste para aplicar o valor; solte para salvar. Depois de selecionar uma barra, as setas ajustam um passo, Shift+seta ajusta dez passos e Home/End selecionam os extremos. Os controles continuam mostrando o valor atual quando o nome é abreviado em telas pequenas.

Na tela principal do Dolly, a mudança cobre FPS inativo/menu/carregamento, limite de partículas, fator e sensibilidade do zoom, horário visual, frequência do HUD e os percentuais de fogo, água e cavalo. O horário percorre 0 a 23.999 ticks, com o horário correspondente no rótulo. O valor 0 de FPS de menu continua significando o limite do monitor.

Nas telas de chat e scoreboard, as barras cobrem as opções numéricas já existentes, incluindo o intervalo de envio da fila, período de agrupamento, histórico, dimensões, escala, espaçamento, opacidades, duração/fade e posição/escala do scoreboard. A janela de agrupamento pode ser ajustada em passos de um segundo. Som individual já possuía barras e foi preservado.

Atalhos, campos de texto, funções ON/OFF e escolhas de modo continuam como botões. Isso inclui a curva do zoom, modo de correção de frestas e atraso de chunks: seus números representam alternativas com comportamentos distintos.

`DollySlider` implementa a interação comum; `DollyNumeric` conecta os valores aos campos validados. Os limites seguem o contrato da configuração. Durante o arraste, o comportamento é aplicado e o HUD é invalidado; a gravação ocorre ao soltar, ao ajustar pelo teclado ou ao fechar a tela. Não há regravação de arquivo a cada pixel percorrido.

## Navegação e saída

Em **Controles**, ficam:

| Opção | Padrão | Comportamento |
| --- | --- | --- |
| Botão LAN | Servidores | Substitui “Abrir para LAN” por acesso à lista de servidores. Outros modos: Oculto e Original. Original preserva também a disponibilidade nativa do botão. |
| Menu de reconexão | ON | Ao clicar em desconectar de um servidor comum, oferece **Sair do servidor**, **Reconectar** e **Cancelar**. Também adiciona Reconectar à tela de desconexão quando há um destino conhecido na sessão. |
| Confirmar saída | ON | O botão de sair do menu principal pede confirmação antes de fechar o Minecraft. Cancelar/Esc retorna ao menu. |

A escolha de reconexão no menu de pausa é restrita a servidores comuns: saída de mundo local e o fluxo especial de Realms permanecem nativos. O destino é lembrado ao iniciar a conexão, com cópia do endereço e preferência de resource pack, e também cobre o construtor de conexão direta. Não há reconexão automática nem repetição em segundo plano.

A confirmação de saída cobre o botão de sair do menu principal; não intercepta o fechamento da janela pelo sistema operacional. Desligar as opções devolve o comportamento original dos respectivos botões.

`DollyNavigation` concentra as políticas e `DollyChoiceScreen` fornece escolhas com cancelamento. `MenuPatches` liga menu principal, pausa, início de conexão e tela de desconexão. O caminho de **Sair do servidor** reutiliza a ação original do Minecraft; reconectar encerra o mundo atual antes de iniciar uma nova conexão nativa.

## Validação

`DollyGroupSevenEightTest` cobre os limites, arraste sem gravação por pixel, gravação ao soltar, setas e Home/End, distinção entre valores e modos, os três modos do botão LAN, confirmação/cancelamento de saída, cópia do destino, disponibilidade do botão na tela de desconexão e cancelamento da saída sem descarregar o mundo. Também verifica persistência/reset e os hooks no JAR final.

Os testes de interface existentes percorrem todas as páginas em resoluções diferentes e verificam limites/ausência de sobreposição. Falta conferir visualmente as barras e confirmar a reconexão com um servidor real; os testes não abrem uma conexão de rede.
