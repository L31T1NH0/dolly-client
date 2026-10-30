# FoamFix 0.6.3a: oportunidades para o Dolly

## Escopo e resultado

Inspeção estática de `build/reference-mods/foamfix-0.6.3a-anarchy-1.8.x.jar`, extraído de `references/modrinth-mods.zip`. SHA-256: `03fa102a9ad8dc580f9de2ac1689b30a89ceab5a5ee614f96d27915362c45f5c`. O nome do arquivo não garante que cada transformação funcione na 1.8.9. O bytecode e os recursos foram lidos sem executar o mod. Não houve benchmark de FPS, heap ou carregamento.

A ideia mais aproveitável é reduzir objetos repetidos nos modelos carregados. Isso pode diminuir RAM e pressão no coletor, mas seu benefício no Dolly precisa ser medido: boa parte da implementação fornecida atende estruturas exclusivas do Forge. Os ganhos anunciados para grandes modpacks não podem ser transferidos para nosso client vanilla com OptiFine.

## Candidatos, em ordem prática

| Ideia | O que o binário faz | Aplicação no Dolly |
| --- | --- | --- |
| Deduplicação de dados de modelos | Percorre modelos depois do bake, compartilha arrays de floats, coleções imutáveis, identificadores e transformações iguais | Primeiro protótipo de RAM: restringir a dados comprovadamente imutáveis e medir com os resource packs usados no PvP. Conferir antes o que o OptiFine já compartilha. |
| Evitar `getSimpleName()` repetido em atualização de entidades | Transformer troca chamadas por `Class.getName()` dentro de `World.updateEntities` | Alteração pequena, benefício provavelmente pequeno e ainda não medido; muda nomes usados para diagnóstico/profiler. Verificar existência da chamada na base final antes de portar. |
| Estados de bloco compactos | Codifica propriedades num inteiro e encontra estados num array, em vez da tabela de transições convencional | Potencial de RAM; alteração estrutural mais arriscada, prioridade posterior. Preservar identidade dos estados, valores inválidos e comportamento de todas as propriedades. |
| Itens 3D gerados sob demanda | Modelo rápido para GUI; quads detalhados só ao uso fora da GUI, guardados por `SoftReference` | Ideia para carregamento e RAM, mas integração cara no pipeline vanilla/OptiFine. Cache liberável também pode causar reconstruções e travadas: medir antes de escolher esse desenho. |
| Liberar modelos antigos antes de recarregar recursos | Substitui entradas antigas por modelo vazio e limpa caches durante texture stitch | Investigar pico de RAM ao recarregar resource packs. Não aplicar a limpeza do mod diretamente: o momento correto depende do ciclo Forge que Dolly não usa. |

## Evidência por mecanismo

Os offsets abaixo pertencem ao método indicado; não são números de linha de fonte recuperado. Os arquivos em [evidence](evidence/) preservam desassemblagem produzida por `java -m jdk.jdeps/com.sun.tools.javap.Main -p -c -s -classpath <jar> <classe>` usando OpenJDK 21. Nenhuma classe do alvo foi carregada para execução.

**Deduplicação:** `client.FoamFixModelDeduplicate.onModelBake(ModelBakeEvent)`, offsets 0–6, consulta `clDeduplicate`; 56–65 aplica a profundidade configurada; 92–185 percorre o registro e chama `Deduplicator.deduplicateObject(model, 0)`; 236–237 solta a referência ao deduplicador após o passe. `Deduplicator.deduplicate0(Object)` trata `float[]` em 5–36, `float[][]` em 46–77 e coleções imutáveis em 208–233. `deduplicateObject(Object,int)`, 0–13 limita profundidade, 29–43 evita revisitar objetos, 44–207 trata modelos e `UnpackedBakedQuad` do Forge; 210–254 trata identificadores e transformações. O algoritmo amplo usa reflexão e estruturas do Forge: portar inteiro seria desnecessário. Evidências: [evento](evidence/foamfix-client_FoamFixModelDeduplicate.txt), [algoritmo](evidence/foamfix-client_Deduplicator.txt).

**Nome de classe:** `FoamFixTransformer.init()`, 75–102, registra o patch de `World` condicionado a `geReplaceSimpleName`. `FoamFixTransformer$5.transform(byte[],String)`, 24–38, seleciona `updateEntities`/`func_72939_s`; `FoamFixReplaceClassSimpleName$FFMethodVisitor.visitMethodInsn`, 0–42, troca especificamente `INVOKEVIRTUAL java/lang/Class.getSimpleName` por `getName`. Evidência: [transformer](evidence/foamfix-coremod_FoamFixTransformer.txt), [implementação](evidence/foamfix-extra.txt). A descrição embutida atribui o ganho ao cache de `getName`; isso é justificativa do autor, não medição nossa.

**Estados compactos:** `FoamyBlockState.func_177226_a(IProperty,Comparable)`, 58–78, conserva o mesmo estado para valor igual e delega alteração ao mapper; 80–143 mantém erro para valor não permitido. `func_177235_a(Map)`, 0–9, gera o índice compacto. `PropertyValueMapper.generateValue(IBlockState)`, 50–68, combina bits e popula `stateMap`; `withProperty(int,IProperty,Comparable)`, 53–68, mascara/substitui bits e indexa o array. Evidências: [estado](evidence/foamfix-common_FoamyBlockState.txt), [mapper](evidence/foamfix-common_PropertyValueMapper.txt).

**Itens sob demanda:** `FoamyItemLayerModel$DynamicItemModel.handlePerspective`, 10–28, escolhe outro modelo fora de GUI. `Dynamic3DItemModel.func_177550_a()`, 0–16, verifica cache; 17–105 reconstrói quads via `ItemLayerModel.getQuadsForSprite` e os guarda em `SoftReference`. `FoamFixDynamicItemModels.register()`, 20–38, substitui o loader Forge. Evidências: [modelo GUI](evidence/foamfix-client_FoamyItemLayerModel_DynamicItemModel.txt), [modelo 3D](evidence/foamfix-client_FoamyItemLayerModel_Dynamic3DItemModel.txt), [loader](evidence/foamfix-client_FoamFixDynamicItemModels.txt).

**Limpeza durante reload:** `FoamFixModelRegistryDuplicateWipe.onTextureStitchPost(TextureStitchEvent.Post)`, 100–141 troca modelos antigos por `DUMMY_MODEL`; 178–229 limpa `bakedModelStore`; 244–316 limpa um cache específico de `ItemModelMesherForge`. A descrição de configuração distingue redução do pico durante carregamento de redução da memória em jogo. Evidência: [limpeza](evidence/foamfix-client_FoamFixModelRegistryDuplicateWipe.txt).

## Defaults e diferença entre opção e efeito

`FoamFixConfig.init(File,boolean)` configura deduplicação ligada, profundidade 6, limpeza ligada, cache LaunchWrapper enfraquecido e redimensionamento de registros ligado. Com o segundo argumento verdadeiro também habilita os patches de propriedades, BlockPos, nomes de classe e itens dinâmicos. Os offsets correspondentes são 19–100 e 103–179. `geSmallLightingOptimize` recebe falso no construtor, 4–6, e não ganha uma opção no `init` inspecionado. Evidência: [configuração](evidence/foamfix-shared_FoamFixConfig.txt).

Há uma inconsistência importante neste artefato: `FoamFixTransformer.init()`, 12, registra `net.minecraft.block.state.BlockStateContainer`, enquanto `FoamyBlockStateContainer` realmente estende `net.minecraft.block.state.BlockState` e usa tipos 1.8. O mesmo transformer, em `transform`, offsets 18 e 40, testa `net.minecraft.util.math.Vec3i` e `net.minecraft.util.math.BlockPos$MutableBlockPos`. Isso impede tratar a existência desses patches, ou seus defaults verdadeiros, como prova de ativação correta na 1.8.9. Não foi executado Forge para observar alcance efetivo. Evidências: [seleção dos alvos](evidence/foamfix-coremod_FoamFixTransformer.txt), [herança real](evidence/foamfix-extra.txt).

## O que não compensa transplantar

- O ganho de aproximadamente 48 MB aparece como texto de configuração para BitSets dos registros FML. Dolly não tem esses registros; esse número não estima economia para nosso client.
- Enfraquecer o cache de bytecode LaunchWrapper e excluir bibliotecas da transformação resolve custos da infraestrutura de coremods. Dolly aplica seus patches durante o build e inicia como versão própria, sem esse pipeline.
- `ExtendedBlockState`, `UnpackedBakedQuad`, `IPerspectiveAwareModel`, eventos de bake/stitch e loaders Forge não existem automaticamente no Dolly. Seria necessário reimplementar apenas a ideia que apresentar benefício medido.
- O patch BlockPos tem implementação para unificar campos/getters mutáveis, mas seus nomes-alvo inconsistentes e o impacto em geometria tornam essa opção menos atraente que as anteriores.

A busca nos adaptadores `src/` e patches `tools/java/` não encontrou deduplicador, mapper de propriedades ou cache de modelos sob demanda próprios do Dolly. Isso não prova ausência de mecanismos equivalentes no OptiFine integrado. A comparação interna com esses mecanismos continua necessária antes de uma implementação. Não foi alterado código de produção.
