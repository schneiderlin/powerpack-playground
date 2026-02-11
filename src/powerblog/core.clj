(ns powerblog.core
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [powerblog.render :as render]))

;; 只有在这个 set 里的 tag 会被 ingest 进 DB，其余的会被过滤并打日志
(def ^:dynamic *allowed-blog-tags*
  #{:clojure :AI :agent :evaluation :trace :system-design
    :decision-making :tools :knowledge-management :career :happiness
    :writing :time-management :design :ui :tooling :design-system
    :linter :type-system 
    :rust :haskell :java :scala :javascript
    :algorithm :distributed-system
    :language :akka :networking :compiler
    :astro :computation :nix :database :rocketmq :open-source
    :software-engineering ; 系统性工程思考类文章，非所有 IT 类都需打此 tag
    :functional-programming :documentation})

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
