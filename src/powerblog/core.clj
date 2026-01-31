(ns powerblog.core
  (:require
   [datomic.api :as d]
   [powerblog.render :as render]))

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

(defn create-tx [file-name txes]
  (let [kind (get-page-kind file-name)]
    (for [tx txes]
      (cond-> tx
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
