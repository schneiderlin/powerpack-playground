(ns lint-rules
  "示例: 自定义 lint 规则

   规则函数接收 hiccup 节点 map: {:tag :div :attrs {:class \"...\"} :children [...]}"
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
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
;; 设计系统 tokens 规则 (基于 design-system/tokens.edn)
;; ============================================================================

(defn get-tokens
  "Load design tokens from design-system/tokens.edn (relative to project root).
   Read from disk each time so token updates are picked up without REPL restart."
  []
  (let [paths [(io/file "design-system/tokens.edn")
               (io/file (System/getProperty "user.dir") "design-system/tokens.edn")]]
    (some (fn [f]
            (when (and f (.exists f))
              (edn/read-string (slurp f))))
          paths)))

(defn check-design-tokens [{:keys [attrs]}]
  (when-let [class-attr (get attrs :class)]
    (let [classes (->> (str/split (str class-attr) #"\s+")
                       (remove str/blank?))
          t (get-tokens)]
      (when (seq classes)
        (let [disallowed (for [c classes
                              p (or (:disallowed-patterns t) [])
                              :when (or (str/includes? c p)
                                        (str/starts-with? c p))]
                          {:severity :error
                           :msg (str "Class \"" c "\" matches disallowed pattern: " p)})
              prefer (for [c classes
                          :when (contains? (or (:prefer t) {}) c)]
                      {:severity :warning
                       :msg (str "Prefer \"" (get-in t [:prefer c]) "\" instead of \"" c "\"")})
              allowed-prefixes (or (:allowed-prefixes t) [])
              not-allowed (when (seq allowed-prefixes)
                            (for [c classes
                                  :when (not (some #(or (= c %) (str/starts-with? c %))
                                                   allowed-prefixes))]
                              {:severity :warning
                               :msg (str "Class \"" c "\" is not in allowed-prefixes")}))
              all (concat disallowed prefer not-allowed)]
          (when (seq all)
            (let [severity (if (some #(= :error (:severity %)) all) :error :warning)
                  message (str/join "; " (map :msg all))]
              {:severity severity
               :message (str "Design tokens: " message)})))))))

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
  (lint/register-rule! :design-tokens check-design-tokens)
  (println "Example lint rules registered successfully!"))

(comment
  (register-example-rules!)
  (lint/get-rules)
  (lint/clear-rules!)
  :rcf)
