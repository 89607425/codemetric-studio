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
- **计算新增 OO 指标：`COA`、`Size1`、`MPC`、`AIF`、`MIF`**
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

#### 新增 OO 指标
| 指标 | 全称 | 说明 | 特点 |
|------|------|------|------|
| COA | Cohesion Among Methods | 类内方法内聚性 | 基于方法对共享字段的访问计算内聚程度 |
| Size1 | Class Size | 成员变量数 | 类的实例变量（字段）数量 |
| MPC | Methods per Class | 类的方法总数 | 本类方法 + 继承方法数 |
| AIF | Attribute Inheritance Factor | 属性继承因子 | 继承属性数 / 总属性数 |
| MIF | Method Inheritance Factor | 方法继承因子 | 继承方法数 / 总方法数 |

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

---

## 3. 一体化启动（推荐）

### 3.1 编译

```bash
mvn clean package
```

### 3.2 启动一体化服务（前端 + 后端 API）

```bash
mvn exec:java "-Dexec.args=serve --host 127.0.0.1 --port 8080 --web-root ./web"
```

浏览器访问：

- `http://127.0.0.1:8080/`

### 3.3 用户流程（无需先跑 CLI）

1. 在页面点击“选择源码目录”或“选择 Java 文件”
2. 可选填写类图/流程图说明、用例点/功能点输入与阈值
3. 点击“开始自动分析”
4. 页面直接展示：项目指标卡、设计输入度量、OO 雷达图、类级明细表
5. 一键下载 `metrics.json` 与 `metrics-report.md`

---

## 4. CLI 用法（保留）

```bash
mvn exec:java "-Dexec.args=analyze --path <待分析项目路径> --out <输出目录> --format <json|md|all> [--module <子目录>] [--threshold <阈值JSON>]"
```

### 参数说明
- `--path <project_path>`：待分析项目根路径（必填）
- `--out <output_dir>`：输出目录（默认 `./out`）
- `--format json|md|all`：输出格式（默认 `all`）
- `--module <subdir>`：仅分析指定子目录
- `--threshold <config.json>`：阈值配置文件路径

Web 服务参数：
- `serve --host <host>`：服务监听地址（默认 `127.0.0.1`）
- `serve --port <port>`：服务端口（默认 `8080`）
- `serve --web-root <dir>`：前端静态目录（默认 `./web`）

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

- 页面显示“无法连接后端服务”：请确认使用 `serve` 命令启动，而不是单独静态服务。
- 分析报“未检测到 .java 文件”：确认上传的是源码目录或 Java 文件，不是编译产物目录。
- 分析慢：浏览器会先读取并上传源码文本，建议优先上传 `src/main/java`。

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
