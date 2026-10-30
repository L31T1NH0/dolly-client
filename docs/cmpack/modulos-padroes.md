# Defaults efetivos de módulos — CMClient 2.8.11, Minecraft 1.8.8

Análise estática; nenhum client executado e nenhum arquivo de produção modificado. O pacote não contém a configuração pessoal do usuário. A tabela descreve inicialização limpa, antes de overrides persistidos. Classes e offsets são do bytecode JVM; trechos estão em [module-defaults.bytecode.txt](evidence/module-defaults.bytecode.txt); os CONST inteiros foram simplificados sem deslocar os offsets.

## Correção importante: opção habilitada não equivale a módulo ativo

- M.<init>(String,int,boolean) NÃO liga o módulo. O boolean recebido controla apenas registro nos containers C.ay/C.aB: offsets 157–206. M.bT, estado ligado, permanece false inicial da JVM.
- M.w(boolean) offsets 0–5 escreve M.bT. M.ad() offsets 0–4 lê M.bT.
- M.bS é outro flag, usado em bP.ak(): offsets 0–27 devolvem false quando owner.bS && !owner.ad(); somente em 28–49 lê o valor da opção. Assim há um gate adicional que não aparece ao olhar apenas o construtor da opção.
- au (Old Animations) define bS=true no offset42, mas não chama w(true) em seu construtor. Resultado limpo: **OFF**. Embora blocking/sword/eating/attacking/armor/heart armazenem true, bP.ak() devolve false enquanto au.ad()==false. Portanto Old Animations NÃO explica a sensação padrão de uma instalação realmente limpa desta build. Poderia explicar uma instalação em que foi ligado em modules.json.
- O (Animations) também define bS=true em42, mas chama O.w(true) em330. Resultado: **ON**, com GUI Animation=true (111), velocidade GUI 3 (152); Sneak Animation=true (220), velocidade armazenada ≈8.5 (259); Chunk Animation=false (321). A animação de sneak é candidata real de diferença visual por padrão; ir no trecho 451–554 atualiza a interpolação de agachar e várias rotinas de render consultam O.cy.

## Ordem de inicialização e overrides

1. z.m() constrói D em61, constrói C e chama C.init em105.
2. C.init instancia os 54 módulos da tabela. M.<init> registra as instâncias e os respectivos construtores podem chamar w(true); C.init não liga a coleção inteira.
3. z.m() cria c (general) e v (modules) em163/185, monta x, chama x.A em200.
4. x.A usa BootstrapMethods[0] -> x.w(m), que chama m.D em53; isso é carregamento, não salvamento (x.B usa x.q(m)->m.C).
5. v.D itera os módulos e v.t(M,JsonElement) lê enabled em25–32, properties em143–217. v.q(M,JsonElement) obtém getAsBoolean e chama M.w em5. m.q(JsonObject,String,Consumer) só aplica chave se objeto e valor não forem null (0–23), preservando constructor quando ausente.
6. Os valores de options podem ser substituídos separadamente do enabled. Não se pode inferir a configuração em que o usuário jogou a partir dos defaults.

## Outra distinção: estado ligado não é gate universal de comportamento

acQ.q(aeE) offsets0–18 chama forEach(M::aa) sobre TODOS os módulos (BootstrapMethods[9] de acQ comprova M.aa). Não há filtro ad nessa chamada. z.v() usa filtro M::ad (BootstrapMethods[1] de z) para a lista de HUD, mas isso não garante que todos os hooks de gameplay consultam o mesmo flag. No caso de Old Animations, o gate via bP.ak está comprovado. No caso de Particles, Eb consulta aw.ad explicitamente. Essa distinção deve ser mantida ao chamar algo de “desligado”.

## Toggle Sprint

- aO.<init>: módulo ON por w(true) em443; Toggle Sprint hK=true em359; Toggle Sneak ht=false em400; Show Status Text hE=true em318.
- aO.aa faz detecção de borda e timing para alternar o estado ao soltar a tecla (190–314), e quando hA ativo e há movimento para frente, chama wn.q(key,true) em386–430. Não é “sprint sempre ativo imediatamente”: o estado alternado é iniciado pela tecla.
- O hook aO.aa é chamado pelo callback global citado acima. Seus valores true são candidatos reais de conforto padrão, mas não justificam prometer maior alcance ou registro de hits.

## Particles

- aw (Particles) não chama w(true): módulo OFF.
- Always Critical fY=false (96), Always Sharpness fX=false (135), multiplicadores fW/fZ=1 (203/270), ranges1–20.
- Eb, no ataque, lê aw.ad em391 e combina isso com opções de partículas forçadas (410/440). Portanto não há tempestade de partículas/critical cosmético habilitado por padrão. jp lê multiplicadores numéricos diretamente, mas o default1 mantém uma emissão por laço.
- O limite das opções gerais D.bC=1000 é outro recurso independente, não confundir com o módulo aw.

## Dependências de plataforma

- acQ.q(k), offsets654–681: cRt=(Qr.VM()==Qs.bVQ). Com a enum identificada como Windows, Anti Cheat é condicionado a Windows; em Linux não há esse backend. Não consultei serviço externo.
- Emotes af inicia com M.w(acQ.cRw), não ON universal. acQ.q(k)923–1037 calcula disponibilidade por ambiente/arquitetura e driver (inclui exclusão llvmpipe).
- Discord RP ae inicia com ae.w(acQ.cRj); acQ define cRj=true em651 nesta rotina, mas há gate/override de suporte no setter. Listado como condicional, sem alegar execução de RPC.

## Inventário do estado inicial

ON abaixo significa que o construtor chama w(true); a ativação de efeitos ainda pode depender de tecla, configuração e hooks. OFF significa valor inicial M.bT false sem ativação no construtor examinado.

| Classe | Módulo | Estado de construtor |
| --- | --- | --- |
| aT | Armor HUD | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| P | Auto Friend | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| Q | AutoGG | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| R | AutoText | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| S | Block Overlay | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| V | Chat | ON (693 invokevirtual      V.w:(Z)V) |
| aJ | Tab | ON (669 invokevirtual      aJ.w:(Z)V) |
| X | Coordinates | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| U | CPS | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| T | Bossbar | ON (120 invokevirtual      T.w:(Z)V) |
| aD | Scoreboard | ON (308 invokevirtual      aD.w:(Z)V) |
| aA | Potion Counter | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| ad | Direction Hud | ON (777 invokevirtual      ad.w:(Z)V) |
| aa | Crosshair | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| ag | FPS | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| aq | Memory | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| aN | TNT Timer | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| aj | Hit Color | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| au | Old Animations | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| O | Animations | ON (330 invokevirtual      O.w:(Z)V) |
| Y | Cosmetics | ON (434 invokevirtual      Y.w:(Z)V) |
| av | Pack Overlay | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| ax | Perspective | ON (186 invokevirtual      ax.w:(Z)V) |
| af | Emotes | Condicional (38 getfield           acQ.cRw:Z; 41 invokespecial      M.w:(Z)V) |
| aQ | Waypoints | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| aH | Skins | ON (110 invokevirtual      aH.w:(Z)V) |
| aS | Zoom | ON (270 invokevirtual      aS.w:(Z)V) |
| ay | Ping Display | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| W | Combo Display | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| aB | Potion Effects | ON (175 invokevirtual      aB.w:(Z)V) |
| aI | Speedometer | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| aE | Server Address | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| aC | Reach Display | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| aK | Time Changer | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| aR | Weather Changer | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| aM | Time Display | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| aO | Toggle Sprint | ON (443 invokevirtual      aO.w:(Z)V) |
| ae | Discord RP | Condicional (189 getfield           acQ.cRj:Z; 192 invokevirtual      ae.w:(Z)V) |
| ar | Motion Blur | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| ao | Item Physics | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| ah | Full Bright | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| ai | Glint Colorizer | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| aw | Particles | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| aW | Keystrokes | ON (1698 invokevirtual      aW.w:(Z)V) |
| az | Play Time | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| ak | Hitbox | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| ap | 2D Items | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| aP | Wavey Capes | ON (633 invokevirtual      aP.w:(Z)V) |
| as | Nametags | ON (337 invokevirtual      as.w:(Z)V) |
| aG | 3D Skin Layers | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| ac | Debug Screen | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| ab | Damage Tint | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| aF | Shiny Pots | OFF (M.bT inicial false; nenhum w(true) no construtor) |
| an | InputFix | ON (38 invokevirtual      an.w:(Z)V) |

## Limites

Não executei o client nem carreguei configurações pessoais. Não há prova causal da sensação subjetiva; o levantamento corrige o que está realmente disponível/ativo no início. A interpretação do módulo antigo como ativo por ter opções true deve ser retirada de qualquer conclusão sobre defaults.
