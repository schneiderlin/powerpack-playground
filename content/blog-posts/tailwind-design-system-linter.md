:page/title "本项目的 Tailwind 设计系统 Linter 实现"
:page/description "说明当前项目如何落地设计系统 Linter 方案的数据源与规则实现。"
:page/date "2025-01-31"
:blog-post/tags [:tooling :design-system :tailwind :linter]
:blog-post/author {:person/id :jan}
:page/body

本文说明**当前项目**如何落地 [《如何用自定义 Linter 确保样式符合设计系统》](/blog-posts/design-system-linter/) 里的方案：数据源、规则实现。

---

## 1. 整体结构

- **设计系统数据源**：`design-system/tokens.edn`，定义「允许 / 禁止 / 推荐替换」的 Tailwind 类（供 Linter 使用）。
- **实际 design token**：在 `resources/public/css/input.css` 里，以 CSS 变量 / Tailwind `@theme` 等形式定义，是最终生效的样式与主题值。`style.css` 是根据 `input.css` 自动生成的构建产物（如经 Tailwind 编译），页面引用的是 `style.css`。
- **Lint 框架**：`src/powerblog/lint.clj`，提供规则注册、Hiccup 遍历、违规收集与报告。
- **自定义规则**：`dev/lint_rules.clj`，包含设计 token 校验及其他规则，在开发环境加载并注册到 `powerblog.lint`。

---

## 2. 设计系统数据源：`design-system/tokens.edn`

项目用一份 EDN 作为「允许清单」和「替换建议」的单一数据源，供 lint 规则读取。  
**注意**：这里只定义 Linter 的规则数据；真正生效的 design token（颜色、间距、字体等）在 **`resources/public/css/input.css`** 中定义（CSS 变量或 Tailwind 配置），编辑主题或新增 token 时应改 `input.css`，与 `tokens.edn` 的允许/禁止/推荐列表保持一致。`style.css` 由 `input.css` 经构建（如 Tailwind）自动生成，页面实际引用的是 `style.css`，不要直接改 `style.css`。

### 2.1 结构说明

| 键 | 含义 | 用途 |
|----|------|------|
| `allowed-prefixes` | 允许的类名或类名前缀列表 | 白名单：class 要么完全匹配某一项，要么以某一项为前缀，否则可报 warning |
| `disallowed-patterns` | 禁止出现的子串/模式 | 黑名单：class 包含或以此开头则报 error（如 `[` 表示禁止任意值 `[...]`，以及 `text-blue-`、`bg-red-` 等） |
| `prefer` | 推荐替换映射 | key 为当前类名，value 为推荐使用的类名；命中则报 warning 建议替换 |

### 2.2 当前配置要点

- **颜色**：只允许 `text-primary-`、`text-gray-`、`text-muted-`、`bg-surface-`、`bg-primary-` 等；禁止 `text-blue-`、`bg-red-`、`text-green-` 等，避免散落色值。
- **布局/组件**：允许 `layout-*`、`hero-`、`heading-`、`section-`、`card-*`、`btn`、`prose`、`toc` 等语义化类。
- **通用工具类**：允许 `flex`、`grid`、`gap-`、`p-`、`m-`、`rounded-`、`shadow-`、`w-`、`h-`、响应式前缀 `sm:`、`md:` 等。
- **禁止**：`[` 表示不鼓励任意值写法；并显式禁止一批未纳入设计系统的颜色类。
- **推荐替换**：例如 `border-gray-200` / `border-gray-300` → `border-border-default`，统一边框语义。

规则实现只依赖这三个 key，不关心具体是 Tailwind 还是别的 utility 体系，只要在规则里按「类名字符串」来匹配即可。

---

## 3. Lint 框架：`src/powerblog/lint.clj`

Lint 负责「规则注册 + Hiccup 遍历 + 报告」。

### 3.1 规则注册

- `register-rule! [name rule-fn]`：注册一条规则，`name` 为关键字，`rule-fn` 接收**节点 map**，返回 **violation map 或 nil**。
- `get-rules`：当前已注册规则。
- `clear-rules!`：清空规则（便于测试或重载）。

节点 map 形如：`{:tag :div :attrs {:class "..."} :children [...]}`。  
Violation map 需包含 `:severity`（`:error` / `:warning`）和 `:message`，可选 `:element`、`:attributes` 等，由报告逻辑使用。

### 3.2 Hiccup 解析与遍历

- 只处理**向量形式的 Hiccup 元素**（首元素为 keyword tag）。
- `parse-element` 把 `[:tag attrs? & children]` 解析为 `{:tag :attrs :children}`。
- `collect-violations` 对整棵 Hiccup 树递归：先对当前节点 `apply-rules-to-node`，再对子节点递归；子节点会做展开（如 layout 里多段 content 的 seq），保证所有元素都会被检查。

### 3.3 执行与报告

- `apply-rules-to-node`：对单个节点依次执行所有已注册规则，收集返回的 violation。
- `run-lint [hiccup]`：对一棵 Hiccup 树执行 `collect-violations`，再 `report-violations` 把结果打印到控制台（错误数、警告数、每条违规的规则名、元素、属性、消息）。

这样，任何能产出 Hiccup 的页面/组件（例如 powerblog 的 layout 与页面）都可以传入 `run-lint` 做一次校验。

---

## 4. 自定义规则：`dev/lint_rules.clj`

自定义规则写在 `dev/lint_rules.clj`，通过 `powerblog.lint/register-rule!` 注册，**不会**被 powerpack 库加载，仅在本项目开发环境中生效。

### 4.1 与设计系统相关的规则：`check-design-tokens`

- **数据来源**：调用 `get-tokens` 读取 `design-system/tokens.edn`（优先当前目录，再 `user.dir`），每次执行都重新读文件，改 tokens 不用重启 REPL。
- **逻辑**：
  1. 从节点 `attrs` 里取出 `:class`，按空白拆成多个 class。
  2. **disallowed**：若某个 class 包含或以其开头命中 `disallowed-patterns` 中任一项，生成 error。
  3. **prefer**：若 class 在 `prefer` 的 key 中，生成 warning，提示应改为对应的 value。
  4. **allowed-prefixes**：若配置了 `allowed-prefixes`，则 class 必须与某一项完全相等或以该项为前缀，否则生成 warning。
- 多条违规会合并成一条 violation，message 用 `"; "` 拼接；只要有一条 error 则整体 severity 为 error，否则为 warning。

### 4.2 其他示例规则（同文件）

- **class 命名**：`check-class-naming`，要求 class 为 kebab-case（不含大写），否则 warning。
- **无障碍**：`check-img-alt`，`<img>` 必须有 `alt`，否则 error。
- **样式方式**：`check-inline-style`，存在内联 `style` 则 warning，建议用 class。

通过 `register-example-rules!` 一次性注册以上四条规则（含 `check-design-tokens`）；在 dev 启动或 REPL 里调用一次即可。

---

## 5. 使用方式

1. **加载规则**（在 REPL 或 dev 入口）：  
   `(require '[lint-rules])` 后执行 `(lint-rules/register-example-rules!)`。
2. **对某棵 Hiccup 跑 lint**：  
   `(require '[powerblog.lint :as lint] '[powerblog.core :as core])`  
   `(lint/run-lint core/!debug)`（或任意返回 Hiccup 的 var/函数）。
3. **查看/清理规则**：  
   `(lint/get-rules)`、`(lint/clear-rules!)`。

CI 或提交前可以写脚本：加载项目 ns、注册规则、对主要 layout/页面调用 `run-lint`，根据返回的 violations 决定是否失败。

---

## 6. 小结

| 层次 | 文件 | 作用 |
|------|------|------|
| 数据源（Linter） | `design-system/tokens.edn` | 定义 Tailwind 类的允许前缀、禁止模式、推荐替换 |
| 实际 token（样式） | `resources/public/css/input.css` | 定义真正生效的 design token（CSS 变量 / Tailwind theme）；`style.css` 由其自动生成，页面引用 `style.css` |
| 框架 | `src/powerblog/lint.clj` | 规则注册、Hiccup 遍历、违规收集与控制台报告（项目自带，非 powerpack） |
| 规则实现 | `dev/lint_rules.clj` | 自定义规则：设计 token 校验、class 命名、img alt、内联 style 等，并注册到 `powerblog.lint` |

这样就把「设计系统 → 允许清单」和「校验逻辑 → 自定义规则」都放在本仓库内，不依赖 powerpack 提供 lint，后续要加新规则或改 tokens 只需改 `tokens.edn` 和 `lint_rules.clj`。
