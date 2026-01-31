(ns dev.lint-rules
  "示例: 自定义 lint 规则

   规则函数接收 hiccup 节点 map: {:tag :div :attrs {:class \"...\"} :children [...]}"
  (:require
   [clojure.string :as str]
   [powerblog.lint :as lint]))

;; ============================================================================
;; 示例规则 1: 检查 class 命名是否遵循 kebab-case
;; ============================================================================

(defn check-class-naming [{:keys [attrs]}]
  (when-let [class-attr (get attrs :class)]
    (let [classes (str/split (str class-attr) #"\s+")
          invalid-classes (filter (fn [c]
                                   (and (not (str/blank? c))
                                        (re-find #"[A-Z]" c)))
                                 classes)]
      (when (seq invalid-classes)
        {:severity :warning
         :message (str "Class names should be kebab-case. Invalid: " (str/join ", " invalid-classes))}))))

;; ============================================================================
;; 示例规则 2: 检查 img 元素是否有 alt 属性
;; ============================================================================

(defn check-img-alt [{:keys [tag attrs]}]
  (when (= tag :img)
    (when-not (contains? attrs :alt)
      {:severity :error
       :message "img element must have an alt attribute"})))

;; ============================================================================
;; 示例规则 3: 检查是否有内联 style 属性
;; ============================================================================

(defn check-inline-style [{:keys [attrs]}]
  (when (contains? attrs :style)
    {:severity :warning
     :message "Avoid inline styles. Use classes instead"}))

;; ============================================================================
;; 注册示例规则
;; ============================================================================

(defn register-example-rules!
  "注册所有示例规则"
  []
  (println "Registering example lint rules...")
  (lint/register-rule! :class-naming check-class-naming)
  (lint/register-rule! :img-alt check-img-alt)
  (lint/register-rule! :no-inline-style check-inline-style)
  (println "Example lint rules registered successfully!"))

(comment
  (register-example-rules!)
  (lint/get-rules)
  (lint/clear-rules!)
  :rcf)
