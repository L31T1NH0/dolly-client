# Otimizações de renderização — 0.1.12

Mudanças destinadas a reduzir trabalho repetido mantendo os recursos de apresentação e as atualizações do jogo.

- **Textos:** leitura do estado da fonte em um array reutilizado, chave de consulta reutilizada e restauração sem clonar arrays nos acertos do cache. A detecção de texto ofuscado não converte a string inteira. O playback deixa de desvincular a textura antes de restaurar a textura final. A consulta de lista gráfica e a restauração de cor permanecem para garantir compatibilidade com o caminho nativo.
- **Scoreboard:** nomes formatados, pontuações e medidas são preparados quando mudam. Os hooks do scoreboard nativo invalidam o resultado para mudanças de pontuação, título, equipes, membros, criação e remoção. Trocas de objetivo, fonte, configuração dos números, recursos e mundo também atualizam o cache. A posição e a escala continuam calculadas para a resolução atual.
- **Chat:** mensagens inalteradas reaproveitam suas linhas, texto de desenho e identificação de menções. Agrupamento de repetições, configuração, nickname e recarga da fonte invalidam os resultados necessários. O relógio e o número de linhas visíveis são consultados uma vez por desenho; o desaparecimento continua dinâmico. O histórico completo continua disponível para busca, rolagem e links.
- **Ocultação:** consultas ao mesmo bloco reaproveitam o resultado somente dentro do frame. O armazenamento tem tamanho fixo e compara todas as coordenadas, inclusive em colisões de hash. Os limites de tempo/consultas e a exclusão dos jogadores permanecem.
- **Terreno:** intervalos que continuam na mesma posição não são copiados sobre si mesmos. A mesclagem de intervalos contíguos e a ordem dos vértices permanecem.
- **HUD:** quando a taxa configurada é igual ou superior ao limite de FPS, o desenho direto evita a captura e composição extras. O menu informa indisponibilidade com Fast Render, shaders ou framebuffer sem suporte. As otimizações de preparação de chat e scoreboard funcionam também no desenho direto.

O profiler e o intervalo dos workers de chunks mantêm suas implementações. Os números anteriores de `chunks.rebuild` contam chamadas, incluindo seções vazias ou saídas antecipadas; não comprovam reconstruções completas redundantes.

## Validação

`python3 tools/build.py` executa as regressões existentes e verificações adicionais de reutilização/invalidação de scoreboard e chat, leitura reutilizável da fonte, colisões e renovação do cache de blocos, política do cache do HUD e equivalência da geometria do terreno. O build verifica o bytecode final em Java 8 e gera o pacote Prism.

Os testes sem display não comprovam redução de temperatura ou consumo. A validação no notebook deve conferir nomes, cores/sombras, chat e links, scoreboard durante pontuações/trocas de equipes, transparências e visibilidade ao colocar/quebrar blocos. Compare capturas no mesmo cenário e limite de FPS; observe o aquecimento após várias partidas.

## Alterações para a próxima compilação

Estas mudanças foram preparadas depois do pacote 0.1.12. A compilação e a execução das regressões estão pendentes a pedido do usuário.

- **HUD:** considera também o intervalo real entre chamadas de desenho. Usa uma média suavizada e uma margem para retomar o cache, evitando alternância constante perto da frequência configurada. Sem economia, desenha diretamente e invalida a imagem antiga; um framebuffer existente permanece alocado para a recuperação, com liberação normal nas transições de mundo, tamanho e configuração. Pausas longas reiniciam a estimativa.
- **Culling:** encerra antes de consultar caixas ou posições de objetos quando o prazo ou as 512 consultas terminam. Os objetos restantes seguem pelo desenho nativo.
- **Câmera do culling:** prepara a posição interpolada dos olhos uma vez por frame, no primeiro uso após a atualização da câmera. Trocas de mundo/jogador invalidam a posição e os resultados de blocos associados.
- **Raw input:** mantém as referências dos componentes X/Y de cada mouse desde a descoberta. O polling usa essas referências; preserva seleção do dispositivo, falhas, deltas fracionários e espera de 1 ms.
- **Textos:** a primeira ocorrência de uma combinação de texto, sombra, cor e estilo usa desenho nativo. Uma segunda ocorrência admite a combinação no cache gráfico. O histórico de candidatos tem limite de 512 entradas por fonte, separado das listas já compiladas; strings únicas não expulsam diretamente listas úteis. A invalidação da fonte também descarta seus candidatos.

Foram adicionadas regressões para a adaptação do HUD a cenários lentos e recuperação, saída antecipada do culling, reutilização/invalidação da posição dos olhos, referências dos eixos sem novas buscas durante polling e admissão limitada de textos. Elas aguardam execução na próxima compilação.
