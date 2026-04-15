const state = {
  raw: null,
  rows: [],
  sourceName: '/out/metrics.json',
  selectedClass: null,
  uploadedFile: null,
  activeTab: 'oo',
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
  const ei = num('fp-ei');
  const eo = num('fp-eo');
  const eq = num('fp-eq');
  const ilf = num('fp-ilf');
  const eif = num('fp-eif');
  const vaf = num('fp-vaf');

  const ufp = ei * 4 + eo * 5 + eq * 4 + ilf * 10 + eif * 7;
  const fp = +(ufp * vaf).toFixed(2);

  renderCards('fp-cards', [
    ['UFP', ufp],
    ['VAF', vaf.toFixed(2)],
    ['调整后 FP', fp],
  ]);
  setStatus('fp-status', '公式：UFP = 4*EI + 5*EO + 4*EQ + 10*ILF + 7*EIF；FP = UFP * VAF');
}

function calcUseCasePoint() {
  const as = num('uc-as');
  const aa = num('uc-aa');
  const ac = num('uc-ac');
  const us = num('uc-us');
  const ua = num('uc-ua');
  const uc = num('uc-uc');
  const tcf = num('uc-tcf');
  const ecf = num('uc-ecf');

  const uaw = as * 1 + aa * 2 + ac * 3;
  const uucw = us * 5 + ua * 10 + uc * 15;
  const uucp = uaw + uucw;
  const ucp = +(uucp * tcf * ecf).toFixed(2);

  renderCards('uc-cards', [
    ['UAW', uaw],
    ['UUCW', uucw],
    ['UUCP', uucp],
    ['TCF', tcf.toFixed(2)],
    ['ECF', ecf.toFixed(2)],
    ['UCP', ucp],
  ]);
  setStatus('uc-status', '公式：UUCP = UAW + UUCW；UCP = UUCP * TCF * ECF');
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
  byId('uc-calc').addEventListener('click', calcUseCasePoint);
  byId('cfg-calc').addEventListener('click', calcCfgComplexity);
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
