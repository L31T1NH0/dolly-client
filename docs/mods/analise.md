# Novos mods de referência: o que aproveitar no Dolly

Após esta investigação, as adaptações selecionadas foram implementadas. Consulte [escopo e validação da integração](integracao.md); os achados abaixo registram o estado da análise anterior.

## Resultado e prioridade

As referências novas contêm três mods: **EntityCulling 1.5.0**, **FoamFix 0.6.3a** e **Krypton 0.3.2**. Há boas ideias de renderização, memória e rede, mas nenhum dos três JARs entra diretamente no Dolly atual, que usa Minecraft 1.8.9, Java 8, OptiFine M5 e patches durante o build.

| Prioridade | Aproveitamento | Benefício esperado, ainda sem medição | Esforço e condição |
| --- | --- | --- | --- |
| 1 | EntityCulling: evitar desenho de entidades/tile entities escondidas por blocos | Menos trabalho de render em lobbies, bases e áreas com muitos objetos ocultos | Médio/alto: thread, estado de visibilidade, nomes e dois pontos de render; validar OptiFine e câmera |
| 2 | FoamFix: compartilhar dados imutáveis repetidos dos modelos | Menor RAM ocupada por modelos; possível redução de pressão no GC | Médio: medir duplicação restante após OptiFine; evitar reflexão ampla/estruturas Forge |
| 3 | Krypton: decoder de frames com menos alocações e cópias | Menor CPU/alocação no processamento local da rede | Médio: adaptar ao Netty 4.0 e preservar protocolo, fragmentação e referências de buffers |
| 4 | Krypton: caminhos curtos e cálculo de tamanho de VarInt | Economia pequena por operação de serialização | Baixo/médio: candidato isolado; só promover com ganho mensurável |
| Depois | FoamFix: modelos 3D de itens sob demanda e estados compactos | RAM e custo inicial de carregamento | Alto: alterações estruturais e interação com resource packs/OptiFine |

Essa ordem é uma avaliação técnica da adequação ao projeto, não um ranking de FPS medido. Para uma primeira melhoria visível, eu escolheria **EntityCulling**, com comportamento conservador e alternância para comparar. Para máquinas com pouca memória, investigaria **deduplicação de modelos** em seguida.

## Diferenças que mudam a decisão

**EntityCulling** é o mais próximo da base: as classes examinadas usam Java 8 e os mapeamentos incluem os alvos ofuscados da 1.8.9. O algoritmo usa raios e cache de opacidade na CPU; não depende de consultas de oclusão à GPU. A infraestrutura Forge/Mixin precisaria ser substituída pelos hooks do Dolly. O cache padrão ocupa cerca de 4 MiB e há trabalho adicional numa thread; ganhar FPS depende de economizar mais render do que custa decidir a visibilidade. Nomes elegíveis, beacon e transições de visibilidade precisam ser preservados. A referência libera culling em terceira pessoa, o que importa para Perspective.

**FoamFix** tem mecanismos reais de deduplicação e modelos sob demanda, mas muitos atendem ao Forge. O transformer também contém nomes-alvo inconsistentes com suas próprias classes 1.8; não é possível assumir que todos os patches anunciados funcionam nessa versão. O texto de economia de 48 MB refere-se a registros FML e não estima ganho para o Dolly.

**Krypton** é de outra geração: metadados declaram Minecraft >=26.2 e classes Java 25. Técnicas de buffers/VarInt podem ser reimplementadas na 1.8.9, mas não se deve incorporar o JAR ou trocar Netty indiscriminadamente. Também há uma diferença concreta entre recursos declarados e código: o JSON inclui consolidação de flushes, mas não foi encontrado consumidor dessas opções nas 25 classes próprias examinadas. Não há evidência para prometer batching funcional desse arquivo, menor ping ou melhor hit registration.

## Detalhes e evidências

- [EntityCulling: algoritmo, defaults, hooks e cuidados](entityculling.md).
- [FoamFix: RAM, modelos, estados e patches inaplicáveis](foamfix.md).
- [Krypton: pipeline, VarInts e diferenças da base](krypton.md).
- [Manifesto de arquivos, hashes e método](evidence/manifest.json).

O REA identificou `references/modrinth-mods.zip` com SHA-256 `05fdd2d880f937159920bec91a436c52567725fabfe3f129cca711b78c49a031`, contendo os JARs FoamFix e EntityCulling com hashes verificados. Inspeção: `ev_b216c82c621d49f64b5d3861ebd800dbf15319d2fb43b2483b3bdac3cefcf0b8`; inventário: `ev_0c8c53094cfb5cd7730297b3031192a27a34938d78ef2774c021a1c0900ca32c`; manifesto do grafo: `agm_b3137481654af9f29bec215bb270c8e81d2ea956f53462b596e30b9f9a18487f`. Os JARs foram materializados em `build/reference-mods/` para leitura estática. O Krypton tem registro REA preservado em arquivo próprio.

## Cobertura e próximos critérios

| Pergunta da investigação | Situação |
| --- | --- |
| Quais arquivos novos foram fornecidos? | Respondida: os dois arquivos novos contêm os três mods descritos |
| Há lógica interessante além do nome/descrição dos mods? | Respondida para os mecanismos selecionados, com classes, métodos e offsets de bytecode |
| O que serve à arquitetura atual? | Respondida como proposta de adaptação; dependências Forge/Fabric e limites descritos |
| O que já existe no Dolly? | Comparados os adaptadores e patches próprios; sem implementação própria equivalente encontrada nos caminhos examinados |
| Há sobreposição com todos os mecanismos internos do OptiFine? | Parcial: responsabilidades gerais distinguidas; deduplicação interna e todos os caminhos de render ainda precisam de comparação específica |
| Quanto melhora FPS/RAM/rede? | Não medido; exige protótipo e benchmark representativo |

A análise foi estática, sem iniciar mods ou jogo. Não foram alteradas funcionalidades nem padrões do Dolly. As melhorias selecionadas aqui são candidatas, não novas decisões de escopo da especificação.

Antes de integrar, medir tempo de frame/CPU e contadores para oclusão, heap e recarga de resource packs para modelos, e alocações/CPU com equivalência de bytes para rede. Culling deve falhar favorecendo visibilidade e não pode interferir em simulação, ataques ou processamento de pacotes. Cada proposta tem critérios específicos nos relatórios individuais.
