(ns magewars-spellbook.app
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-sync.core :refer [om-sync]]
            [om-sync.util :refer [tx-tag edn-xhr]]
            [cljs.core.async :as async :refer [chan]]
            [magewars-spellbook.filtering :as f]
            [magewars-spellbook.data.all :refer [data]]
            [magewars-spellbook.components.card-list :as cl]
            [magewars-spellbook.components.filter-selection :as fs]
            [magewars-spellbook.components.selected-card :as sc]
            [magewars-spellbook.components.mage :as m]))

(enable-console-print!)

(def app-state
  (let [{:keys [cards mages]} data
        cindex (f/card-index cards)]
    (atom {:selected-filters (f/empty-filters f/attribute-filter-types)
           :mage-selection {:selected-mage nil
                            :selected-element nil}
           :cards cards
           :cards-by-name (into {} (map (juxt :name identity) cards))
           :cindex cindex
           :deck {:title "Spellbook"
                  :counts {}}
           :pool {:title "Pool"
                  :counts (into {} (map (juxt :name :count) cards))}
           :mages mages})))

(om/root fs/filter-list app-state
         {:target (.getElementById js/document "filters")
          :shared {:toggle-filter (chan)}})
(om/root m/mage-view app-state
         {:target (.getElementById js/document "mage")})
(om/root cl/all-cards-view app-state
         {:target (.getElementById js/document "cards")})
(om/root sc/selected-card-view app-state
         {:target (.getElementById js/document "selected-card")})
