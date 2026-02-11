#!/usr/bin/env bb

(require '[babashka.deps :as deps])

(deps/add-deps '{:deps {hickory/hickory {:mvn/version "0.7.1"}}})

(require '[hickory.core :as h]
         '[hickory.select :as s]
         '[clojure.string :as str])

(defn html-file->tree [file-path]
  (-> file-path slurp h/parse h/as-hickory))

(defn extract-text [elem]
  (cond
    (string? elem) elem
    (map? elem) (->> elem :content (map extract-text) (str/join ""))
    (sequential? elem) (->> elem (map extract-text) (str/join ""))
    :else ""))

(declare format-inline-elements)

(defn element-to-markdown [elem]
  (case (:tag elem)
    :h1 (let [text (format-inline-elements (:content elem))]
           (format "# %s\n" (str/trim text)))
    :h2 (let [text (format-inline-elements (:content elem))]
           (format "## %s\n" (str/trim text)))
    :h3 (let [text (format-inline-elements (:content elem))]
           (format "### %s\n" (str/trim text)))
    :h4 (let [text (format-inline-elements (:content elem))]
           (format "#### %s\n" (str/trim text)))
    :h5 (let [text (format-inline-elements (:content elem))]
           (format "##### %s\n" (str/trim text)))
    :h6 (let [text (format-inline-elements (:content elem))]
           (format "###### %s\n" (str/trim text)))
    :p (let [text (format-inline-elements (:content elem))]
         (when (seq (str/trim text))
           (str (str/trim text) "\n")))
    :li (let [text (format-inline-elements (:content elem))]
          (format "- %s\n" (str/trim text)))
    :img (let [src (-> elem :attrs :src)
              alt (or (-> elem :attrs :alt) "image")]
           (when src
             (format "![%s](%s)\n" alt src)))
    :a (let [text (format-inline-elements (:content elem))
            href (-> elem :attrs :href)]
         (if href
           (format "[%s](%s)" text href)
           text))
    :em (format "*%s*" (format-inline-elements (:content elem)))
    :strong (format "**%s**" (format-inline-elements (:content elem)))
    :code (format "`%s`" (format-inline-elements (:content elem)))
    ""))

(defn format-inline-elements [content]
  (->> content
       (map (fn [x]
              (cond
                (string? x) x
                (map? x) (element-to-markdown x)
                (sequential? x) (format-inline-elements x)
                :else "")))
       (str/join "")))

(defn extract-title [tree]
  (->> (s/select (s/tag :h1) tree)
       first
       extract-text
       str/trim))

(defn html-to-markdown [file-path]
  (let [tree (html-file->tree file-path)
        title (extract-title tree)
        content-div (first (s/select (s/class "entry-content") tree))
        elements (when content-div
                   (s/select (s/or (s/tag :h1)
                                  (s/tag :h2)
                                  (s/tag :h3)
                                  (s/tag :h4)
                                  (s/tag :h5)
                                  (s/tag :h6)
                                  (s/tag :p)
                                  (s/tag :li)
                                  (s/tag :img)
                                  (s/tag :ul)
                                  (s/tag :ol)) content-div))
        markdown-content (when elements
                          (->> elements
                               (map element-to-markdown)
                               (str/join "\n")))]
    (when title
      (str "# " title "\n\n" (or markdown-content "")))))

(defn -main [& args]
  (let [file-path (first args)]
    (if file-path
      (if-let [result (html-to-markdown file-path)]
        (println result)
        (println "Failed to parse"))
      (println "Usage: bb html-to-markdown.clj <html-file>"))))

(comment
  (-main "tmp/scott-young-blog/stress-impacts-energy.html")
  :rcf)

(when (= *file* (System/getProperty "babashba.file"))
  (apply -main *command-line-args*))