# Primeiro teste do Dolly

## Estado desta build

Versão 0.1.6 gerada a partir do vanilla 1.8.9, sem Forge, com OptiFine HD U M5 integrado ao JAR. Compila para Java 8. Não é uma confirmação de que todas as variantes do CMClient já foram reproduzidas.

| Área | Estado |
| --- | --- |
| Configuração própria e reset | Implementados e testados sem janela |
| Otimizações de modelos, rede, culling e terreno | Adaptações descritas em [integração](mods/integracao.md); jogadores excluídos do culling; validação gráfica e ganhos ainda pendentes |
| VBO, VSync, bobbing, brilho e música iniciais | Conferidos nos campos reais da 1.8.9 |
| Intensidade de iluminação suave | AO nativo do OptiFine, padrão 25%; controle próprio Dolly removido |
| Click delay fix | Patch remove a penalidade específica de 10 ticks; não zera o contador de transições de tela |
| Mouse delay fix | Opção funcional como no CMPack: ON usa rotação do jogador, OFF usa rotação da cabeça, com interpolação; override do OptiFine encaminhado apenas para o jogador local |
| Teclas ao voltar do inventário | Recuperação de movimento por estado físico; ataques e cliques de menu não são reproduzidos |
| Shift no Linux | Normalização de códigos de Shift+2/Shift+6 no processamento de controles; não altera texto das GUIs |
| Toggle Sprint | Alternância pela tecla de sprint, respeitando as restrições normais do jogador |
| Zoom e Perspective | Implementados, com teclas remapeáveis e FOV suave no zoom |
| Animação de agachar | Interpolação da altura usada pela câmera |
| Full Bright e Time Changer | Disponíveis; começam desligados |
| Ping Display e TNT Timer | Disponíveis; começam desligados; TNT aparece no HUD para até oito entidades próximas em até 64 blocos |
| Partículas e limites de FPS | Controles implementados; ambientação começa desligada |
| Fast Render | Implementação OptiFine; padrão ON inclusive após reset; incompatível com shaders ativos |
| Render Regions | Backend nativo OptiFine; implementação regional Dolly removida; falta teste gráfico |
| Old Animations | Transformações de bloqueio/espada, swing ao comer, tint de dano de armadura, corações e guards de uso/ataque implementados; validação visual e paridade completa com a referência ainda pendentes |

As variantes de Old Animations precisam de conferência visual, especialmente a apresentação de itens em terceira pessoa e o swing durante uso de item. A implementação cobre os caminhos citados, mas não é uma comprovação de equivalência visual integral com o CMClient. Os cliques continuam vindo da entrada normal; não há geração automática de ataques.

Os mecanismos técnicos adicionais de entrada, cache de texto/HUD, jogadores, screenshot, idioma, carregamento, texturas, itens e DNS estão descritos na [integração técnica](cmpack/integracao-tecnica.md), incluindo os testes no jogo ainda necessários.

## Compilar

É necessário o OptiFine original em `references/OptiFine_1.8.9_HD_U_M5.jar`, Python 3, um JDK com compilador disponível (saída Java 8), o Minecraft 1.8.9 original e as bibliotecas locais da versão em `tools/version-template.json` e as ferramentas em `tools/libraries.json`. ASM e ASM Tree são usados somente pelo build.

```sh
python3 tools/build.py
```

O script procura a base e os caches usuais de PrismLauncher/Minecraft. Para outros locais:

```sh
python3 tools/build.py --minecraft-jar /caminho/1.8.9.jar --libraries /caminho/libraries
```

Não baixa arquivos automaticamente nem altera a instalação existente. Com um JDK normal, usa `--release 8`; neste ambiente usa também o `rt.jar` local do Java 8 quando o JDK está sem o catálogo de releases antigos.

Resultados:

- `dist/dolly-1.8.9.zip`: pasta de versão pronta para instalação local.
- `dist/Dolly-Client-0.1.6-Prism.zip`: instância pronta para importar no PrismLauncher.
- `dist/dolly-1.8.9/dolly-1.8.9.jar`.
- `dist/dolly-1.8.9/dolly-1.8.9.json`.
- `build/patches.txt`: classes vanilla alteradas.

## Instalar e abrir

### PrismLauncher

1. Clique em **Adicionar instância → Importar** e escolha `dist/Dolly-Client-0.1.6-Prism.zip`.
2. Confirme a importação e abra **Dolly Client 0.1.6** usando sua conta normal do Prism.

O pacote inclui o JAR do Dolly pelo mecanismo de JAR personalizado do Prism, sem Forge e com OptiFine encapsulado. Bibliotecas e assets são resolvidos pelo Prism, aproveitando seu cache existente. As configurações do jogo são criadas com os padrões do Dolly na primeira execução. A instância tem sua própria pasta de jogo.

Quando disponível durante o build, o perfil aponta para `/usr/lib/jvm/java-8-openjdk-amd64/bin/java`. Em outro computador, confira **Editar instância → Configurações → Java** e selecione Java 8.

O perfil usa heap inicial de 256 MiB e máximo de 1024 MiB, em vez de herdar a alocação global do Prism. O processo também usa memória fora do heap. Inclui `-Djavax.accessibility.assistive_technologies=` para evitar a falha do Java 8 do Linux quando sua configuração referencia um AtkWrapper ausente. Esse ajuste desativa a ponte Java de acessibilidade somente para esta instância, sem alterar o sistema.

Se já importou o pacote anterior, é possível corrigir a instância existente em **Editar instância → Configurações**: habilite a configuração própria de memória e defina mínimo **256 MiB**, máximo **1024 MiB**; em **Java**, habilite os argumentos próprios e acrescente `-Djavax.accessibility.assistive_technologies=` aos argumentos existentes. Alternativamente, importe novamente o ZIP atualizado como outra instância.

Formato conferido no [código do PrismLauncher](https://github.com/PrismLauncher/PrismLauncher/blob/develop/launcher/minecraft/PackProfile.cpp), em `installCustomJar_internal`. O empacotamento verifica a integridade do ZIP, a referência ao JAR local e se ele corresponde exatamente à build.

### Launchers com pasta versions

1. Tenha a versão vanilla 1.8.9 instalada pelo launcher.
2. Descompacte a pasta `dolly-1.8.9` do ZIP dentro da pasta `versions` da instalação Minecraft usada pelo launcher que suporta versões locais.
3. Selecione `dolly-1.8.9` e use Java 8 para o primeiro teste. A autenticação continua sendo a normal do launcher.
4. Abra o botão **Dolly Client** no menu principal/opções ou use **Right Shift** dentro do jogo.

Atalhos iniciais: **C** para zoom e **Alt esquerdo** para Perspective. São registrados nos controles vanilla e podem ser remapeados. A roda ajusta o zoom enquanto sua tecla estiver pressionada.

## Verificações já executadas

- Testes de persistência, reset, valores inválidos e preservação do arquivo inválido.
- Reconstrução de 218 classes e 832 recursos do OptiFine, com verificação de todos os MD5 declarados pelo pacote.
- Testes do decoder GDIFF e rejeição de entradas incorretas sem substituir a saída anterior.
- Verificação e ligação das classes alteradas/adaptadores pela JVM, sem iniciar o Minecraft.
- Teste de integração usando GameSettings real: VBO, VSync, brilho, bobbing, música, intensidade de AO e política do clique.
- Verificação adicional de compatibilidade com Java 8.
- Regressões com dispositivos simulados: falha de gravação/reset, zoom fracionário, atalhos remapeados, eventos rápidos/repetidos, perda de foco, Perspective por quadro e HUD por tick.
- Mouse fix ligado/desligado com rotações de jogador/cabeça diferentes e interpolação em 0, 0,5 e 1, passando pelo jogador local real.
- Equivalência temporal de Zoom/Smooth Sneak entre 20, 60, 144, 240 e 1.000 FPS; consultas repetidas de FOV no mesmo quadro.
- Ciclo de framebuffers com recursos simulados, proteção contra shaders e verificação do bytecode dos caminhos nativos de Fast Render/reset e resize.
- Limites de FPS ilimitado, explícito, segundo plano e menu, incluindo ausência de frequência detectada.

O ambiente não conseguiu abrir o display `:0` (`glxinfo` retornou “unable to open display”). Não houve sessão gráfica de Minecraft, teste de servidor, validação visual, benchmark ou medição de latência. As verificações acima não substituem esses testes.

## Sequência recomendada dentro do jogo

1. Abrir os menus Dolly e vídeo/OptiFine, conferir os padrões, salvar e reabrir. VBOs e iluminação suave ficam somente nas opções nativas do OptiFine.
2. Alterar opções e restaurar: Fast Render deve voltar ligado, VSync desligado e iluminação suave a 25%.
3. Em mundo de teste, comparar VBO simples com Render Regions e conferir blocos transparentes, reconstrução ao quebrar/colocar, mudança de distância e troca de mundo.
4. Testar retorno do inventário segurando movimento, atalhos remapeados e perda de foco.
5. Conferir Shift+2/Shift+6 no layout Linux usado e texto no chat.
6. Testar zoom, Perspective, sprint e animação de agachar.
7. Ativar Full Bright, Time Changer, Ping Display e TNT Timer individualmente.

A especificação em `dolly-especificacao.md` continua sendo o escopo; a tabela desta página indica o que a primeira build já cobre e o que ainda falta.
