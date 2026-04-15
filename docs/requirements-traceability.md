# 课程实验要求与实现对照

## 依据文档
- `docs/PRD.md`
- `/Users/houjunyi/Downloads/软件质量保证实验指导书与报告要求/0-软件质量保证-实验指导书-2026.doc`
- `/Users/houjunyi/Downloads/软件质量保证实验指导书与报告要求/2-软件质量保证-实验报告要求.docx`

## 对照结果

1. 面向对象度量（CK/LK相关）
- 已实现：CBO/WMC(映射CS)/DIT/NOC/RFC/LCOM + 方法圈复杂度 + LoC
- 代码位置：
  - `src/main/java/com/codemetricstudio/parser/JavaAstParser.java`
  - `src/main/java/com/codemetricstudio/aggregator/MetricsAggregator.java`
  - `src/main/java/com/codemetricstudio/metrics/*`

2. 代码行度量
- 已实现：总行、空行、注释行、有效代码行
- 代码位置：
  - `src/main/java/com/codemetricstudio/util/LocCounter.java`
  - `web/main.js`（页面交互统计）

3. 控制流图/复杂性度量
- 已实现：圈复杂度统计（if/for/while/case/catch/&&/||/?）
- 代码位置：
  - `src/main/java/com/codemetricstudio/metrics/CyclomaticComplexityCalculator.java`
  - `web/main.js`（页面交互统计）

4. 用例图度量
- 已实现：UAW/UUCW/UUCP/UCP 计算页
- 代码位置：`web/main.js` -> `calcUseCasePoint`

5. 功能点度量
- 已实现：EI/EO/EQ/ILF/EIF + VAF -> UFP/FP
- 代码位置：`web/main.js` -> `calcFunctionPoint`

6. 报告输出
- 已实现：`metrics.json` + `metrics-report.md` + 前端可视化页面
- 代码位置：
  - `src/main/java/com/codemetricstudio/reporter/JsonReporter.java`
  - `src/main/java/com/codemetricstudio/reporter/MarkdownReporter.java`
  - `web/index.html`

## 说明
- 课程文档中的“类图、用例图、流程图输入”已通过页面交互输入（或 JSON 结果输入）覆盖。
- `.oom` 文件当前前端仅做文件选择占位；若需从 `.oom` 自动解析类图，需要补充对应文件格式解析器。
