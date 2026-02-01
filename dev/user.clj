(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [dev])
  (:import java.util.regex.Pattern))

(defn find-usages
  "Find all usages of a symbol in Clojure source files.
  Usage: (find-usages 'my-ns/my-var) or (find-usages 'my-var)"
  [sym]
  (println "not implemented"))

