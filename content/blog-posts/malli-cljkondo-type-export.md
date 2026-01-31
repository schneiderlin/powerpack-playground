:page/title "Clojure Malli + clj-kondo Type Export 实践"
:page/description "给 Clojure 库添加类型导出，让依赖方获得 clj-kondo 静态类型检查。本文记录了使用 Malli schema 定义类型、导出给 clj-kondo 的完整流程，以及路径配置的陷阱和解决方案。"
:page/date "2026-01-31"
:blog-post/tags [:clojure :linter :malli :clj-kondo]
:blog-post/author {:person/id :jan}
:page/body

## 背景

`replicant-main` 是一个库，对外提供了 9 个函数。依赖方在使用这些函数时，如果没有类型检查，很容易传错参数。

比如 `is-digit?` 本应接收 string，但如果传了 number，传统的 lint 检查不出来：

```clojure
;; 这应该是错的，但 clj-kondo 不知道
(rm1/is-digit? 123)
```

解决方案是给库添加类型定义，让 clj-kondo 能够进行静态类型检查。

## 用 Malli Schema 定义类型

在 `interface.cljc` 中为每个对外函数添加 `{:malli/schema}`：

```clojure
(ns com.zihao.replicant-main.interface
  (:require [malli.core :as m]))

;; 9 个对外函数的 schema 定义
make-execute-f
  {:malli/schema [:=> [:cat [:* :any]] :any]}

get-result
  {:malli/schema [:=> [:cat :map :map] :any]}

make-interpolate
  {:malli/schema [:=> [:cat [:* :any]] :any]}

interpolate
  {:malli/schema [:=> [:cat :any :any] :any]}

parse-int
  {:malli/schema [:=> [:cat :string] :int]}

is-digit?
  {:malli/schema [:=> [:cat :any] :boolean]}

gather-form-data
  {:malli/schema [:=> [:cat :any] :map]}

gen-uuid
  {:malli/schema [:=> [:cat] :string]}

make-ws-handler-with-extensions
  {:malli/schema [:=> [:cat [:* :any]] :any]}
```

注意：返回"函数"的地方用 `:any`，因为 Malli instrumentation 对 `:fn` 支持有限，会报错。

## 导出给 clj-kondo

Malli 可以将 schema 转换成 clj-kondo 可读的类型定义。

### 1. 配置 deps.edn

```clojure
{:aliases
 {:export
  {:extra-deps {metosin/malli {:mvn/version "0.16.4"}}
   :exec-fn com.zihao.replicant-main.export/export-types
   :exec-opts {:includes ["malli.dev"]}}}}
```

### 2. 实现导出逻辑

```clojure
(ns com.zihao.replicant-main.export
  (:require [malli.dev :as md]))

(defn export-types []
  (md/start!)
  ;; 生成 .clj-kondo/metosin/malli-types-clj/config.edn
  ;; 拷贝到 resources/clj-kondo/clj-kondo.exports/com.zihao.replicant-main/config.edn
  (let [source ".clj-kondo/metosin/malli-types-clj/config.edn"
        target "resources/clj-kondo/clj-kondo.exports/com.zihao.replicant-main/config.edn"]
    (io/copy (io/file source) (io/file target)))
  (md/stop!))
```

### 3. 执行导出

```bash
clj -M:export
```

生成的 `config.edn` 类似这样：

```edn
{:lint-as
 {replicant-main.interface/make-execute-f clojure.core/fn
  replicant-main.interface/get-result clojure.core/fn}
 :linters
 {:type-mismatch
  {:level :error}
 :ns-excludes
  ["replicant-main.interface"]
 :config-in-ns
  {replicant-main.interface
   {:linters
    {:unresolved-symbol {:level :off}}}}
 :analysis
  {:ns-prefixes
   {replicant-main.interface com.zihao.replicant-main.interface}
   :var-definitions
   [{:ns replicant-main.interface
     :name make-execute-f
     :fixed-arities #{0}
     :arities
     {0 {:args [] :ret :any}}}
    {:ns replicant-main.interface
     :name get-result
     :fixed-arities #{2}
     :arities
     {2 {:args [:map :map] :ret :any}}}
    {:ns replicant-main.interface
     :name is-digit?
     :fixed-arities #{1}
     :arities
     {1 {:args [:string] :ret :boolean}}}
    ;; ...
    ]}}
```

## 依赖方自动加载类型

依赖方执行 `clj-kondo --copy-configs --skip-lint` 后，config 会被拷到项目的 `.clj-kondo` 目录。

### 路径陷阱

clj-kondo 只自动加载 `.clj-kondo/*/*/config.edn`（**两层**）。

如果导出路径是：
```
resources/clj-kondo/clj-kondo.exports/com/zihao/replicant-main/config.edn
```

拷贝后会变成：
```
.clj-kondo/imports/com/zihao/replicant-main/config.edn
```

这是**四层**（imports → com → zihao → replicant-main），**超出了自动加载的范围**。

### 解决方案

把导出路径改成单段 namespace：

```
resources/clj-kondo/clj-kondo.exports/com.zihao.replicant-main/config.edn
```

这样拷贝后是：
```
.clj-kondo/imports/com.zihao.replicant-main/config.edn
```

刚好两层，会被自动加载。

### 更新导出逻辑

```clojure
;; export.clj
(defn export-types []
  (md/start!)
  (let [source ".clj-kondo/metosin/malli-types-clj/config.edn"
        target "resources/clj-kondo/clj-kondo.exports/com.zihao.replicant-main/config.edn"]
    (io/copy (io/file source) (io/file target)))
  (md/stop!))
```

## 类型检查的效果

当依赖方调用错误时，clj-kondo 会立即报错：

```clojure
;; 正确用法
(rm1/is-digit? "123")  ;; ✅ 通过

;; 错误用法（传了数字）
(rm1/is-digit? 123)    ;; ❌ type-mismatch: Expected: string, received: positive integer
```

## 使用方式总结

### 库维护方

1. 在 `interface.cljc` 中为对外函数添加 `{:malli/schema}`
2. 运行 `clj -M:export` 生成类型定义
3. 更新 `resources/clj-kondo/clj-kondo.exports/.../config.edn`
4. 发布新版本

### 依赖方

1. 更新依赖到最新版本
2. 运行 `clj-kondo --copy-configs --skip-lint`
3. 清空缓存：`rm -rf .clj-kondo/cache`
4. 正常使用 clj-kondo lint，类型检查自动生效

## 总结

- **Malli Schema** 是单一数据源，同时用于运行时验证和静态类型检查
- **clj-kondo 自动加载** 只支持两层路径，需要用 `com.zihao.replicant-main` 而非 `com/zihao/replicant-main`
- **依赖方无需配置**，`--copy-configs` 后类型检查自动生效
- **实时反馈**，IDE 集成，零成本

这是确定性 scorer 的另一个实践案例。相比 LLM as judge，类型检查：
- 完全确定
- 可解释
- 可调试
- 稳定
- 低成本

参考：[Take Your Linting Game to the Next Level](https://tonitalksdev.com/take-your-linting-game-to-the-next-level)
