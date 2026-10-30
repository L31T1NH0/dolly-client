/* A local design prototype. It never reads or writes Minecraft's preferences. */
(() => {
  'use strict';
  const {settings, modules, groups} = window.DOLLY_DATA;
  const $ = selector => document.querySelector(selector);
  const esc = value => String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const icon = name => `<svg aria-hidden="true"><use href="#i-${esc(name)}"/></svg>`;
  const defaults = Object.fromEntries(Object.entries(settings).map(([key, setting]) => [key, setting.value]));
  const hudDefaults = [
    {key:'ping',name:'Ping',icon:'signal',x:3,y:6,scale:1,background:0,visible:true},
    {key:'tnt',name:'TNT Timer',icon:'timer',x:3,y:18,scale:1,background:0,visible:true},
    {key:'sprint',name:'Sprint',icon:'run',x:3,y:30,scale:1,background:0,visible:true},
    {key:'slots',name:'Perfil de slots',icon:'slots',x:3,y:42,scale:1,background:0,visible:true},
    {key:'scoreboard',name:'Scoreboard',icon:'board',x:96,y:35,scale:1,background:35,visible:true},
    {key:'chat',name:'Chat',icon:'chat',x:3,y:94,scale:1,background:35,visible:true}
  ];
  const clone = value => JSON.parse(JSON.stringify(value));
  const state = {values:{...defaults},hud:clone(hudDefaults),bindings:{},blockedSlots:0,snap:true};
  const storageKey = 'dolly-settings-prototype-v1';
  let view = 'modules', filter = 'all', selectedModule = 'zoom', selectedGroup = 1, selectedHud = 'ping';
  let controlId = 0, drag = null;
  const history = [];
  const clamp = (value,min,max) => Math.min(max,Math.max(min,value));
  const validNumber = (value,min,max) => typeof value === 'number' && Number.isFinite(value) && value >= min && value <= max;
  try {
    const saved = JSON.parse(localStorage.getItem(storageKey));
    if (saved && saved.version === 1) {
      for (const [key, fallback] of Object.entries(defaults)) {
        const value = saved.values?.[key], setting = settings[key];
        if (typeof value !== typeof fallback) continue;
        if (typeof value === 'number' && (!Number.isFinite(value) || setting.range && !validNumber(value,setting.range[0],setting.range[1])
            || setting.choices && (!Number.isInteger(value) || value < 0 || value >= setting.choices.length))) continue;
        if (typeof value === 'string' && value.length > 1000) continue;
        state.values[key] = value;
      }
      for (const item of state.hud) {
        const stored = Array.isArray(saved.hud) ? saved.hud.find(h => h?.key === item.key) : null;
        if (!stored) continue;
        for (const [key,min,max] of [['x',0,100],['y',0,100],['scale',.5,2],['background',0,80]])
          if (validNumber(stored[key],min,max)) item[key] = stored[key];
        if (typeof stored.visible === 'boolean') item.visible = stored.visible;
      }
      if (Number.isInteger(saved.blockedSlots) && validNumber(saved.blockedSlots,0,510)) state.blockedSlots = saved.blockedSlots;
      if (typeof saved.snap === 'boolean') state.snap = saved.snap;
      for (const key of ['scoreboardKey','slotProfileKey'])
        if (typeof saved.bindings?.[key] === 'string' && saved.bindings[key].length <= 40) state.bindings[key] = saved.bindings[key];
    }
  } catch { /* A fresh preview remains usable when browser storage is unavailable. */ }

  function status(text) { $('#status').textContent = text; }
  function changed() { status('Alterações locais ainda não salvas.'); }
  function save() {
    try {
      localStorage.setItem(storageKey,JSON.stringify({version:1,...state}));
      status('Salvo neste navegador. As configurações do jogo não foram alteradas.');
    } catch { status('Não foi possível salvar neste navegador. A prévia continua funcionando.'); }
  }
  function switchButton(name, enabled, onClick, key) {
    const button = document.createElement('button');
    button.type = 'button'; button.className = 'switch'; button.setAttribute('role','switch');
    button.setAttribute('aria-label',name); button.setAttribute('aria-checked',String(enabled));
    if (key) button.dataset.help = key;
    button.innerHTML = `<span class="switch-label">${enabled ? 'Ligado' : 'Desligado'}</span><span class="switch-track" aria-hidden="true"></span>`;
    button.addEventListener('click',() => onClick(!enabled));
    return button;
  }
  function format(key,value) {
    if (key === 'menuFps' && value === 0) return 'Monitor';
    if (key === 'timeOfDay') { const minute = Math.floor(((value + 6000) % 24000) / 1000 * 60); return `${String(Math.floor(minute/60)).padStart(2,'0')}:${String(minute%60).padStart(2,'0')}`; }
    if (key === 'zoomFactor') return `${value}×`;
    if (key === 'zoomSensitivity') return `${Math.round(value*100)}%`;
    if (/Opacity|fireHeight|waterFogDensity/.test(key)) return `${value}%`;
    return String(value);
  }
  function control(key) {
    const setting = settings[key], id = `setting-${key}-${controlId++}`;
    const wrap = document.createElement('div'); wrap.className = 'control-wrap'; wrap.dataset.setting = key;
    const change = value => { state.values[key] = value; changed(); };
    if (typeof state.values[key] === 'boolean') {
      const mount = () => wrap.replaceChildren(switchButton(setting.name,state.values[key],value => { change(value); mount(); },key));
      mount();
    } else if (key.endsWith('Key')) {
      const button = document.createElement('button'); button.className = 'button';
      button.textContent = state.bindings[key] || 'Definir atalho'; button.setAttribute('aria-label',setting.name); wrap.append(button);
      button.onclick = () => {
        if (button.dataset.binding) return;
        button.dataset.binding = 'true'; button.textContent = 'Pressione uma tecla…';
        const capture = event => {
          event.preventDefault(); event.stopPropagation();
          if (event.key !== 'Escape') { state.bindings[key] = ['Delete','Backspace'].includes(event.key) ? '' : event.code; changed(); }
          button.textContent = state.bindings[key] || 'Definir atalho'; delete button.dataset.binding;
          document.removeEventListener('keydown',capture,true);
          button.removeEventListener('blur',cancel);
        };
        const cancel = () => { document.removeEventListener('keydown',capture,true); delete button.dataset.binding; button.textContent = state.bindings[key] || 'Definir atalho'; };
        document.addEventListener('keydown',capture,true); button.addEventListener('blur',cancel,{once:true});
      };
    } else if (setting.choices) {
      const select = document.createElement('select'); select.id = id; select.setAttribute('aria-label',setting.name);
      setting.choices.forEach((name,index) => select.add(new Option(name,index)));
      select.value = state.values[key]; select.onchange = () => change(Number(select.value)); wrap.append(select);
    } else if (setting.range) {
      const output = document.createElement('output'); output.className = 'range-value'; output.htmlFor = id; output.value = format(key,state.values[key]);
      const input = document.createElement('input'); input.id = id; input.type = 'range';
      [input.min,input.max,input.step] = setting.range; input.value = state.values[key]; input.setAttribute('aria-label',setting.name);
      input.oninput = () => { change(Number(input.value)); output.value = format(key,Number(input.value)); };
      wrap.append(output,input);
    } else {
      const input = document.createElement('input'); input.id = id; input.type = typeof state.values[key] === 'number' ? 'number' : 'text';
      input.value = state.values[key]; input.maxLength = 1000; input.setAttribute('aria-label',setting.name);
      input.onchange = () => { const value = input.type === 'number' ? Number(input.value) : input.value; if (typeof value !== 'number' || Number.isFinite(value)) change(value); };
      wrap.append(input);
    }
    wrap.querySelector('button,input,select')?.setAttribute('data-setting-key',key);
    return wrap;
  }
  function visibleModules() {
    const query = $('#module-search').value.toLocaleLowerCase('pt-BR').trim();
    return modules.filter(mod => (!query || mod.name.toLocaleLowerCase('pt-BR').includes(query)) && (filter === 'all' || Boolean(state.values[mod.key]) === (filter === 'on')));
  }
  function renderModules(focusKey) {
    const list = $('#module-list'), top = list.scrollTop, visible = visibleModules();
    if (!visible.some(mod => mod.key === selectedModule)) selectedModule = visible[0]?.key || null;
    $('#module-count').textContent = `${visible.length} módulos · ${modules.filter(mod => state.values[mod.key]).length} ligados`;
    list.replaceChildren();
    for (const mod of visible) {
      const enabled = Boolean(state.values[mod.key]), row = document.createElement('div');
      row.className = `module-row${enabled ? ' enabled' : ''}${selectedModule === mod.key ? ' selected' : ''}`; row.dataset.module = mod.key;
      const open = document.createElement('button'); open.className = 'module-open'; open.dataset.help = mod.key;
      open.setAttribute('aria-label',`Configurar ${mod.name}`); open.setAttribute('aria-pressed',String(selectedModule === mod.key));
      open.innerHTML = `<span class="module-icon">${icon(mod.icon)}</span><span><span class="module-name">${esc(mod.name)}</span><span class="module-subtitle">${mod.hud ? 'Elemento do HUD' : mod.options.length ? 'Ajustes disponíveis' : 'Comportamento visual'}</span></span>`;
      open.onclick = () => { selectedModule = mod.key; renderModules(); renderDetail(); };
      const toggle = switchButton(mod.name,enabled,value => { state.values[mod.key] = value; changed(); renderModules(mod.key); renderDetail(); },mod.key);
      toggle.id = `toggle-${mod.key}`; row.append(open,toggle); list.append(row);
    }
    if (!visible.length) list.innerHTML = '<div class="empty-state">Nenhum módulo encontrado.<br>Tente outro nome ou filtro.</div>';
    list.scrollTop = top;
    if (focusKey) (document.getElementById(`toggle-${focusKey}`) || list).focus({preventScroll:true});
  }
  function renderDetail() {
    const panel = $('#module-detail'), mod = modules.find(mod => mod.key === selectedModule);
    if (!mod) { panel.innerHTML = '<p class="detail-empty">Selecione um módulo para ver sua função e seus ajustes.</p>'; return; }
    const setting = settings[mod.key];
    panel.innerHTML = `<div class="detail-icon">${icon(mod.icon)}</div><h2 class="detail-title">${esc(mod.name)}</h2><p class="detail-description">${esc(setting.description)}</p><p class="impact ${setting.color}">${esc(setting.impact)}</p><hr class="detail-divider"><div class="detail-controls"></div>`;
    const controls = panel.querySelector('.detail-controls');
    for (const key of mod.options) {
      const row = document.createElement('div'); row.className = 'compact-control'; row.dataset.help = key;
      const label = document.createElement('span'); label.className = 'compact-label'; label.textContent = settings[key].name;
      row.append(label,control(key)); controls.append(row);
    }
    if (mod.key === 'slotProfile') {
      const slots = document.createElement('div'); slots.className = 'slot-buttons'; slots.setAttribute('role','group'); slots.setAttribute('aria-label','Slots bloqueados');
      for (let slot = 0; slot < 9; slot++) {
        const button = document.createElement('button'); const blocked = Boolean(state.blockedSlots & (1 << slot));
        button.className = `button${blocked ? ' blocked' : ''}`; button.textContent = String(slot+1); button.setAttribute('aria-pressed',String(blocked)); button.setAttribute('aria-label',`Slot ${slot+1}: ${blocked ? 'bloqueado' : 'livre'}`);
        button.onclick = () => { const next = state.blockedSlots ^ (1 << slot); if (next === 511) { status('Mantenha pelo menos um slot livre.'); return; } state.blockedSlots = next; changed(); renderDetail(); };
        slots.append(button);
      }
      controls.append(slots); const note = document.createElement('p'); note.className = 'detail-empty'; note.textContent = 'Clique nos slots para bloquear a seleção.'; controls.append(note,control('slotProfileKey'));
    }
    if (!mod.options.length && !mod.hud) controls.innerHTML = '<p class="detail-empty">Este módulo não precisa de ajustes adicionais.</p>';
    if (mod.hud) {
      const button = document.createElement('button'); button.className = 'button'; button.innerHTML = `${icon('hud')}Posicionar no HUD`;
      button.onclick = () => { selectedHud = mod.hud; showView('hud'); }; panel.append(button);
    }
  }
  function renderSettingsNav() {
    const nav = $('#settings-nav'); nav.replaceChildren();
    groups.forEach((group,index) => {
      const button = document.createElement('button'); button.className = index === selectedGroup ? 'active' : '';
      button.setAttribute('aria-current',index === selectedGroup ? 'page' : 'false'); button.innerHTML = `${icon(group.icon)}${esc(group.name)}`;
      button.onclick = () => { selectedGroup = index; renderSettingsNav(); renderSettings(); }; nav.append(button);
    });
  }
  function renderSettings() {
    const group = groups[selectedGroup], content = $('#settings-content');
    content.innerHTML = `<h2>${esc(group.name)}</h2><p class="group-intro">Ajustes agrupados por função. Role para ver todas as opções.</p>`;
    for (const section of group.sections) {
      const block = document.createElement('section'); block.className = 'settings-section'; block.innerHTML = `<h3>${esc(section.name)}</h3>`;
      for (const key of section.keys) {
        const setting = settings[key], row = document.createElement('div'); row.className = 'setting-row'; row.dataset.help = key;
        row.innerHTML = `<div class="setting-copy"><label>${esc(setting.name)}</label><p>${esc(setting.description)}</p><p class="impact ${setting.color}">${esc(setting.impact)}</p></div>`;
        const input = control(key); row.append(input);
        const field = input.querySelector('input,select'); if (field?.id) row.querySelector('label').htmlFor = field.id;
        block.append(row);
      }
      content.append(block);
    }
    content.scrollTop = 0;
  }

  const hudContent = {
    ping:'Ping: 42 ms',tnt:'<span class="tnt-value">TNT: 3.2s</span>',sprint:'<span class="sprint-value">Sprint alternado</span>',slots:'<span class="slot-value">Perfil de slots ativo</span>',
    scoreboard:'<strong>THE BRIDGE</strong><div>Você: 2</div><div>Rival: 1</div><div class="board-small">Vitórias: 7</div>',
    chat:'<div><span class="chat-server">[Dolly]</span> Boa partida!</div><div>Você entrou na arena.</div>'
  };
  function hudItem() { return state.hud.find(item => item.key === selectedHud); }
  function remember(snapshot = state.hud) { history.push(clone(snapshot)); if (history.length > 30) history.shift(); $('#undo').disabled = false; }
  function updatePositionInputs() {
    for (const key of ['x','y']) { const input = $(`#hud-${key}`); if (input && document.activeElement !== input) input.value = hudItem()[key].toFixed(1); }
  }
  function paintHUD() {
    const canvas = $('#hud-canvas'), small = canvas.clientWidth < 450;
    if (!canvas.clientWidth) return;
    for (const item of state.hud) {
      const node = document.getElementById(`hud-${item.key}-item`);
      const base = ['chat','scoreboard'].includes(item.key) ? (small ? 9 : 12) : (small ? 10 : 14);
      node.style.fontSize = `${base*item.scale}px`; node.style.backgroundColor = `rgb(9 17 12 / ${item.background/100})`;
      node.classList.toggle('selected',item.key === selectedHud); node.classList.toggle('invisible',!item.visible);
      node.setAttribute('aria-pressed',String(item.key === selectedHud));
      node.style.left = `${Math.max(0,canvas.clientWidth-node.offsetWidth)*item.x/100}px`;
      node.style.top = `${Math.max(0,canvas.clientHeight-node.offsetHeight)*item.y/100}px`;
    }
    updatePositionInputs();
  }
  function selectHUD(key) { selectedHud = key; paintHUD(); renderHUDElements(); renderHUDProperties(); }
  function moveHUD(item,left,top,snap) {
    const canvas = $('#hud-canvas'), node = document.getElementById(`hud-${item.key}-item`);
    const maxX = Math.max(0,canvas.clientWidth-node.offsetWidth), maxY = Math.max(0,canvas.clientHeight-node.offsetHeight);
    const align = (value,max) => { value = clamp(value,0,max); if (snap) for (const stop of [0,max/2,max]) if (Math.abs(value-stop) < 6) return stop; return value; };
    left = align(left,maxX); top = align(top,maxY);
    item.x = maxX ? Number((left/maxX*100).toFixed(2)) : 0; item.y = maxY ? Number((top/maxY*100).toFixed(2)) : 0;
    $('#guide-x').hidden = !(snap && Math.abs(item.x-50) < .1); $('#guide-y').hidden = !(snap && Math.abs(item.y-50) < .1);
    paintHUD();
  }
  function createHUD() {
    const canvas = $('#hud-canvas');
    for (const item of state.hud) {
      const node = document.createElement('div'); node.id = `hud-${item.key}-item`; node.dataset.hud = item.key;
      node.className = `hud-item ${item.key}`; node.tabIndex = 0; node.setAttribute('role','button'); node.setAttribute('aria-label',`${item.name}: selecionar e mover com as setas`);
      node.innerHTML = `<span class="hud-item-label">${esc(item.name)}</span>${hudContent[item.key]}`;
      node.addEventListener('focus',() => selectHUD(item.key));
      node.addEventListener('pointerdown',event => {
        if (event.button !== 0) return;
        event.preventDefault(); node.focus({preventScroll:true}); selectHUD(item.key);
        drag = {key:item.key,x:event.clientX,y:event.clientY,left:node.offsetLeft,top:node.offsetTop,snapshot:clone(state.hud),moved:false};
        node.setPointerCapture(event.pointerId); node.classList.add('dragging');
      });
      node.addEventListener('pointermove',event => {
        if (!drag || drag.key !== item.key) return;
        const dx = event.clientX-drag.x, dy = event.clientY-drag.y;
        if (!drag.moved && Math.abs(dx)+Math.abs(dy) < 2) return;
        if (!drag.moved) { remember(drag.snapshot); drag.moved = true; }
        moveHUD(item,drag.left+dx,drag.top+dy,state.snap);
      });
      const stop = () => { if (drag?.key !== item.key) return; if (drag.moved) changed(); drag = null; node.classList.remove('dragging'); $('#guide-x').hidden = $('#guide-y').hidden = true; };
      node.addEventListener('pointerup',stop); node.addEventListener('pointercancel',stop); node.addEventListener('lostpointercapture',stop);
      node.addEventListener('keydown',event => {
        if (!['ArrowLeft','ArrowRight','ArrowUp','ArrowDown'].includes(event.key)) return;
        event.preventDefault(); remember(); const step = event.shiftKey ? 10 : 1;
        moveHUD(item,node.offsetLeft + (event.key === 'ArrowLeft' ? -step : event.key === 'ArrowRight' ? step : 0),
          node.offsetTop + (event.key === 'ArrowUp' ? -step : event.key === 'ArrowDown' ? step : 0),false);
        changed();
      });
      canvas.append(node);
    }
    canvas.addEventListener('click',event => { if (event.target === canvas) document.getElementById(`hud-${selectedHud}-item`).focus(); });
  }
  function renderHUDElements() {
    const container = $('#hud-elements'); container.replaceChildren();
    for (const item of state.hud) {
      const button = document.createElement('button'); button.className = `element-select${item.key === selectedHud ? ' active' : ''}`;
      button.setAttribute('aria-pressed',String(item.key === selectedHud)); button.innerHTML = `${icon(item.icon)}${esc(item.name)}<span class="element-status">${item.visible ? 'Visível' : 'Oculto'}</span>`;
      button.onclick = () => selectHUD(item.key); container.append(button);
    }
  }
  function renderHUDProperties() {
    const item = hudItem(), panel = $('#hud-properties');
    panel.className = 'hud-properties'; panel.innerHTML = `<h3>${esc(item.name)}</h3><div id="hud-visible"></div><div class="position-fields"><label>X (%)<input id="hud-x" type="number" min="0" max="100" step="0.1" value="${item.x.toFixed(1)}"></label><label>Y (%)<input id="hud-y" type="number" min="0" max="100" step="0.1" value="${item.y.toFixed(1)}"></label></div><label class="compact-control">Escala<output id="scale-output">${item.scale.toFixed(2)}×</output><input id="hud-scale" type="range" min="0.5" max="2" step="0.05" value="${item.scale}"></label><label class="compact-control">Opacidade do fundo<output id="background-output">${item.background}%</output><input id="hud-background" type="range" min="0" max="80" step="5" value="${item.background}"></label><button id="reset-selected" class="button">Restaurar esta posição</button><p class="selected-note">Ocultos ficam tracejados apenas no editor.</p>`;
    $('#hud-visible').append(switchButton(`Mostrar ${item.name}`,item.visible,value => { remember(); item.visible = value; changed(); paintHUD(); renderHUDElements(); renderHUDProperties(); }));
    for (const key of ['x','y']) $(`#hud-${key}`).onchange = event => {
      const next = Number(event.target.value); if (!Number.isFinite(next)) return;
      remember(); item[key] = clamp(next,0,100); event.target.value = item[key].toFixed(1); changed(); paintHUD();
    };
    for (const [key,label] of [['scale','scale'],['background','background']]) $(`#hud-${key}`).oninput = event => {
      remember(); item[key] = Number(event.target.value); $(`#${label}-output`).value = key === 'scale' ? `${item[key].toFixed(2)}×` : `${item[key]}%`; changed(); paintHUD();
    };
    $('#reset-selected').onclick = () => { remember(); const original = hudDefaults.find(h => h.key === item.key); item.x = original.x; item.y = original.y; changed(); paintHUD(); renderHUDProperties(); };
  }
  function undo() {
    if (!history.length) return;
    const previous = history.pop(); state.hud.forEach(item => Object.assign(item,previous.find(h => h.key === item.key)));
    $('#undo').disabled = !history.length; changed(); paintHUD(); renderHUDElements(); renderHUDProperties();
  }
  function showView(next) {
    view = next; hideTip();
    document.querySelectorAll('.view').forEach(node => node.hidden = node.id !== `${view}-view`);
    document.querySelectorAll('[data-view]').forEach(button => { button.classList.toggle('active',button.dataset.view === view); button.setAttribute('aria-current',button.dataset.view === view ? 'page' : 'false'); });
    const titles = {modules:['Módulos','Os recursos que você escolhe usar.','Lista única · role para ver todos os módulos'],settings:['Configurações','Ajustes técnicos e detalhes do client.','Grupos por função · sem paginação'],hud:['Editar HUD','Cada elemento no lugar que faz sentido para você.','As posições se adaptam ao tamanho da tela']};
    $('#page-title').textContent = titles[view][0]; $('#page-description').textContent = titles[view][1]; $('#footer-hint').textContent = titles[view][2];
    if (view === 'modules') { renderModules(); renderDetail(); }
    if (view === 'settings') { renderSettingsNav(); renderSettings(); }
    if (view === 'hud') { renderHUDElements(); renderHUDProperties(); requestAnimationFrame(paintHUD); }
  }

  let tipTimer, tipTarget;
  const tooltip = $('#tooltip'); let pointerX = 0, pointerY = 0;
  function hideTip() { clearTimeout(tipTimer); tipTarget?.removeAttribute('aria-describedby'); tipTarget = null; tooltip.hidden = true; }
  function positionTip() {
    tooltip.style.left = `${clamp(pointerX+14,8,window.innerWidth-tooltip.offsetWidth-8)}px`;
    tooltip.style.top = `${clamp(pointerY+16,8,window.innerHeight-tooltip.offsetHeight-8)}px`;
  }
  function scheduleTip(target) {
    if (target === tipTarget) return;
    hideTip(); if (!target || !settings[target.dataset.help]) return;
    tipTarget = target;
    tipTimer = setTimeout(() => {
      const setting = settings[target.dataset.help];
      tooltip.innerHTML = `<strong>${esc(setting.name)}</strong><p>${esc(setting.description)}</p><div class="impact ${setting.color}">${esc(setting.impact)}</div>`;
      tooltip.hidden = false; target.setAttribute('aria-describedby','tooltip'); positionTip();
    },300);
  }
  document.addEventListener('pointerover',event => scheduleTip(event.target.closest('[data-help]')));
  document.addEventListener('pointermove',event => { pointerX = event.clientX; pointerY = event.clientY; if (!tooltip.hidden) positionTip(); });
  document.addEventListener('pointerout',event => { if (tipTarget && !tipTarget.contains(event.relatedTarget)) hideTip(); });
  document.addEventListener('focusin',event => { const target = event.target.closest('[data-help]'); if (!target) return; const box = target.getBoundingClientRect(); pointerX = box.left; pointerY = box.bottom; scheduleTip(target); });
  document.addEventListener('focusout',hideTip);
  document.addEventListener('scroll',hideTip,true);
  document.addEventListener('keydown',event => {
    if (event.key === 'Escape') hideTip();
    if (view === 'hud' && (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'z' && !['INPUT','TEXTAREA'].includes(document.activeElement.tagName)) { event.preventDefault(); undo(); }
  });
  document.querySelectorAll('[data-view]').forEach(button => button.onclick = () => showView(button.dataset.view));
  $('.brand').onclick = event => { event.preventDefault(); showView('modules'); };
  $('#module-search').oninput = () => { renderModules(); renderDetail(); };
  document.querySelectorAll('[data-filter]').forEach(button => button.onclick = () => {
    filter = button.dataset.filter;
    document.querySelectorAll('[data-filter]').forEach(node => { node.classList.toggle('active',node === button); node.setAttribute('aria-pressed',String(node === button)); });
    renderModules(); renderDetail();
  });
  $('#snap').checked = state.snap; $('#snap').onchange = event => { state.snap = event.target.checked; changed(); };
  $('#save').onclick = save; $('#undo').onclick = undo;
  $('#reset-layout').onclick = () => { remember(); state.hud.forEach(item => { const original = hudDefaults.find(h => h.key === item.key); item.x = original.x; item.y = original.y; }); changed(); paintHUD(); renderHUDProperties(); };
  new ResizeObserver(() => { if (view === 'hud') paintHUD(); }).observe($('#hud-canvas'));
  createHUD(); showView('modules');
})();
