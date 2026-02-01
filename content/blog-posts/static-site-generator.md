:page/title static site generator
:blog-post/tags [:clojure]
:blog-post/author {:person/id :jan}
:page/body

静态站点生成器引入数据库环节有多个好处。

传统的 static site generator 少了数据库的环节, 流程是: parse content (例如 markdown) → 直接渲染成页面。

加一个数据库环节的流程是: parse content → 写入数据库 → render 时从数据库读内容。

### 好处

**代码复用**
SSG 和 SPA 等不同类型的应用都可以从数据库读取内容并渲染, 可以共享查询和渲染逻辑。

**强大的查询能力**
从数据库查询内容, 支持复杂的筛选、排序和聚合。例如: 按标签筛选文章、获取相关文章推荐、统计文章数量等。传统方式需要遍历所有文件并手动解析, 效率低下。

**类型安全**
通过 schema 定义数据结构, 在开发阶段就能保证数据的一致性。不同的 content 类型可以有各自的 schema, 例如 blog post 的属性包括 title、body、author、tags 等。

**可扩展性**
原始 content 格式可以多样化 (markdown、EDN、JSON 等), 只要通过 ingest function 转换成统一的 schema 即可。这比传统方式限制为单一格式更灵活。

**开发体验**
数据库支持增量更新和热重载, 修改内容后只需重新解析受影响的文件, 不需要重建整个站点。

### 类比

content 和 DB 的关系, 可以看作代码和 LSP server 的关系:
- content 是 source of truth
- DB 只是在开发/运行时提供便利, 就像 LSP 为代码提供智能提示和导航

fdb 这个项目也是类似的套路, 但它直接使用文件系统作为数据库, 少了一些查询和关系建模的 leverage。

### schema 和 ingest

schema 可以是任何形式, 不同的 content 可以有不同的 schema。例如 blog post 的 schema 包括:

- title: string
- body: markdown
- author: ref
- tags: keywords

各种不同的 raw content 格式 (markdown、EDN、JSON), 如果都是 blog post, 可以写不同的 ingest function 转换成符合 schema 的数据。