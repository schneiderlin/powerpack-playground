:page/title "Beads：Agent 记忆的外部化"
:page/description "分析 Beads 工具的实际使用问题，讨论外部记忆与内部 prompt 的权衡"
:page/date "2026-02-01"
:blog-post/tags [:agent :AI :tools]
:blog-post/author {:person/id :jan}
:page/body

## Beads 的核心理念

Beads 是一个面向 AI coding agent 的分布式、git-backed graph issue tracker。它的核心价值是让 agent 的 session 可以随时关闭，下次可以从 bd 数据中找回工作进度。

所有 issues 以 JSONL 格式存储在 `.beads/` 目录中，享受 git 的版本控制特性。这意味着：
- 任务状态可以被版本化、分支、合并
- 多 agent 或多分支协作时不会冲突（使用 hash-based IDs 如 `bd-a1b2`）
- 本地有 SQLite 缓存提升性能

## 实际使用中的问题

但在实际使用中，我发现 Beads 存在一些问题：

### Agent 不会主动使用

Agent 经常不会主动使用 `bd` 命令。即使初始化了 Beads，agent 也只是在内存中维护自己的 todo list，而不去同步到外部状态。

即使我在 prompt 中明确写了"使用 'bd' for task tracking"，agent 仍然需要人类明确提醒："创建一个 bd issue"。

### 状态更新不及时

Agent 经常忘记 close issue。即使任务完成了，它也不会自动更新 bd 的状态。

这个问题导致：
- 即使能从 bd 中恢复进度，但恢复的状态可能不准确
- 依然会丢失一部分工作上下文
- 需要人类不断提醒，失去了自动化的价值

## 工具 vs Prompt

Beads 本质上是一个工具，而不是 prompt。它的设计哲学是：提供外部化的持久存储，让 agent 可以随时重启而不丢失进度。

但这个假设有一个前提：agent 会主动维护外部状态。如果 agent 不愿意主动使用这个工具，那外部存储的价值就大打折扣。

## 替代思路：调优 Prompt

如果 agent 不愿意主动使用外部工具，一个可能的替代方案是：调优 prompt，教 agent 如何：
- 给任务做切分
- 写 todo
- check todo status

具体来说：

### 任务切分策略

不同类型的任务可能需要不同的切分策略：

| 任务类型 | 切分策略 |
|---------|---------|
| 代码重构 | 按文件/模块切分 |
| 新功能开发 | 按依赖顺序切分 |
| bug 修复 | 按调查→定位→修复切分 |
| 文档写作 | 按大纲结构切分 |

### Todo 的生命周期管理

教 agent 在以下时机维护 todo：
- 初始化任务：创建 todo list
- 开始子任务：标记为 in_progress
- 完成子任务：标记为 completed
- 遇到阻塞：添加 blockers
- 产生新发现：添加新 todo

### Prompt 微调

针对不同任务类型，可以有不同的 prompt 模板：

```
代码重构场景：
- 优先分析代码依赖关系
- 按依赖顺序制定重构计划
- 每完成一步立即更新 todo

文档写作场景：
- 先生成大纲作为骨架
- 按大纲逐步填充内容
- 定期检查逻辑一致性
```

## 外部记忆的价值

但这不意味着外部记忆没有价值。在以下场景中，Beads 依然有用：

### Sleep Time Compute

在人类睡觉时运行的 agent，可能需要跨越多个 session。此时外部存储的价值就体现出来了。

### 多人协作

如果有多个 agent 或人类同时工作，外部存储可以作为共享的"黑板"。

### 长期项目追踪

对于跨越数周的项目，Beads 可以作为项目的"活文档"，记录每个决策的上下文。

## 理想的状态管理

理想的状态管理应该是：
- Agent 能主动维护外部状态（无论是 Beads 还是其他工具）
- 在关键节点自动 checkpoint
- 支持增量同步，而不是全量刷新

关于长时间运行 agent 的设计原则，见 [长时间运行的 Agent：设计原则与理论框架](./long-running-agents.md)。
