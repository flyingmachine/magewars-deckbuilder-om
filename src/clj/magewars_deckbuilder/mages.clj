(ns magewars-deckbuilder.mages
  (:require [magewars-deckbuilder.utils :refer :all]))

(def all (mapv #(read-edn "data/mages/" %) [:beastmaster :priestess :warlock :wizard]))
