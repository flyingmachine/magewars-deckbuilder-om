(ns magewars-deckbuilder.app
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-sync.core :refer [om-sync]]
            [om-sync.util :refer [tx-tag edn-xhr]]
            [cljs.core.async :as async :refer [chan]]
            [magewars-deckbuilder.filtering :as f]
            [magewars-deckbuilder.components.card-list :as cl]
            [magewars-deckbuilder.components.filter-selection :as fs]))

(enable-console-print!)

(def app-state
  (atom {:selected-filters (f/empty-filters f/attribute-filter-types)}))

(defn row
  [x y]
  (dom/tr nil
    (dom/td nil x)
    (dom/td nil y)))

(defn display-many
  [x]
  (->> x
       sort
       (map name)
       (clojure.string/join ", ")))

(defn card-details
  [c]
  (dom/table nil
    (row "Type" (name (:type c)))
    (row "Subtypes" (display-many (:subtypes c)))
    (row "School" (name (first (:school c))))
    (row "Level" (second (:school c)))
    (row "Casting Cost" (:casting-cost c))
    (row "Speed" (name (:speed c)))
    (row "Targets" (display-many (:targets c)))))

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





(edn-xhr
 {:method :get
  :url "/cards"
  :on-complete
  (fn [cards]
    (swap! app-state
           (fn [state cards]
             (let [cindex (f/card-index cards)]
               (merge state {:cards cards
                             :cards-by-name (into {} (map (juxt :name identity) cards))
                             :cindex cindex
                             :deck {:title "Deck"
                                    :counts {}}
                             :pool {:title "Pool"
                                    :counts (into {} (map (juxt :name :count) cards))}})))
           cards)

    (om/root fs/filter-list app-state
             {:target (.getElementById js/document "filters")
              :shared {:toggle-filter (chan)}})
    (om/root cl/all-cards-view app-state
             {:target (.getElementById js/document "cards")})
    (om/root selected-card-view app-state
             {:target (.getElementById js/document "selected-card")}))})
