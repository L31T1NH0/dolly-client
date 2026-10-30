# Grupo 7: screenshots locais

A seleção inclui apenas funções locais. Não há upload, envio de imagens a terceiros nem dependência de serviço externo.

Use **F2** para capturar e **Visual → Gerenciar capturas…** para abrir as imagens recentes da sessão. A mensagem de sucesso também oferece **Gerenciar** e **Abrir**, vinculados à captura correspondente. Tirar uma nova screenshot não faz os controles anteriores apontarem para outra imagem.

O gerenciador guarda até 32 capturas da sessão e permite navegar entre elas, ver uma prévia, copiar a imagem para a área de transferência, favoritar, abrir a pasta e excluir com confirmação. Favoritar cria uma cópia em `favorite_screenshots`, preservando o original; colisões de nome recebem um sufixo. Excluir remove somente o arquivo original escolhido após confirmação, mantendo suas cópias favoritas. Cancelar ou pressionar Esc na confirmação conserva o arquivo.

Após reiniciar o jogo, os arquivos continuam no disco e podem ser acessados por **Abrir pasta**, inclusive quando a lista da sessão estiver vazia. A lista não é um catálogo persistente de todas as screenshots antigas.

## Controles e padrões

| Opção | Padrão | Efeito |
| --- | --- | --- |
| Screenshot assíncrona, em Desempenho | ON em configurações novas | Lê os pixels na thread gráfica e grava o PNG em segundo plano. Configurações existentes preservam o valor anterior. |
| Prévia da captura, em Visual | ON | Mostra uma miniatura por cinco segundos durante o jogo. O gerenciador também oferece prévia nos menus. |
| Copiar captura ao salvar, em Visual | OFF | Copia automaticamente a imagem quando a captura termina. Sem isso, o botão do gerenciador copia apenas quando solicitado. |
| Aviso compacto de captura, em Visual | ON | Mostra uma mensagem curta; desligar inclui nome e tamanho em KiB. Os links de gerenciamento permanecem. |

As miniaturas da sessão têm no máximo 480×270 pixels. O clipboard recebe a imagem completa, não apenas seu caminho ou a miniatura. Falhas do sistema ao copiar ou abrir arquivos aparecem no status do gerenciador. As operações de arquivo e clipboard usam a fila de trabalho limitada das screenshots.

## Integração

A base segura do Dolly foi mantida: cada trabalho possui seus pixels, o PNG é gravado em arquivo temporário e finalizado por troca de arquivo, os nomes são exclusivos, há uma tarefa ativa e duas pendentes, e o encerramento aguarda os trabalhos aceitos por até cinco segundos. A leitura gráfica restaura os alinhamentos de pixels e a textura que estavam ativos antes da captura.

O caminho original do OptiFine continua sendo usado quando a opção assíncrona está desligada ou a resolução especial do OptiFine é maior que 1×. O retorno de sucesso desse caminho também registra os controles locais. Não se altera a captura de alta resolução do M5.

`DollyScreenshots` controla captura/gravação; `DollyScreenshotManager` mantém metadados e ações por imagem; `DollyScreenshotScreen` fornece os controles; `DollyPicture` cria e libera a textura na thread gráfica. `dolly.screenshots.ScreenshotFiles` copia favoritos sem sobrescrever nomes existentes. A ação local de gerenciamento é consumida antes da fila de mensagens e não entra no histórico de comandos nem é enviada ao servidor. Identificadores de capturas expiradas não recebem acesso arbitrário a caminhos do computador.

## Validação

Os testes anteriores continuam cobrindo orientação, cores, recorte do framebuffer, PNG, colisões de nomes e fila/encerramento. `DollyGroupSevenEightTest` cobre a identificação independente das capturas, mensagem com ação própria, prévia reduzida, limite de 32 registros, formatos de clipboard, cópias favoritas, falhas no destino, exclusão e cancelamento. O JAR final é verificado para o retorno de sucesso do screenshot nativo.

Falta validar com um contexto OpenGL real a leitura dos pixels e as prévias, a resolução especial do OptiFine e a integração com a área de transferência/abertura de pasta do sistema. Os testes de clipboard verificam o objeto transferível, sem acessar a área de transferência real.

Referência: [ações locais e screenshots assíncronas do Patcher](https://github.com/Sk1erLLC/Patcher/blob/master/src/main/java/club/sk1er/patcher/util/screenshot/AsyncScreenshots.java).
