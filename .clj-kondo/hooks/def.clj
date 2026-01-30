(ns hooks.def
  (:require [clj-kondo.hooks-api :as api]))

(defn- fn-token-node? [{:keys [node lang]}]
  ;; ① Check that the node is a symbol token node
  (when (and (api/token-node? node)
             (symbol? (:value node)))

    ;; ② Resolve the symbol to `ns` and `name`
    (let [{:keys [ns name]} (api/resolve {:name (:value node)})

          ;; ③ Get the _cached_ analysis data
          analysis (get-in (api/ns-analysis ns {:lang lang}) [lang name])]

      ;; ④ Ignore `clojure.core` and `cljs.core`
      (when-not (#{'clojure.core
                   'cljs.core} ns)

        ;; ⑤ Return truthy if the analysis data looks like it's a function
        (some (set (keys analysis)) [:fixed-arities :varargs-min-arity])))))

(defn- traverse? [{:keys [node]}]
  (or
   (api/vector-node? node)
   (api/map-node? node)
   (api/set-node? node)))

(defn- analyze* [{:keys [node lang]}]
  (cond

    ;; ① Call predicate function that returns true for symbols pointing to functions
    (fn-token-node? {:node node
                     :lang lang})

    ;; ② Record finding if predicate returns truthy
    (api/reg-finding! (assoc (meta node)
                             :type :fn-sym-in-def
                             :message (str "fn-sym-in-def: "
                                           node
                                           (if (= :cljs lang)
                                             " - use function wrapping"
                                             " - use var quoting"))))
    
    
    ;; ① Check if we should recursively traverse
    (traverse? {:node node}) 
    
    ;; ② Traverse all child nodes.
    ;; Note the `doall` which is needed because `reg-finding!` is
    ;; a side-effecting function inside `map`
    (doall (map #(analyze* {:node % :lang lang}) (:children node)))))

(defn analyze
  [{:keys [node lang]}]

  ;; ① Call `analyze*` with the last child node
  (when (< 1 (count (:children node)))
    (analyze* {:node (last (:children node))
               :lang lang}))

  ;; ② Return the `node` without transforming it.
  node)
