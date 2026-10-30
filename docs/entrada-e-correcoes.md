# Entrada e correções 0.1.15

- **Ctrl+Shift+T**: envia `/stats <nome>` uma vez por pressionamento. Só funciona dentro do jogo, com foco, se o TAB tiver exatamente dois perfis, incluindo o jogador local, e nenhum espectador. Aceita Ctrl e Shift dos dois lados. Não abre o chat pelo T. Nomes são validados antes de compor o comando. O servidor deve implementar `/stats`; a detecção usa o TAB, sem integração específica para o modo de partida do servidor.
- **Projéteis**: mantém a política de dois ticks, com limite monotônico de 100 ms a partir da primeira consulta ao renderizador. Uma idade que permaneça em zero não sustenta invisibilidade indefinidamente. O mapa usa referências fracas e não modifica idade, trajetória ou pacotes. A causa da idade parada em um servidor real ainda precisa ser reproduzida no jogo.
- **Frestas dos itens**: Unlerp é permanente e aplicado uma vez, tanto nos modelos nativos quanto nos otimizados. O seletor e a chave `itemStitching` foram removidos; arquivos antigos continuam carregando, ignorando essa chave. A otimização de geometria continua independente.
- **Parallax**: ligado nas configurações novas e no reset. O valor salvo anteriormente é preservado.
- **Inventory Position**: ao atualizar efeitos de poção, o inventário retorna ao centro horizontal, mantendo a lista e o desenho dos efeitos.
- **Input Optimizer**: adaptação da antecipação da leitura ao LWJGL 2. O loop chama `Display.processMessages()` antes do timer e dos ticks. Filas de teclado/mouse, suavização, sensibilidade e processamento vanilla são mantidos. Não inclui os backends nativos específicos de Windows/Linux/macOS do projeto moderno e não equivale a uma instalação integral do mod Fabric.

## Verificação

O build executa testes de configuração/reset, correção UV única, projétil com idade congelada, seleção do adversário, repetição da tecla, espectadores, nomes inválidos, inventário com/sem poções em diferentes larguras e ordem do bombeamento dos eventos antes dos ticks no JAR final. Também verifica ligação e bytecode em Java 8 e executa a suíte de regressão existente.

Ainda é necessário validar graficamente e em servidor o comando `/stats`, a visibilidade dos projéteis e o inventário. Não há medição de latência nem garantia de compatibilidade de anticheat.

## Referências

- [Input Optimizer, mecanismo de alinhamento por quadro](https://github.com/Kemmlow/inputoptimizer/blob/b247cad0fc96eba2bb446f196d6bf9a4d7402f63/src/main/java/dev/kemmlow/inputoptimizer/mixin/MixinFrameAlignedInput.java).
- [Patcher, Inventory Position](https://github.com/Sk1erLLC/Patcher/blob/master/src/main/java/club/sk1er/patcher/asm/render/screen/InventoryEffectRendererTransformer.java).
- [TACTILE e compatibilidade com servidores](https://github.com/fallingsword/TACTILE#compatibility): a integração completa e os perfis por servidor foram adicionados no [pacote 0.1.16](mods/tactile.md).
