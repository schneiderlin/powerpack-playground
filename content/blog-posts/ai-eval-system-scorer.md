:page/title "AI Eval System - 打分系统"
:page/description "在构建 AI Eval System 时，打分逻辑应该优先使用确定性方法（linter、type checker）。本文讨论了 LLM as judge 的递归问题和不确定性叠加，以及为什么确定性评分方法更具优势。"
:page/date "2026-01-31"
:blog-post/tags [:tooling :AI :evaluation :linter]
:blog-post/author {:person/id :jan}
:page/body

## 核心原则：优先使用确定性方法

在构建 eval system 时，一个关键的设计决策是：**如何实现打分逻辑？**

直觉上，既然我们在评估 AI 系统，用另一个 AI（LLM as judge）似乎很自然。但这可能是个陷阱。

## LLM as Judge 的问题

### 递归的无限倒退

LLM as judge 有个根本性问题：谁来 judge 这个 judge？

- 你的 LLM 可能给出错误的评分
- 评分标准本身可能模糊不清
- 同一个 judge 在不同时间、不同 prompt 下可能给出不同结果

于是你会想：再加一层 judge 去修正第一个 judge？

这就是递归：judge 1 → judge 2 → judge 3 → ...

最终你还是要回到 human review。问题是：

**这到底是让 human review 的负担变小了，还是让整个系统的不可控性增加了？**

### 不确定性的叠加

每个 LLM 调用都引入不确定性。当你在 eval system 中嵌套多个 LLM：

```
Task (LLM) → Output
     ↓
Scorer (LLM as Judge) → Score
     ↓
Aggregator (可能又是 LLM) → Final Result
```

每一层都放大了随机性。最终的结果难以解释、难以调试、难以改进。

### 代价 vs 收益

LLM as judge 不是没有用。它在某些场景下有价值：

- **无法用规则定义的标准**：比如"语气是否礼貌"、"是否具有同理心"
- **快速原型阶段**：当你还不知道要评什么时，让 LLM 帮你探索维度
- **辅助 human review**：而不是替代 human review

但要清醒认识到：这是在用不确定性换取灵活性，代价是系统变得难以调试和维护。

## 确定性评分方法的优势

相比之下，确定性的评分方法（deterministic functions）有几个核心优势：

### 1. 可解释性

```python
# Linter: 检查代码是否只使用了设计系统定义的颜色
def check_design_system_compliance(code, design_tokens):
    used_colors = extract_colors(code)
    violations = [c for c in used_colors if c not in design_tokens['colors']]
    return len(violations) == 0
```

如果评分不对，你清楚地知道原因。不需要"问 LLM 你为什么这么评"。

### 2. 可调试性

当 eval fail 时：

- LLM as judge：是 LLM 的理解问题？是 prompt 的问题？是模型的问题？难以定位。
- Deterministic linter：直接看哪条规则触发了，为什么触发，逻辑路径清晰。

这恰好符合 GTD 范式：**能快速定位错误，才能快速修复**。

### 3. 稳定性

同一个输入，永远得到同一个评分。这意味着：

- 可以做 regression test（评分不变 = 质量没退化）
- 可以比较不同版本的模型/代码
- 可以做 A/B test

### 4. 低成本

不用为每个 sample 调 LLM，一次 eval 可能省下几百甚至几千个 LLM call。

## 实际案例：Design System Linter

在 [design-system-linter](./design-system-linter.md) 中，我们用完全确定性的方式来评估 AI 生成代码的质量：

**问题**：AI 生成 UI 代码时，颜色、间距、字体可能不一致。

**解决方案**：定义允许清单（design tokens），然后用 linter 检查。

**评分逻辑**：
```python
def score_compliance(generated_code):
    # 提取所有颜色值
    colors = extract_color_values(generated_code)
    
    # 计算违规比例
    violations = sum(1 for c in colors if c not in ALLOWED_COLORS)
    compliance = 1 - (violations / len(colors))
    
    return compliance
```

这个评分器：
- 完全确定
- 可解释（违规率就是 1 - compliance）
- 可调试（直接看哪些值不在允许清单）
- 低成本（正则匹配，毫秒级）

## 评分维度的设计

当你的评分器是 deterministic 的，你会自然地思考：**到底要评什么？**

这反而是个好事。它会逼你把模糊的质量标准，分解成可量化的指标。

### 常见的确定性评分维度

| 维度 | 评分方式 | 适用场景 |
|------|---------|---------|
| Syntax 正确性 | Parse → 是否报错 | 代码、JSON、SQL |
| Schema 验证 | 对照 schema definition | 结构化输出 |
| 安全性检查 | 已知漏洞检测规则 | 代码、prompt injection |
| Factuality | 知识库中的事实对比 | 事实性问答 |
| 一致性 | 检查多个输出之间的冲突 | 多轮对话、长文生成 |
| Performance | 执行时间、资源消耗 | Agent、workflow |
| Design compliance | Linter 风格检查 | UI 代码、样式 |

### Type Checker 作为评分器

如果你的 LLM 输出的是代码，类型检查器就是天然的评分器：

```python
# TypeScript
def score_typescript(code):
    result = subprocess.run(['tsc', '--noEmit', code], capture_output=True)
    errors = parse_tsc_errors(result.stdout)
    return max(0, 1 - len(errors) / MAX_TOLERATED_ERRORS))
```

这比"让 LLM 评代码质量"要靠谱得多。

但需要注意：**强类型语言的表达力是有限的**。很多实际约束无法用类型表达，或者需要做大量类型体操。在 AI 生成代码场景下，灵活的验证机制（如 Clojure 的 spec、Malli）可能比严格的静态类型更实用。

关于这个思考的详细讨论，见 [strong-vs-dynamic-types-ai-codegen](./strong-vs-dynamic-types-ai-codegen.md)。

在 Clojure 中，我们用 Malli + clj-kondo 做类似的类型导出。具体实现细节见 [malli-cljkondo-type-export](./malli-cljkondo-type-export.md)。

## 混合策略：什么时候用 LLM as Judge？

不是说完全不能用 LLM as judge。合理的做法是：

1. **确定性优先**：能用 linter、rule-based 的，绝对不用 LLM
2. **LLM as 辅助**：让 LLM 帮你提取特征，但评分逻辑仍然是 deterministic
   - 例如：用 LLM 提取文本的情感倾向（positive/negative/neutral），然后用 deterministic 逻辑判断是否符合预期
3. **LLM as Judge 仅用于不可规则化的维度**：而且要做好 human review 的准备

## GTD 范式的应用

回到 GTD（Generate, Test and Debug）：

- **Generate**：先用 LLM as judge 快速验证 eval system 是否工作（"差不多对"）
- **Test**：用一些样本测试，观察评分是否合理
- **Debug**：分析评分不合理的原因，逐步替换成 deterministic 逻辑

这正是我们在实践中发现的有效路径。
