(ns magewars-deckbuilder.app
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-sync.core :refer [om-sync]]
            [om-sync.util :refer [tx-tag edn-xhr]]
            [cljs.core.async :as async :refer [chan]]
            [magewars-deckbuilder.filtering :as f]
            [magewars-deckbuilder.components.card-list :as cl]
            [magewars-deckbuilder.components.filter-selection :as fs]))
(declare val-list)

(enable-console-print!)

(def app-state
  (atom {:selected-filters (f/empty-filters f/attribute-filter-types)}))

(defn app-view [{:keys [deck pool] :as app} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (om/build fs/filter-list app)
        (dom/div #js {:className "cards"}
                 (om/build cl/card-list-view {:app app :src deck :dest pool})
                 (om/build cl/card-list-view {:app app :src pool :dest deck}))))))

(defn all-cards-view [{:keys [deck pool] :as app} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (om/build cl/card-list-view {:app app :src deck :dest pool})
        (om/build cl/card-list-view {:app app :src pool :dest deck})))))

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
    (om/root all-cards-view app-state
               {:target (.getElementById js/document "cards")}))})
