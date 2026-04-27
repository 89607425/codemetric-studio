const state = {
  sourceFiles: [],
  response: null,
  selectedClass: null,
};

const COLORS = ['#5470C6', '#91CC75', '#FAC858', '#EE6666', '#73C0DE', '#3BA272', '#FC8452', '#9A60B4', '#EA7CCC', '#5E78D5'];
const AXES = ['CBO', 'NOO', 'NOC', 'NOA', 'DIT', 'CS'];

function $(id) {
  return document.getElementById(id);
}

function setStatus(id, text, type = '') {
  const el = $(id);
  el.textContent = text;
  el.className = `status ${type}`.trim();
}

function toNumber(id, fallback = 0) {
  const v = Number($(id).value);
  return Number.isFinite(v) ? v : fallback;
}

function cards(hostId, list) {
  $(hostId).innerHTML = list.map(([k, v]) => `
    <div class="card"><div class="k">${k}</div><div class="v">${v}</div></div>
  `).join('');
}

function escapeHtml(text) {
  return String(text ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function normalizeRows(metrics) {
  const methodsByClass = new Map();
  for (const m of (metrics.methods || [])) {
    methodsByClass.set(m.className, (methodsByClass.get(m.className) || 0) + 1);
  }

  return (metrics.classes || []).map((c) => ({
    className: c.className,
    cbo: c.cbo || 0,
    noo: c.noo || 0,
    noc: c.noc || 0,
    noa: methodsByClass.get(c.className) || 0,
    dit: c.dit || 0,
    cs: c.wmc || 0,
    risk: (c.wmc || 0) + (c.cbo || 0) * 2 + (c.noc || 0) * 3,
  })).sort((a, b) => b.risk - a.risk);
}

function valueOf(row, axis) {
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

function polar(cx, cy, r, angle) {
  return { x: cx + r * Math.cos(angle), y: cy + r * Math.sin(angle) };
}

function renderLegend(rows) {
  $('classLegend').innerHTML = rows.slice(0, 10).map((row, i) => {
    const dim = state.selectedClass && state.selectedClass !== row.className ? 'dim' : '';
    return `<button class="legend-item ${dim}" data-class="${escapeHtml(row.className)}">
      <span class="dot" style="background:${COLORS[i % COLORS.length]}"></span>
      ${escapeHtml(row.className.split('.').pop())}
    </button>`;
  }).join('');

  $('classLegend').querySelectorAll('.legend-item').forEach((btn) => {
    btn.addEventListener('click', () => {
      const cls = btn.dataset.class;
      state.selectedClass = state.selectedClass === cls ? null : cls;
      renderVisualization();
    });
  });
}

function renderRadar(rows) {
  const host = $('radarHost');
  host.innerHTML = '';
  const topRows = rows.slice(0, 10);
  if (!topRows.length) {
    host.innerHTML = '<div style="padding:16px;color:#7e8aa0;">暂无雷达图数据</div>';
    return;
  }

  const width = host.clientWidth || 640;
  const height = host.clientHeight || 420;
  const cx = width / 2;
  const cy = height / 2 + 8;
  const radius = Math.min(width, height) * 0.33;

  const maxByAxis = {};
  for (const axis of AXES) {
    maxByAxis[axis] = Math.max(...topRows.map((row) => valueOf(row, axis)), 1);
  }

  const svgNS = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(svgNS, 'svg');
  svg.setAttribute('width', String(width));
  svg.setAttribute('height', String(height));

  for (let level = 1; level <= 5; level++) {
    const points = AXES.map((_, i) => {
      const angle = -Math.PI / 2 + (2 * Math.PI * i) / AXES.length;
      return polar(cx, cy, radius * level / 5, angle);
    });
    const poly = document.createElementNS(svgNS, 'polygon');
    poly.setAttribute('points', points.map((p) => `${p.x},${p.y}`).join(' '));
    poly.setAttribute('fill', level % 2 === 0 ? '#f7f9ff' : '#fbfdff');
    poly.setAttribute('stroke', '#dce3ef');
    poly.setAttribute('stroke-width', '1');
    svg.appendChild(poly);
  }

  AXES.forEach((axis, i) => {
    const angle = -Math.PI / 2 + (2 * Math.PI * i) / AXES.length;
    const p = polar(cx, cy, radius, angle);

    const line = document.createElementNS(svgNS, 'line');
    line.setAttribute('x1', String(cx));
    line.setAttribute('y1', String(cy));
    line.setAttribute('x2', String(p.x));
    line.setAttribute('y2', String(p.y));
    line.setAttribute('stroke', '#d5ddeb');
    svg.appendChild(line);

    const labelPoint = polar(cx, cy, radius + 16, angle);
    const text = document.createElementNS(svgNS, 'text');
    text.setAttribute('x', String(labelPoint.x));
    text.setAttribute('y', String(labelPoint.y));
    text.setAttribute('text-anchor', 'middle');
    text.setAttribute('font-size', '17');
    text.setAttribute('fill', '#2f3642');
    text.textContent = axis;
    svg.appendChild(text);
  });

  topRows.forEach((row, i) => {
    const color = COLORS[i % COLORS.length];
    const isDim = state.selectedClass && state.selectedClass !== row.className;

    const points = AXES.map((axis, idx) => {
      const ratio = valueOf(row, axis) / maxByAxis[axis];
      const angle = -Math.PI / 2 + (2 * Math.PI * idx) / AXES.length;
      return polar(cx, cy, radius * ratio, angle);
    });

    const poly = document.createElementNS(svgNS, 'polygon');
    poly.setAttribute('points', points.map((p) => `${p.x},${p.y}`).join(' '));
    poly.setAttribute('fill', color);
    poly.setAttribute('fill-opacity', isDim ? '0.03' : '0.1');
    poly.setAttribute('stroke', color);
    poly.setAttribute('stroke-opacity', isDim ? '0.2' : '0.85');
    poly.setAttribute('stroke-width', state.selectedClass === row.className ? '3' : '2');
    svg.appendChild(poly);
  });

  host.appendChild(svg);
}

function renderTable(rows) {
  if (!rows.length) {
    $('classTableWrap').innerHTML = '<div style="padding:16px;color:#7e8aa0;">暂无类级数据</div>';
    return;
  }

  const body = rows.map((row) => {
    const active = state.selectedClass === row.className ? 'row-active' : '';
    return `<tr class="${active}">
      <td><a class="class-link" data-class="${escapeHtml(row.className)}">${escapeHtml(row.className)}</a></td>
      <td>${row.cbo}</td>
      <td>${row.cs}</td>
      <td>${row.noo}</td>
      <td>${row.noa}</td>
      <td>${row.noc}</td>
      <td>${row.dit}</td>
    </tr>`;
  }).join('');

  $('classTableWrap').innerHTML = `
    <table>
      <thead>
        <tr>
          <th>类名</th><th>CBO</th><th>CS(WMC)</th><th>NOO</th><th>NOA</th><th>NOC</th><th>DIT</th>
        </tr>
      </thead>
      <tbody>${body}</tbody>
    </table>
  `;

  $('classTableWrap').querySelectorAll('.class-link').forEach((link) => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      state.selectedClass = link.dataset.class;
      renderVisualization();
    });
  });
}

function renderVisualization() {
  const metrics = state.response?.projectMetrics;
  if (!metrics) {
    return;
  }
  const rows = normalizeRows(metrics);
  renderLegend(rows);
  renderRadar(rows);
  renderTable(rows);
}

function renderSummary(response) {
  const m = response.projectMetrics;
  const d = response.designMetrics;

  cards('summaryCards', [
    ['项目', m.projectName || '-'],
    ['源码文件数', m.fileCount || 0],
    ['类数', m.classCount || 0],
    ['方法数', m.methodCount || 0],
    ['总行数', m.loc?.totalLines || 0],
    ['有效代码行', m.loc?.codeLines || 0],
    ['告警数', (m.alerts || []).length],
    ['耗时(ms)', response.elapsedMs || 0],
  ]);

  cards('designCards', [
    ['UFP', d?.ufp ?? 0],
    ['功能点FP', d?.functionPoint ?? 0],
    ['UAW', d?.uaw ?? 0],
    ['UUCW', d?.uucw ?? 0],
    ['UUCP', d?.uucp ?? 0],
    ['用例点UCP', d?.useCasePoint ?? 0],
    ['CFG复杂度', d?.cfgCyclomaticComplexity ?? 0],
    ['源码输入', `${state.sourceFiles.length} 文件`],
  ]);
}

async function fileToPayload(file) {
  const content = await file.text();
  const path = file.webkitRelativePath || file.name;
  return { path, content };
}

async function collectSourceFiles() {
  const files = state.sourceFiles || [];
  const javaFiles = files.filter((f) => (f.name || '').endsWith('.java'));
  const payload = [];
  for (const f of javaFiles) {
    payload.push(await fileToPayload(f));
  }
  return payload;
}

function buildRequestPayload(sourceFiles) {
  return {
    projectName: $('projectName').value.trim() || 'uploaded-project',
    module: $('module').value.trim() || null,
    sourceFiles,
    threshold: {
      methodComplexity: toNumber('thComplexity', 10),
      classWmc: toNumber('thWmc', 50),
      classCbo: toNumber('thCbo', 14),
    },
    classDiagramText: $('classDiagramText').value,
    flowDiagramText: $('flowDiagramText').value,
    cfgText: $('flowDiagramText').value,
    functionPointInput: {
      ei: toNumber('fpEi', 0),
      eo: toNumber('fpEo', 0),
      eq: toNumber('fpEq', 0),
      ilf: toNumber('fpIlf', 0),
      eif: toNumber('fpEif', 0),
      vaf: toNumber('fpVaf', 1),
    },
    useCaseInput: {
      actorSimple: toNumber('ucAs', 0),
      actorAverage: toNumber('ucAa', 0),
      actorComplex: toNumber('ucAc', 0),
      ucSimple: toNumber('ucUs', 0),
      ucAverage: toNumber('ucUa', 0),
      ucComplex: toNumber('ucUc', 0),
      tcf: toNumber('ucTcf', 1),
      ecf: toNumber('ucEcf', 1),
    },
  };
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

function bindPickers() {
  $('pickDirBtn').addEventListener('click', () => $('sourceDirInput').click());
  $('pickJavaBtn').addEventListener('click', () => $('sourceFileInput').click());

  $('sourceDirInput').addEventListener('change', (e) => {
    state.sourceFiles = Array.from(e.target.files || []);
    const javaCount = state.sourceFiles.filter((f) => f.name.endsWith('.java')).length;
    setStatus('sourceStatus', `已选择目录文件 ${state.sourceFiles.length} 个，其中 .java ${javaCount} 个。`, javaCount ? 'ok' : 'warn');
  });

  $('sourceFileInput').addEventListener('change', (e) => {
    state.sourceFiles = Array.from(e.target.files || []);
    const javaCount = state.sourceFiles.filter((f) => f.name.endsWith('.java')).length;
    setStatus('sourceStatus', `已选择 Java 文件 ${javaCount} 个。`, javaCount ? 'ok' : 'warn');
  });
}

async function analyze() {
  if (!state.sourceFiles.length) {
    setStatus('analyzeStatus', '请先选择源码目录或 Java 文件。', 'error');
    return;
  }

  $('analyzeBtn').disabled = true;
  setStatus('analyzeStatus', '正在读取源码并发送到后端分析，请稍候...', 'warn');

  try {
    const sourceFiles = await collectSourceFiles();
    if (!sourceFiles.length) {
      throw new Error('未检测到 .java 文件');
    }

    const payload = buildRequestPayload(sourceFiles);
    const response = await fetch('/api/analyze', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      throw new Error(err.message || `HTTP ${response.status}`);
    }

    const data = await response.json();
    state.response = data;
    state.selectedClass = null;

    renderSummary(data);
    renderVisualization();

    $('downloadJsonBtn').disabled = false;
    $('downloadMdBtn').disabled = false;

    setStatus('analyzeStatus', `分析完成：类 ${data.projectMetrics.classCount}，方法 ${data.projectMetrics.methodCount}，耗时 ${data.elapsedMs}ms。`, 'ok');
  } catch (err) {
    setStatus('analyzeStatus', `分析失败：${err.message}`, 'error');
  } finally {
    $('analyzeBtn').disabled = false;
  }
}

function bindActions() {
  $('analyzeBtn').addEventListener('click', analyze);

  $('downloadJsonBtn').addEventListener('click', () => {
    if (!state.response) {
      return;
    }
    download('metrics.json', JSON.stringify(state.response.projectMetrics, null, 2), 'application/json;charset=utf-8');
  });

  $('downloadMdBtn').addEventListener('click', () => {
    if (!state.response) {
      return;
    }
    download('metrics-report.md', state.response.markdownReport || '', 'text/markdown;charset=utf-8');
  });

  window.addEventListener('resize', () => {
    if (state.response) {
      renderVisualization();
    }
  });
}

async function init() {
  bindPickers();
  bindActions();

  try {
    const health = await fetch('/api/health');
    if (health.ok) {
      setStatus('analyzeStatus', '后端服务在线。请选择源码目录后开始分析。', 'ok');
    } else {
      setStatus('analyzeStatus', '后端服务未就绪，请稍后刷新。', 'warn');
    }
  } catch {
    setStatus('analyzeStatus', '无法连接后端服务，请确认使用 serve 命令启动。', 'error');
  }
}

init();
