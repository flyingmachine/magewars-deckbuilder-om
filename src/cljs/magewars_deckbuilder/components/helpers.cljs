(ns magewars-deckbuilder.components.helpers
  (:require [om.dom :as dom :include-macros true]))

(defn row
  [& cells]
  (apply dom/tr nil
         (map (fn [x] (dom/td nil x)) cells)))
