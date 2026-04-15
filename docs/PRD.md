# PRD：CodeMetric Studio（自动化软件度量工具）

## 1. 文档信息
- 项目名称：CodeMetric Studio
- 文档版本：v1.0
- 日期：2026-04-15
- 面向团队：4人课程项目组
- 项目目标：实现一个可运行的代码度量工具，覆盖课程要求中的核心度量模型，并可输出分析报告用于实验报告撰写。

## 2. 背景与目标
### 2.1 背景
课程要求开发一个中小型软件度量自动化工具，覆盖面向对象度量（CK/LK相关点）并可扩展传统度量（LoC、圈复杂度等）。

### 2.2 核心目标（Must）
- 支持对 Java 项目源码进行自动度量。
- 支持以下指标计算：
  - 代码行：LoC（文件级、类级、项目级）
  - 圈复杂度：函数/方法级
  - CK 核心指标：WMC、DIT、NOC、CBO、RFC、LCOM（最小集合）
- 输出：
  - 控制台摘要
  - JSON 结果文件
  - Markdown 报告（可直接贴入课程实验报告）

### 2.3 扩展目标（Should）
- 提供简易 Web 可视化（本地页面）
- 支持按目录/模块筛选
- 支持阈值告警（如复杂度 > 10）

### 2.4 非目标（Out of Scope, v1）
- 不做云端部署与多租户
- 不做 IDE 插件
- 不做跨语言（v1仅 Java）

## 3. 用户与使用场景
### 3.1 目标用户
- 课程项目组成员
- 助教/教师（查看结果与报告）

### 3.2 关键场景
- 输入一个 Java 项目路径，自动产出度量结果。
- 对指定模块进行复测，比较改进前后指标。
- 导出实验报告附录数据。

## 4. 功能需求

## 4.1 功能清单
### F1 项目扫描
- 输入：本地项目路径
- 处理：递归扫描 `.java` 文件，过滤 `target/build/.git`
- 输出：扫描文件列表、扫描统计

### F2 语法解析
- 使用 JavaParser 构建 AST
- 提取：类、接口、方法、继承关系、调用关系、字段引用

### F3 指标计算
- LoC
  - 总行数、空行、注释行、有效代码行
- 圈复杂度
  - 方法级（if/for/while/case/catch/&&/|| 等决策点）
- CK 指标
  - WMC：类中方法复杂度总和
  - DIT：继承深度
  - NOC：子类数
  - CBO：耦合对象类数
  - RFC：可响应方法集合数量
  - LCOM：类内方法与字段内聚缺失度

### F4 报告生成
- JSON：完整机器可读结果
- Markdown：项目概览 + Top风险类 + Top风险方法 + 指标解释
- CLI 输出：关键摘要（总类数、平均复杂度、告警数量）

### F5 告警规则
- 默认阈值：
  - 方法圈复杂度 > 10
  - 类 WMC > 50
  - 类 CBO > 14
- 输出告警列表与建议

## 4.2 命令行需求
- `analyze --path <project_path> --out <output_dir>`
- `analyze --path <project_path> --format json|md|all`
- `analyze --path <project_path> --module <subdir>`
- `analyze --path <project_path> --threshold <config.json>`

## 5. 非功能需求
- 性能：10万行以内项目，分析时间 < 90 秒（普通笔记本）
- 正确性：核心指标单元测试覆盖率 >= 80%
- 可维护性：模块化分层、关键算法有注释
- 可用性：命令行失败时输出明确错误原因

## 6. 技术方案

## 6.1 技术栈
- 语言：Java 17
- 构建：Maven
- AST：JavaParser
- CLI：Picocli（可选）
- 报告模板：FreeMarker（可选）
- 测试：JUnit 5

## 6.2 系统架构
- `cli`：参数解析与任务调度
- `scanner`：文件扫描
- `parser`：AST 解析
- `metrics`：各指标计算器
- `aggregator`：项目级汇总
- `reporter`：JSON/Markdown 输出

数据流：
1. CLI 接收路径
2. Scanner 返回文件集
3. Parser 构建结构模型
4. Metrics 逐项计算
5. Aggregator 汇总与告警
6. Reporter 生成结果

## 7. 数据模型（建议）

```java
class ProjectMetrics {
  String projectName;
  int fileCount;
  int classCount;
  int methodCount;
  LocMetrics loc;
  List<ClassMetrics> classes;
  List<MethodMetrics> methods;
  List<Alert> alerts;
}

class ClassMetrics {
  String className;
  String packageName;
  int wmc;
  int dit;
  int noc;
  int cbo;
  int rfc;
  double lcom;
}

class MethodMetrics {
  String className;
  String methodName;
  int cyclomaticComplexity;
  int loc;
}
```

## 8. 输出格式

## 8.1 JSON（示例）
```json
{
  "projectName": "demo-app",
  "fileCount": 42,
  "classCount": 18,
  "methodCount": 126,
  "alerts": [
    {"type": "COMPLEXITY", "target": "UserService#calc", "value": 16, "threshold": 10}
  ]
}
```

## 8.2 Markdown 报告章节
- 项目概览
- 指标汇总表
- 高风险类 Top10
- 高风险方法 Top10
- 告警与改进建议
- 度量结论（可直接用于实验报告）

## 9. 项目结构（建议）

```text
codemetric-studio/
  pom.xml
  src/main/java/
    cli/
    scanner/
    parser/
    metrics/
    aggregator/
    reporter/
    model/
  src/test/java/
  docs/
    PRD.md
    metric-definitions.md
```

## 10. 开发计划（4人并行）
- 成员A（架构/组长）
  - 定义包结构、接口、主流程
  - 整合模块、输出最终报告模板
- 成员B（算法）
  - 实现 LoC、圈复杂度、WMC/RFC
- 成员C（模型）
  - 实现 DIT/NOC/CBO/LCOM
  - 构建类关系与调用关系
- 成员D（测试与交付）
  - 编写单元测试/集成测试
  - 准备示例项目、结果验证、打包脚本

里程碑：
- M1（D1-D2）：框架搭建 + 文件扫描 + AST 骨架
- M2（D3-D4）：核心指标可跑通
- M3（D5）：报告输出 + 告警规则
- M4（D6-D7）：测试、对比分析、文档终稿

## 11. 验收标准（DoD）
- 可对至少 1 个真实 Java 项目完整跑通分析
- 输出 `metrics.json` 与 `metrics-report.md`
- CK + LoC + 圈复杂度指标可复现且有测试证明
- 告警规则生效且可配置
- README 包含运行命令与样例结果

## 12. 风险与应对
- AST解析边界复杂：先覆盖常见语法，异常文件降级记录并跳过
- LCOM定义差异：在文档中固定算法版本并给示例
- 时间不足：优先保证 Must 功能，Web 可视化放到 Should

## 13. 直接可执行的开发任务（Issue 列表）
- [ ] 初始化 Maven 项目与目录结构
- [ ] 实现 `analyze` CLI 命令
- [ ] 实现 Java 文件扫描器
- [ ] 集成 JavaParser 并提取类/方法元信息
- [ ] 实现 LoC 计算器
- [ ] 实现圈复杂度计算器
- [ ] 实现 CK 指标计算器（WMC/DIT/NOC/CBO/RFC/LCOM）
- [ ] 实现聚合器与阈值告警
- [ ] 实现 JSON 报告输出
- [ ] 实现 Markdown 报告输出
- [ ] 补齐单元测试与集成测试
- [ ] 生成实验报告用图表与结论素材

## 14. 附：首版运行命令（目标）
```bash
mvn clean package
java -jar target/codemetric-studio.jar analyze --path /path/to/java-project --out ./output --format all
```
