# Chat e scoreboard

Abra Right Shift → Visual → Configurar chat ou Configurar scoreboard. Os controles usam o menu Dolly existente, com páginas quando necessário. Shift+clique diminui valores; clique aumenta. Restaurar padrões na tela principal também redefine estes módulos.

## Chat

- Sem horário nas mensagens.
- Agrupar repetidas vem ligado: mensagens idênticas dentro de 120 segundos, incluindo seus componentes e ações, recebem `×N`. Uma repetição atualiza o tempo de exibição e aparece como a ocorrência mais recente. **Só consecutivas** restringe o agrupamento ao comportamento anterior. O período é configurável; veja o [grupo 6](mods/grupo-6.md) para fila de envios, links, imagens e ping no TAB.
- Histórico começa em 1.000 mensagens, ajustável de 100 a 5.000. Não é gravado em um arquivo de histórico pelo módulo. O log normal do Minecraft continua existindo.
- Manter entre servidores vem ligado. A próxima conexão acrescenta um separador com o endereço do servidor ou “Mundo local”. F3+D limpa explicitamente todo o histórico.
- Ctrl+F, com o chat aberto, ativa a busca. O texto digitado filtra as mensagens, ignorando maiúsculas e formatação. O contador indica mensagens, não linhas. Enter mostra uma página mais antiga; Shift+Enter mostra uma mais nova. Page Up/Down e a roda também navegam. Esc ou Ctrl+F encerra a busca; o rascunho normal do chat é preservado. Enter na busca nunca envia uma mensagem.
- Ctrl+clique esquerdo copia o texto completo da mensagem, mesmo quando ela ocupa várias linhas. O clique sem Ctrl preserva links e comandos do servidor.
- O destaque de menções vem ligado; o som começa desligado. O nick é reconhecido como nome completo, sem confundir `Dolly` com `Dolly123`. Palavras adicionais podem ser definidas.
- Filtro de palavras e ocultação de avisos começam desligados, com listas vazias. Edite **Palavras ocultadas**, **Frases de avisos** e **Palavras destacadas**, separando termos ou frases com `;`. As regras procuram trechos de texto, sem distinguir maiúsculas. A lista de avisos é configurada por você: não tenta adivinhar anúncios do servidor. Mensagens filtradas também não aparecem na busca; desativar o filtro volta a mostrá-las enquanto estiverem no histórico.
- Largura, altura, escala, espaçamento, opacidade do texto, fundo e sua opacidade são ajustáveis. A janela limita as dimensões efetivas para caber na tela.
- Exibição começa em 10 segundos, incluindo 1 segundo de fade no final. Fade zero remove a transição; valores maiores que a duração são limitados à duração efetiva. Com o chat aberto, as mensagens permanecem visíveis.
- Ao rolar para mensagens antigas, novas mensagens preservam a mensagem que você estava lendo. O aviso de novas mensagens permite voltar ao final com um clique. O limite do histórico continua valendo: mensagens removidas pelo limite deixam de estar disponíveis.

## Scoreboard

Exibição, números e fundo começam ligados. A escala começa em 1×, fundo em 35%, posição horizontal em 100% e vertical em 50%. As posições são percentuais do espaço disponível para o painel inteiro, mantendo-o dentro da tela. O painel acompanha os dados do servidor e mostra até 15 linhas.

O atalho começa sem atribuição. Em **Atalho de visibilidade**, pressione uma tecla; Delete/Backspace remove a atribuição, Esc cancela. Também é possível remapear **Dolly scoreboard** em Opções → Controles. O atalho só alterna o painel durante o jogo, sem afetar a digitação no chat ou outras telas. A preferência é salva.

## Validação

O build executa testes de histórico, agrupamento, limites, filtros, reconhecimento de nick, fade, persistência e reset, mais regressões dos adaptadores e dos hooks no JAR final. Os testes usam dispositivos e fonte determinísticos; não comprovam aparência ou comportamento do driver OpenGL.

Conferir no jogo:

1. Receber mensagens longas com cores, links e ações de clique/hover; testar clique normal e Ctrl+clique em cada linha, inclusive com escala e espaçamento diferentes.
2. Digitar um rascunho, abrir Ctrl+F, buscar mensagens, navegar e sair com Esc. Confirmar que o rascunho permanece e nenhuma consulta é enviada ao servidor.
3. Rolar o histórico enquanto chegam mensagens e repetições; conferir âncora, contador, aviso e retorno ao final.
4. Trocar de servidor, conferir separador, desligar retenção e confirmar a limpeza. F3+D deve limpar mesmo com retenção ligada.
5. Configurar filtros e destaque, testar diferenças de maiúsculas, nomes parecidos e o som opcional.
6. Alterar todos os controles de aparência em janelas pequenas e grandes; conferir fade com chat fechado.
7. Ajustar scoreboard nas extremidades, ocultar números, alternar pelo atalho e reabrir o jogo para conferir persistência. Conferir títulos e equipes coloridos e atualização das pontuações.
