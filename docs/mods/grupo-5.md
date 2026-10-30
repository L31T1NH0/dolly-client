# Grupo 5: áudio e recarga de recursos

Abra **Visual → Configurar sons…**. A lista contém os eventos de som registrados no Minecraft, incluindo os adicionados por resource packs. A busca aceita o identificador do evento e palavras separadas por espaços no lugar de pontos ou sublinhados.

Cada barra vai de **0% a 200%**: 0% silencia aquele evento, 100% mantém seu volume original. Depois de selecionar uma barra, as setas ajustam em passos de 5 e Delete restaura 100%. O volume geral e o volume das categorias continuam valendo; uma categoria silenciada permanece muda. O resultado respeita o limite de volume do motor, então 200% pode não aumentar um som que já esteja no máximo.

As preferências são salvas por identificador em `soundVolumes`, dentro da configuração do Dolly. Eventos de packs temporariamente ausentes mantêm os valores salvos. **Restaurar volumes** devolve todos os eventos a 100%. A busca não depende de um catálogo externo ou de acesso à internet.

**Recarga separada** começa ligada:

- **F3+S:** recarrega o áudio sem recarregar as texturas.
- **F3+T:** recarrega os recursos visuais sem reiniciar o áudio.
- Desligar a opção restaura a recarga completa original para os dois atalhos.

Os botões dentro do menu permitem pedir as duas recargas separadamente, independentemente da opção dos atalhos. A recarga visual percorre o fluxo original de recursos, preservando apenas o carregador de áudio; não é uma recarga restrita exclusivamente a arquivos PNG.

Se a lista de sons ou uma recarga falhar, o menu apresenta a mensagem e mantém a navegação e as preferências disponíveis. Uma falha na recarga visual sempre libera a proteção temporária do áudio. Falhas ao salvar usam o aviso de persistência já existente no Dolly. O início da recarga de sons é confirmado como uma solicitação, pois a inicialização do dispositivo ocorre pelo motor nativo.

## Integração e validação

`SoundVolumes` valida e serializa as preferências sem dependências do jogo. `DollyAudio` aplica multiplicadores, atualiza os sons em andamento ao confirmar ajustes e coordena recargas. `DollyAudioScreen` oferece busca, paginação e barras. `AudioPatches` conecta o volume final do SoundManager, expõe os identificadores registrados e adapta os dois atalhos. A supressão temporária do carregador de áudio é restrita à thread da recarga visual e restaurada em `finally`.

`DollySoundVolumesTest` cobre limites, serialização e escala. `DollyGroupFiveTest` exercita o cálculo de volume real já modificado, categorias silenciadas, persistência, busca, ajuste, falhas de registro/recarga e a restauração da recarga completa. Também verifica os hooks no JAR final. Os dispositivos dos testes são substitutos: falta validar a reprodução, recarga e percepção do volume em um dispositivo de áudio real.
