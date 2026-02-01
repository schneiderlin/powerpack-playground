(ns debug
  "Debug logging utilities for Clojure applications")

(def ^:private log-atom (atom []))
(def ^:private log-file ".debug.log")

(defn- ensure-log-file []
  (when-not (.exists (java.io.File. log-file))
    (spit log-file "")))

(defn- persist-log []
  (ensure-log-file)
  (spit log-file (pr-str @log-atom)))

(defn- load-log []
  (ensure-log-file)
  (let [content (slurp log-file)]
    (if (empty? content)
      []
      (try
        (read-string content)
        (catch Exception _ [])))))

(defn debug-log
  "Log any EDN data to the debug log.

  Arity 1: Log data
  (debug-log {:hypothesis \"A\" :data \"xyz\"})
  => nil"
  [data]
  (swap! log-atom conj {:data data
                        :timestamp (System/currentTimeMillis)})
  (persist-log)
  nil)

(defn read-log
  "Read all logs or filter by predicate.

  Read all logs:
  (read-log)
  => [{:data {:hypothesis \"A\" :data \"xyz\"} :timestamp 1234567890} ...]

  Filter logs:
  (read-log {:hypothesis \"A\"})
  => [{:data {:hypothesis \"A\" :data \"xyz\"} :timestamp 1234567890} ...]"
  ([]
   (map :data @log-atom))
  ([filter-map]
   (filter #(and (map? %)
                 (every? (fn [[k v]] (= (get % k) v)) filter-map))
           (map :data @log-atom))))

(defn clear-log
  "Clear all debug logs.

  (clear-log)
  => \"log cleared\""
  []
  (reset! log-atom [])
  (ensure-log-file)
  (spit log-file "")
  "log cleared")

(defn reload-log
  "Reload logs from file. Useful if logs were modified externally."
  []
  (reset! log-atom (load-log)))

;; Initialize logs on namespace load
(reload-log)
