(ns magewars-deckbuilder.components.wizard
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as s]))

(def wizards
  [{:class "Beastmaster"
    :spellpoints 120
    :life 36
    :armor 0
    :channeling 9
    :training #{:nature}
    :opposition #{:fire}}
   {:class "Priestess"
    :spellpoints 120
    :life 32
    :armor 0
    :channeling 10
    :training #{:holy}
    :opposition #{:dark}}
   {:class "Warlock"
    :spellpoints 120
    :life 38
    :armor 0
    :channeling 9
    :training #{:dark :fire}
    :opposition #{:holy}}
   {:class "Wizard"
    :spellpoints 120
    :life 32
    :armor 0
    :channeling 10
    :training #{:arcane}
    :opposition #{}}])

(def elements [:fire :earth :air :water])

(defn wizard-li
  [wizard]
  (dom/li nil
    (dom/label nil
      (dom/input #js {:type "radio" :name "wizard"})
      (:class wizard))))

(defn wizard-view [_ owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h3 nil "Wizard")
        (apply dom/ul nil (map wizard-li wizards))
        ()))))
