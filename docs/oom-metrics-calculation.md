# OOM 度量计算审计文档

本文件用于“审计程序逻辑是否正确”，目标不是教材手工口径，而是**与当前代码执行结果一致**。

审计对象：

- `object/网上药店用例图.oom`
- `object/聊天系统类图.oom`

核心实现文件：

- `web/main.js`

---

## 1. 审计范围与结论先行

- **结论1：** 当前前端已支持直接解析 `.oom`，不是只能读 `metrics.json`。
- **结论2：** 用例点、功能点、类图 OO 指标均存在“启发式/近似”设计，属于程序策略，不是错误。
- **结论3：** 你新增的用例 `Comment` 中 `TYPE/FTR/DER/STORE/DET/RET` 已成为 FP 计算的优先输入来源。

---

## 2. 执行链路（按代码真实顺序）

1. 读取文件后进入 `parseOomFile()`。
2. `extractDiagramKind()` 判别类型：
   - `ClassDiagram` -> `parseClassDiagram()`
   - `UseCaseDiagram` -> `parseUseCaseDiagram()`
   - `SequenceDiagram` -> `parseSequenceDiagram()`
3. `applyMetrics()` 将结果放入 `state.raw`。
4. 自动触发：
   - `autoEstimateFunctionPoint()`
   - `autoEstimateUseCasePoint()`
   - `useProjectLoc()`

这意味着：你上传 `.oom` 后，页面会自动填充并计算，不需要额外手工先算一次。

---

## 3. 用例图度量逻辑审计（UCP）

## 3.1 基础计数来源

在 `parseUseCaseDiagram()` 中：

- `actors` 来源：`Actor` 节点
- `useCases` 来源：`UseCase` 节点
- `associations` 来源：`UseCaseAssociation` 节点

并建立：

- `actorDegree`：actor 与 use case 的关联边数
- `useCaseDegree`：use case 与 actor 的关联边数

## 3.2 复杂度分档条件（代码原样）

- Actor：
  - `degree >= 4` => complex
  - `degree >= 2` => average
  - else => simple

- UseCase：
  - `score = useCaseDegree + ceil(name.length / 8)`
  - `score >= 5` => complex
  - `score >= 3` => average
  - else => simple

> 关键点：UseCase 分档不仅看连边，还看名称长度。这是代码中的显式策略。

## 3.3 UCP 公式

`calcUseCasePoint()`：

- `UAW = AS*1 + AA*2 + AC*3`
- `UUC = US*5 + UA*10 + UC*15`
- `UUCP = UAW + UUC`
- `TCF = 0.6 + 0.01*TFactor`
- `EF = 1.4 - 0.03*EFactor`
- `UPC = UUCP * TCF * EF`

## 3.4 自动填充策略

`autoEstimateUseCasePoint()`（usecase-oom 分支）：

- `AS/AA/AC` 与 `US/UA/UC` 直接用解析结果
- `TFactor = min(50, associations + useCaseComplex*2)`
- `EFactor = min(50, actorComplex*4 + useCaseComplex*3)`

---

## 4. 功能点逻辑审计（FP）

## 4.1 统一计算公式

`calcFunctionPoint()`：

- `UFP = EI(3/4/6) + EO(4/5/7) + EQ(3/4/6) + ILF(7/10/15) + EIF(5/7/10)`
- `AFP = UFP * VAF`

## 4.2 复杂度判定函数

- `txComplexity(type, ftr, der)`：事务复杂度（EI/EO/EQ）
- `dataComplexity(type, ret, det)`：数据复杂度（ILF/EIF）

## 4.3 usecase-oom 分支（本次重点）

`autoEstimateFunctionPoint()` 在 `state.raw.useCaseDiagram` 存在时：

1. 优先查找每个用例的 `Comment` 中是否有标签：
   - `TYPE`
   - `FTR`
   - `DER`
   - `STORE`
   - `DET`
   - `RET`
2. 若存在标签：
   - 用 `TYPE/FTR/DER` 计算事务分档
   - 若 `DET>0 且 RET>0`，再用 `STORE/DET/RET` 计算数据分档
3. 若没有标签：
   - 回退到旧的启发式映射（`EI <- useCase分档`, `ILF <- actor分档`）

这正是你要的“隐藏指标不再默认 0”的关键实现。

---

## 5. 类图 OO 逻辑审计

`parseClassDiagram()` 对类图 `.oom` 的口径是“前端近似模型”：

- `WMC = 操作数`
- `DIT = 0`（固定）
- `NOC = 0`（固定）
- `CBO = couplings.size`（来自属性类型/参数类型/返回类型中的非内建类型）
- `RFC = WMC + CBO`
- `LCOM = attrCount/opCount`（有属性且有操作时）

并派生方法指标（用于 FP 自动估算）：

- `cyclomaticComplexity = max(1, 1 + 参数个数 + 命名命中项)`
- `loc = max(3, 2 + 2*参数个数)`

> 因此，类图分支与 CLI(Java AST)分支的指标不可直接混为同一口径。

---

## 6. 当前两个文件的可复现结果

## 6.1 `网上药店用例图.oom`

按当前代码计算（包含你写入的 Comment 指标）：

- Actors = 3
- UseCases = 15
- Associations = 13
- `AS=0, AA=1, AC=2`
- `US=12, UA=3, UC=0`

UCP：

- `UAW=8`
- `UUC=90`
- `UUCP=98`
- `TFactor=13`
- `EFactor=8`
- `TCF=0.73`
- `EF=1.16`
- `UPC=82.99`

FP（VAF=1.00）：

- `UFP=88`
- `AFP=88.00`

## 6.2 `聊天系统类图.oom`

按当前代码计算：

- `classCount=10`
- `methodCount=72`
- `totalAttr=45`
- `relationships=18`（代码中的关系计数）

FP 自动估算（VAF=1.00）：

- `UFP=320`
- `AFP=320.00`

---

## 7. 正确性检查清单（你可逐条验）

- 上传用例图 `.oom` 后：
  - `uc` 页签应自动填 `AS/AA/AC/US/UA/UC`
  - `fp` 页签自动估算应显示“读取到带指标用例”的状态提示
- 删除某个用例 `Comment` 中 `FTR/DER` 后：
  - 对应事务应回退到启发式映射
- 将 `VAF` 改成 1.10：
  - `AFP` 必须等于 `UFP * 1.10`
- 类图 `.oom` 下：
  - `DIT/NOC` 在当前实现中应为 0（这是实现策略）

---

## 8. 你在答辩时可以这样解释

- “我们实现了两层逻辑：默认启发式 + 显式标签优先。  
  用例图缺少 FTR/DER/DET/RET 时走启发式；如果在 `.oom` 用例注释里给出这些值，程序会优先按真实指标计算。”
- “所以结果变化是可解释的：不是算错，而是输入信息完整度不同导致的计算分支不同。”

---

## 9. 功能点度量（FP）专项解释

这一节可以直接作为你答辩时“功能点模块”的讲解稿。

### 9.1 我们的输入是什么

程序支持两类输入进入功能点计算：

- **显式输入（优先）**：用例 `Comment` 中写 `TYPE/FTR/DER/STORE/DET/RET`
- **启发式输入（兜底）**：当显式输入缺失时，从用例分档或类/方法特征自动推断

结论：**显式输入优先级最高**，这保证了你可以把业务真实值“喂给程序”。

### 9.2 程序如何计算事务功能点

对每个事务（EI/EO/EQ）：

1. 读取 `TYPE/FTR/DER`
2. 调用 `txComplexity(type, ftr, der)` 判定复杂度（低/中/高）
3. 将该事务累计到对应桶，例如 `EI.low += 1`

事务权重：

- EI：低 3 / 中 4 / 高 6
- EO：低 4 / 中 5 / 高 7
- EQ：低 3 / 中 4 / 高 6

### 9.3 程序如何计算数据功能点

当用例里存在 `DET` 和 `RET` 时：

1. 读取 `STORE`（ILF/EIF）、`DET`、`RET`
2. 调用 `dataComplexity(store, ret, det)` 判定复杂度（低/中/高）
3. 将该数据功能累计到 `ILF/EIF` 桶

数据权重：

- ILF：低 7 / 中 10 / 高 15
- EIF：低 5 / 中 7 / 高 10

### 9.4 最终公式

- `UFP = EI + EO + EQ + ILF + EIF`（各自按低/中/高加权后求和）
- `AFP = UFP * VAF`

其中 `VAF` 由页面输入（默认 1.00）。

### 9.5 你可以现场举的一个例子

以用例“注册”为例（你已写入注释）：

- `TYPE=EI; FTR=2; DER=8; STORE=ILF; DET=6; RET=2`

程序会做两步：

1. 事务部分：`EI + (FTR=2, DER=8)` -> `EI.avg`（权重 4）
2. 数据部分：`ILF + (RET=2, DET=6)` -> `ILF.low`（权重 7）

所以这个用例对总 UFP 的贡献是：

- `4 + 7 = 11`

### 9.6 为什么这套解释是“代码一致”的

- 因为你不是在讲教材抽象流程，而是在讲你项目里真实的函数分支：
  - `autoEstimateFunctionPoint()`
  - `txComplexity()`
  - `dataComplexity()`
  - `calcFunctionPoint()`

这能保证“讲解逻辑”和“程序输出”一一对应。

