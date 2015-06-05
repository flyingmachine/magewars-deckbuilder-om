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
  [{:keys [mage data]}]
  (om/update! data :selected-mage mage)
  (when-not (wizard? mage)
    (om/update! data :selected-element nil)))

(defn mage-li [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/li nil
        (dom/label nil
          (dom/input #js {:type "radio" :name "mage"
                          :onClick #(select-mage data)})
          (get-in data [:mage :class]))))))

(defn element-li
  [{:keys [element data]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/li nil
        (dom/label nil
          (dom/input #js {:type "radio" :name "element"
                          :onChange (fn [_] (om/update! data :selected-element element))})
          (name element))))))

(defn wizard-element [{:keys [selected-mage] :as data} data owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/ul #js {:className (if-not (wizard? selected-mage) "hidden")}
             (om/build-all element-li (map (fn [e se] {:element e :data data})
                                           elements
                                           (repeat data)))))))

(defn mage-stats [{:keys [selected-element selected-mage]} owner]
  (reify om/IRender
    (render [_]
      (dom/table #js {:className (if (empty? selected-mage) "hidden")
                      :id "mage-stats"}
                 (h/row "Spell points:" (:spellpoints selected-mage))
                 (h/row "Training" (:spellpoints selected-mage))))))

(defn mage-view [{:keys [mage]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h3 nil "Mage")
        (apply dom/ul nil
               (om/build-all mage-li (map (fn [m] {:mage m :data mage}) mages)))
        (om/build wizard-element mage)
        (om/build mage-stats mage)))))
