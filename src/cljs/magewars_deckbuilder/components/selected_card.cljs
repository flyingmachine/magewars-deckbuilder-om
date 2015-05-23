(ns magewars-deckbuilder.components.selected-card
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as s]))

(defprotocol CardDetails
  (c-type [x])
  (target [x]))

(extend-protocol CardDetails
  cljs.core.Keyword
  (c-type [x] (name x))
  (target [x] (name x))

  cljs.core.PersistentVector
  (c-type [x] (str (first x) ": " (second x)))
  (target [x] (s/join ", " (map name x))))

(defmulti schools (fn [s] (type (first s))))
(defmethod schools cljs.core.Keyword
  [[school level]]
  (str (name school) ", level " level))
(defmethod schools :default
  [s]
  (s/join "; " (map schools s)))


(defn display-many
  [x]
  (->> x
       sort
       (map name)
       (clojure.string/join ", ")))

(defn row
  [x y]
  (dom/tr nil
    (dom/td nil x)
    (dom/td nil y)))

(defn card-details
  [c]
  (dom/table nil
    (row "Type" (c-type (:type c)))
    (row "Subtypes" (display-many (:subtypes c)))
    (row "Schools" (schools (:school c)))
    (row "Casting Cost" (:casting-cost c))
    (row "Speed" (name (:speed c)))
    (row "Targets" (s/join "; " (map target (:targets c))))
    (row "Description" (:description c))))

(defn selected-card-view [{:keys [selected-card cards-by-name]} owner]
  (reify
    om/IRender
    (render [_]
      (if-let [card (get cards-by-name selected-card)]
        (dom/div nil
          (dom/h3 nil (:name card))
          (dom/img #js {:src (str "images/" (get-in card [:display :image]))})
          (dom/div #js {:className "card-details"}
                   (card-details (:display card))))
        (dom/div nil)))))
