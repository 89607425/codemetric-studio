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

const METRIC_AXES = ['WMC', 'DIT', 'NOC', 'CBO', 'RFC', 'LCOM'];
const COLORS = ['#123FBD', '#5D8FE8', '#2F3A56', '#D60000', '#7EADEB', '#13A86B', '#D97706', '#6B7A90', '#9FB7D8', '#0B43C7', '#94A3B8', '#B42318'];
const BUILTIN_TYPES = new Set(['void', 'boolean', 'byte', 'short', 'int', 'long', 'float', 'double', 'char', 'string', 'integer', 'object', 'list', 'map', 'set', 'date', 'datetime', 'money']);
const API_BASE_URL = location.port === '9090' ? '' : `${location.protocol}//${location.hostname || '127.0.0.1'}:9090`;
const ANALYZE_API_URL = `${API_BASE_URL}/api/analyze-project`;
const AI_ANALYSIS_API_URL = `${API_BASE_URL}/api/ai-analysis`;

const TAB_TITLE = {
  fp: '功能点度量',
  uc: '用例图度量',
  oo: '面向对象度量总览',
  cfg: '控制流图度量',
  loc: '代码行度量',
  ai: '智能分析',
};

const TAB_DESC = {
  fp: '功能点度量用于估算软件功能规模，支持基于结构数据自动估算，也支持手工录入事务功能和数据功能。',
  uc: '用例图度量支持手工输入，也支持直接上传 .oom 用例图并自动估算参与者、用例与修正因子。',
  oo: '面向对象度量既可展示 metrics.json 结果，也可直接从类图 .oom 中抽取类、属性、操作和关系做近似度量。',
  cfg: '控制流图度量支持代码文本统计，也支持从 metrics.json 方法复杂度或顺序图 .oom 的交互消息中自动估算。',
  loc: '代码行度量支持粘贴代码统计，也支持读取当前加载的数据源，包括类图、用例图和顺序图的结构规模。',
  ai: '智能分析会调用本地后端代理的硅基流动 API，结合用户关注点、当前指标和图结构评估设计质量。',
};

const TAB_BADGES = {
  fp: ['FTR/DER 自动判级', 'RET/DET 自动判级', '支持 metrics 与 .oom'],
  uc: ['UAW/UUC/UUCP 计算', '可直接读取用例图 .oom', '支持人工修正'],
  oo: ['类图雷达图', '类级明细表', '支持导出 XML'],
  cfg: ['代码决策点统计', '方法 CC 自动读取', '顺序图消息估算'],
  loc: ['总代码量统计', '注释/空行拆分', '设计图规模读取'],
  ai: ['硅基流动 API', '上下文指标汇总', '设计质量建议'],
};

const METRIC_TOOLTIPS = {
  '事务类型': '事务功能分类：EI 表示外部输入，EO 表示外部输出，EQ 表示外部查询。',
  '事务 FTR': 'FTR 是事务功能引用的数据文件类型数量，用于判断 EI/EO/EQ 的复杂度。',
  '事务 DER': 'DER 是事务涉及的数据元素数量，用于判断 EI/EO/EQ 的复杂度。',
  '数据类型': '数据功能分类：ILF 是系统维护的内部逻辑文件，EIF 是系统引用但不维护的外部接口文件。',
  '数据 RET': 'RET 是数据功能中的记录元素类型数量，用于判断 ILF/EIF 的复杂度。',
  '数据 DET': 'DET 是数据功能中的数据元素类型数量，用于判断 ILF/EIF 的复杂度。',
  'VAF(0.65~1.35)': 'VAF 是价值调整因子，用于把未调整功能点 UFP 修正为最终功能点 FP。',
  UFP: 'Unadjusted Function Point，未调整功能点，由 EI/EO/EQ/ILF/EIF 的数量和复杂度加权得到。',
  VAF: 'Value Adjustment Factor，功能点调整因子，通常范围为 0.65 到 1.35。',
  '调整后 FP': '调整后功能点，计算公式为 UFP × VAF，用于表示修正后的功能规模。',
  'EI 低/中/高': 'EI 是 External Input，外部输入，例如新增、修改、删除等把数据送入系统的事务。',
  'EO 低/中/高': 'EO 是 External Output，外部输出，例如报表、统计、通知等从系统输出结果的事务。',
  'EQ 低/中/高': 'EQ 是 External Inquiry，外部查询，通常包含查询请求和查询结果，不维护内部数据。',
  'ILF 低/中/高': 'ILF 是 Internal Logical File，系统内部维护的核心业务数据。',
  'EIF 低/中/高': 'EIF 是 External Interface File，系统引用但不维护的外部数据。',
  '简单参与者': '简单参与者通常通过简单接口与系统交互，权重为 1。',
  '一般参与者': '一般参与者交互方式或接口复杂度中等，权重为 2。',
  '复杂参与者': '复杂参与者交互方式复杂或涉及外部系统集成，权重为 3。',
  '简单用例': '简单用例业务步骤少、分支少，权重为 5。',
  '一般用例': '一般用例业务步骤和分支复杂度中等，权重为 10。',
  '复杂用例': '复杂用例业务步骤多、分支多或规则复杂，权重为 15。',
  TFactor: '技术复杂度因子总分，用于计算技术复杂度修正系数 TCF。',
  EFactor: '环境复杂度因子总分，用于计算环境修正系数 EF。',
  UAW: 'Unadjusted Actor Weight，未调整参与者权重，由简单/一般/复杂参与者加权得到。',
  UUC: 'Unadjusted Use Case Weight，未调整用例权重，由简单/一般/复杂用例加权得到。',
  UUCP: 'Unadjusted Use Case Points，未调整用例点，计算公式为 UAW + UUC。',
  TCF: 'Technical Complexity Factor，技术复杂度修正系数，当前按 0.6 + 0.01 × TFactor 计算。',
  EF: 'Environmental Factor，环境修正系数，当前按 1.4 - 0.03 × EFactor 计算。',
  UPC: 'Use Case Points，修正后的用例点，计算公式为 UUCP × TCF × EF。',
};

function byId(id) {
  return document.getElementById(id);
}

function setStatus(id, message, isError = false) {
  const el = byId(id);
  if (!el) return;
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

function markdownToHtml(markdown) {
  const lines = String(markdown || '').split(/\r?\n/);
  const html = [];
  let listOpen = false;

  lines.forEach((rawLine) => {
    const line = rawLine.trim();
    if (!line) {
      if (listOpen) {
        html.push('</ul>');
        listOpen = false;
      }
      return;
    }

    const bullet = line.match(/^[-*]\s+(.+)$/);
    if (bullet) {
      if (!listOpen) {
        html.push('<ul>');
        listOpen = true;
      }
      html.push(`<li>${inlineMarkdownToHtml(bullet[1])}</li>`);
      return;
    }

    if (listOpen) {
      html.push('</ul>');
      listOpen = false;
    }

    const heading = line.match(/^(#{1,3})\s+(.+)$/);
    if (heading) {
      const level = heading[1].length + 2;
      html.push(`<h${level}>${inlineMarkdownToHtml(heading[2])}</h${level}>`);
      return;
    }

    html.push(`<p>${inlineMarkdownToHtml(line)}</p>`);
  });

  if (listOpen) {
    html.push('</ul>');
  }

  return html.join('');
}

function inlineMarkdownToHtml(text) {
  return escapeHtml(text)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>');
}

function num(id) {
  const value = Number(byId(id)?.value);
  return Number.isFinite(value) ? value : 0;
}

function ensureNumber(value, fallback = 0) {
  const result = Number(value);
  return Number.isFinite(result) ? result : fallback;
}

function metricTipHtml(label) {
  const text = String(label);
  const tooltip = METRIC_TOOLTIPS[text];
  if (!tooltip) return escapeHtml(text);
  return `<span class="metric-tip" tabindex="0" data-tooltip="${escapeHtml(tooltip)}">${escapeHtml(text)}</span>`;
}

function renderCards(hostId, entries) {
  byId(hostId).innerHTML = entries.map(([k, v]) => `
    <div class="card"><div class="k">${metricTipHtml(k)}</div><div class="v">${escapeHtml(v)}</div></div>
  `).join('');
}

function renderHeroBadges(tabKey) {
  const badges = TAB_BADGES[tabKey] || ['可视化度量', '结果展示', '自动分析'];
  ['heroBadge1', 'heroBadge2', 'heroBadge3'].forEach((id, index) => {
    const el = byId(id);
    if (el) el.textContent = badges[index] || '';
  });
}

function hydrateStaticCopy() {
  const hint = document.querySelector('.hint');
  if (hint) {
    hint.innerHTML = '优先上传本工具生成的 <code>metrics.json</code>。现在也支持直接上传 <code>.oom</code> 类图、用例图、顺序图，或上传整个 Java 项目文件夹并调用本地分析接口。';
  }
  const fileName = byId('fileName');
  if (fileName) {
    fileName.textContent = '未选择文件，默认读取 /out/metrics.json';
  }
}

function ensureProjectUploadControls() {
  const uploadRow = document.querySelector('#sideActionRow') || document.querySelector('.right-panel .upload-row');
  if (!uploadRow) {
    return;
  }

  if (!byId('projectUploadBtn')) {
    const button = document.createElement('button');
    button.id = 'projectUploadBtn';
    button.className = 'btn secondary';
    button.textContent = '上传 Java 项目';
    const exportButton = byId('exportXmlBtn');
    if (exportButton?.parentElement === uploadRow) {
      uploadRow.insertBefore(button, exportButton);
    } else {
      uploadRow.appendChild(button);
    }
  }

  if (byId('projectDirInput')) {
    return;
  }

  const input = document.createElement('input');
  input.id = 'projectDirInput';
  input.type = 'file';
  input.multiple = true;
  input.hidden = true;
  input.setAttribute('webkitdirectory', '');
  input.setAttribute('directory', '');
  document.body.appendChild(input);
}

const OO_METRIC_TITLES = {
  WMC: '加权方法数',
  DIT: '继承树深度',
  NOC: '子类数量',
  CBO: '对象间耦合度',
  RFC: '类响应集',
  LCOM: '方法内聚缺失度',
  NOP: '可被重写的方法数',
  NOM: '实际被重写的方法数',
  NOO: '重载方法数量',
  POD: '多态度',
  overrideRatio: '重写率',
  overloadRatio: '重载率',
  SK: '特化指数',
  DAC: '数据抽象耦合',
  MOA: '聚合度量',
  CAM: '计算抽象度量',
  CIS: '类接口规模',
  AIF: '属性继承因子',
  MIF: '方法继承因子',
};

function metricHeader(label) {
  const title = OO_METRIC_TITLES[label];
  return title
    ? `<th><span class="metric-abbr" title="${escapeHtml(title)}">${escapeHtml(label)}</span></th>`
    : `<th>${escapeHtml(label)}</th>`;
}

function describeCapabilities(data) {
  if (!data) {
    return `
      <strong>当前输入类型与可做度量</strong><br>
      未加载数据时：功能点、用例点、面向对象、控制流、代码行都只能手工输入。<br>
      上传 metrics.json 后：五类度量都可以直接或辅助完成。<br>
      上传 .oom 后：不同图类型支持的度量不同，页面会自动提示。
    `;
  }

  if (data.inputType === 'class-oom') {
    return `
      <strong>当前为类图 .oom</strong><br>
      可以直接做：面向对象度量，例如类数、属性数、操作数、耦合近似、类级表格与雷达图。<br>
      可以辅助做：功能点的启发式估算，但这不是标准功能点做法，因为类图缺少明确事务语义。<br>
      不适合直接做：标准用例图度量，因为类图本身没有参与者和用例关系。<br>
      不适合直接做：精确控制流复杂度，因为类图不描述分支、循环和执行路径。
    `;
  }

  if (data.inputType === 'usecase-oom') {
    return `
      <strong>当前为用例图 .oom</strong><br>
      可以直接做：用例图度量，例如参与者复杂度、用例复杂度、UAW、UUC、UUCP、UPC。<br>
      可以辅助做：功能点启发式估算，把用例和参与者粗略映射到事务功能和数据功能。<br>
      不能直接做：标准面向对象度量，因为用例图没有类、属性、方法结构。<br>
      不能直接做：精确控制流复杂度，因为用例图不描述程序分支路径。
    `;
  }

  if (data.inputType === 'sequence-oom') {
    return `
      <strong>当前为顺序图 .oom</strong><br>
      可以直接做：交互规模统计、消息数、参与对象数，以及控制流复杂度的近似估算。<br>
      可以辅助做：代码行/结构规模读取。<br>
      不适合直接做：标准功能点度量，因为顺序图不完整表达 ILF/EIF 与事务分类。<br>
      不适合直接做：标准用例图度量和面向对象度量，因为它缺少参与者-用例模型和完整类结构。
    `;
  }

  return `
    <strong>当前为 metrics.json</strong><br>
    可以较完整地支撑：功能点自动估算、用例点自动估算、面向对象度量、控制流复杂度读取、代码行度量。<br>
    其中功能点和用例点的“自动估算”仍然是启发式辅助，不等于完全替代人工判级。
  `;
}

function ensureInfoBlocks() {
  const ooHost = byId('tab-oo')?.querySelector('.chart-wrap');
  const cfgBody = byId('tab-cfg')?.querySelector('.panel-body');
  const locBody = byId('tab-loc')?.querySelector('.panel-body');

  if (cfgBody && !byId('cfgGuide')) {
    cfgBody.insertAdjacentHTML('afterbegin', `
      <div class="hint" id="cfgGuide">
        <strong>控制流说明</strong><br>
        如果输入是代码或 metrics.json，可以读取或统计圈复杂度 CC。<br>
        如果输入是顺序图 .oom，这里给出的 CC 是基于消息与循环片段的近似估算，不是源码级精确结果。
      </div>
    `);
  }
  if (locBody && !byId('locGuide')) {
    locBody.insertAdjacentHTML('afterbegin', `
      <div class="hint" id="locGuide">
        <strong>代码行说明</strong><br>
        粘贴代码时统计的是总代码行、空行、注释行、有效代码行。<br>
        上传 .oom 时这里展示的是图结构规模，不是源码真实 LoC。
      </div>
    `);
  }
  if (ooHost && !byId('ooGuide')) {
    ooHost.insertAdjacentHTML('afterbegin', `<div class="hint" id="ooGuide"></div>`);
  }
  if (!byId('inputCapabilityGuide')) {
    const summaryBox = byId('summaryBox');
    if (summaryBox?.parentElement) {
      summaryBox.insertAdjacentHTML('beforebegin', `<div class="hint" id="inputCapabilityGuide"></div>`);
    }
  }
}

function renderCapabilityGuide(data) {
  const guide = byId('inputCapabilityGuide');
  if (guide) {
    guide.innerHTML = describeCapabilities(data);
  }
  const ooGuide = byId('ooGuide');
  if (ooGuide) {
    ooGuide.innerHTML = describeCapabilities(data);
  }
}

async function analyzeJavaProjectFiles(files) {
  const javaFiles = Array.from(files || []).filter((file) => {
    const relativePath = file.webkitRelativePath || file.name;
    return relativePath.toLowerCase().endsWith('.java');
  });

  if (!javaFiles.length) {
    throw new Error('所选文件夹中没有找到 .java 文件');
  }

  const payloadFiles = await Promise.all(javaFiles.map(async (file) => ({
    relativePath: file.webkitRelativePath || file.name,
    content: await file.text(),
  })));

  const projectName = (javaFiles[0].webkitRelativePath || '').split('/')[0] || 'uploaded-java-project';
  const response = await fetch(ANALYZE_API_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      projectName,
      files: payloadFiles,
    }),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || `接口调用失败（HTTP ${response.status}）`);
  }
  return {
    projectName,
    metrics: data,
    fileCount: javaFiles.length,
  };
}

function allByLocalName(root, localName) {
  return Array.from(root.getElementsByTagName('*')).filter((node) => node.localName === localName);
}

function directChildrenByLocalName(root, localName) {
  return Array.from(root?.children || []).filter((node) => node.localName === localName);
}

function firstDirectChild(root, localName) {
  return directChildrenByLocalName(root, localName)[0] || null;
}

function childText(root, localName) {
  const node = firstDirectChild(root, localName);
  return (node?.textContent || '').trim();
}

function getOomNodeName(node) {
  return childText(node, 'Name') || childText(node, 'Code') || node.getAttribute('Id') || '';
}

function getRefId(node, collectionName) {
  const collection = firstDirectChild(node, collectionName);
  const refNode = Array.from(collection?.getElementsByTagName('*') || []).find((child) => child.hasAttribute('Ref'));
  return refNode?.getAttribute('Ref') || '';
}

function sanitizeTypeName(typeName) {
  return String(typeName || '')
    .replace(/[：:]/g, ':')
    .replace(/[<>()\[\],]/g, ' ')
    .trim();
}

function normalizeTypeName(typeName) {
  const cleaned = sanitizeTypeName(typeName);
  if (!cleaned) return '';
  return cleaned.split(/\s+/)[0].split(':').pop() || '';
}

function isBuiltinType(typeName) {
  return BUILTIN_TYPES.has(normalizeTypeName(typeName).toLowerCase());
}

function buildDiagramLoc(summary) {
  const total = ensureNumber(summary.total, 0);
  const blank = ensureNumber(summary.blank, 0);
  const comment = ensureNumber(summary.comment, 0);
  return {
    totalLines: total,
    blankLines: blank,
    commentLines: comment,
    codeLines: Math.max(0, total - blank - comment),
  };
}

function extractDiagramKind(xmlDoc) {
  if (allByLocalName(xmlDoc, 'ClassDiagram').length) return 'class';
  if (allByLocalName(xmlDoc, 'UseCaseDiagram').length) return 'usecase';
  if (allByLocalName(xmlDoc, 'SequenceDiagram').length) return 'sequence';
  return 'unknown';
}

function parseClassDiagram(xmlDoc, sourceName) {
  const classNodes = allByLocalName(xmlDoc, 'Class').filter((node) => node.hasAttribute('Id') && getOomNodeName(node));
  const classIdToName = new Map(classNodes.map((node) => [node.getAttribute('Id'), getOomNodeName(node)]));
  const classes = [];
  const methods = [];
  let totalAttributes = 0;
  let totalOperations = 0;
  let relationshipCount = 0;

  // 解析继承关系 (Generalization)
  // Object1 = 子类, Object2 = 父类
  const generalizationNodes = allByLocalName(xmlDoc, 'Generalization');
  const childToParent = new Map(); // 子类 -> 父类
  const parentChildCount = new Map(); // 父类 -> 直接子类数量

  // 初始化所有类的子类计数
  classNodes.forEach((node) => {
    parentChildCount.set(node.getAttribute('Id'), 0);
  });

  generalizationNodes.forEach((gen) => {
    const obj1 = getRefId(gen, 'Object1'); // 子类
    const obj2 = getRefId(gen, 'Object2'); // 父类
    if (obj1 && obj2) {
      childToParent.set(obj1, obj2);
      const count = parentChildCount.get(obj2) || 0;
      parentChildCount.set(obj2, count + 1);
    }
  });

  // 解析各种耦合关系
  // CBO 计入：Association、Composition、Aggregation、Dependency、Implementation
  // CBO 不计入：继承关系（Generalization）

  // 初始化每个类的耦合集合
  const classCouplings = new Map();
  classNodes.forEach((node) => {
    classCouplings.set(node.getAttribute('Id'), new Set());
  });

  // 解析关联关系 (Association) - 双向关系
  const associationNodes = allByLocalName(xmlDoc, 'Association');
  associationNodes.forEach((assoc) => {
    const obj1 = getRefId(assoc, 'Object1');
    const obj2 = getRefId(assoc, 'Object2');
    if (obj1 && obj2) {
      classCouplings.get(obj1)?.add(obj2);
      classCouplings.get(obj2)?.add(obj1);
    }
  });

  // 解析组合关系 (Composition) - 双向关系，整体与部分
  const compositionNodes = allByLocalName(xmlDoc, 'Composition');
  compositionNodes.forEach((comp) => {
    const obj1 = getRefId(comp, 'Object1');
    const obj2 = getRefId(comp, 'Object2');
    if (obj1 && obj2) {
      classCouplings.get(obj1)?.add(obj2);
      classCouplings.get(obj2)?.add(obj1);
    }
  });

  // 解析聚合关系 (Aggregation) - 双向关系，整体与部分
  const aggregationNodes = allByLocalName(xmlDoc, 'Aggregation');
  aggregationNodes.forEach((agg) => {
    const obj1 = getRefId(agg, 'Object1');
    const obj2 = getRefId(agg, 'Object2');
    if (obj1 && obj2) {
      classCouplings.get(obj1)?.add(obj2);
      classCouplings.get(obj2)?.add(obj1);
    }
  });

  // 解析依赖关系 (Dependency) - Object1 依赖 Object2，单向
  const dependencyNodes = allByLocalName(xmlDoc, 'Dependency');
  dependencyNodes.forEach((dep) => {
    const obj1 = getRefId(dep, 'Object1'); // 依赖方
    const obj2 = getRefId(dep, 'Object2'); // 被依赖方
    if (obj1 && obj2) {
      classCouplings.get(obj1)?.add(obj2);
    }
  });

  // 解析实现关系 (Realization/Implementation) - Object1 实现 Object2，单向
  const realizationNodes = allByLocalName(xmlDoc, 'Realization');
  realizationNodes.forEach((real) => {
    const obj1 = getRefId(real, 'Object1');
    const obj2 = getRefId(real, 'Object2');
    if (obj1 && obj2) {
      classCouplings.get(obj1)?.add(obj2);
    }
  });

  // 收集所有类的方法信息（用于判断重写和重载）
  const classMethods = new Map(); // 类ID -> { methodNames: Set, methodDetails: [{name, paramCount}] }
  classNodes.forEach((node) => {
    const classId = node.getAttribute('Id');
    const operations = allByLocalName(firstDirectChild(node, 'Operations') || node, 'Operation')
      .filter((op) => op.hasAttribute('Id'));
    
    const methodNames = new Set();
    const methodDetails = [];
    
    operations.forEach((op) => {
      const name = getOomNodeName(op).toLowerCase();
      const paramsContainer = firstDirectChild(op, 'Parameters');
      const params = paramsContainer ? Array.from(paramsContainer.childNodes).filter((node) => node.nodeType === 1 && node.localName === 'Parameter' && node.hasAttribute('Id')) : [];
      methodNames.add(name);
      methodDetails.push({ name, paramCount: params.length });
    });
    
    classMethods.set(classId, { methodNames, methodDetails, opCount: operations.length });
  });

  // 计算每个类的 DIT (继承树深度)
  function calcDit(classId) {
    let depth = 0;
    let current = classId;
    const visited = new Set();
    while (current && !visited.has(current)) {
      visited.add(current);
      const parent = childToParent.get(current);
      if (parent) {
        depth++;
        current = parent;
      } else {
        break;
      }
    }
    return depth;
  }

  // 计算 NOO（被覆盖的方法数）和 overrideRatio
  // overrideRatio = 子类重写方法数 / 直接父类方法数
  function calcNooAndParentMethods(classId, methodNames) {
    let noo = 0;
    let parentNom = 0;
    const parent = childToParent.get(classId); // 直接父类
    
    if (parent) {
      const parentInfo = classMethods.get(parent);
      if (parentInfo) {
        parentNom = parentInfo.opCount; // 直接父类的方法数
        const countedMethods = new Set();
        
        // 遍历继承链上的所有父类，收集重写的方法
        let current = classId;
        const visited = new Set();
        while (current && !visited.has(current)) {
          visited.add(current);
          const p = childToParent.get(current);
          if (p) {
            const pInfo = classMethods.get(p);
            if (pInfo) {
              methodNames.forEach((name) => {
                const lowerName = name.toLowerCase();
                if (pInfo.methodNames.has(lowerName) && !countedMethods.has(lowerName)) {
                  countedMethods.add(lowerName);
                  noo++;
                }
              });
            }
            current = p;
          } else {
            break;
          }
        }
      }
    }
    return { noo, parentNom };
  }

  // 计算 overloadRatio（重载方法数 / NOM）
  // 重载：同一类中方法名相同但参数个数不同
  function calcOverloadRatio(methodDetails, nom) {
    if (nom <= 1) return 0;
    let overloadedCount = 0;
    const nameToMethods = new Map();
    
    methodDetails.forEach(({ name, paramCount }) => {
      if (!nameToMethods.has(name)) {
        nameToMethods.set(name, []);
      }
      nameToMethods.get(name).push(paramCount);
    });
    
    // 统计有重载的方法
    let overloadedMethods = 0;
    nameToMethods.forEach((paramsList) => {
      if (paramsList.length > 1) {
        overloadedMethods++;
      }
    });
    
    overloadedCount = overloadedMethods;
    
    // 调试日志
    console.log('=== overloadRatio debug ===');
    console.log('methodDetails:', methodDetails);
    console.log('nameToMethods:', [...nameToMethods.entries()]);
    console.log('overloadedCount:', overloadedCount, 'nom:', nom);
    
    return nom > 0 ? +(overloadedCount / nom).toFixed(2) : 0;
  }

  // 测试 overloadRatio 计算
  function testOverloadRatio() {
    // 测试用例1: 有重载的方法
    const methods1 = [
      { name: 'add', paramCount: 1 },
      { name: 'add', paramCount: 2 },  // 重载
      { name: 'calculate', paramCount: 3 }
    ];
    console.log('测试1 (有重载):', calcOverloadRatio(methods1, 3), '期望: 0.33');
    
    // 测试用例2: 没有重载的方法
    const methods2 = [
      { name: 'insert', paramCount: 1 },
      { name: 'update', paramCount: 1 },
      { name: 'delete', paramCount: 1 }
    ];
    console.log('测试2 (无重载):', calcOverloadRatio(methods2, 3), '期望: 0');
    
    // 测试用例3: 构造函数重载
    const methods3 = [
      { name: 'userdaoimpl', paramCount: 0 },
      { name: 'userdaoimpl', paramCount: 1 },
      { name: 'userdaoimpl', paramCount: 2 }
    ];
    console.log('测试3 (构造函数重载):', calcOverloadRatio(methods3, 3), '期望: 0.33');
  }
  // testOverloadRatio(); // 暂时注释掉

  // 计算 POD (Probability of Defect) - 缺陷概率
  // 公式: POD = 1 - (1 - CBO/20) * (1 - DIT/7) * (1 - NOM/30)
  function calcPod(cbo, dit, nom) {
    const cboFactor = Math.min(cbo / 20, 1);
    const ditFactor = Math.min(dit / 7, 1);
    const nomFactor = Math.min(nom / 30, 1);
    const pod = 1 - (1 - cboFactor) * (1 - ditFactor) * (1 - nomFactor);
    return +pod.toFixed(2);
  }

  classNodes.forEach((classNode) => {
    const className = getOomNodeName(classNode);
    const classId = classNode.getAttribute('Id');
    const attributeNodes = allByLocalName(firstDirectChild(classNode, 'Attributes') || classNode, 'Attribute').filter((node) => node.hasAttribute('Id'));
    const operationNodes = allByLocalName(firstDirectChild(classNode, 'Operations') || classNode, 'Operation').filter((node) => node.hasAttribute('Id'));
    const couplings = new Set();
    let publicMethodCount = 0;
    let parameterCount = 0;

    // 收集通过关系图谱耦合的类
    const relationCouplings = classCouplings.get(classId);
    if (relationCouplings) {
      relationCouplings.forEach((coupledClassId) => {
        const coupledClassName = classIdToName.get(coupledClassId);
        if (coupledClassName) {
          couplings.add(coupledClassName);
        }
      });
    }

    attributeNodes.forEach((attr) => {
      const dataType = classIdToName.get(getRefId(attr, 'ObjectDataType')) || childText(attr, 'DataType');
      const normalized = normalizeTypeName(dataType);
      if (normalized && !isBuiltinType(normalized) && normalized !== className) couplings.add(normalized);
    });

    const methodNames = []; // 收集方法名用于计算 NOO
    const methodDetails = []; // 收集方法详情用于计算 overloadRatio

    operationNodes.forEach((op) => {
      const methodName = getOomNodeName(op);
      methodNames.push(methodName);
      const visibility = childText(op, 'Operation.Visibility');
      // 获取 Parameters 容器下的所有 Parameter 子元素
      const paramsContainer = firstDirectChild(op, 'Parameters');
      const params = paramsContainer ? Array.from(paramsContainer.childNodes).filter((node) => node.nodeType === 1 && node.localName === 'Parameter' && node.hasAttribute('Id')) : [];
      const returnType = childText(op, 'ReturnType');

      // 收集方法详情用于计算重载
      methodDetails.push({ name: methodName.toLowerCase(), paramCount: params.length });

      if (visibility.includes('+') || !visibility) publicMethodCount += 1;
      if (!isBuiltinType(returnType) && normalizeTypeName(returnType) !== className && normalizeTypeName(returnType)) {
        couplings.add(normalizeTypeName(returnType));
      }

      params.forEach((param) => {
        parameterCount += 1;
        const paramType = classIdToName.get(getRefId(param, 'ObjectDataType')) || childText(param, 'Parameter.DataType');
        const normalized = normalizeTypeName(paramType);
        if (normalized && !isBuiltinType(normalized) && normalized !== className) couplings.add(normalized);
      });

      methods.push({
        className,
        methodName,
        cyclomaticComplexity: Math.max(1, 1 + params.length + (/if|check|validate|calc|process|handle|login|register/i.test(methodName) ? 1 : 0)),
        loc: Math.max(3, 2 + params.length * 2),
      });
    });

    totalAttributes += attributeNodes.length;
    totalOperations += operationNodes.length;
    relationshipCount += couplings.size;

    const attrCount = attributeNodes.length;
    const opCount = operationNodes.length;
    const dit = calcDit(classId);
    const noc = parentChildCount.get(classId) || 0;
    const nop = attrCount; // NOP = Number of Properties (属性数)
    const nom = opCount; // NOM = Number of Methods (方法数)
    
    // 计算 NOO 和 overrideRatio (子类重写方法数 / 父类方法数)
    const { noo, parentNom } = dit > 0 ? calcNooAndParentMethods(classId, methodNames) : { noo: 0, parentNom: 0 };
    const overrideRatio = parentNom > 0 ? +(noo / parentNom).toFixed(2) : 0;
    
    // 调试日志：输出 overrideRatio 计算过程
    if (dit > 0) {
      console.log(`[${className}] DIT=${dit}, NOO=${noo}, 父类NOM=${parentNom}, overrideRatio=${overrideRatio}`);
    }
    
    // 计算 overloadRatio (重载方法数 / NOM)
    const overloadRatio = calcOverloadRatio(methodDetails, nom);
    
    // 调试日志：输出方法详情
    console.log(`[${className}] NOM=${nom}, methodDetails:`, methodDetails);
    
    // 计算 POD (缺陷概率): POD = 1 - (1 - CBO/20) * (1 - DIT/7) * (1 - NOM/30)
    const pod = calcPod(couplings.size, dit, nom);

    classes.push({
      className,
      wmc: opCount,
      dit,
      noc,
      cbo: couplings.size,
      rfc: opCount + couplings.size,
      lcom: attrCount && opCount ? +(attrCount / opCount).toFixed(2) : 0,
      nop,
      nom,
      noo,
      pod,
      overrideRatio,
      overloadRatio,
      sk: 0,
      dac: couplings.size,
      moa: couplings.size,
      cam: opCount ? +(parameterCount / opCount).toFixed(2) : 0,
      cis: publicMethodCount,
      aif: 0,
      mif: 0,
    });
  });

  return {
    inputType: 'class-oom',
    sourceKind: 'class',
    projectName: sourceName,
    fileCount: 1,
    classCount: classes.length,
    methodCount: methods.length,
    classes,
    methods,
    alerts: [],
    loc: buildDiagramLoc({ total: classes.length + methods.length + totalAttributes + relationshipCount, blank: 0, comment: 0 }),
    classDiagram: {
      classes: classes.length,
      attributes: totalAttributes,
      operations: totalOperations,
      relationships: relationshipCount,
    },
  };
}

function parseUseCaseDiagram(xmlDoc, sourceName) {
  const actorNodes = allByLocalName(xmlDoc, 'Actor').filter((node) => node.hasAttribute('Id') && getOomNodeName(node));
  const useCaseNodes = allByLocalName(xmlDoc, 'UseCase').filter((node) => node.hasAttribute('Id') && getOomNodeName(node));
  const associations = allByLocalName(xmlDoc, 'UseCaseAssociation').filter((node) => node.hasAttribute('Id'));

  const actors = actorNodes.map((node) => ({ id: node.getAttribute('Id'), name: getOomNodeName(node) }));
  const useCases = useCaseNodes.map((node) => ({ id: node.getAttribute('Id'), name: getOomNodeName(node) }));
  const actorMap = new Map(actors.map((item) => [item.id, item]));
  const useCaseMap = new Map(useCases.map((item) => [item.id, item]));
  const actorDegree = new Map(actors.map((item) => [item.id, 0]));
  const useCaseDegree = new Map(useCases.map((item) => [item.id, 0]));

  associations.forEach((assoc) => {
    const obj1 = getRefId(assoc, 'Object1');
    const obj2 = getRefId(assoc, 'Object2');
    if (actorMap.has(obj1) && useCaseMap.has(obj2)) {
      actorDegree.set(obj1, ensureNumber(actorDegree.get(obj1)) + 1);
      useCaseDegree.set(obj2, ensureNumber(useCaseDegree.get(obj2)) + 1);
    } else if (actorMap.has(obj2) && useCaseMap.has(obj1)) {
      actorDegree.set(obj2, ensureNumber(actorDegree.get(obj2)) + 1);
      useCaseDegree.set(obj1, ensureNumber(useCaseDegree.get(obj1)) + 1);
    }
  });

  let actorSimple = 0;
  let actorAverage = 0;
  let actorComplex = 0;
  actors.forEach((actor) => {
    const degree = ensureNumber(actorDegree.get(actor.id));
    if (degree >= 4) actorComplex += 1;
    else if (degree >= 2) actorAverage += 1;
    else actorSimple += 1;
  });

  let useCaseSimple = 0;
  let useCaseAverage = 0;
  let useCaseComplex = 0;
  useCases.forEach((useCase) => {
    const score = ensureNumber(useCaseDegree.get(useCase.id)) + Math.ceil(useCase.name.length / 8);
    if (score >= 5) useCaseComplex += 1;
    else if (score >= 3) useCaseAverage += 1;
    else useCaseSimple += 1;
  });

  return {
    inputType: 'usecase-oom',
    sourceKind: 'usecase',
    projectName: sourceName,
    fileCount: 1,
    classCount: 0,
    methodCount: 0,
    classes: [],
    methods: [],
    alerts: [],
    loc: buildDiagramLoc({ total: actors.length + useCases.length + associations.length, blank: 0, comment: 0 }),
    useCaseDiagram: {
      actors,
      useCases,
      associations: associations.length,
      actorSimple,
      actorAverage,
      actorComplex,
      useCaseSimple,
      useCaseAverage,
      useCaseComplex,
    },
  };
}

function parseSequenceDiagram(xmlDoc, sourceName) {
  const objectNodes = allByLocalName(xmlDoc, 'UMLObject').filter((node) => node.hasAttribute('Id') && getOomNodeName(node));
  const messageNodes = allByLocalName(xmlDoc, 'Message').filter((node) => node.hasAttribute('Id') && getOomNodeName(node));
  const fragmentNodes = allByLocalName(xmlDoc, 'InteractionFragment').filter((node) => node.hasAttribute('Id'));
  const participantMap = new Map(objectNodes.map((node) => [node.getAttribute('Id'), getOomNodeName(node)]));

  const participants = objectNodes.map((node) => ({
    id: node.getAttribute('Id'),
    name: getOomNodeName(node),
  }));

  const messages = messageNodes.map((node) => ({
    name: getOomNodeName(node),
    from: participantMap.get(getRefId(node, 'Object2')) || '-',
    to: participantMap.get(getRefId(node, 'Object1')) || '-',
    action: childText(node, 'Message.Action'),
    flow: childText(node, 'ControlFlow'),
  }));

  const loopCount = fragmentNodes.filter((node) => /for|while|loop/i.test(childText(node, 'InteractionFragment.Condition'))).length;
  const returnCount = messages.filter((message) => /return/i.test(message.name) || message.flow === 'R').length;
  const createCount = messages.filter((message) => message.action === 'C' || /create/i.test(message.name)).length;
  const cfgComplexity = Math.max(1, 1 + loopCount + Math.max(0, messages.length - returnCount - 1));

  return {
    inputType: 'sequence-oom',
    sourceKind: 'sequence',
    projectName: sourceName,
    fileCount: 1,
    classCount: participants.length,
    methodCount: messages.length,
    classes: [],
    methods: [],
    alerts: [],
    loc: buildDiagramLoc({ total: participants.length + messages.length + fragmentNodes.length, blank: 0, comment: 0 }),
    sequenceDiagram: {
      participants,
      messages,
      fragments: fragmentNodes.length,
      loopCount,
      returnCount,
      createCount,
      cfgComplexity,
    },
  };
}

function parseOomFile(text, sourceName) {
  const xmlDoc = new DOMParser().parseFromString(text, 'application/xml');
  if (xmlDoc.querySelector('parsererror')) {
    throw new Error('OOM 文件不是有效的 XML');
  }

  const kind = extractDiagramKind(xmlDoc);
  if (kind === 'class') return parseClassDiagram(xmlDoc, sourceName);
  if (kind === 'usecase') return parseUseCaseDiagram(xmlDoc, sourceName);
  if (kind === 'sequence') return parseSequenceDiagram(xmlDoc, sourceName);
  throw new Error('暂不支持识别该 OOM 图类型');
}

function buildAiAnalysisContext() {
  const data = state.raw || {};
  const classes = Array.isArray(data.classes) ? data.classes : [];
  const methods = Array.isArray(data.methods) ? data.methods : [];
  const alerts = Array.isArray(data.alerts) ? data.alerts : [];
  const topRiskClasses = [...state.rows]
    .sort((a, b) => b.risk - a.risk)
    .slice(0, 10)
    .map((row) => ({
      className: row.name,
      wmc: row.wmc,
      dit: row.dit,
      noc: row.noc,
      cbo: row.cbo,
      rfc: row.rfc,
      lcom: row.lcom,
      moa: row.moa,
      cam: row.cam,
      riskScore: row.risk,
    }));
  const highComplexMethods = [...methods]
    .sort((a, b) => ensureNumber(b.cyclomaticComplexity, 1) - ensureNumber(a.cyclomaticComplexity, 1))
    .slice(0, 10)
    .map((method) => ({
      className: method.className,
      methodName: method.methodName,
      cyclomaticComplexity: ensureNumber(method.cyclomaticComplexity, 1),
      loc: ensureNumber(method.loc),
    }));

  return {
    sourceName: state.sourceName,
    inputType: data.inputType || 'metrics-json',
    projectName: data.projectName || '',
    summary: {
      fileCount: ensureNumber(data.fileCount),
      classCount: ensureNumber(data.classCount),
      methodCount: ensureNumber(data.methodCount),
      loc: data.loc || {},
      alertCount: alerts.length,
      averageWmc: average(classes, 'wmc'),
      averageCbo: average(classes, 'cbo'),
      averageDit: average(classes, 'dit'),
      averageLcom: average(classes, 'lcom'),
      averageCyclomaticComplexity: average(methods, 'cyclomaticComplexity', 1),
    },
    classDiagram: data.classDiagram || null,
    useCaseDiagram: data.useCaseDiagram ? {
      actorCount: data.useCaseDiagram.actors?.length || 0,
      useCaseCount: data.useCaseDiagram.useCases?.length || 0,
      associations: data.useCaseDiagram.associations,
      actorSimple: data.useCaseDiagram.actorSimple,
      actorAverage: data.useCaseDiagram.actorAverage,
      actorComplex: data.useCaseDiagram.actorComplex,
      useCaseSimple: data.useCaseDiagram.useCaseSimple,
      useCaseAverage: data.useCaseDiagram.useCaseAverage,
      useCaseComplex: data.useCaseDiagram.useCaseComplex,
    } : null,
    sequenceDiagram: data.sequenceDiagram ? {
      participantCount: data.sequenceDiagram.participants?.length || 0,
      messageCount: data.sequenceDiagram.messages?.length || 0,
      fragments: data.sequenceDiagram.fragments,
      loopCount: data.sequenceDiagram.loopCount,
      returnCount: data.sequenceDiagram.returnCount,
      createCount: data.sequenceDiagram.createCount,
      cfgComplexity: data.sequenceDiagram.cfgComplexity,
      sampleMessages: (data.sequenceDiagram.messages || []).slice(0, 20),
    } : null,
    topRiskClasses,
    highComplexMethods,
    alerts: alerts.slice(0, 12),
  };
}

function average(items, field, fallback = 0) {
  if (!items.length) return 0;
  const total = items.reduce((sum, item) => sum + ensureNumber(item[field], fallback), 0);
  return +(total / items.length).toFixed(2);
}

async function runAiAnalysis() {
  const statusId = 'ai-status';
  const result = byId('ai-result');
  if (!state.raw) {
    setStatus(statusId, '请先上传 metrics.json、.oom 文件，或分析 Java 项目后再进行智能分析。', true);
    return;
  }

  const userInput = byId('ai-input')?.value || '';
  const button = byId('ai-run');
  if (button) button.disabled = true;
  if (result) {
    result.innerHTML = '<div class="hint">正在生成分析结果...</div>';
  };

  try {
    const response = await fetch(AI_ANALYSIS_API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        userInput,
        context: buildAiAnalysisContext(),
      }),
    });
    const data = await response.json();
    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('当前 9090 端口上的后端不是最新版本，缺少 /api/ai-analysis 接口，请重启后端服务');
      }
      throw new Error(data.error || `接口调用失败（HTTP ${response.status}）`);
    }
    if (result) {
      result.innerHTML = markdownToHtml(data.content || '');
    }
    setStatus(statusId, `智能分析完成，模型：${data.model || 'SiliconFlow'}`);
  } catch (err) {
    if (result) {
      result.innerHTML = '';
    }
    const message = err instanceof TypeError
      ? '无法连接 http://127.0.0.1:9090/api/ai-analysis，请先启动或重启本地后端服务'
      : err.message;
    setStatus(statusId, `智能分析失败：${message}。`, true);
  } finally {
    if (button) button.disabled = false;
  }
}

function switchTab(tabKey) {
  state.activeTab = tabKey;
  byId('sectionTitle').textContent = TAB_TITLE[tabKey] || '度量';
  byId('sectionDesc').textContent = TAB_DESC[tabKey] || '';
  renderHeroBadges(tabKey);

  document.querySelectorAll('.tab-pane').forEach((el) => el.classList.add('hidden'));
  byId(`tab-${tabKey}`)?.classList.remove('hidden');
  byId('ooTablePanel').classList.toggle('hidden', tabKey !== 'oo');

  byId('menuNav').querySelectorAll('.menu-btn').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.key === tabKey);
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
  if (!response.ok) throw new Error(`无法读取 /out/metrics.json（HTTP ${response.status}）`);
  return response.json();
}

function deriveRows(metrics) {
  const classes = Array.isArray(metrics.classes) ? metrics.classes : [];
  return classes.map((c) => {
    const row = {
      name: c.className || '-',
      wmc: ensureNumber(c.wmc),
      dit: ensureNumber(c.dit),
      noc: ensureNumber(c.noc),
      cbo: ensureNumber(c.cbo),
      rfc: ensureNumber(c.rfc),
      lcom: ensureNumber(c.lcom),
      nop: ensureNumber(c.nop),
      nom: ensureNumber(c.nom),
      noo: ensureNumber(c.noo),
      pod: ensureNumber(c.pod),
      overrideRatio: ensureNumber(c.overrideRatio),
      overloadRatio: ensureNumber(c.overloadRatio),
      sk: ensureNumber(c.sk),
      dac: ensureNumber(c.dac),
      moa: ensureNumber(c.moa),
      cam: ensureNumber(c.cam),
      cis: ensureNumber(c.cis),
      aif: ensureNumber(c.aif),
      mif: ensureNumber(c.mif),
    };
    row.risk = row.cbo * 2 + row.wmc + row.noc * 3 + row.dit + row.rfc;
    return row;
  });
}

function renderSummary() {
  const data = state.raw || {};
  const loc = data.loc || {};
  const inputTypeLabel = {
    'class-oom': '类图 .oom',
    'usecase-oom': '用例图 .oom',
    'sequence-oom': '顺序图 .oom',
  }[data.inputType] || 'metrics.json';

  byId('summaryBox').innerHTML = [
    `项目：${escapeHtml(data.projectName || '-')}`,
    `文件：${ensureNumber(data.fileCount)}，类/对象：${ensureNumber(data.classCount)}，方法/消息：${ensureNumber(data.methodCount)}`,
    `总规模：${ensureNumber(loc.totalLines)}，有效结构量：${ensureNumber(loc.codeLines)}`,
    `输入类型：${escapeHtml(inputTypeLabel)}`,
    `数据源：${escapeHtml(state.sourceName)}`,
  ].map((item) => `<div>${item}</div>`).join('');
}

function getTopRows(limit = 12) {
  return [...state.rows].sort((a, b) => b.risk - a.risk).slice(0, limit);
}

function valueForAxis(row, axis) {
  if (axis === 'WMC') return row.wmc;
  if (axis === 'DIT') return row.dit;
  if (axis === 'NOC') return row.noc;
  if (axis === 'CBO') return row.cbo;
  if (axis === 'RFC') return row.rfc;
  if (axis === 'LCOM') return row.lcom;
  return 0;
}

function polarToCartesian(cx, cy, r, angle) {
  return { x: cx + r * Math.cos(angle), y: cy + r * Math.sin(angle) };
}

function renderLegend() {
  const host = byId('classLegend');
  if (!host) return;
  const items = getTopRows();
  host.innerHTML = items.map((row, index) => {
    const dim = state.selectedClass && state.selectedClass !== row.name ? 'dim' : '';
    return `<button class="legend-item ${dim}" data-class="${escapeHtml(row.name)}">
      <span class="legend-dot" style="background:${COLORS[index % COLORS.length]}"></span>
      ${escapeHtml(row.name.split('.').pop())}
    </button>`;
  }).join('');

  host.querySelectorAll('.legend-item').forEach((btn) => {
    btn.addEventListener('click', () => {
      const className = btn.dataset.class;
      state.selectedClass = state.selectedClass === className ? null : className;
      renderLegend();
      renderTable();
      renderRadar();
    });
  });
}

function renderRadar() {
  const host = byId('radarHost');
  if (!host) return;
  host.innerHTML = '';

  const rows = getTopRows();
  if (!rows.length) {
    host.innerHTML = '<div style="padding:16px;color:#52647d;">暂无可展示的类图/面向对象数据。</div>';
    return;
  }

  const width = host.clientWidth || 860;
  const height = host.clientHeight || 520;
  const cx = width / 2;
  const cy = height / 2 + 10;
  const radius = Math.min(width, height) * 0.33;
  const maxByAxis = {};
  METRIC_AXES.forEach((axis) => {
    maxByAxis[axis] = Math.max(...rows.map((row) => valueForAxis(row, axis)), 1);
  });

  const svgNS = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(svgNS, 'svg');
  svg.setAttribute('width', String(width));
  svg.setAttribute('height', String(height));

  for (let level = 1; level <= 5; level += 1) {
    const points = METRIC_AXES.map((_, index) => {
      const angle = -Math.PI / 2 + (2 * Math.PI * index) / METRIC_AXES.length;
      return polarToCartesian(cx, cy, (radius * level) / 5, angle);
    });
    const polygon = document.createElementNS(svgNS, 'polygon');
    polygon.setAttribute('points', points.map((p) => `${p.x},${p.y}`).join(' '));
    polygon.setAttribute('fill', level % 2 === 0 ? 'rgba(18,63,189,0.05)' : 'rgba(18,63,189,0.02)');
    polygon.setAttribute('stroke', '#dfe7f2');
    polygon.setAttribute('stroke-width', '1');
    svg.appendChild(polygon);
  }

  METRIC_AXES.forEach((axis, index) => {
    const angle = -Math.PI / 2 + (2 * Math.PI * index) / METRIC_AXES.length;
    const point = polarToCartesian(cx, cy, radius, angle);
    const label = polarToCartesian(cx, cy, radius + 18, angle);
    const line = document.createElementNS(svgNS, 'line');
    line.setAttribute('x1', String(cx));
    line.setAttribute('y1', String(cy));
    line.setAttribute('x2', String(point.x));
    line.setAttribute('y2', String(point.y));
    line.setAttribute('stroke', '#d7e0ec');
    line.setAttribute('stroke-width', '1');
    svg.appendChild(line);

    const text = document.createElementNS(svgNS, 'text');
    text.setAttribute('x', String(label.x));
    text.setAttribute('y', String(label.y));
    text.setAttribute('text-anchor', 'middle');
    text.setAttribute('font-size', '13');
    text.setAttribute('fill', '#52647d');
    text.textContent = axis;
    svg.appendChild(text);
  });

  rows.forEach((row, index) => {
    const color = COLORS[index % COLORS.length];
    const isDim = state.selectedClass && state.selectedClass !== row.name;
    const points = METRIC_AXES.map((axis, axisIndex) => {
      const angle = -Math.PI / 2 + (2 * Math.PI * axisIndex) / METRIC_AXES.length;
      const ratio = valueForAxis(row, axis) / maxByAxis[axis];
      return polarToCartesian(cx, cy, radius * ratio, angle);
    });

    const polygon = document.createElementNS(svgNS, 'polygon');
    polygon.setAttribute('points', points.map((p) => `${p.x},${p.y}`).join(' '));
    polygon.setAttribute('fill', color);
    polygon.setAttribute('fill-opacity', isDim ? '0.03' : '0.08');
    polygon.setAttribute('stroke', color);
    polygon.setAttribute('stroke-width', state.selectedClass === row.name ? '3' : '2');
    polygon.setAttribute('stroke-opacity', isDim ? '0.2' : '0.9');
    svg.appendChild(polygon);

    points.forEach((point) => {
      const dot = document.createElementNS(svgNS, 'circle');
      dot.setAttribute('cx', String(point.x));
      dot.setAttribute('cy', String(point.y));
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
    byId('tableWrap').innerHTML = '<div style="padding:16px;color:#9bb8db;">当前数据源没有类级明细。</div>';
    return;
  }

  const body = rows.map((row) => {
    const active = state.selectedClass === row.name ? 'row-active' : '';
    return `<tr class="${active}">
      <td><a class="class-link" data-class="${escapeHtml(row.name)}">${escapeHtml(row.name)}</a></td>
      <td>${row.wmc}</td>
      <td>${row.dit}</td>
      <td>${row.noc}</td>
      <td>${row.cbo}</td>
      <td>${row.rfc}</td>
      <td>${row.lcom.toFixed(2)}</td>
      <td>${row.nop}</td>
      <td>${row.nom}</td>
      <td>${row.noo}</td>
      <td>${row.pod.toFixed(2)}</td>
      <td>${row.overrideRatio.toFixed(2)}</td>
      <td>${row.overloadRatio.toFixed(2)}</td>
      <td>${row.sk.toFixed(2)}</td>
      <td>${row.dac}</td>
      <td>${row.moa}</td>
      <td>${row.cam.toFixed(2)}</td>
      <td>${row.cis}</td>
      <td>${row.aif.toFixed(2)}</td>
      <td>${row.mif.toFixed(2)}</td>
    </tr>`;
  }).join('');

  byId('tableWrap').innerHTML = `
    <table>
      <thead>
        <tr>
          <th>类名</th>
          ${['WMC', 'DIT', 'NOC', 'CBO', 'RFC', 'LCOM', 'NOP', 'NOM', 'NOO', 'POD', 'overrideRatio', 'overloadRatio', 'SK', 'DAC', 'MOA', 'CAM', 'CIS', 'AIF', 'MIF'].map(metricHeader).join('')}
        </tr>
      </thead>
      <tbody>${body}</tbody>
    </table>
  `;

  byId('tableWrap').querySelectorAll('.class-link').forEach((link) => {
    link.addEventListener('click', (event) => {
      event.preventDefault();
      state.selectedClass = link.dataset.class;
      renderTable();
      renderRadar();
      renderLegend();
    });
  });
}

function txComplexity(type, ftr, der) {
  if (type === 'EI') {
    if (ftr <= 1) return der <= 15 ? 'low' : 'avg';
    if (ftr === 2) return der <= 4 ? 'low' : der <= 15 ? 'avg' : 'high';
    return der <= 4 ? 'avg' : 'high';
  }
  if (ftr <= 1) return der <= 19 ? 'low' : 'avg';
  if (ftr <= 3) return der <= 5 ? 'low' : der <= 19 ? 'avg' : 'high';
  return der <= 5 ? 'avg' : 'high';
}

function dataComplexity(type, ret, det) {
  const lowDet = det <= 19;
  const midDet = det >= 20 && det <= 50;
  if (type === 'ILF' || type === 'EIF') {
    if (ret === 1) return lowDet || midDet ? 'low' : 'avg';
    if (ret <= 5) return lowDet ? 'low' : midDet ? 'avg' : 'high';
    return lowDet ? 'avg' : 'high';
  }
  return 'low';
}

function complexityLabel(level) {
  return { low: '低', avg: '中', high: '高' }[level] || level;
}

function calcFunctionPoint() {
  const vaf = num('fp-vaf');
  const c = state.fpCounts;
  const ufp =
    c.EI.low * 3 + c.EI.avg * 4 + c.EI.high * 6 +
    c.EO.low * 4 + c.EO.avg * 5 + c.EO.high * 7 +
    c.EQ.low * 3 + c.EQ.avg * 4 + c.EQ.high * 6 +
    c.ILF.low * 7 + c.ILF.avg * 10 + c.ILF.high * 15 +
    c.EIF.low * 5 + c.EIF.avg * 7 + c.EIF.high * 10;
  const fp = +(ufp * vaf).toFixed(2);

  renderCards('fp-cards', [
    ['UFP', ufp],
    ['VAF', vaf.toFixed(2)],
    ['调整后 FP', fp],
    ['EI 低/中/高', `${c.EI.low}/${c.EI.avg}/${c.EI.high}`],
    ['EO 低/中/高', `${c.EO.low}/${c.EO.avg}/${c.EO.high}`],
    ['EQ 低/中/高', `${c.EQ.low}/${c.EQ.avg}/${c.EQ.high}`],
    ['ILF 低/中/高', `${c.ILF.low}/${c.ILF.avg}/${c.ILF.high}`],
    ['EIF 低/中/高', `${c.EIF.low}/${c.EIF.avg}/${c.EIF.high}`],
  ]);
  setStatus('fp-status', '已完成功能点计算。你可以继续手工修正复杂度分档，再次点击计算即可。');
}

function addTransactionFunction() {
  const type = byId('fp-tx-type').value;
  const ftr = Math.max(0, Math.floor(num('fp-tx-ftr')));
  const der = Math.max(0, Math.floor(num('fp-tx-der')));
  const level = txComplexity(type, ftr, der);
  state.fpCounts[type][level] += 1;
  calcFunctionPoint();
  setStatus('fp-status', `已添加事务功能：${type}，FTR=${ftr}，DER=${der}，复杂度=${complexityLabel(level)}。`);
}

function addDataFunction() {
  const type = byId('fp-data-type').value;
  const ret = Math.max(0, Math.floor(num('fp-data-ret')));
  const det = Math.max(0, Math.floor(num('fp-data-det')));
  const level = dataComplexity(type, ret, det);
  state.fpCounts[type][level] += 1;
  calcFunctionPoint();
  setStatus('fp-status', `已添加数据功能：${type}，RET=${ret}，DET=${det}，复杂度=${complexityLabel(level)}。`);
}

function resetFunctionPointCounts() {
  state.fpCounts = {
    EI: { low: 0, avg: 0, high: 0 },
    EO: { low: 0, avg: 0, high: 0 },
    EQ: { low: 0, avg: 0, high: 0 },
    ILF: { low: 0, avg: 0, high: 0 },
    EIF: { low: 0, avg: 0, high: 0 },
  };
  calcFunctionPoint();
  setStatus('fp-status', '已重置功能点统计。');
}

function inferTxType(methodName) {
  const name = String(methodName || '').toLowerCase();
  if (/^(add|create|insert|save|update|delete|remove|set|register)/.test(name)) return 'EI';
  if (/^(get|find|query|list|search|count|check|load)/.test(name)) return 'EQ';
  return 'EO';
}

function inferDataType(className) {
  const name = String(className || '').toLowerCase();
  if (/(client|proxy|external|remote|adapter|gateway)/.test(name)) return 'EIF';
  return 'ILF';
}

function autoEstimateFunctionPoint() {
  if (!state.raw) {
    setStatus('fp-status', '没有可用数据，请先加载 metrics.json 或 .oom 文件。', true);
    return;
  }

  resetFunctionPointCounts();

  if (state.raw.useCaseDiagram) {
    const d = state.raw.useCaseDiagram;
    state.fpCounts.EI.low += d.useCaseSimple;
    state.fpCounts.EI.avg += d.useCaseAverage;
    state.fpCounts.EI.high += d.useCaseComplex;
    state.fpCounts.ILF.low += d.actorSimple;
    state.fpCounts.ILF.avg += d.actorAverage;
    state.fpCounts.ILF.high += d.actorComplex;
    calcFunctionPoint();
    setStatus('fp-status', `已基于用例图 .oom 自动估算：用例 ${d.useCases.length} 个，参与者 ${d.actors.length} 个。结果为启发式映射，建议结合实验报告再校正。`);
    return;
  }

  const methods = Array.isArray(state.raw.methods) ? state.raw.methods : [];
  const classes = Array.isArray(state.raw.classes) ? state.raw.classes : [];
  const classByName = new Map(classes.map((item) => [item.className, item]));

  methods.forEach((method) => {
    const owner = classByName.get(method.className) || {};
    const type = inferTxType(method.methodName);
    const ftr = Math.max(0, ensureNumber(owner.cbo));
    const der = Math.max(1, Math.round(ensureNumber(method.loc) / 3 + ensureNumber(method.cyclomaticComplexity, 1)));
    const level = txComplexity(type, ftr, der);
    state.fpCounts[type][level] += 1;
  });

  classes.forEach((klass) => {
    const type = inferDataType(klass.className);
    const ret = Math.max(1, ensureNumber(klass.noc) + 1);
    const det = Math.max(1, ensureNumber(klass.wmc) * 2 + ensureNumber(klass.cbo));
    const level = dataComplexity(type, ret, det);
    state.fpCounts[type][level] += 1;
  });

  calcFunctionPoint();
  setStatus('fp-status', `已基于当前数据源自动估算功能点：事务功能 ${methods.length} 个，数据功能 ${classes.length} 个。`);
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
  const tcf = +(0.6 + 0.01 * tFactor).toFixed(2);
  const ef = +(1.4 - 0.03 * eFactor).toFixed(2);
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
  setStatus('uc-status', '已完成用例点计算。');
}

function setUcInput(id, value) {
  const el = byId(id);
  if (el) el.value = String(Math.max(0, Math.round(value)));
}

function autoEstimateUseCasePoint() {
  if (!state.raw) {
    setStatus('uc-status', '没有可用数据，请先加载 metrics.json 或 .oom 文件。', true);
    return;
  }

  if (state.raw.useCaseDiagram) {
    const d = state.raw.useCaseDiagram;
    setUcInput('uc-as', d.actorSimple);
    setUcInput('uc-aa', d.actorAverage);
    setUcInput('uc-ac', d.actorComplex);
    setUcInput('uc-us', d.useCaseSimple);
    setUcInput('uc-ua', d.useCaseAverage);
    setUcInput('uc-uc', d.useCaseComplex);
    setUcInput('uc-tfactor', Math.min(50, d.associations + d.useCaseComplex * 2));
    setUcInput('uc-efactor', Math.min(50, d.actorComplex * 4 + d.useCaseComplex * 3));
    calcUseCasePoint();
    setStatus('uc-status', '已根据用例图 .oom 自动填充参与者和用例复杂度。');
    return;
  }

  const classes = Array.isArray(state.raw.classes) ? state.raw.classes : [];
  const methods = Array.isArray(state.raw.methods) ? state.raw.methods : [];
  const alerts = Array.isArray(state.raw.alerts) ? state.raw.alerts : [];

  let as = 0;
  let aa = 0;
  let ac = 0;
  classes.forEach((klass) => {
    const name = String(klass.className || '').toLowerCase();
    const score = ensureNumber(klass.cbo) + ensureNumber(klass.wmc);
    if (/(controller|api|gateway|facade)/.test(name) || score >= 12) ac += 1;
    else if (/(service|manager|handler)/.test(name) || score >= 6) aa += 1;
    else as += 1;
  });

  let us = 0;
  let ua = 0;
  let uc = 0;
  methods.forEach((method) => {
    const name = String(method.methodName || '').toLowerCase();
    const complexity = ensureNumber(method.cyclomaticComplexity, 1);
    const loc = ensureNumber(method.loc);
    if (complexity >= 8 || loc >= 30 || /(process|workflow|orchestrate)/.test(name)) uc += 1;
    else if (complexity >= 4 || loc >= 12 || /(update|create|delete|submit|handle)/.test(name)) ua += 1;
    else us += 1;
  });

  const avgCbo = classes.length ? classes.reduce((sum, item) => sum + ensureNumber(item.cbo), 0) / classes.length : 0;
  const avgComplexity = methods.length ? methods.reduce((sum, item) => sum + ensureNumber(item.cyclomaticComplexity, 1), 0) / methods.length : 0;
  const avgDit = classes.length ? classes.reduce((sum, item) => sum + ensureNumber(item.dit), 0) / classes.length : 0;
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
  setStatus('uc-status', '已基于当前数据源自动估算用例点。');
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
    ['主要控制节点', ifCount + forCount + whileCount + caseCount + catchCount],
  ]);
  setStatus('cfg-status', `统计明细：if=${ifCount}, for=${forCount}, while=${whileCount}, case=${caseCount}, catch=${catchCount}, &&=${andCount}, ||=${orCount}, ?=${ternaryCount}`);
}

function populateCfgSelectors() {
  const classSelect = byId('cfg-class-select');
  const methodSelect = byId('cfg-method-select');
  if (!classSelect || !methodSelect) return;

  const methods = Array.isArray(state.raw?.methods) ? state.raw.methods : [];
  const classNames = [...new Set(methods.map((item) => item.className).filter(Boolean))].sort();
  classSelect.innerHTML = '<option value="">请选择类</option>';
  classNames.forEach((name) => {
    const option = document.createElement('option');
    option.value = String(name);
    option.textContent = String(name);
    classSelect.appendChild(option);
  });

  methodSelect.innerHTML = '<option value="">请选择方法</option>';
}

function onCfgClassChange() {
  const className = byId('cfg-class-select').value;
  const methodSelect = byId('cfg-method-select');
  const methods = Array.isArray(state.raw?.methods) ? state.raw.methods : [];
  const filtered = methods.filter((item) => item.className === className).sort((a, b) => String(a.methodName).localeCompare(String(b.methodName)));

  methodSelect.innerHTML = '<option value="">请选择方法</option>';
  filtered.forEach((method, index) => {
    const option = document.createElement('option');
    option.value = String(index);
    option.textContent = `${method.methodName} (CC=${ensureNumber(method.cyclomaticComplexity, 1)}, LOC=${ensureNumber(method.loc)})`;
    methodSelect.appendChild(option);
  });
}

function calcCfgFromMetrics() {
  if (state.raw?.sequenceDiagram) {
    const d = state.raw.sequenceDiagram;
    renderCards('cfg-cards', [
      ['圈复杂度 CC', d.cfgComplexity],
      ['交互消息数', d.messages.length],
      ['参与对象数', d.participants.length],
      ['循环片段数', d.loopCount],
    ]);
    setStatus('cfg-status', `已基于顺序图 .oom 估算控制流复杂度，当前消息数 ${d.messages.length}，对象数 ${d.participants.length}。`);
    return;
  }

  if (!state.raw || !Array.isArray(state.raw.methods)) {
    setStatus('cfg-status', '没有可用 metrics 数据，请先加载 metrics.json 或顺序图 .oom。', true);
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

  const methods = state.raw.methods.filter((item) => item.className === className).sort((a, b) => String(a.methodName).localeCompare(String(b.methodName)));
  const method = methods[Number(methodIndex)];
  if (!method) {
    setStatus('cfg-status', '未找到对应方法，请重新选择。', true);
    return;
  }

  renderCards('cfg-cards', [
    ['圈复杂度 CC', ensureNumber(method.cyclomaticComplexity, 1)],
    ['决策点总数', Math.max(0, ensureNumber(method.cyclomaticComplexity, 1) - 1)],
    ['方法 LOC', ensureNumber(method.loc)],
  ]);
  setStatus('cfg-status', `已读取 ${className}#${method.methodName} 的复杂度数据。`);
}

function countLocFromText(text) {
  const lines = text.split(/\r?\n/);
  let blank = 0;
  let comment = 0;
  let code = 0;
  let inBlock = false;

  lines.forEach((raw) => {
    const line = raw.trim();
    if (!line) {
      blank += 1;
      return;
    }
    if (inBlock) {
      comment += 1;
      if (line.includes('*/')) inBlock = false;
      return;
    }
    if (line.startsWith('//')) {
      comment += 1;
      return;
    }
    if (line.startsWith('/*')) {
      comment += 1;
      if (!line.includes('*/')) inBlock = true;
      return;
    }
    if (line.includes('/*')) {
      code += 1;
      if (!line.includes('*/')) inBlock = true;
      return;
    }
    code += 1;
  });

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
  setStatus('loc-status', '已基于输入文本完成 LoC 统计。');
}

function useProjectLoc() {
  if (!state.raw?.loc) {
    setStatus('loc-status', '当前没有可读取的规模数据。', true);
    return;
  }
  const loc = state.raw.loc;
  renderCards('loc-cards', [
    ['总行数/总规模', ensureNumber(loc.totalLines)],
    ['空行', ensureNumber(loc.blankLines)],
    ['注释行', ensureNumber(loc.commentLines)],
    ['有效结构量', ensureNumber(loc.codeLines)],
  ]);
  setStatus('loc-status', `已读取当前数据源的规模信息：${state.sourceName}`);
}

function toXml(rows) {
  const lines = ['<?xml version="1.0" encoding="UTF-8"?>', '<classMetrics>'];
  rows.forEach((row) => {
    lines.push(`  <class name="${escapeHtml(row.name)}">`);
    lines.push(`    <WMC>${row.wmc}</WMC>`);
    lines.push(`    <DIT>${row.dit}</DIT>`);
    lines.push(`    <NOC>${row.noc}</NOC>`);
    lines.push(`    <CBO>${row.cbo}</CBO>`);
    lines.push(`    <RFC>${row.rfc}</RFC>`);
    lines.push(`    <LCOM>${row.lcom.toFixed(2)}</LCOM>`);
    lines.push(`    <NOP>${row.nop}</NOP>`);
    lines.push(`    <NOM>${row.nom}</NOM>`);
    lines.push(`    <NOO>${row.noo}</NOO>`);
    lines.push(`    <POD>${row.pod.toFixed(2)}</POD>`);
    lines.push(`    <OverrideRatio>${row.overrideRatio.toFixed(2)}</OverrideRatio>`);
    lines.push(`    <OverloadRatio>${row.overloadRatio.toFixed(2)}</OverloadRatio>`);
    lines.push(`    <SK>${row.sk.toFixed(2)}</SK>`);
    lines.push(`    <DAC>${row.dac}</DAC>`);
    lines.push(`    <MOA>${row.moa}</MOA>`);
    lines.push(`    <CAM>${row.cam.toFixed(2)}</CAM>`);
    lines.push(`    <CIS>${row.cis}</CIS>`);
    lines.push(`    <AIF>${row.aif.toFixed(2)}</AIF>`);
    lines.push(`    <MIF>${row.mif.toFixed(2)}</MIF>`);
    lines.push('  </class>');
  });
  lines.push('</classMetrics>');
  return lines.join('\n');
}

function download(name, content, type) {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function applyMetrics(data, sourceName) {
  state.raw = data;
  state.rows = deriveRows(data);
  state.sourceName = sourceName;
  if (state.selectedClass && !state.rows.some((row) => row.name === state.selectedClass)) {
    state.selectedClass = null;
  }

  renderSummary();
  renderCapabilityGuide(data);
  populateCfgSelectors();
  renderLegend();
  renderRadar();
  renderTable();

  // Keep the visible metric panels in sync when a new project is uploaded.
  autoEstimateFunctionPoint();
  autoEstimateUseCasePoint();
  useProjectLoc();
  switchTab(state.activeTab);

  const classCount = state.rows.length;
  const typeName = {
    'class-oom': '类图 .oom',
    'usecase-oom': '用例图 .oom',
    'sequence-oom': '顺序图 .oom',
  }[data.inputType] || 'metrics.json';

  setStatus('chartStatus', `已加载 ${typeName} 数据，当前可展示类级对象 ${classCount} 个。`);
  setStatus('rightStatus', '数据已更新。你可以切换到对应度量模块继续自动估算或手工修正。');
}

function bindEvents() {
  byId('menuNav').querySelectorAll('.menu-btn').forEach((btn) => {
    btn.addEventListener('click', () => switchTab(btn.dataset.key));
  });

  byId('uploadBtn').addEventListener('click', () => byId('fileInput').click());
  byId('projectUploadBtn')?.addEventListener('click', () => byId('projectDirInput')?.click());

  byId('fileInput').addEventListener('change', async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    state.uploadedFile = file;
    byId('fileName').textContent = `已选择：${file.name}`;

    try {
      const text = await file.text();
      const lowerName = file.name.toLowerCase();
      if (lowerName.endsWith('.json')) {
        applyMetrics(JSON.parse(text), file.name);
        setStatus('rightStatus', `已成功解析 ${file.name}`);
        return;
      }
      if (lowerName.endsWith('.oom')) {
        applyMetrics(parseOomFile(text, file.name), file.name);
        setStatus('rightStatus', `已成功解析 ${file.name}，并按图类型接入相应度量模块。`);
        return;
      }
      throw new Error('仅支持 .json 与 .oom 文件');
    } catch (err) {
      setStatus('rightStatus', `文件解析失败：${err.message}`, true);
    }
  });

  byId('projectDirInput')?.addEventListener('change', async (event) => {
    const files = event.target.files;
    if (!files || !files.length) {
      return;
    }

    byId('fileName').textContent = `已选择 Java 项目文件夹，共 ${files.length} 个文件`;
    setStatus('rightStatus', '正在上传 Java 项目并调用本地分析接口，请稍候...');

    try {
      const result = await analyzeJavaProjectFiles(files);
      applyMetrics(result.metrics, `${result.projectName} (frontend-upload)`);
      setStatus('rightStatus', `已完成 Java 项目分析：共上传 ${result.fileCount} 个文件，并已自动生成前端展示数据。`);
    } catch (err) {
      setStatus('rightStatus', `Java 项目分析失败：${err.message}。请确认已先启动本地服务：mvn exec:java "-Dexec.args=serve --port 9090"`, true);
    } finally {
      event.target.value = '';
    }
  });

  byId('startBtn').addEventListener('click', async () => {
    if (state.raw) {
      renderRadar();
      renderTable();
      setStatus('rightStatus', '当前数据已刷新。');
      return;
    }
    try {
      applyMetrics(await fetchDefaultMetrics(), '/out/metrics.json');
      setStatus('rightStatus', '已从默认路径读取并完成分析。');
    } catch (err) {
      setStatus('rightStatus', `分析失败：${err.message}`, true);
    }
  });

  byId('exportXmlBtn').addEventListener('click', () => {
    if (!state.rows.length) {
      setStatus('rightStatus', '当前没有可导出的类级数据。', true);
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
  byId('ai-run')?.addEventListener('click', runAiAnalysis);

  window.addEventListener('resize', () => {
    if (state.activeTab === 'oo' && state.rows.length) renderRadar();
  });
}

async function init() {
  hydrateStaticCopy();
  ensureProjectUploadControls();
  ensureInfoBlocks();
  renderCapabilityGuide(null);
  bindEvents();
  calcFunctionPoint();
  calcUseCasePoint();
  calcCfgComplexity();
  calcLocFromInput();
  setStatus('chartStatus', '当前为初始化状态，尚未加载任何度量数据。');
  setStatus('rightStatus', '请上传 metrics.json、.oom 文件，或点击“上传 Java 项目”开始分析。');
  renderSummary();
  renderLegend();
  renderRadar();
  renderTable();
  switchTab('fp');
}

init();
