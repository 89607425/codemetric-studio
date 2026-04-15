# CodeMetric Studio

CodeMetric Studio 是一个面向 Java 项目的自动化代码度量工具，满足课程项目的核心要求：LoC、圈复杂度、CK 指标（WMC/DIT/NOC/CBO/RFC/LCOM）、阈值告警与报告导出。

## 功能
- 递归扫描 Java 文件，自动过滤 `target`、`build`、`.git`
- 计算 LoC（总行/空行/注释行/有效代码行）
- 计算方法圈复杂度（if/for/while/case/catch/&&/||）
- 计算 CK 指标：WMC、DIT、NOC、CBO、RFC、LCOM
- 输出：控制台摘要 + `metrics.json` + `metrics-report.md`
- 支持模块分析与自定义阈值

## 环境
- Java 17+
- Maven 3.9+

## 运行

```bash
cd /Users/houjunyi/Downloads/codemetric-studio
mvn clean package
mvn exec:java -Dexec.args="analyze --path /path/to/java-project --out /path/to/output --format all"
```

## CLI 参数
- `--path <project_path>`：待分析项目路径（必填）
- `--out <output_dir>`：输出目录（默认 `./out`）
- `--format json|md|all`：输出格式（默认 `all`）
- `--module <subdir>`：仅分析指定子目录
- `--threshold <config.json>`：阈值配置文件

示例：

```bash
mvn exec:java -Dexec.args="analyze --path /Users/houjunyi/Downloads/chunfeng --module src/main/java --out /Users/houjunyi/Downloads/codemetric-out --format all --threshold /Users/houjunyi/Downloads/codemetric-studio/docs/threshold.sample.json"
```

## 阈值配置示例

```json
{
  "methodComplexity": 10,
  "classWmc": 50,
  "classCbo": 14
}
```

## 输出文件
- `metrics.json`：完整机器可读结果
- `metrics-report.md`：实验报告可直接粘贴的 Markdown 报告

## 本地前端可视化
1. 先生成分析结果到项目内 `out` 目录：

```bash
mvn exec:java -Dexec.args="analyze --path /Users/houjunyi/Downloads/chunfeng --out /Users/houjunyi/Downloads/codemetric-studio/out --format all"
```

2. 在项目根目录启动静态服务：

```bash
cd /Users/houjunyi/Downloads/codemetric-studio
python3 -m http.server 8080
```

3. 浏览器打开：
- `http://localhost:8080/web/`

页面默认读取 `http://localhost:8080/out/metrics.json`。

### 页面模块说明（对应课程实验步骤）
- `功能点度量`：支持 EI/EO/EQ/ILF/EIF + VAF，输出 UFP 与调整后 FP。
- `用例图度量`：支持参与者复杂度、用例复杂度、TCF、ECF，输出 UAW/UUCW/UUCP/UCP。
- `面向对象度量`：读取 `metrics.json` 中 CK 相关数据，展示雷达图与类级明细表，支持 XML 导出。
- `控制流图度量`：对输入代码片段统计决策点并计算圈复杂度（近似法）。
- `代码行度量`：支持输入代码文本统计 LoC，也可直接读取项目分析结果中的 LoC。

说明：
- 当前后端 CLI 主要针对 Java 源码进行自动化解析与 CK/LoC/复杂度计算。
- 前端中的 `功能点/用例图/控制流图` 支持按课程模型进行交互式计算，便于实验演示与报告截图。

## 项目结构

```text
codemetric-studio/
  pom.xml
  README.md
  web/
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
    threshold.sample.json
```
