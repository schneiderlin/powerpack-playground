(ns powerblog.core
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [powerblog.render :as render]))

;; 只有在这个 set 里的 tag 会被 ingest 进 DB，其余的会被过滤并打日志
;; 每个 tag 的注释说明其含义与适用场景
(def ^:dynamic *allowed-blog-tags*
  #{:clojure       ; Clojure 语言、生态、实践
    :AI            ; 人工智能、大模型、AI 应用与产品
    :agent         ; AI Agent、智能体、自主系统
    :evaluation    ; 评估、评测（如 AI eval、打分体系）
    :trace         ; 追踪、trace 分析、执行轨迹
    :system-design ; 系统设计、架构
    :decision-making ; 决策、决策边界、自主权
    :tools         ; 工具（如 Agent 工具、开发工具）
    :knowledge-management ; 知识管理、笔记、信息组织
    :career        ; 职业、工作、职场
    :happiness     ; 幸福、满足感、生活
    :writing       ; 写作、创作、表达
    :time-management ; 时间管理、效率
    :design        ; 设计（产品/视觉/交互）
    :ui            ; 用户界面、前端 UI、视图层
    :tooling       ; 开发工具、工作流工具
    :design-system ; 设计系统、组件规范、Design Token
    :linter        ; 静态检查、Linter、代码/样式规范
    :type-system   ; 类型系统、类型理论、强类型/动态类型
    :rust          ; Rust 语言
    :haskell       ; Haskell 语言
    :java          ; Java 语言、JVM
    :scala         ; Scala 语言
    :javascript    ; JavaScript 语言、前端运行时
    :algorithm     ; 算法、数据结构
    :distributed-system ; 分布式系统、一致性、共识
    :language      ; 自然语言、外语学习、语言习得
    :learning      ; 学习、学习方法、教育
    :akka          ; Akka 框架、Actor 模型
    :networking    ; 网络、协议、网络层
    :compiler      ; 编译器、解释器、语言实现
    :astro         ; Astro 静态站/博客框架
    :computation   ; 计算模型、增量计算、UI 计算
    :nix           ; Nix 包管理、可复现构建
    :database      ; 数据库、存储、索引
    :rocketmq      ; RocketMQ 消息队列
    :open-source   ; 开源、开源社区与协作
    :software-engineering ; 系统性工程思考类文章，非所有 IT 类都需打此 tag
    :functional-programming ; 函数式编程、FP 概念（Functor/Monad 等）
    :documentation ; 文档、写作与维护文档
    })

(comment
  (require '[powerpack.dev :as dev])
  (def app (dev/get-app))
  (def db (d/db (:datomic/conn app)))
  (render/get-posts-by-tag db :clojure)
  :rcf)

(defn get-page-kind [file-name]
  (cond
    (re-find #"^blog-posts/" file-name)
    :page.kind/blog-post

    (re-find #"^index\.md" file-name)
    :page.kind/frontpage

    (re-find #"\.md$" file-name)
    :page.kind/article))

(defn filter-tags-to-allowed [tags allowed]
  (when (seq tags)
    (let [tags-vec (if (sequential? tags) tags [tags])
          filtered (filterv allowed tags-vec)
          invalid (remove allowed tags-vec)]
      (when (seq invalid)
        (binding [*out* *err*]
          (println (str "[powerblog] 以下 tag 不在允许列表中，已忽略: "
                       (str/join ", " (map name invalid))))))
      filtered)))

(defn create-tx [file-name txes]
  (let [kind (get-page-kind file-name)
        allowed *allowed-blog-tags*]
    (for [tx txes]
      (cond-> tx
        (contains? tx :blog-post/tags)
        (update :blog-post/tags #(vec (or (filter-tags-to-allowed % allowed) [])))
        (and (:page/uri tx) kind)
        (assoc :page/kind kind)))))

(comment
  render/!debug

  (require '[powerblog.lint :as lint])
  (lint/element? render/!debug)
  (lint/run-lint render/!debug)

  (lint/run-lint [:html
                  [:div {}
                   [:div {:class "invalidName"}]
                   [:span {:class "invalidName"}]]])
  :rcf)

(def config
  {:site/title "The Powerblog"
   :datomic/schema-file "resources/schema.edn"
   :powerpack/port 8000
   :powerpack/log-level :debug
   :powerpack/render-page #'render/render-page
   :powerpack/create-ingest-tx #'create-tx
   :optimus/bundles {"app.css"
                     {:public-dir "public"
                      :paths ["/styles.css"]}}
   :optimus/assets [{:public-dir "public"
                     :paths ["/favicon.svg"]}]
   :optimus/options {:minify-js-assets? false
                     :minify-css-assets? false}})
