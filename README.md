# CodeMetric Studio

CodeMetric Studio 是一个面向 Java 项目的课程级软件度量平台，包含：

- 后端 CLI：自动扫描 Java 工程，计算 LoC、圈复杂度、CK 指标并输出报告
- 前端页面：提供功能点、用例点、面向对象、控制流、代码行等可视化与交互式度量

---

## 1. 当前已实现能力

### 后端 CLI（自动化）
- 递归扫描 Java 文件（自动过滤 `target`、`build`、`.git`）
- 计算 LoC：总行、空行、注释行、有效代码行
- 计算方法圈复杂度（`if/for/while/case/catch/&&/||/?`）
- 计算 CK 指标：`WMC`、`DIT`、`NOC`、`CBO`、`RFC`、`LCOM`
- **计算多态性指标：`NOP`、`NOM`、`NOO`、`POD`**
- **计算扩展 OO 指标：`SK`、`DAC`、`MOA`、`MFA`、`CAM`、`CIS`、`NSC`**
- 阈值告警（方法复杂度、类 WMC、类 CBO、多态度）
- 输出 `metrics.json` 与 `metrics-report.md`

### 指标口径（已按教学口径调整）

#### CK 核心指标
| 指标 | 全称 | 说明 |
|------|------|------|
| WMC | Weighted Methods per Class | 按类中定义的方法个数统计 |
| DIT | Depth of Inheritance Tree | 继承树深度 |
| NOC | Number of Children | 直接子类数量 |
| CBO | Coupling Between Objects | 耦合对象类数 |
| RFC | Response for a Class | 本类方法数 + 调用到的项目内其他方法数（去重） |
| LCOM | Lack of Cohesion of Methods | 类内方法与字段内聚缺失度 |

#### 多态性指标（课程补充）
| 指标 | 全称 | 说明 | 解读 |
|------|------|------|------|
| NOP | Number of Polymorphic Methods | 可被重写的方法数（非static、非private、非final） | 反映类的多态潜力 |
| NOM | Number of Overridden Methods | 实际被重写的方法数 | 反映多态的实际使用程度 |
| NOO | Number of Overloads | 重载方法数量 | 反映方法的灵活性 |
| POD | Polymorphism Degree | 多态度 = NOM/NOP | 0=未使用多态，1=完全多态 |
| **OverrideRatio** | **重写率** | **= 子类重写方法数 / 父类方法总数** | **反映子类对父类的覆盖程度** |
| **OverloadRatio** | **重载率** | **= 重载方法数 / 所有方法数** | **反映方法重载的使用程度** |

#### 扩展 OO 指标
| 指标 | 全称 | 说明 | 特点 |
|------|------|------|------|
| SK | Specialization Index | 特化指数 = NOC/DIT | 衡量类在继承层次中的特化程度 |
| DAC | Data Abstraction Coupling | 数据抽象耦合 | 衡量类的"抽象宽度"，反映与其他类的关联程度 |
| MOA | Measure of Aggregation | 聚合度量 | 衡量类的组合程度，作为实例变量的用户定义类型数 |
| MFA | Measure of Functional Abstraction | 功能抽象度量 | 外部方法调用/总调用，反映依赖反转倾向 |
| CAM | Computational Abstraction Metric | 计算抽象度量 | 衡量参数类型设计的复用程度 |
| CIS | Class Interface Size | 类接口大小 | public方法数量，反映类的复杂度 |
| NSC | Number of Static Methods | 静态方法数 | 反映过程式设计倾向 |

### 前端页面（交互 + 自动估算）
- **功能点度量**
  - 手工录入：事务功能（`FTR/DER`）、数据功能（`RET/DET`）自动判级
  - 一键自动估算：基于 `metrics.json` 启发式生成 EI/EO/EQ/ILF/EIF 分档计数
  - 结果：`UFP`、`VAF`、`调整后 FP`
- **用例图度量**
  - 支持 `UAW`、`UUC`、`UUCP`
  - 按公式计算：`TCF = 0.6 + 0.01*TFactor`，`EF = 1.4 - 0.03*EFactor`，`UPC = UUCP*TCF*EF`
  - 一键自动估算：基于 `metrics.json` 自动生成参与者/用例分档与 `TFactor/EFactor`
- **面向对象度量**
  - 读取 `metrics.json` 展示雷达图和类级明细
  - 支持 XML 导出
- **控制流图度量**
  - 手工模式：粘贴代码，近似统计决策点并计算 CC
  - 自动化A：基于 `metrics.json` 的类/方法下拉，一键读取方法 CC
- **代码行度量**
  - 手工模式：粘贴代码统计 LoC
  - 自动模式：一键读取项目 `metrics.json` 的 LoC

---

## 2. 环境要求

- Java 17+
- Maven 3.9+
- Python 3.x（用于本地静态页面服务）

---

## 3. 快速开始

### 3.1 编译项目

```bash
mvn clean package
```

### 3.2 运行 CLI 分析（示例：坏味道 UML 样例）

```bash
mvn exec:java "-Dexec.args=analyze --path bad-uml-sample --out out/bad-uml-analysis --format all --threshold docs/threshold.sample.json"
```

若是 PowerShell，推荐保持 `-Dexec.args=...` 整体加双引号。

### 3.3 启动前端页面

```bash
python -m http.server 8080
```

浏览器访问：

- `http://localhost:8080/web/`

默认读取：

- `http://localhost:8080/out/metrics.json`

---

## 4. CLI 用法

```bash
mvn exec:java "-Dexec.args=analyze --path <待分析项目路径> --out <输出目录> --format <json|md|all> [--module <子目录>] [--threshold <阈值JSON>]"
```

### 参数说明
- `--path <project_path>`：待分析项目根路径（必填）
- `--out <output_dir>`：输出目录（默认 `./out`）
- `--format json|md|all`：输出格式（默认 `all`）
- `--module <subdir>`：仅分析指定子目录
- `--threshold <config.json>`：阈值配置文件路径

### 阈值配置示例

```json
{
  "methodComplexity": 10,
  "classWmc": 50,
  "classCbo": 14
}
```

---

## 5. 结果文件说明

- `metrics.json`：完整机器可读度量结果（前端可直接加载）
- `metrics-report.md`：报告型输出（便于实验文档粘贴）

---

## 6. 内置样例

- `sample-java-project`：基础样例（小规模）
- `bad-uml-sample`：按 UML 结构构造的复杂样例（更适合展示风险与多指标）

示例（输出到前端默认路径）：

```bash
mvn exec:java "-Dexec.args=analyze --path bad-uml-sample --out out --format all --threshold docs/threshold.sample.json"
```

---

## 7. 常见问题

- 页面空白或无数据：确认通过 `python -m http.server 8080` 从项目根目录启动，并检查 `out/metrics.json` 是否存在。
- 控制流“自动读取”下拉为空：先加载/上传 `metrics.json`，再切换到控制流页签。
- 功能点/用例点自动估算结果偏差：当前为启发式自动估算，最终请按业务事实人工复核。

---

## 8. 项目结构

```text
codemetric-studio/
  pom.xml
  README.md
  web/
    index.html
    main.js
  src/main/java/com/codemetricstudio/
    cli/
    scanner/
    parser/
    metrics/
    aggregator/
    reporter/
    model/
    config/
    util/
  src/test/java/com/codemetricstudio/
  docs/
    PRD.md
    requirements-traceability.md
    threshold.sample.json
  sample-java-project/
  bad-uml-sample/
```
