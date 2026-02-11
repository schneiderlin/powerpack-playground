(ns fetch-specific-article
  (:require [babashka.curl :as curl]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def output-dir "tmp/scott-young-blog")

(defn fetch-article [url]
  (println "Fetching:" url)
  (try
    (let [response (curl/get url {:headers {"User-Agent" "Mozilla/5.0"}
                                   :compressed false
                                   :throw false})]
      (if (= 200 (:status response))
        (:body response)
        (do
          (println "Error:" (:status response))
          nil)))
    (catch Exception e
      (println "Exception:" (.getMessage e))
      nil)))

(defn save-article [url content]
  (when content
    (let [slug (-> url
                   (str/split #"/")
                   last)]
      (let [file-path (str output-dir "/" slug ".html")]
        (io/make-parents file-path)
        (spit file-path content)
        (println "Saved:" file-path)))))

(defn -main []
  (let [url "https://www.scotthyoung.com/blog/2017/08/09/two-kinds-of-difficulty/"
        content (fetch-article url)]
    (save-article url content)))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
