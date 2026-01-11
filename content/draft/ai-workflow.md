## AI Workflow: /research /create_plan /implement_plan

这三个命令来自 [humanlayer.dev](https://www.humanlayer.dev/) 的 GitHub 仓库,是一套组织 AI 辅助开发的工作流。

核心理念: **每个环节的输出都是完整的、独立的**。下一个环节只需要读取上一个环节的最终输出,不需要知道中间的出错过程和用户如何介入修复。这样可以避免 context rot。

## 问题切分

传统的一问一答式 AI 对话有个问题: 对话越长,context 越多,但有用的信息比例越低。中间的试错、调试、用户修正,这些过程对最终结果有贡献,但对下一步 AI 来说是噪音。

把任务切分为三个阶段:

1. **Research** - 收集信息,理解现状
2. **Create Plan** - 设计方案
3. **Implement** - 执行方案

## 三个命令

### /research

收集关于某个问题的信息,结果存放在 `thoughts/` 目录。

详细说明: [[research-prompt]](research-prompt.md)

**输出**: 完整的调研文档,自包含,可独立阅读。

### /create_plan

基于 research 的结果,创建详细的实施计划。

详细说明: [[create-plan-prompt]](create-plan-prompt.md)

**输出**: 完整的实施计划,包含要改的文件、改动内容、验证步骤。

### /implement_plan

执行计划,写代码。

详细说明: [[implement-plan-prompt]](implement-plan-prompt.md)

**输出**: 代码改动 + 验证结果。

## 人类介入的优先级

**用户必须认真阅读 research 的输出,确认没问题再进入下一步。**

错误是放大的:
- 1 行错误的 research → 100 行错误的 plan
- 1 行错误的 plan → 100 行错误的 code

越早发现问题,成本越低。用户的精力应该放在最影响质量的地方:

```
research > plan > implement
```

- Research 阶段要最仔细,理解错了后面全错
- Plan 阶段要审查方案合理性和完整性
- Implement 阶段相对机械,出问题容易修复

## 避免 Context Rot

Context rot 是指: 随着对话进行,context 中积累了大量历史信息,但新信息的质量在下降。

通过这三个命令的切分:

| 阶段 | 输入 | 输出 | 中间过程 |
|------|------|------|----------|
| /research | 用户问题 | 调研文档 | 丢弃 |
| /create_plan | 调研文档 | 实施计划 | 丢弃 |
| /implement | 实施计划 | 代码改动 | 丢弃 |

每个阶段只保留**最终输出**,中间的试错、讨论、修正都丢弃。

下次需要修改时:
- 修改计划文件,重新 implement
- 或重新 research,更新理解

不需要回顾之前的对话历史。

## 组合使用

典型 workflow:

```
/research 理解现有的错误处理机制
... 阅读 thoughts/error-handling.md ...
/create_plan 添加统一的错误日志
... 审阅 thoughts/shared/plans/error-logging.md ...
/implement_plan thoughts/shared/plans/error-logging.md
```

每个环节都是**独立的、可重入的**。

## 人类在哪介入

每个阶段后都要审查:

- Research 后: **仔细审阅**调研文档,补充遗漏点
- Plan 后: 审阅计划,调整方案
- Implement 后: 手动修复 edge case,更新计划,重新 implement

介入的产物是**文档**,不是对话历史。

## 和传统对话的区别

传统对话:
```
User: 帮我加 OAuth
AI: 好的,先看一下现有代码... 发现了 AuthController...
User: 不对,认证逻辑在 AuthService 里
AI: 明白,那我们在 AuthService 加...
... (几十轮来回)
```

三阶段:
```
/research 认证模块
--> thoughts/auth-module.md (完整文档)

/create_plan 添加 OAuth
--> thoughts/shared/plans/oauth.md (完整计划)

/implement_plan thoughts/shared/plans/oauth.md
--> 代码改动
```

如果有问题,修改计划文件,重新 implement。不需要回顾之前 AI 走了哪些弯路。

## 状态外置

对话历史是 transient state,容易丢失。

文档和代码是 persistent state,是"真理的来源"。

三阶段 workflow 把状态外置到文件系统:
- `thoughts/` - 知识
- `thoughts/shared/plans/` - 计划
- 代码 - 实现

AI 每次执行只读取需要的文件,不依赖历史对话。

## 可审计性

每个阶段的产出都是可读的文档:
- Research 文档可以当作项目的知识库
- Plan 文档可以当作技术设计文档
- Implement 的改动就是 git commit

人类可以 review 这些文档,AI 也可以基于这些文档继续工作。

## 总结

/research /create_plan /implement_plan 的核心思想:

1. **切分** - 把大任务分解为独立的阶段
2. **输出完整** - 每个阶段的输出是自包含的文档
3. **丢弃过程** - 中间过程不传递给下一阶段
4. **状态外置** - 用文件而不是对话历史存储状态

这样 AI 辅助开发就可以像传统开发一样,每个步骤的输入输出都是清晰的、可审查的、可重入的。
