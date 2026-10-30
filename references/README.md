# Referências locais

Esta pasta reúne os binários usados nas investigações:

- `cmpack_1.8.8_2.8.11-beta.zip`: referência de jogabilidade e módulos do CMClient.
- `OptiFine_1.8.9_HD_U_M5.jar`: entrada local obrigatória para integrar o OptiFine M5 no build, além da investigação de VBO e Render Regions.
- `modrinth-mods.zip`: contém FoamFix 0.6.3a e EntityCulling 1.5.0 para investigação de memória e renderização.
- `krypton-0.3.2.jar`: referência de otimização de rede; este artefato usa Fabric/Minecraft moderno e Java 25, exigindo adaptação das ideias para a base 1.8.9/Java 8.

A [análise dos novos mods](../docs/mods/analise.md) registra oportunidades, prioridades, diferenças de versão e evidências de bytecode. Esses três mods continuam apenas como referências e não entram no build atual.

Os binários são ignorados pelo Git. Este README e os relatórios em `docs/` permanecem versionáveis. O CMClient não entra na distribuição. O instalador OptiFine é usado somente na compilação; suas classes e recursos de runtime são incorporados ao JAR privado do Dolly.
