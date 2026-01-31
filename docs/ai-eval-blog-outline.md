# AI Eval System 博客大纲

## 一、引言
### 1.1 什么是 AI Eval System
- 定义：用于评估 AI 系统性能的系统性框架
- 重要性：保障 AI 系统质量、可靠性和可迭代性
- 传统评估方法的痛点

## 二、AI Eval System 的核心组件
### 2.1 Task（任务）
- Non-deterministic function 的本质
- 三种常见形态：
  - 单个 LLM call
  - Workflow（多步骤流程）
  - Agent（自主决策系统）
- 输入输出规范

### 2.2 Dataset（数据集）
- 构成要素：
  - 具体示例的输入输出
  - Meta data（元数据）
  - 人类评分（golden labels）
- 数据集设计原则
- 数据质量 vs 数据量

### 2.3 Scorer（评分器）
- 核心原则：优先使用确定性方法
- LLM as Judge 的问题：
  - 递归的无限倒退（谁来 judge judge？）
  - 不确定性的叠加
  - 可调试性差
- 确定性评分方法的优势：
  - 可解释性
  - 可调试性
  - 稳定性（适合 regression test）
  - 低成本
- 常见确定性评分维度：
  - Syntax 正确性（Parse → 是否报错）
  - Schema 验证
  - 安全性检查
  - Design compliance（Linter 风格检查）
  - Type Checker 作为评分器
- 混合策略：什么时候用 LLM as Judge？

## 三、从 Generate-and-Test 到 Generate-Test-and-Debug
### 3.1 传统范式的局限
- Generate and Test 的问题：
  - 盲目试错
  - 失败后完全丢弃
  - 重复成本高

### 3.2 Sussman 的洞察
- "Almost-right" 理念
- 调试优于重写
- 科学家和工程师的真实工作方式

### 3.3 GTD 范式（Generate, Test and Debug）
- 核心流程：
  1. 快速生成近似解
  2. 测试验证
  3. 定向调试修复
  4. 迭代优化

## 四、依赖导向回溯（Dependency-directed Backtracking）
### 4.1 传统回溯 vs 依赖回溯
- Chronological Backtracking 的盲目性
- 依赖网络的构建价值

### 4.2 依赖链追踪机制
- 结论与前提假设的显式链接
- 矛盾检测与定位
- 最小化修改单元

## 五、AI Eval 中的迭代策略
### 5.1 快速原型阶段
- 用启发式规则搭建"差不多对"的评估框架
- 不追求完美，先让系统跑起来

### 5.2 测试与诊断
- 运行 eval system 识别问题
- 依赖分析定位失败根源
- 区分 task 问题、dataset 问题、scorer 问题

### 5.3 精准调试（Critics 模块）
- 常见修复策略：
  - Block Promotion（块提升）：重排序评估步骤
  - Individual Promotion：调整子目标优先级
  - 引入条件分支：处理不确定场景
- 最小化修改原则

### 5.4 知识积累与泛化
- 将成功修复泛化为调试规则
- 构建"评估模式库"
- Constant-folding 问题类别

## 六、实践建议
### 6.1 Eval System 设计原则
- 先构造，后修复
- 维护因果图（记录"为什么这样设计"）
- 局部化失败处理
- 明确保护约束

### 6.2 工具与工程实践
- 如何构建可维护的 eval pipeline
- 版本控制与回归测试
- 持续集成中的 eval

### 6.3 团队协作
- 评审机制
- 知识共享
- 逐步建立 eval culture

## 七、案例分析
### 7.1 Case 1: 优化 LLM as Judge
- 初始 scorer 设计
- 通过 GTD 迭代提升准确率

### 7.2 Case 2: Agent 行为评估
- Task 复杂度带来的挑战
- Dataset 采样策略优化

### 7.3 Case 3: 评分维度扩展
- 从单一指标到多维评估
- Scorer 组合策略

## 八、未来展望
### 8.1 Auto-Eval 的发展方向
### 8.2 AI 辅助调试评估系统
### 8.3 开源 eval ecosystem
