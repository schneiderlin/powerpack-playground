```edn
:page/title "长时间运行的 Agent：设计原则与理论框架"
:page/description "探讨如何有意义地利用 AI subscription 多余额度，以及长时间运行 agent 的设计挑战"
:page/date "2026-01-31"
:blog-post/tags [:agent :AI :system-design]
:blog-post/author {:person/id :jan}
:page/body
```

## 背景

AI 模型厂商普遍提供 subscription 制度，通常每 5 小时刷新一次额度。在这种模式下，如果将 AI 作为 copilot 使用——即人类主导、每次只运行一个 agent（因为人类无法 multitask）——一般很难消耗完所有额度。

这引发了一个问题：是否有某种方式，可以有意义地消耗这些多余额度，同时又不占用太多人类时间？

## 可能的方向

### 1. 自我反思与优化

记录 LLM 的所有 interaction traces，然后让 agent 进行评价，找出成功和失败的 pattern，总结结果并写入文档。更进一步，可以自动化这个过程，直接修改 agent 的 prompt 或 instruction。

这种方法的核心是：让 agent 通过分析历史经验来改进自身。

### 2. 代码与文档的一致性维护

阅读代码和文档，保持它们的一致性。这可以解决文档经常落后于代码、被遗忘更新的问题。agent 可以查看 git 历史，识别出哪些文档已经过时。

这种做法有两个价值：
- 实际效用：保持文档的准确性
- 未来参考：这些文档对后续的 agent 工作有参考价值

## Sleep Time Compute

Agent 主导的长任务，在人类睡觉或工作时自主运行。这是长时间运行 agent 的典型应用场景。

### 特点

- 不需要人类持续监督
- 可以连续运行数小时
- 充分利用 AI subscription 多余额度

### 示例：文章质量自动化评审

自动评审代码库中的文章，用金字塔原理作为评分器生成改进建议。详细讨论见 [pyramid-principle-writing-eval](./pyramid-principle-writing-eval.md)。

这个任务非常适合 sleep time compute：
- 有明确的 guard rail（金字塔原理测试）
- 输出形式可控（只生成诊断报告，不直接修改原文）
- 可以批量处理，充分利用夜间时间
- 评审结果有实际价值

## 核心挑战

### Guard rail 的必要性

长时间运行的 agent 任务，必须有明确的 guard rail。否则：
- 随着时间推移、步骤增多，agent 越容易跑偏
- 可能导致后面的一串 token 是白费的
- 生成的结果没用
- 占用人类 review 的精力

### 输出形式的权衡

长时间运行 agent 的输出应该是什么形式？

**只保持结论**：
- 优点：简洁，直接给结果
- 缺点：如果结论错了，整个输出就废了

**包含中间过程**：
- 优点：即使结论错了，人类也能从过程中得到部分有用的东西；可以找到 agent 开始偏离的那个点，从那里重新开始
- 缺点：输出量大，人类 review 成本高

这是一个重要的设计决策。

## 理论框架

本文作为理论基础，可用于评测当前井喷式出现的各种 agent 工具。评测的核心问题是：

- 这个工具解决的是理论框架中的哪部分问题？
- 它是如何解决的？
- 它在 guard rail 和输出形式上做了什么权衡？

通过这个框架，可以更系统地理解不同 agent 工具的设计哲学和适用场景。
