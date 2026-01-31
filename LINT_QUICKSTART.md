# Attribute Lint 快速开始

## 安装完成

Lint 系统已经成功集成到您的 powerpack 项目中！

## 基本使用

### 1. 注册示例规则

在开发环境的 REPL 中执行：

```clojure
(require '[dev.lint-rules :as lint-rules])
(lint-rules/register-example-rules!)
```

### 2. 访问页面

访问任何页面（如 http://localhost:8000），lint 会自动执行并在控制台显示结果。

### 3. 查看结果

控制台会显示类似：

```
✓ No attribute lint issues found
```

或

```
🔍 Attribute Lint Results:
  1 errors, 2 warnings

[ERROR] img-alt - img element must have an alt attribute
  Element: <img>
  Attributes:
    src: "/image.jpg"

[WARN] class-naming - Class names should be kebab-case. Invalid: myClass
  Element: <div>
  Attributes:
    class: "myClass"
```

## 创建自定义规则

```clojure
(require '[powerblog.lint :as lint])

(defn my-rule [node]
  (when (some-condition node)
    {:severity :warning
     :message "My custom rule"}))

(lint/register-rule! :my-rule my-rule)
```

## 控制命令

```clojure
(require '[powerblog.lint :as lint])

;; 查看已注册的规则
(lint/get-rules)

;; 禁用 lint
(lint/disable-lint!)

;; 启用 lint
(lint/enable-lint!)

;; 重新 lint 所有页面
(lint/reset-linted-uris!)

;; 清除所有规则
(lint/clear-rules!)
```

## 示例规则文件

查看 `dev/lint_rules.clj` 获取更多示例规则：

- `check-class-naming` - 检查 class 命名
- `check-img-alt` - 检查 img alt 属性
- `check-inline-style` - 检查内联样式

## 详细文档

完整文档请查看 `LINT_README.md`

## 故障排除

### Lint 没有执行
1. 确保规则已注册：`(lint/get-rules)`
2. 重置已 lint 的 URI：`(lint/reset-linted-uris!)`
3. 刷新页面

### 规则没有生效
1. 检查规则函数是否正确返回 violation map
2. 确保规则已注册
3. 查看控制台是否有错误
