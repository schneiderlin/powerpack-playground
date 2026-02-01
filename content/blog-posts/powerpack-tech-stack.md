:page/title "博客使用的技术栈 - Powerpack"
:page/description "An overview of the technologies and tools used to build this blog with Powerpack, including Clojure, Datomic, Tailwind CSS, and more."
:page/date "2025-02-01"
:blog-post/tags [:clojure :powerpack]
:blog-post/author {:person/id :jan}
:page/body

本文档介绍基于 Powerpack 构建的博客所使用的技术栈。与传统 SSG 不同,Powerpack 采用"内容解析 → 写入数据库 → 从数据库渲染"的架构,使 SSG 与 SPA 可以复用代码,并支持在内容中嵌入数据库查询。

## 核心框架

### Powerpack
Powerpack 是一个基于 Clojure 的静态站点生成器,与传统 SSG (如 Hugo、Astro)的核心区别在于引入了数据库层:

- 传统 SSG: content → parse → render → HTML
- Powerpack: content → parse → ingest to DB → render from DB → HTML

这种架构的优势包括:
1. **代码复用**: SSG 和 SPA 都从数据库读取内容,可以共享查询和渲染逻辑
2. **强大查询**: 在渲染时使用 Datalog 查询数据库,支持复杂的内容关联和筛选
3. **扩展性**: 内容层作为 source of truth,数据库提供开发和运行时的便利

### Clojure & ClojureScript
- **Clojure**: 服务端渲染,使用 Hiccup 生成 HTML
- **ClojureScript**: 可选的前端交互(本博客目前主要使用 vanilla JavaScript)

依赖配置 (deps.edn):
```clojure
{:paths ["src" "resources"]
 :deps {no.cjohansen/powerpack {:mvn/version "2025.10.22"}}}
```

## 数据层

### Datomic
Powerpack 内置使用 Datomic 作为数据库,提供强大的查询能力:

**Schema 定义** (resources/schema.edn):
```clojure
[{:db/ident :page/uri
  :db/valueType :db.type/string
  :db/cardinality :db.cardinality/one
  :db/unique :db.unique/identity}
 {:db/ident :page/title
  :db/valueType :db.type/string
  :db/cardinality :db.cardinality/one}
 {:db/ident :blog-post/tags
  :db/valueType :db.type/keyword
  :db/cardinality :db.cardinality/many}]
```

**查询示例**:
```clojure
;; 按标签获取文章
(d/q '[:find [?e ...]
       :in $ ?tag
       :where
       [?e :blog-post/tags ?tag]]
     db :clojure)
```

**内容摄入**:
在 powerblog.core 中定义 `create-tx` 函数,将文件内容转换为符合 schema 的 transaction:
```clojure
(defn create-tx [file-name txes]
  (let [kind (get-page-kind file-name)]
    (for [tx txes]
      (cond-> tx
        (and (:page/uri tx) kind)
        (assoc :page/kind kind)))))
```

## 渲染层

### Hiccup
使用 Hiccup 作为 HTML 生成 DSL,所有组件都是 Clojure 的 vector:

```clojure
(defn layout [{:keys [lang] :as opts} & content]
  [:html {:lang (or lang "en")}
   (layout-head opts)
   (layout-body content)])

(defn blog-post-card [post]
  [:a {:href (:page/uri post)
       :class "card card-hoverable block post"}
   [:div {:class "p-6 space-y-4"}
    [:h3 (:page/title post)]
    [:div (for [tag (:blog-post/tags post)]
            [:span {:class "badge"} (name tag)])]]])
```

### Markdown 处理
使用 Powerpack 内置的 markdown 工具:

```clojure
[require '[powerpack.markdown :as md]]

(defn body-to-html-string [body]
  (->> body
       str/split-lines
       md/unindent-but-first
       (str/join "\n")
       (md/md-to-html)))
```

## 样式系统

### Tailwind CSS
使用 Tailwind CSS 作为样式框架,配合自定义设计系统:

**编译配置** (bb.edn):
```clojure
{:tasks
 {css {:doc  "Build CSS once"
       :task (shell "npx @tailwindcss/cli -i ./resources/public/input.css -o ./resources/public/styles.css")}
  tw  {:doc  "Watch Tailwind CSS"
       :task (shell "npx @tailwindcss/cli -i ./resources/public/input.css -o ./resources/public/styles.css --watch")}}}
```

**Wabi-Sabi 设计系统** (design-system/tokens.edn):
定义了语义化的设计 token,限制 arbitrary values,确保设计一致性:
- 禁止 `text-blue-`, `bg-red-` 等非语义化颜色
- 优先使用 `border-border-default` 替代 `border-gray-200`
- 语义化组件类: `card`, `btn`, `badge`, `prose`, `toc`

### Google Fonts
在 layout-head 中引入字体:
```clojure
[:link {:rel "stylesheet"
        :href "https://fonts.googleapis.com/css2?family=Crimson+Text:ital,wght@0,400;0,600;0,700;1,400&family=JetBrains+Mono:wght@400;500&family=Noto+Serif+SC:wght@400;500;600;700&display=swap"}]
```

## 功能特性

### 阅读进度条
原生 JavaScript 实现,无需依赖:

```clojure
(def progress-bar-script
  "(function() {
     function updateProgressBar() {
       const scrollTop = document.documentElement.scrollTop;
       const scrollHeight = document.documentElement.scrollHeight - document.documentElement.clientHeight;
       const scrollPercent = (scrollTop / scrollHeight) * 100;
       progressBar.style.setProperty('--progress', scrollPercent + '%');
     }
     document.addEventListener('scroll', updateProgressBar);
   })();")
```

### 目录 (Table of Contents)
从 markdown 生成的 HTML 中提取标题,构建嵌套目录:

```clojure
(defn extract-headings [html]
  (let [pattern #"<h([2-6])[^>]*id=\"([^\"]+)\"[^>]*>(.*?)</h\\1>"]
    (re-seq pattern html)))

(defn build-toc [headings]
  ;; 构建嵌套的 TOC 结构
  )
```

### 响应式导航栏
移动端使用 JavaScript 切换菜单:
```javascript
document.getElementById('mobile-menu-button').addEventListener('click', function() {
  document.getElementById('navbar-default').classList.toggle('hidden');
});
```

## 开发工具

### Babashka (bb)
使用 Babashka 作为任务运行器:
- `bb css`: 构建 CSS
- `bb tw`: 监听 CSS 变化

### CLJ-Kondo
配置 clj-kondo 进行静态分析和 LSP 支持 (.clj-kondo/config.edn)

### Hot Reload
Powerpack 内置支持文件变更后的 hot reload:
```clojure
{:powerpack/port 8000
 :powerpack/log-level :debug}
```

## 部署

### 静态导出
通过 `-X:build` 触发导出:
```clojure
:aliases {:build {:exec-fn powerblog.export/export!}}
```

### 资源优化
使用 Optimus 处理静态资源:
```clojure
:optimus/bundles {"app.css"
                  {:public-dir "public"
                   :paths ["/styles.css"]}}
:optimus/options {:minify-js-assets? false
                   :minify-css-assets? false}
```

## 总结

Powerpack 的技术栈体现了 Clojure 社区的理念:
- **表达力优于框架**: Clojure 本身的组合能力替代了繁重的框架
- **数据驱动架构**: Datomic 提供强大的查询和数据建模能力
- **简洁性**: Hiccup 代替模板引擎,EDN 代替复杂的配置
- **工具链轻量**: Babashka + CLJ-Kondo 提供高效的开发体验

这种架构特别适合内容驱动的站点,可以在保持静态站点高性能的同时,获得动态站点的灵活性和可维护性。
