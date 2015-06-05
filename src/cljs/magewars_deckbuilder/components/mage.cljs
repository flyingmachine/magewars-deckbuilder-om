(ns magewars-deckbuilder.components.mage
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [magewars-deckbuilder.components.helpers :as h]
            [clojure.string :as s]))

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
  [{:keys [element data wizard]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/li nil
        (dom/label nil
          (dom/input #js {:type "radio" :name "element"
                          :disabled (not wizard)
                          :checked (= (:selected-element data) element)
                          :onChange (fn [_] (om/update! data :selected-element element))})
          (name element))))))

(defn wizard-element [{:keys [selected-mage] :as data} data owner]
  (reify
    om/IRender
    (render [_]
      (let [wizard (wizard? selected-mage)]
        (apply dom/ul #js {:className (if-not wizard "disabled")}
               (om/build-all element-li (map (fn [e se] {:element e :data data :wizard wizard})
                                             elements
                                             (repeat data))))))))

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

(defn load-default
  [mage deck pool]
  (om/transact! pool :counts #(merge-with + % (:counts deck)))
  (om/update! deck :counts (:default-deck mage))
  (om/transact! pool :counts
                (fn [c]
                  (->> (merge-with - c (:default-deck mage))
                       (filter #(> (second %) 0))
                       (into {})))))

(defn mage-stats [{:keys [mage-selection cards-by-name deck pool]} owner]
  (reify om/IRender
    (render [_]
      (let [{:keys [selected-mage selected-element]} mage-selection]
        (dom/div nil
          (dom/table #js {:className (if (empty? selected-mage) "disabled")
                          :id "mage-stats"}
                     (h/row "Spell points" (str (used-spellpoints selected-mage selected-element cards-by-name deck)
                                                "/"
                                                (:spellpoints selected-mage)))
                     (h/row "Training" (training selected-mage selected-element))
                     (h/row "Opposition" (display-kws (:opposition selected-mage))))
          (dom/div nil
            (dom/button #js {:onClick #(load-default selected-mage deck pool)}
                        (str "Load default " (:class selected-mage) " deck"))))))))

(defn download-view [mage element deck]
  (println deck)
  (js/encodeURIComponent
   (str "Mage: " (:class mage) "\n"
        (if element (str "Element: " (name element) "\n"))
        "\nCards:\n"
        (apply str
               (map (fn [[name c]]
                      (str c " " name "\n"))
                    (sort-by first (:counts deck)))))))

(defn download
  [{:keys [mage deck]}]
  (set! (.-location js/document)
        (str "data:Application/octet-stream,"
             (download-view (:selected-mage mage)
                            (:selected-element mage)
                            deck))))

(defn mage-view [{:keys [mage-selection mages] :as app} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h3 nil "Mage")
        (apply dom/ul nil
               (om/build-all mage-li (map (fn [m] {:mage m :data mage-selection}) mages)))
        (om/build wizard-element mage-selection)
        (om/build mage-stats app)
        (dom/button #js {:onClick (fn [_] (download app))
                         :download "mage"}
                    "Download")))))
