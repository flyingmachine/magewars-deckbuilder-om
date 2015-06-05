(ns magewars-deckbuilder.utils
  (:require [clojure.java.io :as io]))

;; reading static card sets
(defn slurp-resource
  [path]
  (-> path
      io/resource
      slurp))

(defn read-resource
  [path]
  (-> path
      slurp-resource
      read-string))

(defn read-edn
  [& pieces]
  (->> (concat pieces [".edn"])
       (map name)
       (clojure.string/join "")
       read-resource))
