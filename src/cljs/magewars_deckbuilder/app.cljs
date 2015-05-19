(ns magewars-deckbuilder.app
  (:require [cljs.core.async :as async :refer [put! chan alts!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-sync.core :refer [om-sync]]
            [om-sync.util :refer [tx-tag edn-xhr]]
            [magewars-deckbuilder.filtering :as f]))

(enable-console-print!)

(def app-state
  (atom {:selected-filters (f/empty-filters f/attribute-filter-types)}))

(defn app-view [app owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/ul nil
             (map #(dom/li nil (:name %)) (:cards app))))))


(edn-xhr
 {:method :get
  :url "/cards"
  :on-complete
  (fn [cards]
    (swap! app-state
           (fn [state cards]
             (let [cindex (f/card-index cards)]
               (merge state {:cards cards
                             :cindex cindex
                             :deck-count {}
                             :pool-count (into {} (map (juxt :name :count) cards))})))
           cards)
    (om/root app-view app-state
             {:target (.getElementById js/document "app")}))})
