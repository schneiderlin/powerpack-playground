#!/usr/bin/env bb
(ns fetch-all-links
  (:require [babashka.curl :as curl]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def output-dir "tmp/scott-young-blog")
(def all-slug-path (str output-dir "/all-slug.html"))

;; Extract blog article URLs from all-slug.html (href="https://www.scotthyoung.com/blog/...")
(def blog-url-pattern #"href=\"(https://www\.scotthyoung\.com/blog/[^\"]+)\"")

(defn extract-links [html]
  (->> (re-seq blog-url-pattern html)
       (map second)
       (map #(str/replace % #"/$" "")) ; strip trailing slash
       distinct
       vec))

(defn slug-from-url [url]
  (-> url (str/split #"/") last))

(defn fetch-article [url]
  (println "Fetching:" url)
  (try
    (let [response (curl/get url {:headers {"User-Agent" "Mozilla/5.0"}
                                  :compressed false
                                  :throw false})]
      (if (= 200 (:status response))
        (:body response)
        (do
          (println "  Error status:" (:status response))
          nil)))
    (catch Exception e
      (println "  Exception:" (.getMessage e))
      nil)))

(defn save-article [url content]
  (when content
    (let [slug (slug-from-url url)
          file-path (str output-dir "/" slug ".html")]
      (io/make-parents file-path)
      (spit file-path content)
      (println "  Saved:" file-path))))

(defn -main []
  (println "Reading" all-slug-path)
  (when-not (io/file all-slug-path)
    (println "File not found:" all-slug-path)
    (System/exit 1))
  (let [html (slurp all-slug-path)
        urls (extract-links html)]
    (println "Found" (count urls) "links")
    (doseq [url urls]
      (when-let [content (fetch-article url)]
        (save-article url content))
      ;; be nice to the server
      (Thread/sleep 500))))

(comment
  (-main) 
  :rcf)


(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
