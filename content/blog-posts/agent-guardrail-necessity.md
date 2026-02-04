:page/title "Guardrail 的必要性"
:page/description "长时间运行的 agent 任务必须有明确的 guardrail，否则容易跑偏、浪费 token、增加人类 review 成本。"
:page/date "2026-02-04"
:blog-post/tags [:agent :AI :system-design]
:blog-post/author {:person/id :jan}
:page/body

本文讨论为什么长时间运行的 agent 任务需要 guardrail。更完整的背景与设计原则见 [长时间运行的 Agent：设计原则与理论框架](./long-running-agents.md)。

## Guardrail 的必要性

长时间运行的 agent 任务，必须有明确的 guardrail。否则：

- 随着时间推移、步骤增多，agent 越容易跑偏
- 可能导致后面的一串 token 是白费的
- 生成的结果没用
- 占用人类 review 的精力

Guardrail 的形式可以多样：例如明确的评分标准（如金字塔原理测试）、输出形式约束（只生成诊断报告、不直接修改原文）、或测试中的具体错误提示（如「maybe try :partial true?」）——核心是给 agent 或人类明确的边界与修正方向，避免盲目试错。
