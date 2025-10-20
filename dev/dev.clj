(ns dev
  (:require [powerblog.core :as blog]
            [powerpack.dev :as dev]
            [powerpack.watcher :as watcher]
            [clojure.java.io :as io]))

(defmethod dev/configure! :default []
  blog/config)

(comment
  (dev/start)
  (dev/stop)
  (dev/reset)

  (def app (dev/get-app))

  ;; watch 的问题
  (require '[powerpack.watcher :as watcher])
  (require '[nextjournal.beholder :as beholder])
  (require '[clojure.java.io :as io])
  (require '[powerpack.files :as files])
  (require '[clojure.string :as str])
  (watcher/get-watch-paths app)


  ;; 获取不到 app event, 所以没 publish 出去
  (watcher/get-app-event app
                         {:type :modify
                          :path (java.nio.file.Paths/get "content" (into-array ["test.md"]))})


  (defn parent? [dir f]
    (let [dir-path (str (.getAbsolutePath (files/as-file dir)) "\\")
          file-path (.getAbsolutePath (files/as-file f))]
      (and (str/starts-with? file-path dir-path)
           (not= (str/replace dir-path #"/$" "")
                 (str/replace file-path #"/$" "")))))

  ;; 这里判断错了
  (files/parent? "content"
                 "c:\\Users\\zihao\\Desktop\\workspace\\private\\powerpack-playground\\content\\test.md")

  ;; 最后多了个 "/" 的问题, windows 不需要这个. 或者是 windows 换成 \\
  (let [f "c:\\Users\\zihao\\Desktop\\workspace\\private\\powerpack-playground\\content\\test.md"
        dir "c:\\Users\\zihao\\Desktop\\workspace\\private\\powerpack-playground\\content"
        dir-path (files/as-file dir)
        file-path (files/as-file f)]
    #_[dir-path file-path]
    [(.getParentFile file-path) dir-path
     (= (.getParentFile file-path) dir-path)]
    #_(and (str/starts-with? file-path dir-path)
         (not= (str/replace dir-path #"/$" "")
               (str/replace file-path #"/$" ""))))




  (require '[datomic.api :as d])
  (def db (d/db (:datomic/conn app)))

  (->> (d/entity db [:page/uri "/"]))
  (->> (d/entity db [:page/uri "/blog-posts/first-post/"])
       :blog-post/author
       (into {}))

  :rcf)
