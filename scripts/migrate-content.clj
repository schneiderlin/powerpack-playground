#!/usr/bin/env bb
(ns migrate-content
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn normalize-line-endings [s]
  (str/replace s #"\r\n" "\n"))

(defn parse-yaml-pairs [yaml]
  (let [lines (str/split-lines yaml)]
    (into {}
          (for [line lines
                :when (not (str/blank? line))
                :when (not (str/starts-with? line "---"))
                :let [[k v] (str/split (str/replace line #":\s+" ":") #":" 2)
                      k' (keyword "page" (str/lower-case (str/trim k)))
                      v' (when v (str/trim v))]
                :when v']
            [k' v']))))

(defn parse-yaml-frontmatter [content]
  (let [content (normalize-line-endings content)
        frontmatter-regex #"(?s)^---\n(.*?)\n---"
        frontmatter-match (re-find frontmatter-regex content)]
    (if frontmatter-match
      (let [yaml-content (second frontmatter-match)
            body-content (str/replace-first content frontmatter-regex "")]
        {:frontmatter (parse-yaml-pairs yaml-content)
         :body body-content})
      {:frontmatter {}
       :body content})))

(defn parse-tags [tags-str]
  (when tags-str
    (-> tags-str
        (str/replace #"\[|\]" "")
        (str/replace #"\"" "")
        (str/replace #"\n" " ")
        (str/split #",\s*")
        (->> (mapv (comp keyword str/trim))))))

(defn convert-frontmatter-to-edn [frontmatter]
  (let [tags (parse-tags (get frontmatter :page/tags))]
    {:page/title (or (get frontmatter :page/title) "Untitled")
     :page/description (or (get frontmatter :page/description) "")
     :page/date (or (get frontmatter :page/date) "")
     :blog-post/tags tags
     :blog-post/author {:person/id :jan}}))

(defn kebab-case [s]
  (-> s
      (str/replace #"_+" "-")
      (str/replace #"\.mdx?" ".md")
      (str/lower-case)))

(defn migrate-file [input-path output-path]
  (println "Migrating:" input-path)
  (let [content (slurp input-path)
        {:keys [frontmatter body]} (parse-yaml-frontmatter content)
        edn-frontmatter (convert-frontmatter-to-edn frontmatter)]
    (spit output-path
          (str/join "\n"
                    [(str ":page/title " (pr-str (:page/title edn-frontmatter)))
                     (str ":page/description " (pr-str (:page/description edn-frontmatter)))
                     (str ":page/date " (pr-str (:page/date edn-frontmatter)))
                     (str ":blog-post/tags " (pr-str (:blog-post/tags edn-frontmatter)))
                     ":blog-post/author {:person/id :jan}"
                     ":page/body"
                     ""]))
    (spit output-path (str/trim body) :append true)
    (println "  ->" output-path)))

(defn -main []
  (println "Migrating blog posts...")
  (let [source-dir "linzihao/src/content/blog"
        target-dir "content/blog-posts"]
    (.mkdirs (io/file target-dir))
    (doseq [lang ["en" "zh"]
            :let [lang-dir (io/file source-dir lang)]
            :when (.exists lang-dir)
            file (file-seq lang-dir)
            :when (and (.isFile file)
                       (re-find #"\.mdx?$" (.getName file)))]
      (let [input-path (.getPath file)
            filename (.getName file)
            output-filename (kebab-case filename)
            output-path (io/file target-dir output-filename)]
        (migrate-file input-path output-path))))
  (println "Migration complete!"))

(when *command-line-args*
  (-main))
