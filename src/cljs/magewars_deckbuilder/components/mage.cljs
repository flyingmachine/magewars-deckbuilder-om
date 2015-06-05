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

(defn training-list
  [mage element]
  (if element
    (conj (:training mage) element)
    (:training mage)))

(defn display-kws
  [kws]
  (->> kws
       sort
       (map name)
       (s/join ", ")))

(defn training
  [selected-mage selected-element]
  (display-kws (training-list selected-mage selected-element)))

;; TODO make more elegant?
(defn spellpoints
  [training opposition card]
  (let [school (get-in card [:display :school])
        school (if (keyword? (first school)) [school] school)]
    (reduce (fn [sum [school cost]]
              (+ sum
                 (cond (contains? training school) cost
                       (contains? opposition school) (* 3 cost)
                       :else (* 2 cost))))
            0
            school)))

(defn used-spellpoints
  [mage element cards-by-name deck]
  (let [training (training-list mage element)
        opposition (:opposition mage)]
    (reduce (fn [sum [name c]]
              (+ sum (* c (spellpoints training opposition (get cards-by-name name)))))
            0 
            (:counts deck))))

(defn mage-stats [{:keys [mage cards-by-name deck]} owner]
  (reify om/IRender
    (render [_]
      (let [{:keys [selected-mage selected-element]} mage]
        (dom/table #js {:className (if (empty? selected-mage) "hidden")
                        :id "mage-stats"}
                   (h/row "Spell points:" (str (used-spellpoints selected-mage selected-element cards-by-name deck)
                                               "/"
                                               (:spellpoints selected-mage)))
                   (h/row "Training" (training selected-mage selected-element))
                   (h/row "Opposition" (display-kws (:opposition selected-mage))))))))

(defn download-view [mage element deck]
  (println deck)
  (js/encodeURIComponent
   (str "Mage: " (:class mage) "\n"
        (if element (str "Element: " (name element) "\n"))
        "\nCards:\n"
        (apply str
               (map (fn [[name c]]
                      (str name " " c "\n"))
                    (sort-by first (:counts deck)))))))

(defn download
  [{:keys [mage deck]}]
  (set! (.-location js/document)
        (str "data:Application/octet-stream,"
             (download-view (:selected-mage mage)
                            (:selected-element mage)
                            deck))))

(defn mage-view [{:keys [mage] :as app} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h3 nil "Mage")
        (apply dom/ul nil
               (om/build-all mage-li (map (fn [m] {:mage m :data mage}) mages)))
        (om/build wizard-element mage)
        (om/build mage-stats app)
        (dom/button #js {:onClick (fn [_] (download app))
                         :download "mage"}
                    "Download")))))
