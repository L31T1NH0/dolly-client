# Integração dos mecanismos técnicos no Dolly

As opções estão nas abas Controles, Visual e Desempenho do menu Dolly. As opções já existentes de recuperação de teclas e culling continuam sendo os pontos de controle desses mecanismos. O [levantamento do CMPack](mecanismos-tecnicos.md) descreve o binário de referência; a implementação abaixo é adaptada à base 1.8.9 + OptiFine M5.

| Mecanismo | Padrão | Implementação Dolly |
| --- | --- | --- |
| Mouse no inventário | ON | Botões extras associados aos atalhos 1–9 fazem swap com o item sob o cursor; consome a ação para evitar também executar pick-block |
| Corrigir texto | ON | Aceita caracteres definidos com keycode zero, além dos eventos normais de keydown |
| Raw Mouse Input | OFF | JInput da plataforma, deltas transferidos atomicamente, worker único e fallback para LWJGL se indisponível |
| Recuperar teclas | ON, existente | Estados contínuos de movimento/sprint recuperados sem gerar novos cliques |
| Cache de texto | ON | Cache por fonte, invalidado em recarga e alterações de Unicode/bidi; limite de 5.000 medidas por fonte |
| Otimizar jogadores | ON | Reaproveita sqrt/atan para velocidades repetidas, preservando interpolação, swing e os campos usados por bobbing/capas |
| Screenshot assíncrona | OFF | Snapshot exclusivo por captura, um gravador e duas capturas pendentes; gravação PNG por arquivo temporário e feedback de sucesso/erro na thread principal |
| Idioma rápido | OFF | Recarga de traduções e flags nativas de fonte; erro ou ausência de recursos usa recarga completa |
| Carregar mundo rápido | ON | Evita coleta explícita e telas intermediárias na troca de mundo |
| Atraso de chunks | 15 ms | Modos OFF/15/50/110/150/200 ms; a espera interruptível atua apenas na fila de tarefas de construção |
| Cache do HUD | ON quando disponível, 24 Hz | Framebuffer transparente com composição adequada; camadas sensíveis ao fundo ficam por quadro, sem worker OpenGL |
| Limpar ao desconectar | OFF | Depois de 1 s sem mundo, libera texturas estáticas recarregáveis sem uso há 30 min; reconexão cancela a limpeza |
| Frestas dos itens | Unlerp permanente | Compensa o encolhimento UV específico do M5, sem seletor de modo |
| Ocultar objetos cobertos | ON, existente | Culling conservador próprio, com orçamento por quadro; jogadores continuam excluídos |
| DNS alternativo | ON | Consulta SRV via 1.1.1.1, timeout e cache limitado de um minuto |
| DNS de fallback | OFF | Se o alternativo falhar, tenta o resolvedor do sistema; não usa um serviço proprietário do CMPack |

## Diferenças intencionais em relação à cópia literal

- O raw mouse usa o backend disponível de JInput na plataforma em vez de forçar o plugin de Windows. Perda de foco, tela aberta, troca de mundo, captura/soltura do cursor e reset descartam deltas pendentes. Enquanto o backend não estiver pronto, ou após falha, permanece a captura LWJGL. Isso não altera a sensibilidade configurada.
- O cache de texto não sobrevive às mudanças das métricas da fonte. Fontes diferentes não compartilham medidas.
- A otimização de jogadores não retorna antecipadamente pulando campos de animação. Reutiliza o resultado das funções caras quando os argumentos se repetem, com resultado idêntico ao caminho normal.
- Screenshots não compartilham o array de pixels. Fila cheia informa que é preciso aguardar; não bloqueia a thread gráfica para gravar a imagem. O encerramento tenta concluir as capturas aceitas antes de sair, com prazo limitado e log de pendências.
- A limpeza considera o último bind, não a criação do identificador. Só aceita a classe exata de textura simples recarregável, sem emissive e sem aliases no mapa do gerenciador. Skins baixadas, texturas dinâmicas, atlas e subclasses desconhecidas são preservados. Texturas removidas voltam a ser carregadas quando usadas. Se alguma for liberada, a limpeza também recarrega os recursos na tela de desconexão, renovando IDs retidos pelos listeners do OptiFine (por exemplo, céu personalizado). Essa operação pode causar uma pausa no menu e só ocorre com a opção ligada e candidatos efetivamente removidos. Limpeza e exclusão GL ficam na thread principal; não se força GC por essa opção.
- O cache do HUD é invalidado em transições de mundo, alterações de opções/idioma e recarga de recursos; resize/escala também invalidam seu conteúdo. Quando não há framebuffer ou a composição é incompatível, usa o desenho normal. Fast Render pode desabilitar o suporte a framebuffer; ligar a opção não força um backend gráfico incompatível.
- O Unlerp desfaz o encolhimento UV do M5, em vez de reutilizar a constante da versão 1.8.8. A otimização de modelos considera contornos de cada frame de uma textura animada e mantém a correção UV aplicada uma única vez. Desde 0.1.15 não há seletor: valores antigos de `itemStitching` são ignorados.
- DNS só afeta a descoberta do endereço/porta do servidor. Não há promessa de reduzir ping nem mudança nos pacotes da partida.

## Verificação e teste no jogo

Os testes automatizados exercitam configurações/persistência/reset, eventos de texto e swaps reais de inventário com dispositivos simulados, handoff/falha/concorrência do raw mouse, cache de fonte e invalidação, geometria de itens, escrita de PNGs isolados, DNS simulado, políticas de limpeza e os hooks do JAR final. O build também executa a verificação da JVM Java 8 e as regressões anteriores.

Ainda é necessário testar com driver OpenGL e hardware reais:

1. Em Controles, remapear um slot para botão lateral e testar o swap no inventário; confirmar que não dispara pick-block simultaneamente. Testar texto com os layouts utilizados.
2. Ligar Raw Mouse Input, girar a câmera e alternar foco/inventário/mundo. Confirmar retorno normal se o dispositivo/backend não estiver disponível. Não há validação de latência física neste ambiente.
3. Trocar resource pack/idioma e conferir fontes e alinhamento. Testar os dois modos de frestas em itens sólidos, com buracos e animados.
4. Ligar screenshot assíncrona e pressionar F2 em sequência; conferir arquivos distintos, resultado visual e feedback. Supersampling mantém o caminho nativo.
5. Conferir HUD em frequências diferentes, escala/resize, chat, overlays e alternância de Fast Render/shaders. O caminho sem cache precisa continuar disponível.
6. Comparar reconstrução de chunks com e sem atraso e o comportamento visual de jogadores. Não foi medido ganho de FPS ou frametime.
