# Krypton 0.3.2: oportunidades para o Dolly

## Resultado

O melhor aproveitamento deste JAR é estudar redução de alocações/cópias no enquadramento dos pacotes e caminhos curtos de VarInt. Não é um mod instalável no Dolly: o artefato declara Fabric, Minecraft >=26.2, compilação contra 26.3 e mixins Java 25. Todas as 25 classes próprias inspecionadas têm major version 69; o Dolly usa Java 8 (52) e Netty 4.0.23.Final.

Isso não demonstra redução de ping ou melhora de registro de golpes. A hipótese é economizar CPU/alocações locais; latência de transporte e aceitação dos ataques pelo servidor não foram medidas.

## Identidade e limites

- Entrada: `references/krypton-0.3.2.jar`; SHA-256 no [manifesto](evidence/manifest.json).
- REA `inspect_artifact`: `ev_6f6b7133ef0261d4f0a734c820aee31490a7a714fb7be94001e297f369f3c210`; registro completo em [krypton-rea.json](evidence/krypton-rea.json).
- Metadados preservados em [krypton-metadata.json](evidence/krypton-metadata.json), incluindo licença declarada LGPL-3.0 e dependência embutida `velocity-native-3.4.0-SNAPSHOT.jar`.
- Bytecode lido com `javap` do JDK 21, sem carregar/executar classes do mod. A biblioteca Velocity embutida foi inventariada, mas seus algoritmos internos e binários nativos não foram auditados.
- A comparação abaixo usa o JAR vanilla local indicado por `build/environment.json`. Os patches próprios atuais de `PatchClient` não selecionam as classes de rede examinadas. Não foi demonstrada equivalência integral entre a rede vanilla e todas as classes do JAR final com OptiFine.

## 1. Decoder de frames com menos cópias: candidato principal de rede

**Observado:** `Varint21FrameDecoderMixin.decode`, offsets 48–119, guarda o índice, lê o comprimento e espera mais bytes quando o frame está incompleto. Para um corpo completo, usa `readRetainedSlice` em 106–118. `readRawVarInt21`, 0–95, tem caminho de leitura agrupada com `getIntLE` e operações de bits; buffers pequenos seguem outro método.

Na base 1.8.9, `eq.decode`, 5–8, cria `byte[3]` por chamada; em 51–69 cria um `em` sobre um buffer temporário; em 93–105 entrega o corpo via `readBytes(int)`. Há, portanto, alocações e uma chamada de cópia concretas para investigar. Fontes: [Krypton pipeline](evidence/krypton-pipeline.bytecode.txt) e [rede vanilla](evidence/vanilla-network.bytecode.txt).

**Aplicação proposta:** implementar um decoder Java 8 compatível com Netty 4.0, preservando os limites e as regras da 1.8.9. O conceito de uma fatia com retenção pode ser adaptado às APIs antigas; não copiar diretamente chamadas modernas como `readRetainedSlice` ou `getIntLE` sem verificar a biblioteca local.

**Condição para aceitar:** comparação byte a byte e de índices com a base, frames divididos em todos os pontos, múltiplos frames juntos, comprimentos inválidos, desconexão e contagem de referências. O ganho deve aparecer em alocações/CPU numa carga representativa. Buffer retido por muito tempo também pode manter uma região grande de memória viva.

## 2. VarInts: mudança pequena, benefício ainda desconhecido

**Observado:** `VarIntMixin.write`, 0–55, trata valores de um e dois bytes diretamente, com uma escrita de `writeShort` no segundo caso; `writeVarIntFull`, 50–230, agrupa escritas maiores. `VarIntUtil.getVarIntLength`, 0–8, consulta uma tabela de 33 entradas indexada por `Integer.numberOfLeadingZeros`. Na base, `em.a(int)`, 0–27, usa um laço para calcular comprimento.

Fonte: [micro-otimizações](evidence/krypton-microopt.bytecode.txt).

**Aplicação proposta:** candidato isolado para `PacketBuffer` (`em`), sem mudar o protocolo. Validar zero, negativos, transições 127/128, 16383/16384 e limites de int. Maior velocidade numa microcarga não implica melhora perceptível no jogo; medir também o custo total de rede.

## 3. UTF-8 sem array temporário: útil como ideia

**Observado:** `Utf8StringMixin.write`, 0–78, valida comprimento, calcula bytes com `ByteBufUtil.utf8Bytes` e escreve com `ByteBuf.writeCharSequence`. A base `em.a(String)`, 0–7 e 64–77, cria `String.getBytes(UTF_8)` e depois escreve o array.

**Aplicação proposta:** avaliar codificação direta somente se o perfil mostrar esse custo. As APIs Netty diferem e os limites não são os mesmos: a base examinada limita a sequência codificada a 32767 bytes. Não transplantar a validação moderna nem alterar a forma de tratar caracteres inválidos inadvertidamente.

## 4. Separar cabeçalho do corpo

**Observado:** `MinecraftVarintPrepender.encode`, 0–78, aloca o cabeçalho, grava o comprimento e entrega duas saídas: cabeçalho e corpo retido. A escolha entre heap/direct depende do backend de cifra (`<clinit>`, 10–27).

**Inferência:** pode evitar uma cópia do corpo, mas adiciona gestão de buffers e depende da interação com compressão/cifra/transportes. Prioridade inferior ao decoder, até existir perfil da saída do Dolly.

## 5. Compressão e cifra nativas: adiar

**Observado:** `pipeline.compression.ConnectionMixin.setupCompression`, 188–228, cria compressor Velocity com nível **4**. Encoder e decoder usam buffers compatíveis e `deflate`/`inflate`; removê-los fecha recursos. `pipeline.encryption.ConnectionMixin.setupEncryption`, 8–114, cria cifras Velocity e instala os handlers; o ponto de ativação adicional examinado está no login do servidor. Implementações e offsets estão em [pipeline](evidence/krypton-pipeline.bytecode.txt).

**Limite:** presença dessas classes não prova aceleração nativa no computador do usuário. Não foram medidos fallback Java, suporte de plataforma, compatibilidade Netty 4.0/Java 8 ou custo de empacotamento. Isso exigiria uma investigação própria da versão de Velocity adequada e do ciclo de vida da conexão; não é inclusão simples no build atual.

## Configuração embutida não equivale a funcionalidade ativa

O JSON inclui `flusb_consolidation.*=true` (grafia literal), flags de natives falsas e nível 4. Entretanto:

- `KryptonMixinPlugin.shouldApplyMixin`, 0–1, retorna verdadeiro sem consultar configuração.
- Nenhuma das 25 classes próprias referencia `default_config`, `flusb` ou as chaves `pipeline.compression_natives` no constant pool examinado.
- `ConfigurableAutoFlush` existe como interface; nas classes próprias examinadas não há consumidor além da declaração.
- O nível 4 está diretamente no bytecode, e não foi encontrada leitura do JSON nesse conjunto de classes.

Portanto, **não foi demonstrada consolidação de flushes neste artefato**. A presença das opções não autoriza atribuir a ele batching funcional. O recorte não inclui todos os detalhes internos de bibliotecas externas.

`KryptonSharedInitializer.<clinit>`, 8–23, define `io.netty.allocator.maxOrder=9` quando a propriedade está ausente. Isso também não é um ajuste pronto para o Dolly: depende do alocador e da versão do Netty. [Inicialização e plugin](evidence/krypton-init.bytecode.txt).

## Seleção sugerida

1. Prototipar decoder de frames com menos alocações, se o perfil de rede justificar.
2. Considerar VarInts junto dessa medição.
3. Deixar UTF-8/prepender para uma segunda etapa e natives para investigação separada.
4. Não transportar otimizações de rastreamento do servidor para o client multiplayer. `ServerEntityMixin` examinado apenas substitui a lista inicial de passageiros por `ImmutableList.of()`; não melhora por si só o rastreamento executado no servidor remoto.

São propostas de implementação futura. Nenhuma foi aplicada ao Dolly nesta investigação.
