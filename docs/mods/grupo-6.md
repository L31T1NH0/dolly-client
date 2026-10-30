# Grupo 6: chat e TAB

Os controles ficam em **Visual → Configurar chat**. Preferências antigas recebem os novos padrões somente para chaves ausentes. O reset geral do Dolly restaura os padrões abaixo.

| Controle | Padrão | Efeito no jogo |
| --- | --- | --- |
| Agrupar repetidas | ON | Reúne mensagens iguais com contador e move a repetição mais recente para o final do histórico. |
| Só consecutivas | OFF | Desligado permite reunir repetições mesmo com outras mensagens no meio; ligado mantém o comportamento anterior do Dolly. |
| Agrupar por (s) | 120 | Repetições além desse tempo desde a última ocorrência ficam separadas. Ajustável entre 1 e 600 segundos. |
| Fila de envio | OFF | Ligado espaça mensagens e comandos enviados pelo jogador, mantendo a ordem de envio. |
| Intervalo de envio (s) | 1 | Intervalo de 0,1 a 60 segundos entre envios, em passos de 0,1. Use o intervalo adequado ao servidor. Shift+clique diminui. |
| Mostrar ações dos links | ON | Passar o mouse mostra o comando, sugestão, arquivo ou endereço associado. O clique e a confirmação de links do Minecraft continuam nativos. |
| Histórico dos cliques | ON | Comandos enviados por mensagens clicáveis entram no histórico nativo, recuperável pela seta para cima com o chat aberto. Histórico permanece na sessão. |
| Prévia de imagens | ON | Após 350 ms sobre um link direto PNG, JPG/JPEG ou GIF, tenta mostrar uma miniatura sem abrir o navegador. GIF usa o primeiro quadro. |
| Ping numérico no TAB | ON | Substitui as barras pelo número em milissegundos fornecido pelo servidor, com cores por faixa. Desligar restaura as barras. Valor desconhecido aparece como `?`. |

## Fila

A primeira mensagem pode sair imediatamente; as seguintes respeitam o intervalo. O chat mostra quantas estão pendentes e **Ctrl+Backspace** cancela a fila. Cancelar preserva o intervalo do último envio. Há limite de 100 mensagens pendentes; quando cheio, o chat informa que o novo envio foi recusado. A mensagem continua recuperável no histórico nativo de envios.

Desconectar, descarregar o mundo ou desativar a opção cancela os envios pendentes. Trocas de mundo que preservam a conexão sem descarregar o mundo não reiniciam a fila. Apenas mensagens e comandos do jogador passam pela fila; pacotes de movimento, combate e conexão continuam nativos. Uma pausa/travada não produz uma rajada de mensagens ao voltar: cada atualização envia no máximo uma. O intervalo não garante aceitação por servidores que tenham regras adicionais de chat.

## Agrupamento e ações

O agrupamento segue a ideia do Compact Chat do Patcher, integrado ao histórico existente do Dolly. Busca até as últimas 100 mensagens, respeita o período escolhido e não cruza os separadores de servidores. Linhas vazias e divisórias compostas de cinco ou mais `-`, `=` ou `▬` não são compactadas. Componentes completos são comparados: textos iguais com comandos, links ou estilos diferentes ficam separados. O contador é acrescentado a uma cópia visual, preservando os componentes originais e seus cliques.

A inspeção mostra a ação ao passar o mouse; não acrescenta uma janela de confirmação a cada clique. A busca Ctrl+F, cópia Ctrl+clique, menções, filtros e histórico entre servidores continuam disponíveis.

## Imagens e limites

Apenas links diretos com extensão compatível são usados; páginas HTML, vídeos e páginas de álbuns não são convertidos em imagens. Download e decodificação rodam fora da thread do jogo, com uma tarefa ativa e uma pendente. Há limites de 4 MiB por arquivo, 16 milhões de pixels, três redirecionamentos e timeouts de conexão/leitura. Endereços locais são recusados. A miniatura tem no máximo 320×180 pixels e é ajustada ao espaço da tela.

O cache contém até oito prévias durante a abertura do chat. Fechar o chat libera as texturas e as prévias; uma requisição já em andamento pode concluir em segundo plano e é descartada. Se um link falhar, o chat informa **Prévia indisponível** e o clique continua disponível. A opção pode ser desligada nas configurações do chat.

## Integração e validação

`ChatHistory` controla a janela e o agrupamento; `MessageQueue` trata ordem, limites e relógio monotônico. `DollyChatSending` conecta o envio real do jogador à fila e adiciona comandos clicados ao histórico nativo. `DollyChatActions` inspeciona o componente sob o mouse. `LinkImage` valida/decodifica as imagens; `DollyImagePreview` gerencia tarefas e texturas. `DollyTabPing` desenha o valor sem alterar nomes, pontuações ou a conexão de rede. Os hooks ficam em `ChatPatches`.

`DollyChatFeaturesTest` cobre espaçamento, ausência de rajadas, cancelamento, limite da fila, agrupamento temporal e entre mensagens, limites/decodificação de imagens e recusa de endereços locais. `DollyGroupSixTest` executa o envio do jogador realmente modificado com uma conexão de teste, cancela na desconexão, verifica ações distintas, preferências/reset e os hooks do JAR final. Os testes de interface existentes percorrem as novas páginas em tamanhos diferentes.

Falta validar em uma partida real a apresentação dos tooltips/prévias, sua criação/descarte de texturas e a legibilidade do TAB. Downloads de sites externos não foram usados como prova nos testes automatizados.

Referência: [Compact Chat do Patcher](https://github.com/Sk1erLLC/Patcher/blob/master/src/main/java/club/sk1er/patcher/util/chat/ChatHandler.java).
