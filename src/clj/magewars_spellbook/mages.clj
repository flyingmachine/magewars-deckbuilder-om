(ns magewars-spellbook.mages
  (:require [magewars-spellbook.utils :refer :all]))

(def all (mapv #(read-edn "data/mages/" %) [:beastmaster :priestess :warlock :wizard]))
