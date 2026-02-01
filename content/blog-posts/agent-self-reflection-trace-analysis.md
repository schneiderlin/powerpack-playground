:page/title "Agent 自我反思：使用 Trace 分析改进"
:page/description "介绍如何使用 opencode 插件记录 LLM interaction traces，并让 agent 分析这些 traces 以发现模式和改进自身"
:page/date "2026-02-01"
:blog-post/tags [:agent :AI :evaluation :trace]
:blog-post/author {:person/id :jan}
:page/body

## 背景

在长时间运行的 agent 系统中，一个重要的改进方向是：记录 agent 的所有 interaction traces，然后让 agent 进行自我评价和分析，找出成功和失败的 pattern，总结经验并应用到未来的行为中。

这篇文章讨论这个想法的具体实现。

## Trace 记录格式

OpenCode 提供了用户级别的插件机制，可以自动记录每一次 LLM interaction。记录会保存在项目目录的 `.opencode/eval_dataset.jsonl` 文件中。

### EvalRecord 数据结构

每条记录（一行 JSON）包含以下字段：

```json
{
  "id": "ml0ugt86-aawtk3sg",
  "timestamp": "2026-01-30T12:13:32.022Z",
  "session_id": "ses_3f12d5a37ffel1OuutBtaMT2l9",
  "system_prompt": {
    "header": "...",
    "body": "...",
    "rules": ["rule1", "rule2"]
  },
  "user_message": "resources/public/css/styles.css added to git ignore...",
  "attached_files": ["path/to/file1", "path/to/file2"],
  "tools_snapshot": [...],
  "working_directory": "C:\\Users\\zihao\\Desktop\\workspace\\private\\powerpack-playground",
  "tags": ["code-generation", "debugging"],
  "response": {
    "content": "I'll remove the file from git history...",
    "tool_calls": [
      {
        "role": "assistant",
        "messageId": "msg_xxx",
        "tool": "bash",
        "callID": "call_xxx",
        "args": {"command": "git filter-branch..."},
        "result": "error: cannot lock ref..."
      }
    ],
    "usage": {"input_tokens": 1234, "output_tokens": 5678},
    "model": "gpt-4",
    "latency_ms": 23691
  }
}
```

### 关键字段说明

- **id**: 唯一标识符
- **timestamp**: 记录时间
- **session_id**: 对话会话 ID
- **system_prompt**: agent 使用的 system prompt（包含 header、body、rules）
- **user_message**: 用户输入
- **attached_files**: 附加的文件列表
- **tools_snapshot**: 可用工具列表
- **working_directory**: 工作目录
- **tags**: 自动检测的任务类型标签（如 code-generation、debugging）
- **response**: agent 的响应
  - **content**: 文本内容
  - **tool_calls**: 工具调用记录（包括输入参数和执行结果）
  - **usage**: token 使用量
  - **model**: 模型名称
  - **latency_ms**: 响应延迟

## 使用 Eval 插件记录 Trace

### 安装配置

Eval 插件已经放在 `C:\Users\zihao\.opencode\plugin\eval.ts`。OpenCode 会在启动时自动加载 `.opencode/plugin` 目录下的所有插件。

插件依赖的 npm 包会在首次加载时自动安装，无需手动处理。

### 插件工作原理

Eval 插件通过以下钩子记录每次交互：

1. **`experimental.chat.messages.transform`**: 在消息转换阶段捕获初始信息
   - 提取 system prompt
   - 提取 user message
   - 检测 tags
   - 提取附加文件

2. **`experimental.text.complete`**: 在完成阶段捕获完整响应
   - 提取响应内容
   - 提取 tool calls（包括参数和结果）
   - 记录使用量和延迟
   - 写入 `.opencode/eval_dataset.jsonl`

### Tag 自动检测

插件会根据 user message 的内容自动添加标签：

- `code-generation`: 消息包含代码块
- `debugging`: 包含 "fix" 或 "bug"
- `refactoring`: 包含 "refactor"
- `testing`: 包含 "test"

## Agent 自我分析 Prompt

要让 agent 分析 traces 并总结模式，可以使用以下 prompt：

```markdown
你是一个 AI 行为分析专家。你的任务是分析 LLM interaction traces，总结成功和失败的 pattern。

## 分析目标

1. 找出成功案例的共同特征
2. 找出失败案例的共同特征
3. 提出改进建议

## 成功标准

- 任务成功完成
- 没有重试或错误
- 用户没有提出修正
- 工具调用结果正确

## 失败标准

- 工具调用失败
- 用户要求修正
- 需要多轮对话才能完成
- 输出不符合预期

## 分析方法

1. 按任务类型分组（使用 tags 字段）
2. 对于每个任务类型：
   - 分析成功案例的共同特征（system prompt、工具使用顺序、响应模式等）
   - 分析失败案例的共同特征
3. 提出具体的改进建议

## 输出格式

对于每个任务类型，输出：

### 任务类型：{tag}

- **成功案例特征**:
  - System prompt 的模式：...
  - 工具使用模式：...
  - 其他特征：...
- **失败案例特征**:
  - System prompt 的模式：...
  - 工具使用模式：...
  - 其他特征：...
- **改进建议**:
  - 建议 1：...
  - 建议 2：...

### 总体建议

[跨任务类型的总体改进建议]

## 注意事项

- 关注 system prompt 和实际行为的关联
- 关注工具调用的顺序和参数选择
- 关注响应的长度和详细程度
- 不需要统计数字，关注定性分析
```

## 执行分析

### 让 Agent 分析

将 `.opencode/eval_dataset.jsonl` 文件的内容提供给 agent，并附上上面的分析 prompt。agent 会分析这些 traces 并输出改进建议。

分析结果保存到项目的 `tmp/` 目录，例如 `tmp/trace-analysis.md`，由人类 review。

### 分析输出示例

分析结果会以 markdown 格式输出，包含：

1. 按任务类型分组的模式识别
2. 成功和失败的特征总结
3. 具体的改进建议

输出示例：

```markdown
### 任务类型：code-generation

- **成功案例特征**:
  - System prompt 总是包含 "Use the available tools extensively"
  - 总是先使用 Read 工具查看相关文件
  - 响应长度平均 200 字
- **失败案例特征**:
  - System prompt 没有明确要求检查依赖
  - 直接开始写代码，没有先探索项目结构
  - 缺少错误检查
- **改进建议**:
  - 在 system prompt 中添加："Before writing code, explore the project structure"
  - 添加明确的错误处理要求
  - 添加依赖检查步骤
```

## 应用改进建议

### 自动化改进

理想情况下，可以让 agent 直接根据分析结果修改自己的 behavior：

1. 更新 system prompt
2. 调整工具使用策略
3. 修改 response 风格

### 人工 Review

输出保存在 `tmp/` 目录，由人类 review 后再决定是否应用改进建议。这种半自动化方式更安全。

## 局限性和挑战

### 数据质量

- 记录中的"成功"和"失败"需要人工标注或启发式判断
- 缺少用户满意度的显式反馈

### 分析复杂度

- 不同任务类型的差异很大，难以找到通用模式
- Context 的复杂性（项目结构、依赖关系）难以量化

### 改进循环

- 如何验证改进建议是否有效？
- 如何避免过度拟合历史数据？
- 如何处理新的任务类型？

## 未来方向

- 添加人类反馈机制（手动评分）
- 使用强化学习自动优化
- 实时在线学习和适应
- 跨项目知识迁移

## 实战应用

### 第一次分析：失败教训

在 2026-02-01，我第一次尝试用这个方法分析自己的 traces。结果很糟糕。

**失败的"成功特征"：**
- "多个工具配合完成" —— 废话，Agent 当然会用工具
- "提出方案后询问用户确认" —— 共识，不需要分析
- "提供完整文档" —— 这是基本要求

这些问题本质上是：
1. 说出了显而易见的事实
2. 没有深入到具体的行为模式
3. 没有识别出真正的痛点

### 第二次分析：人类反馈后的改进

用户指出了真正的问题："工具选错是对的，我可能需要根据不同任务提供更多任务 specific 的工具"。

重新分析后，我识别出了**5 个具体的任务模式**：

1. **Git 状态相关操作**：缺少执行前的状态检查工具
2. **技术栈不匹配检测**：缺少项目技术栈自动识别工具
3. **代码库深度探索**：缺少精确的调用链查询工具
4. **集成调试**：缺少快速 REPL 验证工具
5. **多轮配置迭代**：缺少配置验证和回滚工具

更重要的是，我为每个模式提出了**具体的工具需求**：

```clojure
;; 而不是通用的 bash，我需要：
git-status --what-if "git filter-branch"
detect-stack
call-chain :powerpack/render-page
repl-eval "(keys (ns-publics 'html5-walker.walker))"
test-config :load-namespace "powerblog.core"
```

### 关键洞察

这次实践暴露了一个重要问题：**Agent 自我分析容易陷入"说正确的废话"陷阱**。

LLM 倾向于：
- 输出通用、看起来合理的建议
- 避免深入到具体的、可操作的层面
- 满足于表面的模式识别

而真正有价值的分析应该：
- 识别细粒度的任务模式（不是"代码生成"这种粗分类）
- 指出具体的工具缺口（不是"用更多工具"这种模糊建议）
- 导向可操作的改进（而不是"注意工具选择"这种空泛提醒）

### 改进后的分析 Prompt

基于这次教训，优化后的 prompt 应该强调：

```markdown
## 避免说废话

❌ 不要说：
- "Agent 会使用多个工具"
- "在行动前先确认"
- "提供详细的说明"

✅ 应该说：
- "在 Git 相关任务中，缺少执行前的状态检查工具"
- "对于 ClojureScript 项目，需要技术栈检测工具，否则会误用 ESLint"
- "深度探索代码库时，需要调用链查询工具，而不是返回数千行无关代码"

## 聚焦于任务模式和工具缺口

1. 识别具体的任务模式（例如："Git 状态相关操作"、"技术栈不匹配检测"）
2. 对于每个模式，找出：
   - Agent 的实际行为
   - 缺少的工具或能力
   - 如果重来，需要什么工具
3. 提出具体的工具需求，而不是通用的改进建议
```

### 迭代循环

这次经历展示了完整的改进循环：

```
1. 记录 traces ✅
2. 初次分析 ❌ （说废话）
3. 人类反馈 🎯 （指出真正的问题）
4. 改进分析 ✅ （识别模式和工具缺口）
5. 应用改进 🔄 （需要后续实现新工具）
```

关键是：**人类反馈在这个循环中不可替代**。Agent 很难自动识别出自己在"说废话"。
