# CodeMetric Studio

CodeMetric Studio 是一个面向软件度量实验的课程级工具，支持：

- 后端 CLI 分析 Java 项目，生成 `metrics.json` 和 `metrics-report.md`
- 前端可视化查看功能点、用例点、面向对象、控制流、代码行等度量结果
- 前端直接上传 `.oom` 设计图文件
- 前端直接上传整个 Java 项目文件夹，并调用本地分析接口完成分析

---

## 1. 当前支持的输入方式

前端目前支持 3 类输入：

1. `metrics.json`
2. `.oom` 设计图文件
3. Java 项目文件夹

不同输入方式对应的用途不同。

| 输入类型 | 适用场景 | 可直接支持的度量 |
|---|---|---|
| `metrics.json` | 已经完成 Java 项目分析，想直接可视化查看结果 | 面向对象度量、控制流图度量、代码行度量，并可辅助自动估算功能点和用例点 |
| `.oom` 类图 | 已有 UML 类图，想做结构类度量 | 面向对象度量为主，可辅助做功能点启发式估算 |
| `.oom` 用例图 | 已有用例图，想做需求规模估算 | 用例图度量为主，可辅助做功能点启发式估算 |
| `.oom` 顺序图 | 已有顺序图，想看交互复杂度 | 控制流复杂度近似估算、结构规模统计 |
| Java 项目文件夹 | 想从源码直接分析 | 先分析生成内存中的度量结果，再在前端展示全部可视化模块 |

---

## 2. 前端页面如何使用

前端页面地址：

- `http://localhost:8080/web/`

页面打开后默认是初始化状态，不会自动读取已有的 `out/metrics.json`。

### 2.1 上传 `metrics.json`

适合已经通过 CLI 分析过 Java 项目的人。

操作步骤：

1. 点击前端右侧的“点击上传”
2. 选择 `metrics.json`
3. 页面会自动加载类级指标、方法复杂度、LoC 等信息

这类输入最完整，适合：

- 面向对象度量
- 控制流图度量
- 代码行度量
- 功能点自动估算
- 用例点自动估算

注意：

- 功能点和用例点的“自动估算”是启发式辅助，不等于完全替代人工判级

### 2.2 上传 `.oom` 文件

前端支持直接上传以下设计图：

- 类图 `.oom`
- 用例图 `.oom`
- 顺序图 `.oom`

#### 上传类图 `.oom`

适合做：

- 面向对象度量

页面会尝试提取：

- 类
- 属性
- 操作
- 类之间的关系

可得到的结果包括：

- 类级表格
- OO 雷达图
- 近似 CK/扩展 OO 指标

限制：

- 不能直接得到源码级圈复杂度
- 不能直接得到标准用例图度量
- 功能点只能做启发式估算，不是标准功能点做法

#### 上传用例图 `.oom`

适合做：

- 用例图度量

页面会尝试提取：

- Actor
- Use Case
- Actor 和 Use Case 的关联关系

可得到的结果包括：

- UAW
- UUC
- UUCP
- TCF / EF
- UPC

限制：

- 不能直接做面向对象度量
- 不能直接做源码级控制流复杂度
- 功能点只支持启发式映射估算

#### 上传顺序图 `.oom`

适合做：

- 控制流复杂度近似估算
- 交互规模统计

页面会尝试提取：

- 参与对象
- 消息数
- 循环片段

可得到的结果包括：

- 交互消息数
- 参与对象数
- 近似圈复杂度
- 图结构规模统计

限制：

- 不适合直接做标准功能点度量
- 不适合直接做标准用例图度量
- 不适合直接做完整的面向对象度量

### 2.3 上传整个 Java 项目文件夹

这是现在新增的功能。

适合场景：

- 不想先手工跑 CLI
- 希望直接在前端上传源码目录并马上看到分析结果

操作步骤：

1. 启动前端页面
2. 启动本地分析接口
3. 点击“上传 Java 项目”
4. 选择整个 Java 项目文件夹
5. 前端会读取其中的 `.java` 文件并发给本地接口分析
6. 分析结果会直接显示在页面上

这类输入分析完成后，前端可用于：

- 面向对象度量
- 控制流图度量
- 代码行度量
- 功能点自动估算
- 用例点自动估算

注意：

- 当前前端上传 Java 项目后，分析结果是直接返回前端展示的
- 默认不会自动保存成新的 `metrics.json`
- 也不会覆盖原来的 `out/metrics.json`

---

## 3. 前端里各度量模块是什么意思

### 3.1 功能点度量

功能点度量主要关心事务功能和数据功能。

页面中会看到这些指标：

- `EI`：External Input，外部输入
- `EO`：External Output，外部输出
- `EQ`：External Inquiry，外部查询
- `ILF`：Internal Logical File，内部逻辑文件
- `EIF`：External Interface File，外部接口文件
- `FTR`：File Type Referenced
- `DER`：Data Element Referenced
- `RET`：Record Element Type
- `DET`：Data Element Type
- `VAF`：Value Adjustment Factor

功能点模块支持：

- 手工输入并计算
- 基于 `metrics.json` 自动估算
- 基于 `.oom` 用例图/类图做启发式估算

### 3.2 用例图度量

页面会计算：

- `UAW`
- `UUC`
- `UUCP`
- `TCF`
- `EF`
- `UPC`

最适合的输入是：

- 用例图 `.oom`

也支持：

- 手工输入
- 基于 `metrics.json` 自动估算

### 3.3 面向对象度量

适合的输入：

- `metrics.json`
- 类图 `.oom`
- 前端上传的 Java 项目分析结果

主要展示：

- WMC
- DIT
- NOC
- CBO
- RFC
- LCOM
- 以及扩展 OO 指标

### 3.4 控制流图度量

支持 3 种方式：

1. 手工粘贴代码
2. 从 `metrics.json` 读取方法复杂度
3. 从顺序图 `.oom` 估算交互复杂度

### 3.5 代码行度量

支持：

- 手工粘贴代码统计 LoC
- 从 `metrics.json` 读取项目 LoC
- 从 `.oom` 读取图结构规模

---

## 4. 后端终端如何使用

## 4.1 环境要求

- Java 17+
- Maven 3.9+
- Python 3.x

## 4.2 编译项目

```bash
mvn clean package
```

## 4.3 使用 CLI 分析 Java 项目

```bash
mvn exec:java "-Dexec.args=analyze --path <你的Java项目路径> --out out --format all"
```

常用参数：

- `--path`：待分析 Java 项目根目录，必填
- `--out`：输出目录，默认 `./out`
- `--format`：输出格式，支持 `json`、`md`、`all`
- `--module`：仅分析某个子目录
- `--threshold`：阈值配置文件路径

示例：

```bash
mvn exec:java "-Dexec.args=analyze --path bad-uml-sample --out out --format all --threshold docs/threshold.sample.json"
```

输出文件：

- `out/metrics.json`
- `out/metrics-report.md`

## 4.4 启动前端静态页面

在项目根目录执行：

```bash
python -m http.server 8080
```

然后访问：

```text
http://localhost:8080/web/
```

## 4.5 启动前端上传 Java 项目所需的本地分析接口

如果你要在前端直接上传整个 Java 项目文件夹，需要先启动这个本地接口：

```bash
mvn exec:java "-Dexec.args=serve --port 9090"
```

启动后，前端会调用：

```text
http://127.0.0.1:9090/api/analyze-project
```

说明：

- 这个接口只在本机使用
- 前端上传 Java 项目时会把 `.java` 文件内容发给它
- 它会在临时目录中分析，再把 JSON 结果返回给前端
- 默认不会把结果持久化保存到 `out/metrics.json`

---

## 5. 推荐使用流程

### 方案 A：先分析再看结果

适合想保留结果文件的人。

1. 用 CLI 分析 Java 项目
2. 生成 `out/metrics.json`
3. 打开前端页面
4. 上传 `metrics.json` 或手动读取它

### 方案 B：前端直接上传 Java 项目

适合想快速看结果的人。

1. 启动前端静态页
2. 启动 `serve` 本地接口
3. 在前端点击“上传 Java 项目”
4. 直接选择整个 Java 项目文件夹

### 方案 C：只上传设计图

适合课程实验中先做 UML、再做度量演示的人。

1. 打开前端页面
2. 上传 `.oom` 文件
3. 根据图类型查看对应度量

---

## 6. 常见问题

### Q1：页面打开后为什么没有自动显示 `out/metrics.json`？

因为当前版本已经改成初始化状态，避免一打开页面就混入旧数据。

### Q2：前端上传 Java 项目后，会生成新的 `metrics.json` 吗？

当前不会。

现在的行为是：

- 后端临时分析
- 把 JSON 结果直接返回前端
- 前端直接展示
- 不覆盖原来的 `out/metrics.json`
- 也不自动新建新的 `metrics.json`

### Q3：为什么前端上传 Java 项目时提示接口失败？

通常是因为没有先启动本地分析接口。

请先执行：

```bash
mvn exec:java "-Dexec.args=serve --port 9090"
```

### Q4：为什么 `.oom` 上传后有些指标不能算？

因为不同图只包含不同层面的信息：

- 类图偏结构
- 用例图偏需求
- 顺序图偏交互

不是所有图都能直接支持所有度量方法。

### Q5：功能点和用例点的自动估算为什么和人工结果不完全一样？

因为当前自动估算是启发式辅助，适合实验展示和初步估算，最终结果仍建议人工复核。

---

## 7. 项目结构

```text
codemetric-studio/
  pom.xml
  README.md
  USER-GUIDE.md
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
    service/
  src/test/java/com/codemetricstudio/
  docs/
    PRD.md
    requirements-traceability.md
    threshold.sample.json
  object/
    *.oom
  bad-uml-sample/
```
