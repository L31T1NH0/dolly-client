# Perfil de slots

Abra Right Shift → Controles → Perfil de slots. Cada botão representa um slot da hotbar, de 1 a 9. **Livre** permite selecionar o slot; **Bloqueado** impede sua seleção enquanto o perfil estiver ON.

O perfil começa desligado, sem slots bloqueados e sem atalho. Há um único perfil salvo. Desligá-lo libera todos os slots e mantém a lista marcada para a próxima ativação. Restaurar padrões limpa os bloqueios e remove o atalho.

## Controles

- Clique em cada slot para alternar entre Livre e Bloqueado. O último slot livre não pode ser bloqueado.
- Use **Perfil de slots: ON/OFF** para ativar/desativar manualmente.
- Clique em **Atalho** e pressione uma tecla. Esc cancela; Delete/Backspace remove a atribuição. Também é possível remapear **Dolly slot profile** nos controles nativos, incluindo botões do mouse.
- O atalho alterna o perfil a cada pressionamento, sem alternar novamente por repetição da tecla. Não funciona enquanto você digita no chat ou usa outra tela.
- Ao ativar ou editar um perfil ativo, um slot atual bloqueado é substituído pelo próximo slot livre, avançando pela hotbar e voltando ao primeiro após o nono.
- O HUD indica **Perfil de slots ativo** quando há bloqueios habilitados.

## Seleção

Uma tecla de hotbar que aponta para um slot bloqueado mantém o slot atual. Isso também vale para teclas da hotbar remapeadas. A roda pula slots bloqueados nos dois sentidos, com retorno circular. **Bloquear scroll** continua tendo prioridade: se estiver ligado, a roda não troca nenhum slot. Zoom pela roda e rolagem dos menus mantêm o funcionamento existente.

A seleção pelo botão do meio também recusa slots bloqueados, inclusive o caminho de seleção de slot vazio no criativo. O perfil não altera os itens: organização no inventário e troca de itens pelas telas continuam nativas. O menu de espectador não recebe os bloqueios.

As preferências ficam em `config/dolly.properties`; o atalho também é salvo nas opções nativas para persistir corretamente após reiniciar.

## Verificação

O build testa todas as combinações válidas de slots bloqueados, navegação nos dois sentidos, teclas, seleção pelo botão do meio, ativação/desativação, repetição de tecla, digitação no chat, inventários de outras entidades, espectador, salvamento/reset, validação de configurações e limites da tela de opções. Os testes também verificam os hooks no JAR final.

Ainda conferir dentro do jogo a aparência do menu/HUD e o uso com dispositivos reais: bloquear 2/4, tentar suas teclas, liberar o scroll e percorrer a hotbar nos dois sentidos; ativar o perfil segurando um slot bloqueado; testar o botão do meio, chat, inventário, zoom, espectador e reinício.
