(ns magewars-deckbuilder.components.mage
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [magewars-deckbuilder.components.helpers :as h]
            [clojure.string :as s]))

(def mages
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
(defn wizard? [x] (= "Wizard" (:class x)))

(defn select-mage
  [mage select-mage]
  (om/update! select-mage mage))

(defn mage-li [{:keys [mage selected-mage]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/li nil
        (dom/label nil
          (dom/input #js {:type "radio" :name "mage"
                          :onClick #(om/update! selected-mage mage)})
          (:class mage))))))

(defn element-li
  [element selected-element]
  (dom/li nil
    (dom/label nil
      (dom/input #js {:type "radio" :name "element"})
      (name element))))

(defn wizard-element [{:keys [selected-mage selected-element]} owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/ul #js {:className (if-not (= "Wizard" (:class selected-mage)) "hidden")}
             (map element-li elements (repeat selected-element))))))

(defn mage-stats [{:keys [selected-element selected-mage]} owner]
  (reify om/IRender
    (render [_]
      (dom/table #js {:className (if (empty? selected-mage) "hidden")
                      :id "mage-stats"}
                 (h/row "Spell points:" (:spellpoints selected-mage))
                 (h/row "Training" (:spellpoints selected-mage))))))

(defn mage-view [app owner]
  (reify
    om/IRender
    (render [_]
      (let [data (select-keys app [:selected-mage :selected-element])]
        (dom/div nil
          (dom/h3 nil "Mage")
          (apply dom/ul nil (om/build-all mage-li (map #(merge data {:mage %}) mages)))
          (om/build wizard-element data)
          (om/build mage-stats data))))))

;; 
