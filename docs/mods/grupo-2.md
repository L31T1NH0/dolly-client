# Grupo 2: jogadores e objetos do mundo

A otimização de visibilidade mantém jogadores locais, remotos e NPCs derivados da classe de jogador fora dos testes de ocultação. Nomes e corpos seguem as regras originais de visibilidade do jogo. Isso não força jogadores a aparecerem através de paredes nem muda regras de equipes ou invisibilidade.

Mobs nomeados, itens nomeados, TNT nomeada e suportes de armadura usados para hologramas podem agora ser candidatos quando uma barreira opaca cobre sua caixa ampliada. Mantém-se o orçamento e o algoritmo conservador existente, sem resultados reaproveitados de outro quadro. Montarias, passageiros, mobs com laço, câmeras deslocadas e renderizadores especiais não suportados conservam as exceções de segurança. Nomes de entidades que não são jogadores podem desaparecer junto delas quando forem ocultadas.

## Controles

| Aba | Controle | Padrão | Efeito |
| --- | --- | --- | --- |
| Desempenho | Ocultar objetos cobertos | ON | Otimiza os candidatos escondidos por obstáculos; jogadores sempre excluídos. Usa a configuração existente `entityCulling`. |
| Visual | Ocultar livros de encanto | ON | Esconde somente o livro da mesa de encantamento, preservando o bloco e a interação. |
| Visual | Item único por pilha | ON | Desenha uma única cópia por pilha de itens no chão. Não muda a quantidade real nem a coleta. |
| Visual | Itens sem balanço | ON | Remove o movimento visual de subir/descer. A rotação, a física de queda e o movimento real dos itens continuam nativos. |

As três opções novas são `hideEnchantmentBooks`, `singleGroundItem` e `staticGroundItems`. Podem ser alteradas durante o jogo sem reiniciar. Ficam salvas em `config/dolly.properties`; arquivos antigos recebem os padrões das chaves ausentes e restaurar padrões volta a ligá-las.

## Integração e testes

`DollyCulling` mantém a exclusão antecipada da classe-base de jogador e amplia a seleção de candidatos. `DollyWorldObjects` controla exclusivamente a apresentação de itens. `WorldObjectPatches` liga os controles ao contador de cópias e ao cálculo de balanço no renderizador do OptiFine M5 e adiciona o retorno antecipado no renderizador do livro. O build confere as assinaturas e a expressão de balanço esperadas.

`DollyGroupTwoTest` verifica jogadores locais/remotos sem consultar seus nomes/caixas, suportes e objetos nomeados, exceção de montaria, culling desligado, os limites reais do contador nativo de cópias, preservação da quantidade da pilha, balanço ligado/desligado e o retorno do livro antes de acessar dados ou OpenGL. Também verifica os hooks no JAR final. Persistência e reset são exercitados por `DollyCoreTest`.

A validação gráfica permanece pendente: conferir no jogo jogadores saindo de trás de obstáculos, hologramas, livros, itens 2D/3D e configurações ligadas/desligadas. Não há medição de ganho de FPS.
