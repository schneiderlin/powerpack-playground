# Powerblog Attribute Linter

一个用于检查 HTML attributes 的 lint 系统，集成到 powerpack 的渲染流程中。

## 功能特性

- ✅ 规则注册机制 - 轻松添加自定义 lint 规则
- ✅ 集成到 powerpack - 自动在渲染时执行 lint
- ✅ 控制台报告 - 彩色输出，清晰的错误和警告
- ✅ 支持开发和导出模式 - 在两个模式下都能工作
- ✅ 性能优化 - 每个页面只 lint 一次

## 快速开始

### 1. 基本配置

Lint 系统已经在 `src/powerblog/core.clj` 中配置好：

```clojure
(def config
  {... 
   :powerpack/page-post-process-fns [#'powerblog.lint/get-lint-post-processor]
   ...})
```

### 2. 注册自定义规则

在开发环境的 REPL 中执行：

```clojure
(require '[dev.lint-rules :as lint-rules])
(lint-rules/register-example-rules!)
```

### 3. 访问页面

现在访问任何页面，lint 会自动执行并在控制台显示结果。

## 使用指南

### 创建自定义规则

规则函数接收一个 DOM 节点，返回 violation map 或 nil：

```clojure
(defn my-custom-rule [node]
  (when (condition-to-check node)
    {:severity :error           ; :error 或 :warning
     :message "Description of the issue"}))
```

注册规则：

```clojure
(require '[powerblog.lint :as lint])

(lint/register-rule! :my-rule my-custom-rule)
```

### 规则函数可以访问的节点信息

- `(.getNodeName node)` - 元素标签名（如 "div", "img"）
- `(.getAttributes node)` - NamedNodeMap of attributes
- `(.getNamedItem node "attr-name")` - 获取特定属性
- `(.getValue attribute)` - 获取属性值

### 示例规则

查看 `dev/lint_rules.clj` 获取更多示例：

- `check-class-naming` - 检查 class 命名是否遵循 kebab-case
- `check-img-alt` - 检查 img 元素是否有 alt 属性
- `check-inline-style` - 检查是否有内联 style 属性

## API 参考

### 注册规则

```clojure
(lint/register-rule! :rule-name rule-fn)
```

### 查看已注册的规则

```clojure
(lint/get-rules)
```

### 清除所有规则

```clojure
(lint/clear-rules!)
```

### 控制 lint 执行

```clojure
;; 启用 lint（默认已启用）
(lint/enable-lint!)

;; 禁用 lint
(lint/disable-lint!)

;; 重置已 lint 的 URI（重新 lint 所有页面）
(lint/reset-linted-uris!)
```

### 手动执行 lint

```clojure
(lint/run-lint "<html>...</html>")
```

## 配置选项

### 修改配置

在 `src/powerblog/core.clj` 中：

```clojure
(def config
  {... 
   :powerpack/page-post-process-fns [#'powerblog.lint/get-lint-post-processor]
   ...})
```

### 仅在开发模式启用

```clojure
(def config
  {...
   :powerpack/page-post-process-fns (when dev?
                                       [#'powerblog.lint/get-lint-post-processor])
   ...})
```

## 输出格式

### 成功时

```
✓ No attribute lint issues found
```

### 发现问题时

```
🔍 Attribute Lint Results:
  2 errors, 1 warnings

[ERROR] img-alt - img element must have an alt attribute
  Element: <img>
  Attributes:
    src: "/image.jpg"
    class: "myImage"

[WARN] class-naming - Class names should be kebab-case. Invalid: myImage
  Element: <img>
  Attributes:
    class: "myImage"
    src: "/image.jpg"

[WARN] class-naming - Class names should be kebab-case. Invalid: myDiv
  Element: <div>
  Attributes:
    class: "myDiv"
```

## 性能考虑

- 每个页面 URI 只会 lint 一次（直到调用 `reset-linted-uris!`）
- Lint 在页面渲染后执行，不影响渲染速度
- 可以通过 `disable-lint!` 临时禁用

## 扩展：HUD 集成

未来可以通过扩展 powerpack 的 HUD 系统在浏览器中显示 lint 结果。

## 调试技巧

### 查看当前规则

```clojure
(lint/get-rules)
```

### 测试单个规则

```clojure
(require '[html5-walker.walker :as html5-walker])

(def node (-> "<div class=\"badClass\"></div>"
              html5-walker/parse
              (html5-walker/find-nodes [:div])
              first))

(dev.lint-rules/check-class-naming node)
```

## 故障排除

### Lint 没有执行

1. 检查规则是否已注册：`(lint/get-rules)`
2. 检查 lint 是否启用：`(lint/enable-lint!)`
3. 重置已 lint 的 URI：`(lint/reset-linted-uris!)`
4. 刷新页面

### 规则没有生效

1. 检查规则函数是否正确返回 violation map
2. 检查规则是否成功注册
3. 查看控制台是否有规则执行错误

## 贡献

添加新规则时：
1. 在 `dev/lint_rules.clj` 或新文件中定义规则
2. 添加清晰的文档字符串
3. 提供示例
4. 测试规则的准确性

## 许可证

与 powerpack 项目相同
