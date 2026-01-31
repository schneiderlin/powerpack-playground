(ns powerblog.lint
  (:require [clojure.string :as str]))

;; ============================================================================
;; 规则注册机制
;; ============================================================================

(defonce ^:private registered-rules (atom {}))

(defn register-rule!
  "注册一个 lint 规则

   参数:
   - name: 规则的关键字标识符
   - rule-fn: 规则函数，接收一个 node map {:tag :div :attrs {:class \"...\"} :children [...]}，返回 violation map 或 nil

   rule-fn 返回的 violation map 格式:
   {:severity :error/:warning
    :element \"标签名\"
    :attributes {:attr1 \"val1\"}
    :message \"问题描述\"}

   或者返回 nil 表示没有问题"
  [name rule-fn]
  (swap! registered-rules assoc name rule-fn))

(defn get-rules []
  @registered-rules)

(defn clear-rules! []
  (reset! registered-rules {}))

;; ============================================================================
;; Hiccup 解析与遍历
;; ============================================================================

(defn element? [form]
  (and (vector? form)
       (pos? (count form))
       (keyword? (first form))))

(defn- expand-children
  "Flatten children so that sequence children (e.g. layout content) are traversed.
   Hiccup often has ([:parent ...] (child1 child2 ...)) — we recurse into each element in the seq."
  [children]
  (mapcat (fn [c]
            (cond (element? c) [c]
                  (seq? c)     (filter element? c)
                  :else        []))
          children))

(defn- parse-element
  "Parse hiccup element [:tag attrs? & children] into {:tag :attrs :children}."
  [form]
  (when (element? form)
    (let [tag (first form)
          rest (next form)
          [attrs children] (if (and (seq rest) (map? (first rest)))
                             [(first rest) (next rest)]
                             [{} rest])]
      {:tag tag :attrs (or attrs {}) :children (or children ())})))

(defn- node-info [{:keys [tag attrs]}]
  {:element (str "<" (name tag) ">")
   :attributes (when (seq attrs) (into {} (map (fn [[k v]] [(keyword k) (str v)]) attrs)))
   :text (when-let [text (some-> attrs :class str)]
           (when-not (str/blank? text)
             (str/trim (subs text 0 (min 50 (count text))))))})

;; ============================================================================
;; Lint 执行引擎
;; ============================================================================

(defn apply-rules-to-node
  "对单个节点应用所有规则，返回 violations 列表"
  [node]
  (let [rules (get-rules)]
    (reduce-kv
     (fn [violations rule-name rule-fn]
       (try
         (if-let [violation (rule-fn node)]
           (conj violations (assoc violation :rule-name rule-name))
           violations)
         (catch Exception e
           (println (str "Error in rule " rule-name ": " (ex-message e)))
           violations)))
     []
     rules)))

(defn collect-violations
  "遍历 hiccup 树并收集所有 violations"
  [hiccup]
  (when (element? hiccup)
    (let [node (parse-element hiccup)
          {:keys [children]} node
          violations (apply-rules-to-node node)
          enriched (map #(merge % (node-info node)) violations)
          expanded (expand-children children)
          child-violations (mapcat collect-violations expanded)]
      (concat enriched child-violations))))

(comment
  (require '[powerblog.render :as render])
  (parse-element render/!debug)
  :rcf)

;; ============================================================================
;; 报告接口
;; ============================================================================

(defn- severity->color [severity]
  (case severity
    :error "\u001B[31m"   ; Red
    :warning "\u001B[33m" ; Yellow
    "\u001B[0m"))        ; Reset

(defn- severity->label [severity]
  (case severity
    :error "[ERROR]"
    :warning "[WARN]"
    "[INFO]"))

(defn print-violation
  "在控制台打印单个 violation"
  [violation]
  (let [{:keys [severity rule-name element attributes message]} violation
        color (severity->color severity)
        reset "\u001B[0m"
        label (severity->label severity)]
    (println (str color label reset " " (name rule-name) " - " message))
    (println (str "  Element: " element))
    (when (seq attributes)
      (println "  Attributes:")
      (doseq [[k v] attributes]
        (println (str "    " (name k) ": \"" v "\""))))))

(defn report-violations
  "报告所有 violations 到控制台"
  [violations]
  (if (empty? violations)
    (println "✓ No attribute lint issues found")
    (let [error-count (count (filter #(= :error (:severity %)) violations))
          warning-count (count (filter #(= :warning (:severity %)) violations))]
      (println (str "\n🔍 Attribute Lint Results:"))
      (println (str "  " error-count " errors, " warning-count " warnings\n"))
      (doseq [violation violations]
        (print-violation violation))
      (println))))

(defn run-lint
  "对 hiccup 数据运行 lint：收集 violations 并报告"
  [hiccup]
  (let [violations (collect-violations hiccup)]
    (report-violations violations)
    violations))

(comment
  (require '[powerblog.render :as render])
  render/!debug
  :rcf)

