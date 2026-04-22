const state = {
  raw: null,
  rows: [],
  sourceName: '/out/metrics.json',
  selectedClass: null,
  uploadedFile: null,
  activeTab: 'oo',
  fpCounts: {
    EI: { low: 0, avg: 0, high: 0 },
    EO: { low: 0, avg: 0, high: 0 },
    EQ: { low: 0, avg: 0, high: 0 },
    ILF: { low: 0, avg: 0, high: 0 },
    EIF: { low: 0, avg: 0, high: 0 },
  },
};

const METRIC_AXES = ['CBO', 'NOO', 'NOC', 'NOA', 'DIT', 'CS'];
const COLORS = ['#5470C6', '#91CC75', '#FAC858', '#EE6666', '#73C0DE', '#3BA272', '#FC8452', '#9A60B4', '#EA7CCC', '#5E78D5', '#8BC34A', '#D96F65'];
const TAB_TITLE = {
  fp: '功能点度量',
  uc: '用例图度量',
  oo: '面向对象度量总览',
  cfg: '控制流图度量',
  loc: '代码行度量',
};

function byId(id) {
  return document.getElementById(id);
}

function setStatus(id, message, isError = false) {
  const el = byId(id);
  if (!el) {
    return;
  }
  el.textContent = message || '';
  el.classList.toggle('error', Boolean(isError));
}

function escapeHtml(text) {
  return String(text ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function num(id) {
  const v = Number(byId(id).value);
  return Number.isFinite(v) ? v : 0;
}

function renderCards(hostId, entries) {
  byId(hostId).innerHTML = entries.map(([k, v]) => `
    <div class="card"><div class="k">${escapeHtml(k)}</div><div class="v">${escapeHtml(v)}</div></div>
  `).join('');
}

function switchTab(tabKey) {
  state.activeTab = tabKey;
  byId('sectionTitle').textContent = TAB_TITLE[tabKey] || '度量';

  document.querySelectorAll('.tab-pane').forEach((el) => el.classList.add('hidden'));
  const activePane = byId(`tab-${tabKey}`);
  if (activePane) {
    activePane.classList.remove('hidden');
  }

  const showOoTable = tabKey === 'oo';
  byId('ooTablePanel').classList.toggle('hidden', !showOoTable);

  byId('menuNav').querySelectorAll('.menu-btn').forEach((b) => {
    b.classList.toggle('active', b.dataset.key === tabKey);
  });

  if (tabKey === 'oo') {
    renderLegend();
    renderRadar();
    renderTable();
  }
  if (tabKey === 'cfg') {
    populateCfgSelectors();
  }
}

async function fetchDefaultMetrics() {
  const response = await fetch('/out/metrics.json', { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`无法读取 /out/metrics.json（HTTP ${response.status}）`);
  }
  return response.json();
}

function deriveRows(metrics) {
  const classes = Array.isArray(metrics.classes) ? metrics.classes : [];
  const methods = Array.isArray(metrics.methods) ? metrics.methods : [];

  const methodsByClass = new Map();
  for (const m of methods) {
    const key = m.className || '';
    methodsByClass.set(key, (methodsByClass.get(key) || 0) + 1);
  }

  return classes.map((c) => {
    const name = c.className || '-';
    const cbo = Number(c.cbo || 0);
    const noo = 0;
    const noa = Number(methodsByClass.get(name) || 0);
    const noc = Number(c.noc || 0);
    const dit = Number(c.dit || 0);
    const cs = Number(c.wmc || 0);
    const risk = cbo * 2 + cs + noc * 3 + dit;

    return { name, cbo, noo, noa, noc, dit, cs, risk };
  });
}

function renderSummary() {
  const data = state.raw || {};
  const loc = data.loc || {};
  const alerts = Array.isArray(data.alerts) ? data.alerts.length : 0;

  byId('summaryBox').innerHTML = [
    `项目：${escapeHtml(data.projectName || '-')}`,
    `文件：${Number(data.fileCount || 0)}，类：${Number(data.classCount || 0)}，方法：${Number(data.methodCount || 0)}`,
    `总行数：${Number(loc.totalLines || 0)}，有效代码行：${Number(loc.codeLines || 0)}`,
    `告警数量：${alerts}`,
    `数据源：${escapeHtml(state.sourceName)}`,
    '映射说明：CS 使用 WMC；NOA 使用类内方法数；NOO 在当前数据模型中缺失，暂记 0。',
  ].map((x) => `<div>${x}</div>`).join('');
}

function getTopRows(limit = 12) {
  return [...state.rows].sort((a, b) => b.risk - a.risk).slice(0, limit);
}

function valueForAxis(row, axis) {
  switch (axis) {
    case 'CBO': return row.cbo;
    case 'NOO': return row.noo;
    case 'NOC': return row.noc;
    case 'NOA': return row.noa;
    case 'DIT': return row.dit;
    case 'CS': return row.cs;
    default: return 0;
  }
}

function polarToCartesian(cx, cy, r, angle) {
  return { x: cx + r * Math.cos(angle), y: cy + r * Math.sin(angle) };
}

function renderLegend() {
  const host = byId('classLegend');
  const items = getTopRows();
  if (!host) {
    return;
  }

  const html = items.map((r, idx) => {
    const dim = state.selectedClass && state.selectedClass !== r.name ? 'dim' : '';
    return `<button class="legend-item ${dim}" data-class="${escapeHtml(r.name)}">
      <span class="legend-dot" style="background:${COLORS[idx % COLORS.length]}"></span>
      ${escapeHtml(r.name.split('.').pop())}
    </button>`;
  }).join('');

  host.innerHTML = html;
  host.querySelectorAll('.legend-item').forEach((btn) => {
    btn.addEventListener('click', () => {
      const cls = btn.dataset.class;
      state.selectedClass = state.selectedClass === cls ? null : cls;
      renderLegend();
      renderTable();
      renderRadar();
    });
  });
}

function renderRadar() {
  const host = byId('radarHost');
  if (!host) {
    return;
  }

  const rows = getTopRows();
  host.innerHTML = '';
  if (!rows.length) {
    host.innerHTML = '<div style="padding:16px;color:#727b8c;">暂无雷达图数据。</div>';
    return;
  }

  const width = host.clientWidth || 860;
  const height = host.clientHeight || 520;
  const cx = width / 2;
  const cy = height / 2 + 10;
  const radius = Math.min(width, height) * 0.33;

  const maxByAxis = {};
  for (const axis of METRIC_AXES) {
    maxByAxis[axis] = Math.max(...rows.map((r) => valueForAxis(r, axis)), 1);
  }

  const svgNS = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(svgNS, 'svg');
  svg.setAttribute('width', String(width));
  svg.setAttribute('height', String(height));

  for (let level = 1; level <= 5; level += 1) {
    const pts = METRIC_AXES.map((_, i) => {
      const angle = -Math.PI / 2 + (2 * Math.PI * i) / METRIC_AXES.length;
      return polarToCartesian(cx, cy, (radius * level) / 5, angle);
    });

    const poly = document.createElementNS(svgNS, 'polygon');
    poly.setAttribute('points', pts.map((p) => `${p.x},${p.y}`).join(' '));
    poly.setAttribute('fill', level % 2 === 0 ? '#f7f9ff' : '#fbfdff');
    poly.setAttribute('stroke', '#dce3ef');
    poly.setAttribute('stroke-width', '1');
    svg.appendChild(poly);
  }

  METRIC_AXES.forEach((axis, i) => {
    const angle = -Math.PI / 2 + (2 * Math.PI * i) / METRIC_AXES.length;
    const p = polarToCartesian(cx, cy, radius, angle);
    const axisLine = document.createElementNS(svgNS, 'line');
    axisLine.setAttribute('x1', String(cx));
    axisLine.setAttribute('y1', String(cy));
    axisLine.setAttribute('x2', String(p.x));
    axisLine.setAttribute('y2', String(p.y));
    axisLine.setAttribute('stroke', '#d5ddeb');
    axisLine.setAttribute('stroke-width', '1');
    svg.appendChild(axisLine);

    const lp = polarToCartesian(cx, cy, radius + 18, angle);
    const text = document.createElementNS(svgNS, 'text');
    text.setAttribute('x', String(lp.x));
    text.setAttribute('y', String(lp.y));
    text.setAttribute('text-anchor', 'middle');
    text.setAttribute('font-size', '24');
    text.setAttribute('fill', '#2f3642');
    text.textContent = axis;
    svg.appendChild(text);
  });

  rows.forEach((row, idx) => {
    const color = COLORS[idx % COLORS.length];
    const isDim = state.selectedClass && state.selectedClass !== row.name;

    const pts = METRIC_AXES.map((axis, i) => {
      const angle = -Math.PI / 2 + (2 * Math.PI * i) / METRIC_AXES.length;
      const ratio = valueForAxis(row, axis) / maxByAxis[axis];
      return polarToCartesian(cx, cy, radius * ratio, angle);
    });

    const poly = document.createElementNS(svgNS, 'polygon');
    poly.setAttribute('points', pts.map((p) => `${p.x},${p.y}`).join(' '));
    poly.setAttribute('fill', color);
    poly.setAttribute('fill-opacity', isDim ? '0.03' : '0.08');
    poly.setAttribute('stroke', color);
    poly.setAttribute('stroke-width', state.selectedClass === row.name ? '3' : '2');
    poly.setAttribute('stroke-opacity', isDim ? '0.2' : '0.9');
    svg.appendChild(poly);

    pts.forEach((p) => {
      const dot = document.createElementNS(svgNS, 'circle');
      dot.setAttribute('cx', String(p.x));
      dot.setAttribute('cy', String(p.y));
      dot.setAttribute('r', state.selectedClass === row.name ? '4.2' : '3.2');
      dot.setAttribute('fill', color);
      dot.setAttribute('fill-opacity', isDim ? '0.25' : '0.95');
      svg.appendChild(dot);
    });
  });

  host.appendChild(svg);
}

function renderTable() {
  const rows = [...state.rows].sort((a, b) => b.risk - a.risk);
  if (!rows.length) {
    byId('tableWrap').innerHTML = '<div style="padding:16px;color:#727b8c;">暂无类指标数据。</div>';
    return;
  }

  const body = rows.map((r) => {
    const active = state.selectedClass === r.name ? 'row-active' : '';
    return `<tr class="${active}">
      <td><a class="class-link" data-class="${escapeHtml(r.name)}">${escapeHtml(r.name)}</a></td>
      <td>${r.cbo}</td>
      <td>${r.cs}</td>
      <td>${r.noo}</td>
      <td>${r.noa}</td>
      <td>${r.noc}</td>
      <td>${r.dit}</td>
    </tr>`;
  }).join('');

  byId('tableWrap').innerHTML = `
    <table>
      <thead>
        <tr>
          <th>类名</th>
          <th>对象间的耦合度<br>CBO</th>
          <th>类规模度量<br>CS</th>
          <th>方法重写数<br>NOO</th>
          <th>增加方法数目<br>NOA</th>
          <th>子类数目<br>NOC</th>
          <th>继承树深度<br>DIT</th>
        </tr>
      </thead>
      <tbody>${body}</tbody>
    </table>
  `;

  byId('tableWrap').querySelectorAll('.class-link').forEach((a) => {
    a.addEventListener('click', (e) => {
      e.preventDefault();
      state.selectedClass = a.dataset.class;
      renderTable();
      renderRadar();
      renderLegend();
    });
  });
}

function calcFunctionPoint() {
  const vaf = num('fp-vaf');
  const c = state.fpCounts;
  const ufp =
    c.EI.low * 3 + c.EI.avg * 4 + c.EI.high * 6
    + c.EO.low * 4 + c.EO.avg * 5 + c.EO.high * 7
    + c.EQ.low * 3 + c.EQ.avg * 4 + c.EQ.high * 6
    + c.ILF.low * 7 + c.ILF.avg * 10 + c.ILF.high * 15
    + c.EIF.low * 5 + c.EIF.avg * 7 + c.EIF.high * 10;
  const fp = +(ufp * vaf).toFixed(2);

  renderCards('fp-cards', [
    ['UFP', ufp],
    ['VAF', vaf.toFixed(2)],
    ['调整后 FP', fp],
    ['EI(简/中/复)', `${c.EI.low}/${c.EI.avg}/${c.EI.high}`],
    ['EO(简/中/复)', `${c.EO.low}/${c.EO.avg}/${c.EO.high}`],
    ['EQ(简/中/复)', `${c.EQ.low}/${c.EQ.avg}/${c.EQ.high}`],
    ['ILF(简/中/复)', `${c.ILF.low}/${c.ILF.avg}/${c.ILF.high}`],
    ['EIF(简/中/复)', `${c.EIF.low}/${c.EIF.avg}/${c.EIF.high}`],
  ]);
  setStatus('fp-status', '已按教学口径计算：事务功能由 FTR/DER 判级，数据功能由 RET/DET 判级；UFP 为分档加权和，AFP = UFP * VAF。');
}

function txComplexity(type, ftr, der) {
  if (type === 'EI') {
    if (ftr <= 1) {
      if (der <= 4) return 'low';
      if (der <= 15) return 'low';
      return 'avg';
    }
    if (ftr === 2) {
      if (der <= 4) return 'low';
      if (der <= 15) return 'avg';
      return 'high';
    }
    if (der <= 4) return 'avg';
    if (der <= 15) return 'high';
    return 'high';
  }

  // EO / EQ: 常见教学口径按 IFPUG 复杂度矩阵
  if (ftr <= 1) {
    if (der <= 5) return 'low';
    if (der <= 19) return 'low';
    return 'avg';
  }
  if (ftr <= 3) {
    if (der <= 5) return 'low';
    if (der <= 19) return 'avg';
    return 'high';
  }
  if (der <= 5) return 'avg';
  if (der <= 19) return 'high';
  return 'high';
}

function dataComplexity(type, ret, det) {
  const lowDet = det <= 19;
  const midDet = det >= 20 && det <= 50;
  if (type === 'ILF') {
    if (ret === 1) {
      if (lowDet) return 'low';
      if (midDet) return 'low';
      return 'avg';
    }
    if (ret <= 5) {
      if (lowDet) return 'low';
      if (midDet) return 'avg';
      return 'high';
    }
    if (lowDet) return 'avg';
    if (midDet) return 'high';
    return 'high';
  }

  // EIF
  if (ret === 1) {
    if (lowDet) return 'low';
    if (midDet) return 'low';
    return 'avg';
  }
  if (ret <= 5) {
    if (lowDet) return 'low';
    if (midDet) return 'avg';
    return 'high';
  }
  if (lowDet) return 'avg';
  if (midDet) return 'high';
  return 'high';
}

function addTransactionFunction() {
  const type = byId('fp-tx-type').value;
  const ftr = Math.max(0, Math.floor(num('fp-tx-ftr')));
  const der = Math.max(0, Math.floor(num('fp-tx-der')));
  const level = txComplexity(type, ftr, der);
  state.fpCounts[type][level] += 1;
  setStatus('fp-status', `已添加事务功能：${type}，FTR=${ftr}，DER=${der}，复杂度=${level === 'low' ? '简单' : level === 'avg' ? '一般' : '复杂'}。`);
  calcFunctionPoint();
}

function addDataFunction() {
  const type = byId('fp-data-type').value;
  const ret = Math.max(0, Math.floor(num('fp-data-ret')));
  const det = Math.max(0, Math.floor(num('fp-data-det')));
  const level = dataComplexity(type, ret, det);
  state.fpCounts[type][level] += 1;
  setStatus('fp-status', `已添加数据功能：${type}，RET=${ret}，DET=${det}，复杂度=${level === 'low' ? '简单' : level === 'avg' ? '一般' : '复杂'}。`);
  calcFunctionPoint();
}

function resetFunctionPointCounts() {
  state.fpCounts = {
    EI: { low: 0, avg: 0, high: 0 },
    EO: { low: 0, avg: 0, high: 0 },
    EQ: { low: 0, avg: 0, high: 0 },
    ILF: { low: 0, avg: 0, high: 0 },
    EIF: { low: 0, avg: 0, high: 0 },
  };
  setStatus('fp-status', '已重置功能点计数。');
  calcFunctionPoint();
}

function inferTxType(methodName) {
  const n = String(methodName || '').toLowerCase();
  if (/^(add|create|insert|save|update|delete|remove|set|register)/.test(n)) {
    return 'EI';
  }
  if (/^(get|find|query|list|search|count|check|load)/.test(n)) {
    return 'EQ';
  }
  return 'EO';
}

function inferDataType(className) {
  const n = String(className || '').toLowerCase();
  if (/(client|proxy|external|remote|adapter|gateway)/.test(n)) {
    return 'EIF';
  }
  return 'ILF';
}

function autoEstimateFunctionPoint() {
  if (!state.raw) {
    setStatus('fp-status', '没有可用 metrics 数据，请先加载 /out/metrics.json 或上传 JSON。', true);
    return;
  }

  state.fpCounts = {
    EI: { low: 0, avg: 0, high: 0 },
    EO: { low: 0, avg: 0, high: 0 },
    EQ: { low: 0, avg: 0, high: 0 },
    ILF: { low: 0, avg: 0, high: 0 },
    EIF: { low: 0, avg: 0, high: 0 },
  };

  const methods = Array.isArray(state.raw.methods) ? state.raw.methods : [];
  const classes = Array.isArray(state.raw.classes) ? state.raw.classes : [];
  const classByName = new Map(classes.map((c) => [c.className, c]));

  let txCount = 0;
  let dataCount = 0;

  for (const m of methods) {
    const owner = classByName.get(m.className) || {};
    const type = inferTxType(m.methodName);
    const ftr = Math.max(0, Number(owner.cbo || 0));
    const der = Math.max(1, Math.round(Number(m.loc || 0) / 3 + Number(m.cyclomaticComplexity || 1)));
    const level = txComplexity(type, ftr, der);
    state.fpCounts[type][level] += 1;
    txCount += 1;
  }

  for (const c of classes) {
    const type = inferDataType(c.className);
    const ret = Math.max(1, Number(c.noc || 0) + 1);
    const det = Math.max(1, Number(c.wmc || 0) * 2 + Number(c.cbo || 0));
    const level = dataComplexity(type, ret, det);
    state.fpCounts[type][level] += 1;
    dataCount += 1;
  }

  calcFunctionPoint();
  setStatus('fp-status', `已自动估算：事务功能 ${txCount} 个，数据功能 ${dataCount} 个。说明：该结果为启发式推断，请按实际业务复核。`);
}

function calcUseCasePoint() {
  const as = num('uc-as');
  const aa = num('uc-aa');
  const ac = num('uc-ac');
  const us = num('uc-us');
  const ua = num('uc-ua');
  const uc = num('uc-uc');
  const tFactor = num('uc-tfactor');
  const eFactor = num('uc-efactor');

  const uaw = as * 1 + aa * 2 + ac * 3;
  const uuc = us * 5 + ua * 10 + uc * 15;
  const uucp = uaw + uuc;
  const tcf = +(0.6 + (0.01 * tFactor)).toFixed(2);
  const ef = +(1.4 + (-0.03 * eFactor)).toFixed(2);
  const upc = +(uucp * tcf * ef).toFixed(2);

  renderCards('uc-cards', [
    ['UAW', uaw],
    ['UUC', uuc],
    ['UUCP', uucp],
    ['TFactor', tFactor.toFixed(0)],
    ['EFactor', eFactor.toFixed(0)],
    ['TCF', tcf.toFixed(2)],
    ['EF', ef.toFixed(2)],
    ['UPC', upc],
  ]);
  setStatus('uc-status', '公式：UUCP = UAW + UUC；TCF = 0.6 + 0.01*TFactor；EF = 1.4 - 0.03*EFactor；UPC = UUCP * TCF * EF');
}

function setUcInput(id, value) {
  const el = byId(id);
  if (el) {
    el.value = String(Math.max(0, Math.round(value)));
  }
}

function autoEstimateUseCasePoint() {
  if (!state.raw) {
    setStatus('uc-status', '没有可用 metrics 数据，请先加载 /out/metrics.json 或上传 JSON。', true);
    return;
  }

  const classes = Array.isArray(state.raw.classes) ? state.raw.classes : [];
  const methods = Array.isArray(state.raw.methods) ? state.raw.methods : [];
  const alerts = Array.isArray(state.raw.alerts) ? state.raw.alerts : [];

  let as = 0;
  let aa = 0;
  let ac = 0;
  for (const c of classes) {
    const name = String(c.className || '').toLowerCase();
    const score = Number(c.cbo || 0) + Number(c.wmc || 0);
    if (/(controller|api|gateway|facade)/.test(name) || score >= 12) {
      ac += 1;
    } else if (/(service|manager|handler)/.test(name) || score >= 6) {
      aa += 1;
    } else {
      as += 1;
    }
  }

  let us = 0;
  let ua = 0;
  let uc = 0;
  for (const m of methods) {
    const mName = String(m.methodName || '').toLowerCase();
    const complexity = Number(m.cyclomaticComplexity || 1);
    const loc = Number(m.loc || 0);
    if (complexity >= 8 || loc >= 30 || /(process|workflow|orchestrate)/.test(mName)) {
      uc += 1;
    } else if (complexity >= 4 || loc >= 12 || /(update|create|delete|submit|handle)/.test(mName)) {
      ua += 1;
    } else {
      us += 1;
    }
  }

  const avgCbo = classes.length ? classes.reduce((s, c) => s + Number(c.cbo || 0), 0) / classes.length : 0;
  const avgComplexity = methods.length ? methods.reduce((s, m) => s + Number(m.cyclomaticComplexity || 0), 0) / methods.length : 0;
  const avgDit = classes.length ? classes.reduce((s, c) => s + Number(c.dit || 0), 0) / classes.length : 0;
  const tFactor = Math.max(0, Math.min(50, Math.round(avgCbo * 4 + avgComplexity * 1.5 + alerts.length * 2)));
  const eFactor = Math.max(0, Math.min(50, Math.round(8 + avgDit * 3 + Math.max(0, avgComplexity - 2) * 1.2)));

  setUcInput('uc-as', as);
  setUcInput('uc-aa', aa);
  setUcInput('uc-ac', ac);
  setUcInput('uc-us', us);
  setUcInput('uc-ua', ua);
  setUcInput('uc-uc', uc);
  setUcInput('uc-tfactor', tFactor);
  setUcInput('uc-efactor', eFactor);

  calcUseCasePoint();
  setStatus('uc-status', `已自动估算：参与者(简/中/复)=${as}/${aa}/${ac}，用例(简/中/复)=${us}/${ua}/${uc}。TFactor=${tFactor}，EFactor=${eFactor}（启发式，建议人工复核）。`);
}

function calcCfgComplexity() {
  const code = byId('cfg-code').value || '';
  const count = (pattern) => (code.match(pattern) || []).length;

  const ifCount = count(/\bif\b/g);
  const forCount = count(/\bfor\b/g);
  const whileCount = count(/\bwhile\b/g);
  const caseCount = count(/\bcase\b/g);
  const catchCount = count(/\bcatch\b/g);
  const andCount = count(/&&/g);
  const orCount = count(/\|\|/g);
  const ternaryCount = count(/\?/g);

  const complexity = 1 + ifCount + forCount + whileCount + caseCount + catchCount + andCount + orCount + ternaryCount;

  renderCards('cfg-cards', [
    ['圈复杂度 CC', complexity],
    ['决策点总数', complexity - 1],
    ['if/for/while/case/catch', ifCount + forCount + whileCount + caseCount + catchCount],
  ]);
  setStatus('cfg-status', `统计明细：if=${ifCount}, for=${forCount}, while=${whileCount}, case=${caseCount}, catch=${catchCount}, &&=${andCount}, ||=${orCount}, ?=${ternaryCount}`);
}

function populateCfgSelectors() {
  const classSelect = byId('cfg-class-select');
  const methodSelect = byId('cfg-method-select');
  if (!classSelect || !methodSelect) {
    return;
  }

  const methods = Array.isArray(state.raw?.methods) ? state.raw.methods : [];
  const classNames = [...new Set(methods.map((m) => m.className).filter(Boolean))].sort();
  classSelect.innerHTML = '';
  const defaultClassOption = document.createElement('option');
  defaultClassOption.value = '';
  defaultClassOption.textContent = '请选择类';
  classSelect.appendChild(defaultClassOption);
  for (const name of classNames) {
    const option = document.createElement('option');
    option.value = String(name);
    option.textContent = String(name);
    classSelect.appendChild(option);
  }

  methodSelect.innerHTML = '';
  const defaultMethodOption = document.createElement('option');
  defaultMethodOption.value = '';
  defaultMethodOption.textContent = '请选择方法';
  methodSelect.appendChild(defaultMethodOption);
}

function onCfgClassChange() {
  const className = byId('cfg-class-select').value;
  const methodSelect = byId('cfg-method-select');
  if (!methodSelect) {
    return;
  }
  const methods = Array.isArray(state.raw?.methods) ? state.raw.methods : [];
  const filtered = methods
    .filter((m) => m.className === className)
    .sort((a, b) => String(a.methodName).localeCompare(String(b.methodName)));

  methodSelect.innerHTML = '';
  const defaultOption = document.createElement('option');
  defaultOption.value = '';
  defaultOption.textContent = '请选择方法';
  methodSelect.appendChild(defaultOption);
  filtered.forEach((m, idx) => {
    const option = document.createElement('option');
    option.value = String(idx);
    option.textContent = `${m.methodName} (CC=${Number(m.cyclomaticComplexity || 0)}, LOC=${Number(m.loc || 0)})`;
    methodSelect.appendChild(option);
  });
}

function calcCfgFromMetrics() {
  if (!state.raw || !Array.isArray(state.raw.methods)) {
    setStatus('cfg-status', '没有可用 metrics 数据，请先加载 /out/metrics.json 或上传 JSON。', true);
    return;
  }
  const className = byId('cfg-class-select').value;
  const methodIndex = byId('cfg-method-select').value;
  if (!className) {
    setStatus('cfg-status', '请先选择类。', true);
    return;
  }
  if (methodIndex === '') {
    setStatus('cfg-status', '请先选择方法。', true);
    return;
  }

  const methods = state.raw.methods
    .filter((m) => m.className === className)
    .sort((a, b) => String(a.methodName).localeCompare(String(b.methodName)));
  const method = methods[Number(methodIndex)];
  if (!method) {
    setStatus('cfg-status', '未找到方法，请重新选择。', true);
    return;
  }

  const complexity = Number(method.cyclomaticComplexity || 1);
  const loc = Number(method.loc || 0);
  renderCards('cfg-cards', [
    ['圈复杂度 CC', complexity],
    ['决策点总数', Math.max(0, complexity - 1)],
    ['方法 LOC', loc],
  ]);
  setStatus('cfg-status', `已自动读取：${className}#${method.methodName}，CC=${complexity}，LOC=${loc}（数据来源 metrics.json）。`);
}

function countLocFromText(text) {
  const lines = text.split(/\r?\n/);
  let blank = 0;
  let comment = 0;
  let code = 0;
  let inBlock = false;

  for (const raw of lines) {
    const line = raw.trim();
    if (!line) {
      blank += 1;
      continue;
    }

    if (inBlock) {
      comment += 1;
      if (line.includes('*/')) {
        inBlock = false;
      }
      continue;
    }

    if (line.startsWith('//')) {
      comment += 1;
      continue;
    }

    if (line.startsWith('/*')) {
      comment += 1;
      if (!line.includes('*/')) {
        inBlock = true;
      }
      continue;
    }

    if (line.includes('/*')) {
      code += 1;
      if (!line.includes('*/')) {
        inBlock = true;
      }
      continue;
    }

    code += 1;
  }

  return { total: lines.length, blank, comment, code };
}

function calcLocFromInput() {
  const result = countLocFromText(byId('loc-code').value || '');
  renderCards('loc-cards', [
    ['总行数', result.total],
    ['空行', result.blank],
    ['注释行', result.comment],
    ['有效代码行', result.code],
  ]);
  setStatus('loc-status', '已基于输入代码完成 LoC 统计。');
}

function useProjectLoc() {
  if (!state.raw || !state.raw.loc) {
    setStatus('loc-status', '当前没有项目级 LoC 数据，请先上传 metrics.json 或使用默认 /out/metrics.json。', true);
    return;
  }
  const loc = state.raw.loc;
  renderCards('loc-cards', [
    ['总行数', Number(loc.totalLines || 0)],
    ['空行', Number(loc.blankLines || 0)],
    ['注释行', Number(loc.commentLines || 0)],
    ['有效代码行', Number(loc.codeLines || 0)],
  ]);
  setStatus('loc-status', `已读取项目级 LoC 数据（来源：${state.sourceName}）。`);
}

function toXml(rows) {
  const lines = ['<?xml version="1.0" encoding="UTF-8"?>', '<classMetrics>'];
  for (const r of rows) {
    lines.push(`  <class name="${r.name}">`);
    lines.push(`    <CBO>${r.cbo}</CBO>`);
    lines.push(`    <CS>${r.cs}</CS>`);
    lines.push(`    <NOO>${r.noo}</NOO>`);
    lines.push(`    <NOA>${r.noa}</NOA>`);
    lines.push(`    <NOC>${r.noc}</NOC>`);
    lines.push(`    <DIT>${r.dit}</DIT>`);
    lines.push('  </class>');
  }
  lines.push('</classMetrics>');
  return lines.join('\n');
}

function download(name, content, type) {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

function applyMetrics(data, sourceName) {
  state.raw = data;
  state.rows = deriveRows(data);
  state.sourceName = sourceName;
  if (state.selectedClass && !state.rows.some((r) => r.name === state.selectedClass)) {
    state.selectedClass = null;
  }

  renderSummary();
  populateCfgSelectors();
  renderLegend();
  renderRadar();
  renderTable();

  const alertCount = Array.isArray(data.alerts) ? data.alerts.length : 0;
  setStatus('chartStatus', `已加载 ${state.rows.length} 个类，当前告警 ${alertCount} 条。`);
  setStatus('rightStatus', '数据已更新，可点击类名或图例查看单类高亮。');
}

function bindEvents() {
  byId('menuNav').querySelectorAll('.menu-btn').forEach((btn) => {
    btn.addEventListener('click', () => switchTab(btn.dataset.key));
  });

  byId('uploadBtn').addEventListener('click', () => byId('fileInput').click());

  byId('fileInput').addEventListener('change', async (event) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }

    state.uploadedFile = file;
    byId('fileName').textContent = `已选择：${file.name}`;

    if (!file.name.toLowerCase().endsWith('.json')) {
      setStatus('rightStatus', '已记录文件名。当前仅支持解析 JSON；.oom 请先转换为 metrics.json。', true);
      return;
    }

    try {
      const text = await file.text();
      const json = JSON.parse(text);
      applyMetrics(json, file.name);
      setStatus('rightStatus', `已成功解析 ${file.name}`);
    } catch (err) {
      setStatus('rightStatus', `文件解析失败：${err.message}`, true);
    }
  });

  byId('startBtn').addEventListener('click', async () => {
    if (state.raw) {
      renderRadar();
      renderTable();
      setStatus('rightStatus', '分析已刷新。');
      return;
    }

    try {
      const data = await fetchDefaultMetrics();
      applyMetrics(data, '/out/metrics.json');
      setStatus('rightStatus', '已从默认路径读取并分析。');
    } catch (err) {
      setStatus('rightStatus', `分析失败：${err.message}`, true);
    }
  });

  byId('exportXmlBtn').addEventListener('click', () => {
    if (!state.rows.length) {
      setStatus('rightStatus', '没有可导出的类数据。', true);
      return;
    }
    download('class-metrics.xml', toXml(state.rows), 'application/xml;charset=utf-8');
    setStatus('rightStatus', '已导出 class-metrics.xml');
  });

  byId('fp-calc').addEventListener('click', calcFunctionPoint);
  byId('fp-auto').addEventListener('click', autoEstimateFunctionPoint);
  byId('fp-add-tx').addEventListener('click', addTransactionFunction);
  byId('fp-add-data').addEventListener('click', addDataFunction);
  byId('fp-reset').addEventListener('click', resetFunctionPointCounts);
  byId('uc-calc').addEventListener('click', calcUseCasePoint);
  byId('uc-auto').addEventListener('click', autoEstimateUseCasePoint);
  byId('cfg-calc').addEventListener('click', calcCfgComplexity);
  byId('cfg-auto').addEventListener('click', calcCfgFromMetrics);
  byId('cfg-class-select').addEventListener('change', onCfgClassChange);
  byId('loc-calc').addEventListener('click', calcLocFromInput);
  byId('loc-use-project').addEventListener('click', useProjectLoc);

  window.addEventListener('resize', () => {
    if (state.activeTab === 'oo' && state.rows.length) {
      renderRadar();
    }
  });
}

async function init() {
  bindEvents();

  calcFunctionPoint();
  calcUseCasePoint();
  calcCfgComplexity();
  calcLocFromInput();

  try {
    const data = await fetchDefaultMetrics();
    applyMetrics(data, '/out/metrics.json');
    byId('fileName').textContent = '默认加载成功：/out/metrics.json';
  } catch (err) {
    setStatus('chartStatus', `默认数据未加载：${err.message}`, true);
    setStatus('rightStatus', '请先运行 analyze 生成 out/metrics.json，或点击“点击上传”导入 JSON。', true);
    renderSummary();
    renderLegend();
    renderRadar();
    renderTable();
  }

  switchTab('oo');
}

init();
