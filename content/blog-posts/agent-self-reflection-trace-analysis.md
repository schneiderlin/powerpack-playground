:page/title "Agent 自我反思：使用 Trace 分析改进"
:page/description "介绍如何使用 opencode 插件记录 LLM interaction traces，并让 agent 分析这些 traces 以发现模式和改进自身"
:page/date "2026-02-01"
:blog-post/tags [:agent :AI :evaluation :trace]
:blog-post/author {:person/id :jan}
:page/body

## 背景

在长时间运行的 agent 系统中，一个重要的改进方向是：记录 agent 的所有 interaction traces，然后让 agent 进行自我评价和分析，找出成功和失败的 pattern，总结经验并应用到未来的行为中。

这篇文章讨论这个想法的具体实现。

## Trace 分类框架

参考 [writing-eval](./writing-eval.md) 的确定性评分思路，trace 评分也应该先分类，每个类别有明确的评分标准。

### 分类维度

| 类别 | 示例任务 | 评分维度 |
|------|---------|---------|
| 编程-调试 | Bug 定位、错误排查、性能分析 | 诊断准确性、解决方案有效性 |
| 编程-新 feature | 函数实现、新功能开发 | 功能完整性、代码质量 |
| 编程-配置 | 环境配置、依赖管理、部署脚本 | 配置准确性、兼容性 |
| 编程-探索 | 理解代码结构、查找调用关系 | 探索范围、结果精确度 |

### 编程类评分：Magic Box vs Power Tool

#### 使用 Spectrum

```
Magic Box ←──────────── Power Tool
"build me a wedding site"    "@xxx.clj 里面都是纯函数，给他们生成单元测试"
```

#### 评分原则

把 agent 看作**外部 senior developer**，不熟悉项目代码和业务逻辑。

#### 分析范围

trace 分析不仅是评估 agent 表现，还要评估人类 instruction 的质量：

| 维度 | 评估对象 | Magic Box | Power Tool |
|------|---------|-----------|------------|
| 任务边界理解 | 人类 instruction + agent 行为 | 泛泛描述，边界模糊 | 明确目标文件、上下文 |
| 信息提供 | 人类 instruction | 假设 agent 理解背景 | 提供必要的上下文文件 |
| agent 行为 | agent 工具使用、上下文检索 | 泛泛搜索 | 精确定位相关内容 |

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

## 评分维度

| 维度 | 评分方式 | 适用场景 |
|------|---------|---------|
| Task 边界理解 | 人类 instruction + agent 行为分析 | 所有 trace |
| 信息提供质量 | 人类 instruction 提供的上下文文件 | 所有 trace |
| 执行准确性 | 工具调用结果分析 | 所有 trace |
| 输出质量 | User 反馈或自动验证 | 所有 trace |

## Agent 自我分析 Prompt

要让 agent 分析 traces 并总结模式，可以使用以下 prompt：

```markdown
你是一个 AI 行为分析专家。你的任务是分析 LLM interaction traces，总结成功和失败的 pattern。

## 分析目标

1. 找出成功案例的共同特征
2. 找出失败案例的共同特征
3. 提出改进建议

## 成功标准（编程类）

- 人类 instruction 清晰，提供必要上下文
- Magic Box → Power Tool spectrum 位置靠近 Power Tool 端
- 上下文检索精确，返回相关代码
- 输出可验证，提供测试或验证步骤

## 失败标准（编程类）

- 人类 instruction 泛泛，缺少必要上下文
- Magic Box → Power Tool spectrum 位置靠近 Magic Box 端
- 上下文检索泛泛，返回大量无关信息
- 输出不精确，需要多轮修正

## 分析方法

### 编程类分析

1. 按任务类型分组（使用 tags 字段）：调试、新 feature、配置、探索
2. 对于每个任务类型：
    - 评估人类 instruction 质量（是否提供必要上下文）
    - 评估 Magic Box vs Power Tool spectrum 位置
    - 分析成功案例的共同特征（人类 instruction、agent 行为）
    - 分析失败案例的共同特征
3. 提出具体的改进建议（人类 instruction 模式、工具需求）

## 输出格式

对于每个任务类型，输出：

### 任务类型：{tag}

- **Magic Box vs Power Tool 评估**:
  - 平均得分：X/1
  - 主要偏差维度：上下文检索 / 任务理解 / 输出质量
- **成功案例特征**:
  - 人类 instruction 模式：...
  - Agent 上下文检索策略：...
  - 任务边界理解：...
- **失败案例特征**:
  - 人类 instruction 问题：...
  - Agent 上下文检索问题：...
  - 任务理解问题：...
- **改进建议**:
  - 人类 instruction 模式：...
  - 工具需求：...

### 总体建议

[跨任务类型的总体改进建议]

## 注意事项

- 关注 Magic Box vs Power Tool spectrum 的位置
- 关注人类 instruction 质量：是否提供必要上下文
- 关注上下文检索：是精确定位，还是泛泛搜索
- 关注任务边界理解：是明确边界，还是泛泛提问
- 关注输出可验证性：是否提供测试或验证步骤
```

## 执行分析

### 让 Agent 分析

将 `.opencode/eval_dataset.jsonl` 文件的内容提供给 agent，并附上上面的分析 prompt。agent 会分析这些 traces 并输出改进建议。

分析结果保存到项目的 `tmp/` 目录，例如 `tmp/trace-analysis.md`，由人类 review。

### 分析输出示例

分析结果会以 markdown 格式输出，包含：

1. 按任务类型分组的模式识别
2. Magic Box vs Power Tool spectrum 评估
3. 工具缺口分析
4. 具体的改进建议

输出示例：

```markdown
### 任务类型：编程-调试

- **Magic Box vs Power Tool 评估**:
  - 平均得分：0.4/1
  - 主要偏差维度：上下文检索、任务理解
- **成功案例特征**:
  - 人类 instruction：提供错误日志、相关代码文件
  - Agent 上下文检索：使用 Grep 精确查找错误相关代码
  - 任务边界：明确只调试特定功能
- **失败案例特征**:
  - 人类 instruction：泛泛描述"有个 bug"
  - Agent 上下文检索：返回整个项目树，无关信息多
  - 任务边界：泛泛询问项目结构
- **改进建议**:
  - 人类 instruction 模式：提供错误日志、复现步骤、相关代码
  - 工具需求：error-diagnostic --log-file --stack-trace
```

## 应用改进建议

### 自动化改进

理想情况下，可以让 agent 直接根据分析结果修改自己的 behavior：

1. 更新 system prompt
2. 调整工具使用策略
3. 提供人类 instruction 模式建议

### 人工 Review

输出保存在 `tmp/` 目录，由人类 review 后再决定是否应用改进建议。这种半自动化方式更安全。

## 局限性和挑战

### 数据质量

- 记录中的"成功"和"失败"需要人工标注或启发式判断
- 缺少人类 instruction 质量的显式反馈
- Magic Box vs Power Tool spectrum 的位置需要主观判断

### 分析复杂度

- 不同任务类型的差异很大，难以找到通用模式
- Context 的复杂性（项目结构、依赖关系）难以量化
- 人类 instruction 和 agent 行为的关联难以精确建模

### 改进循环

- 如何验证改进建议（instruction 模式、工具需求）是否有效？
- 如何避免过度拟合历史数据？
- 如何处理新的任务类型？

## 未来方向

- 添加人类反馈机制（手动评分 trace 和 instruction 质量）
- 使用强化学习自动优化 instruction 模式和 agent 行为
- 实时在线学习和适应
- 跨项目知识迁移
- 建立 instruction 质量标准和最佳实践

## 实战应用

### 第一次分析：失败教训

在 2026-02-01，我第一次尝试用这个方法分析自己的 traces。结果很糟糕。

**失败的"成功特征"：**
- "使用多个工具完成" —— 废话，Agent 当然会用工具
- "提出方案后询问用户确认" —— 这没有说明任务边界的理解方式
- "提供完整文档" —— 这是基本要求，不是关键特征
- "逐步执行命令" —— 太泛泛，没有说明上下文检索的精准性

这些问题本质上是：
1. 说出了显而易见的事实
2. 没有识别 Magic Box vs Power Tool spectrum 的关键差异
3. 没有分析人类 instruction 的质量
4. 没有深入到具体的行为模式（上下文检索）

### 第二次分析：人类反馈后的改进

用户指出了真正的问题："工具选错是对的，我可能需要根据不同任务提供更多任务 specific 的工具"。

重新分析后，我识别出了**5 个具体的任务模式**，并评估了它们在 Magic Box vs Power Tool spectrum 上的位置：

1. **Git 状态相关操作**（Power Tool 得分：0.3）
   - 人类 instruction：泛泛描述"修改 git 历史"
   - Agent 上下文检索：返回大量无关 git 历史
   - 工具需求：git-status --what-if

2. **技术栈不匹配检测**（Power Tool 得分：0.2）
   - 人类 instruction：没有说明项目技术栈
   - Agent 行为：错误地用 ESLint 检查 ClojureScript
   - 工具需求：detect-stack

3. **代码库深度探索**（Power Tool 得分：0.4）
   - 人类 instruction：泛泛描述"理解这个项目"
   - Agent 上下文检索：返回数千行无关代码
   - 工具需求：call-chain --function-name

4. **集成调试**（Power Tool 得分：0.3）
   - 人类 instruction：缺少错误日志和复现步骤
   - Agent 行为：泛泛搜索，难以定位问题
   - 工具需求：repl-eval --expression

5. **多轮配置迭代**（Power Tool 得分：0.5）
   - 人类 instruction：没有提供当前配置状态
   - Agent 行为：配置错误后需要反复修改
   - 工具需求：test-config --validate

关键是：**每个模式都对应明确的人类 instruction 改进和工具缺口，而不是泛泛的"改进建议"**。

```clojure
;; 人类 instruction 应该：
"@xxx.md 里面有 git 状态，帮我删除某个文件的历史"
"这是一个 ClojureScript 项目，@xxx_module 有问题"
"@error-log.md 有错误日志，帮我定位问题"
"当前配置在 @config.edn，帮我验证"
```

### 关键洞察

这次实践暴露了一个重要问题：**Agent 自我分析容易陷入"说正确的废话"陷阱**。

LLM 倾向于：
- 输出通用、看起来合理的建议（如"使用多个工具"）
- 只分析 agent 行为，忽略人类 instruction 质量
- 满足于表面的模式识别

而真正有价值的分析应该：
- 识别细粒度的任务模式（调试、新 feature、配置、探索）
- 评估 Magic Box vs Power Tool spectrum 的位置
- 分析人类 instruction 质量（是否提供必要上下文）
- 指出具体的工具缺口（不是"用更多工具"这种模糊建议）
- 导向可操作的改进（新增特定工具，而不是"注意工具选择"）

**核心问题**：Power Tool 的本质不仅是 agent 行为，还包括人类 instruction 的质量。人类指令应该更偏向 Power Tool 用法，提供明确的目标文件、上下文信息，而不是泛泛的"build me a wedding site"。

### 改进后的分析 Prompt

基于这次教训，优化后的 prompt 应该强调：

```markdown
## 避免 Magic Box 式的废话

❌ 不要说：
- "Agent 会使用多个工具" —— 这是废话，不区分行为质量
- "在行动前先确认" —— 这没有说明任务边界的理解方式
- "提供详细的说明" —— 这没有说明输出可验证性
- "人类的 instruction 不够明确" —— 这太泛泛

✅ 应该说：
- "在 Git 相关任务中，人类 instruction 应该提供当前 git 状态文件"
- "对于调试任务，人类 instruction 应该提供错误日志和复现步骤"
- "深度探索代码库时，需要调用链查询工具，而不是返回数千行无关代码"

## 聚焦于任务模式和改进方向

1. 识别具体的任务模式（例如："编程-调试"、"编程-配置"）
2. 对于每个模式，评估 Magic Box vs Power Tool spectrum 位置
3. 对于每个模式，找出：
   - 人类 instruction 的改进方向
   - Agent 的实际行为（Magic Box 倾向 vs Power Tool 倾向）
   - 缺少的精确工具或能力
4. 提出具体的改进建议，而不是通用的建议
```

### 迭代循环

这次经历展示了完整的改进循环：

```
1. 记录 traces ✅
2. 初次分析 ❌ （只分析 agent，忽略人类 instruction）
3. 人类反馈 🎯 （指出应同时分析人类 instruction）
4. 改进分析 ✅ （识别任务模式、评估 instruction 质量）
5. 应用改进 🔄 （改进 instruction 模式、实现新工具）
```

关键是：**人类反馈在这个循环中不可替代**。Agent 很难自动识别出自己在"说废话"，也很难主动评估人类 instruction 的质量。
