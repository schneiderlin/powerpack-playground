(ns hooks.my
  (:require [clj-kondo.hooks-api :as api]))

;; ---------------------------------------------------------------------------
;; Design system: read from config only (no file I/O).
;; Hooks run in sandboxed SCI; config is passed in and may contain
;; [:linters :hiccup-class-attr :design-system] with :allowed-prefixes,
;; :disallowed-patterns, and :prefer. Canonical source: design-system/design-tokens.edn
;; (copy into config or use a pre-process step).
;; ---------------------------------------------------------------------------

(defn- keyword-node-value [node]
  (:k node))

(defn- map-has-class-key? [node]
  (when (api/map-node? node)
    (let [children (:children node)]
      (some (fn [key-node]
              (when (api/keyword-node? key-node)
                (= :class (keyword-node-value key-node))))
            (take-nth 2 children)))))

(defn- class-value-node [attrs-node]
  (when (api/map-node? attrs-node)
    (let [children (:children attrs-node)]
      (loop [i 0]
        (when (< (inc i) (count children))
          (let [k (nth children i)
                v (nth children (inc i))]
            (if (and (api/keyword-node? k) (= :class (keyword-node-value k)))
              v
              (recur (+ i 2)))))))))

(defn- hiccup-vector-with-class? [node]
  (when (api/vector-node? node)
    (let [children (:children node)]
      (when (>= (count children) 2)
        (let [tag-node (first children)
              attrs-node (second children)]
          (when (and (api/keyword-node? tag-node)
                     (api/map-node? attrs-node))
            (map-has-class-key? attrs-node)))))))

;; clojure.core only: no clojure.string in SCI hook sandbox.
(defn- starts-with? [s prefix]
  (and (>= (count s) (count prefix))
       (= prefix (subs s 0 (count prefix)))))

(defn- includes? [s sub]
  (let [n (count s) m (count sub)]
    (when (>= n m)
      (loop [i 0]
        (when (<= i (- n m))
          (or (= sub (subs s i (+ i m)))
              (recur (inc i))))))))

(defn- token-allowed? [token {:keys [allowed-prefixes]}]
  (when (seq allowed-prefixes)
    (some (fn [prefix]
            (or (= token prefix)
                (starts-with? token prefix)))
          allowed-prefixes)))

(defn- token-disallowed? [token {:keys [disallowed-patterns]}]
  (when (seq disallowed-patterns)
    (some (fn [pattern]
            (or (includes? token pattern)
                (starts-with? token pattern)))
          disallowed-patterns)))

(defn- token-prefer [token {:keys [prefer]}]
  (get prefer token))

(defn- validate-class-string [class-str value-node design-system]
  (let [s (str class-str)
        tokens (seq (re-seq #"\S+" s))]
    (when tokens
      (doseq [token tokens]
        (cond
          (token-disallowed? token design-system)
          (api/reg-finding! (assoc (meta value-node)
                                  :type :hiccup-class-attr
                                  :message (str "Class '" token "' is disallowed by design system")))

          (token-prefer token design-system)
          (let [preferred (token-prefer token design-system)]
            (api/reg-finding! (assoc (meta value-node)
                                    :type :hiccup-class-attr
                                    :message (str "Use " preferred " instead of " token))))

          (and (seq (:allowed-prefixes design-system))
               (not (token-allowed? token design-system)))
          (api/reg-finding! (assoc (meta value-node)
                                  :type :hiccup-class-attr
                                  :message (str "Class '" token "' not in design system (allowed prefixes apply)"))))))))

(defn- traverse? [{:keys [node]}]
  (or (api/list-node? node)
      (api/vector-node? node)
      (api/map-node? node)
      (api/set-node? node)))

(defn- analyze* [{:keys [node lang config]}]
  (cond
    (hiccup-vector-with-class? node)
    (let [attrs-node (second (:children node))
          class-val-node (class-value-node attrs-node)
          design-system (get-in config [:linters :hiccup-class-attr :design-system] {})]
      (when (and class-val-node (seq design-system))
        (if (api/string-node? class-val-node)
          (validate-class-string (api/sexpr class-val-node) class-val-node design-system)
          nil))
      (doall (map #(analyze* {:node % :lang lang :config config}) (:children node))))

    (traverse? {:node node})
    (doall (map #(analyze* {:node % :lang lang :config config}) (:children node)))))

(defn analyze
  [{:keys [node lang config]}]
  (when (< 1 (count (:children node)))
    (analyze* {:node (last (:children node))
               :lang lang
               :config (or config {})}))
  node)
