(ns magewars-deckbuilder.components.mage
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
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

(defn mage-view [{:keys [selected-mage] :as app} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h3 nil "Mage")
        (apply dom/ul nil (om/build-all mage-li (map (fn [m sm] {:mage m
                                                                :selected-mage sm})
                                                     mages
                                                     (repeat selected-mage))))
        (om/build wizard-element (select-keys app [:selected-mage :selected-element]))))))

;; 
