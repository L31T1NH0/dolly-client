# Editor do HUD: referência CMPack

Artefato: `references/cmpack_1.8.8_2.8.11-beta.zip`, membro `cmpack_1.8.8/CMPack_app.jar`.
REA confirmou a identidade do ZIP e seu inventário: `ev_fe8d120fb43b9542f6674219953bc9fb9f15555a121edacba5a01a0591c9cf3b`. SHA-256 do ZIP: `43e45120f1ffaf8793b692427a9b6eea376aadc56b054c608861b548a51722cc`.

A análise estática de `bK extends bf` identifica o editor de posições de módulos. O bytecode está em [evidence/hud-editor.bytecode.txt](evidence/hud-editor.bytecode.txt). SHA-256 de `bK.class`: `109df50424a57d0390e4f8b623433b6a4d040a3fd6263747155a59331aca3654`.

- `q(M,bq)`, offsets 4–89: converte as coordenadas físicas do mouse para a escala da GUI e guarda o deslocamento em relação à posição do módulo (`lC`, `lD`).
- `i(int,int)`, offsets 0–112: calcula a posição a partir das coordenadas atuais, âncora e deslocamento guardado, e atualiza `M.q(B)`.
- `e(int,int,float)`, offsets 0–80: aplica uma camada translúcida e guias sobre toda a tela. Nos offsets 115–137, mantém o arraste enquanto o botão está pressionado, chamando `i(int,int)`.
- `w(int,int,int)`, offsets 17–35: finaliza o módulo selecionado e aplica a última posição ao soltar.

Essas observações são do bytecode, sem executar o CMPack. A adaptação do Dolly mantém as coordenadas reais da GUI e o deslocamento de captura, mas usa seus próprios renderizadores de ping/TNT/sprint/slots/scoreboard/chat. Os limites registrados durante o desenho normal permitem selecionar o conteúdo real. Os controles de propriedades aparecem sob demanda. Não foram incorporadas classes nem imagens do CMPack.
