#!/usr/bin/env bb
(ns convert-all-html-to-md
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

;; Reuse conversion logic from html-to-markdown.clj (load defines html-to-markdown in this ns)
(declare html-to-markdown)
(load-file "scripts/html-to-markdown.clj")

(def source-dir "tmp/scott-young-blog")
(def md-dir (str source-dir "/md"))

(defn html-files []
  (->> (io/file source-dir)
       file-seq
       (filter #(str/ends-with? (.getName %) ".html"))
       (remove #(= "all-slug.html" (.getName %)))
       (sort-by #(.getPath %))))

(defn html-path->md-path [html-path]
  (let [name (-> (io/file html-path) .getName (str/replace #"\.html$" ".md"))]
    (str md-dir "/" name)))

(defn -main []
  (io/make-parents (str md-dir "/.keep"))
  (let [files (html-files)]
    (println "Found" (count files) "HTML files in" source-dir)
    (doseq [f files]
      (let [path (.getPath f)
            out (html-path->md-path path)]
        (if-let [md (html-to-markdown path)]
          (do
            (spit out md)
            (println "  ->" (.getName (io/file out))))
          (println "  skip (no content):" (.getName f))))))
  (println "Done. Markdown in" md-dir))

(when (= *file* (System/getProperty "babashka.file"))
  (-main))
