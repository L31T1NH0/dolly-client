# Catálogo do CMClient para escolher o Dolly

Fonte: CMClient 2.8.11 beta para Minecraft 1.8.8 fornecido no projeto. São 54 módulos e 58 configurações gerais, mais opções internas já verificadas. Valores de construtor/inicialização limpa; perfis pessoais podem substituí-los. Os nomes indicam a finalidade dos módulos, mas nem todos tiveram seu funcionamento integral reconstruído.

Cada item tem um ID para facilitar escolhas por mensagem. A coluna Dolly registra a seleção atual. A [especificação do Dolly](../dolly-especificacao.md) prevalece sobre a referência CMClient. Inclusão de um módulo não implica ativação automática quando esse padrão não foi especificado.

## Decisões já confirmadas para o Dolly

| Recurso | Decisão |
| --- | --- |
| Base | Minecraft 1.8.9, versão própria, sem Forge |
| OptiFine | Fora do primeiro teste por padrão |
| VBOs | Opção separada; ligado por padrão |
| Render Regions | Opção separada; ligado por padrão; depende de VBO/suporte gráfico |
| Fix Linux de Shift+2/Shift+6 | Incluído e ligado por padrão no Linux |
| Movimento ao fechar o inventário | Incluído; retomar a tecla que continua pressionada |

Novas escolhas: Fast Render ligado também após reset; VSync desligado; balanço ao andar desligado; música 0%; iluminação suave 25%; brilho 100%. Os fixes de clique/mira/teclas, Shift no Linux, Toggle Sprint, animação de agachar, zoom e controles de FPS/partículas foram mantidos.

Essas decisões são requisitos registrados. A primeira build já foi gerada; veja a [cobertura atual e limitações](../primeiro-teste.md).

## Módulos

Ligado indica estado inicial do módulo. Alguns efeitos dependem de tecla/ambiente. Desligado não significa ausente: o módulo está disponível para ativação.

| ID | Módulo | Padrão CMClient | Dolly |
| --- | --- | --- | --- |
| M01 | Armor HUD | Desligado | Fora da seleção atual |
| M02 | Auto Friend | Desligado | Fora da seleção atual |
| M03 | AutoGG | Desligado | Fora da seleção atual |
| M04 | AutoText | Desligado | Fora da seleção atual |
| M05 | Block Overlay | Desligado | Fora da seleção atual |
| M06 | Chat | Ligado | Fora da seleção atual |
| M07 | Tab | Ligado | Fora da seleção atual |
| M08 | Coordinates | Desligado | Fora da seleção atual |
| M09 | CPS | Desligado | Fora da seleção atual |
| M10 | Bossbar | Ligado | Fora da seleção atual |
| M11 | Scoreboard | Ligado | Fora da seleção atual |
| M12 | Potion Counter | Desligado | Fora da seleção atual |
| M13 | Direction Hud | Ligado | Fora da seleção atual |
| M14 | Crosshair | Desligado | Fora da seleção atual |
| M15 | FPS | Desligado | Fora da seleção atual |
| M16 | Memory | Desligado | Fora da seleção atual |
| M17 | TNT Timer | Desligado | Incluído; estado inicial não especificado |
| M18 | Hit Color | Desligado | Fora da seleção atual |
| M19 | Old Animations | Desligado | Incluído; animações solicitadas ativas |
| M20 | Animations | Ligado | Mantida a animação de agachar, ligada |
| M21 | Cosmetics | Ligado | Fora da seleção atual |
| M22 | Pack Overlay | Desligado | Fora da seleção atual |
| M23 | Perspective | Ligado | Incluído; estado inicial não especificado |
| M24 | Emotes | Condicional | Fora da seleção atual |
| M25 | Waypoints | Desligado | Fora da seleção atual |
| M26 | Skins | Ligado | Fora da seleção atual |
| M27 | Zoom | Ligado | Mantido, ligado conforme referência |
| M28 | Ping Display | Desligado | Incluído; estado inicial não especificado |
| M29 | Combo Display | Desligado | Fora da seleção atual |
| M30 | Potion Effects | Ligado | Fora da seleção atual |
| M31 | Speedometer | Desligado | Fora da seleção atual |
| M32 | Server Address | Desligado | Fora da seleção atual |
| M33 | Reach Display | Desligado | Fora da seleção atual |
| M34 | Time Changer | Desligado | Incluído; estado inicial não especificado |
| M35 | Weather Changer | Desligado | Fora da seleção atual |
| M36 | Time Display | Desligado | Fora da seleção atual |
| M37 | Toggle Sprint | Ligado | Mantido, ligado conforme referência |
| M38 | Discord RP | Condicional | Fora da seleção atual |
| M39 | Motion Blur | Desligado | Fora da seleção atual |
| M40 | Item Physics | Desligado | Fora da seleção atual |
| M41 | Full Bright | Desligado | Incluído; estado inicial não especificado |
| M42 | Glint Colorizer | Desligado | Fora da seleção atual |
| M43 | Particles | Desligado | Controles gerais de partículas mantidos; extras forçados não selecionados |
| M44 | Keystrokes | Ligado | Fora da seleção atual |
| M45 | Play Time | Desligado | Fora da seleção atual |
| M46 | Hitbox | Desligado | Fora da seleção atual |
| M47 | 2D Items | Desligado | Fora da seleção atual |
| M48 | Wavey Capes | Ligado | Fora da seleção atual |
| M49 | Nametags | Ligado | Fora da seleção atual |
| M50 | 3D Skin Layers | Desligado | Fora da seleção atual |
| M51 | Debug Screen | Desligado | Fora da seleção atual |
| M52 | Damage Tint | Desligado | Fora da seleção atual |
| M53 | Shiny Pots | Desligado | Fora da seleção atual |
| M54 | InputFix | Ligado | Fora da seleção atual |

## Configurações gerais

A posição de cada grupo acompanha a ordem do construtor. Cores abaixo foram conferidas nos argumentos de java.awt.Color; não se trata de cores escolhidas para o Dolly.

| ID | Configuração | Padrão CMClient | Dolly |
| --- | --- | --- | --- |
| C01 | Marca no menu de pausa (Pause Menu Watermark) | Ligado | Sem decisão específica |
| C02 | Marca no inventário (Inventory Watermark) | Ligado | Sem decisão específica |
| C03 | Modo canhoto (Left hand mode) | Desligado | Sem decisão específica |
| C04 | Balanço mínimo da câmera (Minimal View Bobbing) | Desligado | Balanço ao andar desligado; modo mínimo não necessário |
| C05 | Correção de atalhos no mouse (Mouse bind fix) | Ligado | Sem decisão específica |
| C06 | Correção de direção da mira (Mouse delay fix) | Ligado | Incluído e ligado por padrão |
| C07 | Correção do bloqueio após errar ataque (Click delay fix) | Ligado | Incluído e ligado por padrão |
| C08 | Raw Mouse Input (Raw Mouse Input) | Desligado | Sem decisão específica |
| C09 | Contador de FPS suavizado (Show smooth FPS Counter) | Desligado | Sem decisão específica |
| C10 | Ocultar módulos ao abrir Tab (Hide modules on Tab) | Desligado | Sem decisão específica |
| C11 | Tela cheia sem bordas (Borderless Fullscreen) | Ligado somente no Windows com resolução fullscreen Default; desligado no Linux | Sem decisão específica |
| C12 | Recuperação do estado das teclas (Modern Keybind Handling) | Ligado | Incluído e ligado por padrão |
| C13 | Correção de frestas nos itens (Item Stitching Fix) | Desativado; alternativas Outline e Unlerp | Sem decisão específica |
| C14 | Posição do inventário (Inventory Mode) | Default; alternativas Center e Right | Sem decisão específica |
| C15 | Fundo transparente (Transparent Background) | Desligado | Sem decisão específica |
| C16 | Desfoque de fundo (Blur Background) | Desligado | Sem decisão específica |
| C17 | Raio do desfoque (Blur Radius) | 5.0 | Sem decisão específica |
| C18 | Velocidade do desfoque (Blur Animation Speed) | 15.0 | Sem decisão específica |
| C19 | Trocar hotbar pela roda (Hotbar Scrolling) | Ligado | Sem decisão específica |
| C20 | Carregamento gradual de chunks (Lazy Chunk Loading) | Off (Vanilla); alternativas Lowest, Low, Medium, High e Highest | Sem decisão específica |
| C21 | Limite de FPS com janela inativa (FPS limit on inactive) | 30 FPS | Mantido; 30 FPS como referência |
| C22 | Limite de FPS no menu (FPS limit in Main Menu) | Taxa do primeiro monitor; fallback 60 FPS | Mantido; taxa do monitor, fallback 60 FPS |
| C23 | Brilho de encantamento (Show enchant glint) | Ligado | Sem decisão específica |
| C24 | Partículas ao quebrar blocos (Digging particles) | Ligado | Mantido; ligado conforme referência |
| C25 | Mostrar conquistas (Show achievements) | Desligado | Sem decisão específica |
| C26 | Tremor da câmera ao tomar dano (Show hurt camera) | Ligado | Sem decisão específica |
| C27 | Fogo nos jogadores (Show fire on players) | Ligado | Sem decisão específica |
| C28 | Fogo na tela (Show fire on hud) | Ligado | Sem decisão específica |
| C29 | Flechas nos jogadores (Show arrows on players) | Ligado | Sem decisão específica |
| C30 | Flechas no chão (Show arrows on ground) | Ligado | Sem decisão específica |
| C31 | Renderizar portais do End (Show end portals) | Ligado | Sem decisão específica |
| C32 | Renderizar molduras (Show item frames) | Ligado | Sem decisão específica |
| C33 | Renderizar vegetação (Render foliage) | Ligado | Sem decisão específica |
| C34 | Screenshot assíncrona (Asynchronous screenshot) | Desligado | Sem decisão específica |
| C35 | Carregamento rápido de mundo (Fast world loading) | Desligado | Sem decisão específica |
| C36 | Troca rápida de idioma (Fast language switch) | Desligado | Sem decisão específica |
| C37 | Tela de carregamento de terreno (Show downloading terrain screen) | Ligado | Sem decisão específica |
| C38 | Atualização otimizada de jogadores (Optimized players updating) | Ligado | Sem decisão específica |
| C39 | Cache de largura de texto (String Width Caching) | Ligado | Sem decisão específica |
| C40 | Partículas de void fog/barrier (Render void fog and barrier particles) | Desligado | Mantido; render desligado conforme referência |
| C41 | Otimizar rotina de void fog/barrier (Optimized void fog and barrier particles) | Ligado | Mantido; otimização ligada conforme referência |
| C42 | Limite de partículas (Particle limit) | 1.000 por lista interna, não limite global | Mantido; 1.000 por lista como referência |
| C43 | Limpar memória ao desconectar (Memory cleanup on disconnect) | Desligado | Sem decisão específica |
| C44 | Entity Culling (Entity Culling) | Ligado | Sem decisão específica |
| C45 | Cache do HUD (HUD Caching) | Conforme suporte a framebuffer | Sem decisão específica |
| C46 | Limite de atualização do HUD em cache (Framelimit in HUD Caching) | 60 Hz | Sem decisão específica |
| C47 | DNS alternativo para SRV (Alternate DNS resolver) | Ligado; SRV via 1.1.1.1 | Sem decisão específica |
| C48 | DNS de fallback (Fallback DNS resolver) | Desligado | Sem decisão específica |
| C49 | Partículas no menu principal (Main menu particles) | Desligado | Sem decisão específica |
| C50 | Fundo moderno (Modern background) | Ligado | Sem decisão específica |
| C51 | Caixa de seleção moderna (Modern selection box) | Ligado | Sem decisão específica |
| C52 | Barra de rolagem moderna (Modern scroll bar) | Ligado | Sem decisão específica |
| C53 | Mostrar overlay da GUI (Show GUI overlay) | Desligado | Sem decisão específica |
| C54 | Cor da interface (UI Color) | RGB 217, 217, 217 — #D9D9D9 | Sem decisão específica |
| C55 | Estilo de botão (Button type) | Modern 1; alternativas Vanilla e Modern 2 | Sem decisão específica |
| C56 | Cor dos botões (Button Color) | RGB 255, 255, 255 — #FFFFFF | Sem decisão específica |
| C57 | Fonte moderna nos botões (Modern button font) | Desligado | Sem decisão específica |
| C58 | Anti Cheat (Anti Cheat) | Opção inicial ligada; backend condicionado a Windows, desligado no Linux | Sem decisão específica |

## Opções internas já verificadas

Essa seção não é um inventário completo de todas as propriedades dos 54 módulos. Registra as opções confirmadas na investigação, para não apresentar suposições como padrões.

| Módulo | Opção interna | Padrão armazenado |
| --- | --- | --- |
| Toggle Sprint | Alternar sprint | Ligado |
| Toggle Sprint | Alternar sneak | Desligado |
| Toggle Sprint | Mostrar texto de estado | Ligado |
| Animations | Animação de GUI | Ligada |
| Animations | Velocidade da GUI | 3 |
| Animations | Animação de agachar | Ligada |
| Animations | Velocidade de agachar | Aproximadamente 8,5 |
| Animations | Animação de chunks | Desligada |
| Zoom | Suavizar zoom | Ligado |
| Zoom | Suavizar câmera durante zoom | Ligado |
| Zoom | Ajustar zoom pela roda | Ligado |
| Zoom | Nível | 4, faixa de 2 a 10 |
| Old Animations | Bloqueio 1.7 | Ligado internamente |
| Old Animations | Espada ao bater 1.7 | Ligado internamente |
| Old Animations | Comer 1.7 | Ligado internamente |
| Old Animations | Atacar 1.7 | Ligado internamente; também altera lógica de uso/ataque |
| Old Animations | Cor de dano de armadura 1.7 | Ligado internamente |
| Old Animations | Barra de corações 1.7 | Ligado internamente |
| Old Animations | Posição dos itens segurados 1.7 | Desligado |
| Old Animations | Posição do arco 1.7 | Desligado |
| Old Animations | Posição da vara 1.7 | Desligado |
| Particles | Sempre mostrar critical | Desligado |
| Particles | Sempre mostrar sharpness | Desligado |
| Particles | Multiplicadores | 1, faixa de 1 a 20 |

**Old Animations começa desligado como módulo**, portanto seus valores internos ligados não produzem esses efeitos até ativá-lo. Particles também começa desligado. InputFix trata texto em telas; o fix Linux do Shift é separado.

## Gráficos já examinados no CMClient

Valores adicionais de GameSettings/OptiFine presentes no pacote CMClient; são independentes dos 58 ajustes gerais acima. Não representam automaticamente o preset do Dolly.

| Ajuste | Padrão encontrado no CMClient | Dolly |
| --- | --- | --- |
| Sensibilidade | 0,5 | Sem decisão específica |
| FOV | 70 | Sem decisão específica |
| Distância de renderização | 8 chunks | Sem decisão específica |
| Limite de FPS do jogo | 260 como sentinela de ilimitado; VSync ainda pode limitar | Sem decisão específica |
| VSync | Ligado inicialmente | Desligado por padrão e após reset |
| View bobbing | Ligado | Desligado por padrão e após reset |
| Dynamic FOV | Ligado | Sem decisão específica |
| Gamma | 0 | Brilho normal em 100% |
| Fancy graphics | Ligado | Sem decisão específica |
| Ambient occlusion | Índice 2 | Intensidade de iluminação suave em 25%; modo não especificado |
| Mipmaps | 4 | Sem decisão específica |
| Fast Render | Ligado inicialmente | Ligado por padrão e após reset |
| Smooth FPS | Desligado | Sem decisão específica |
| Fast Math | Desligado | Sem decisão específica |
| Antialiasing | 0 | Sem decisão específica |
| Filtragem anisotrópica | 1 | Sem decisão específica |
| Atualizações de chunks | 1 | Sem decisão específica |
| Atualizações dinâmicas de chunks | Desligadas | Sem decisão específica |
| Sombras de entidades | Ligadas | Sem decisão específica |
| Música do jogo | Não catalogada nesta referência | 0% por padrão e após reset |

No CMClient, o reset gráfico desliga VSync e Fast Render, apesar de começarem ligados. A opção VBO usa heurística de GPU nesse pacote; no Dolly, o padrão já escolhido é ligado. Fast Render e VBO são recursos distintos. O OptiFine M5 avulso analisado depois tem outros padrões.

## Referências e cobertura

- [Investigação dos padrões](padroes.md).
- [Módulos, ativação e persistência](modulos-padroes.md).
- [Catálogo técnico das configurações gerais](defaults-gerais.json).
- [VBO e Render Regions](../optifine/vbos.md).

Este catálogo serve para escolher recursos e padrões. Não confirma ganhos de desempenho, não é export de uma sessão pessoal e não substitui a tabela de cobertura da primeira build.
