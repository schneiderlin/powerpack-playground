:page/title "AI Eval System - 引言"
:page/description "AI Eval System 是用来系统性地测试和评估 AI 系统质量的框架。本文介绍了 Eval System 的核心问题、传统方法的局限，以及如何用 GTD 范式来构建这样的系统。"
:page/date "2026-01-31"
:blog-post/tags [:tooling :AI :evaluation]
:blog-post/author {:person/id :jan}
:page/body

## 什么是 AI Eval System

就像名字说的，AI Eval System 是用来 evaluate AI 的 performance 的系统。

简单来说，它是一个让你能系统性地测试和评估 AI 系统（特别是 LLM、Agent、Workflow）质量的框架。

为什么需要这样的系统？因为我们面对的不再是 deterministic 的代码，而是 non-deterministic 的 AI 输出。传统的单元测试、集成测试在这里不够用了——你不能简单地断言 `assert output == expected`，因为每次运行都可能得到不同的结果。

## 核心问题

当你构建一个 LLM 应用、一个 Agent、或者一个复杂的 AI workflow 时，你会面临这些问题：

- **怎么知道它在变好还是变坏？** 当你调整 prompt、更换模型、或者修改流程时，如何量化地评估效果？
- **怎么测试各种 corner case？** 一个 Agent 可能有成千上万种行为路径，手动测试不可能覆盖。
- **怎么保证稳定性？** 模型更新、API 变化，如何确保你的系统不会退化？
- **怎么让团队协作？** 当多人同时开发，如何统一标准评估质量？

这些都不是简单的测试用例能解决的。

## 传统方法的局限

常见的做法是：

1. **手动测试**：扔几个 query 到 chat interface，看看输出如何。主观、不可重复、难以规模化。

2. **简单的 golden dataset**：准备一些问题和标准答案，然后对比。但对于 open-ended 的问题，什么是"正确答案"？

3. **Ad-hoc 的评估脚本**：写一些临时的脚本跑测试。难以维护，难以共享，难以持续。

这些方法在早期可能够用，但随着系统复杂度增加，它们会成为瓶颈。

## 这篇文章要做什么

这不是一个"完美方案"的教程。这篇文章记录的是我在构建和使用 AI Eval System 时的思考和实践。

它会是一篇 living document。随着我的理解加深，随着工具和方法的演进，内容会不断更新。

我会先描述 Eval System 的核心组件（Task、Dataset、Scorer），然后讨论如何用 GTD（Generate, Test and Debug）范式来迭代构建这样的系统。

最后，我会分享一些具体的实践案例和经验。

Digital garden 的思想是：不追求完美，先种下种子，慢慢生长。
