# AI & Agent 开发

## Agent 设计问题
- Meta 瓶颈问题：如何识别要解决的问题，如何让 Agent 从多个角度主动提问
  - 见 [agent-questioning](./agent-questioning.md)

- 找到旧问题和新知识之间的关联：有时经过一段时间后，人类在其他领域学习了新知识/解决方案，可以桥接到旧问题。如何找到这些关联？
  - 见 [agent-questioning](./agent-questioning.md)

- 知识发现 Agent：如何让 AI 在循环中执行，爬取网上的新知识、新博客，和问题清单对比，如果有关联则简单总结并记录连接，让用户有空时详细看
  - 无解决方案引用

- Agent 自我分析产生"显然/无用"的洞察而非可行动、具体的改进；如何识别特定任务模式和工具缺口
  - 见 [agent-self-reflection-trace-analysis](./agent-self-reflection-trace-analysis.md)

- 验证和过拟合问题：如何验证改进建议是否有效？如何避免过拟合历史数据？如何处理新任务类型？
  - 见 [agent-self-reflection-trace-analysis](./agent-self-reflection-trace-analysis.md)

- AI Agent 不主动使用外部记忆工具（Beads），忘记更新任务状态，失去外部状态收益
  - 见 [beads-agent-memory](./beads-agent-memory.md)

- 代码分析工具限制：文件搜索（如 find-usages）效率低且不准确；需要 LSP 级别的语义分析来理解符号引用、类型信息和代码结构，而非简单的字符串匹配
  - 无解决方案引用

## 长期运行 Agent
- 如何有意义地消费 AI 订阅配额；设计有适当防护的 Agent 避免随时间漂移
  - 见 [long-running-agents](./long-running-agents.md)

- 输出形式的权衡决策：长期运行 Agent 的输出应该是什么形式？仅结论（简洁但如果错了就全没了）还是包含中间过程（对人类更有用但审查成本更高）？
  - 见 [long-running-agents](./long-running-agents.md)

# 工具 & 基础设施

## 增量 UI
- 缺少从模型到 IR 的增量更新（React 有 v-dom 到 DOM，但没有模型到 v-dom）。这更难，因为模型和 IR 是用户定义的，不像 DOM 那样标准
  - 见 [incremental-UI](./incremental-UI.md)

# 内容管理

## 博客更新可见性
- 如果不断更新文章，早期读者会看到未完成的内容。稍后更新时，老读者没有有效的方式来"diff"更改，迫使他们重新阅读所有内容
  - 见 [blog-standard](./blog-standard.md)

## 隐藏草稿
- 需要实现自定义组件来隐藏草稿，选项为点击展开
  - 见 [blog-standard](./blog-standard.md)

# 学习系统

## 语言学习
- Duolingo 太简单，无法达到心流状态。需要游戏化以快速通过这个学习阶段
  - 见 [postmortem-bahasa-project1](./postmortem-bahasa-project1.md)

# 可视化

## 写作进度
- 写作进度的可视化图表（TODO，未实现）
  - 见 [writing-environment](./writing-environment.md)

## 编译器开发
- 需要插入图表来解释概念（特别是 De Bruijn 索引）（TODO，推迟到未来）
- AST 可视化（使用 d3，推迟到未来系列）
- 栈 VM 计算过程可视化（TODO，推迟到未来）
  - 见 [build-language-in-clojure](./build-language-in-clojure.md)

# 规划中的内容

## AI 评估系统
- 依赖导向回溯、迭代策略和案例研究等主题（大纲文档，尚未撰写）
  - 见 [ai-eval-blog-outline](../docs/ai-eval-blog-outline.md)
