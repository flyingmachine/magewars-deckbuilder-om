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

(defn remove-card
  [data card-name src]
  (let [count (get-in data [src :counts card-name])]
    (if (= 1 count)
      (assoc-in data [src :counts] (dissoc (get-in data [src :counts]) card-name))
      (update-in data [src :counts card-name] dec))))

(defn add-card
  [data card-name dest]
  (update-in data [dest :counts card-name] (fnil inc 0)))

(defn move-card
  [data card-name src dest]
  (-> data
    (remove-card card-name src)
    (add-card card-name dest)))

(defn move-card!
  [data card-name src dest]
  (om/transact! data #(move-card % card-name src dest)))

(defn card-list [{:keys [app src dest]} owner]
  (let [{:keys [title counts]} (src app)]
    (reify
      om/IRender
      (render [_]
        (dom/div nil
          (dom/h2 nil title)
          (apply
           dom/ul nil
           (map #(dom/li
                     #js {:onClick
                          (fn [e]
                            (move-card! app (first %) src dest))}
                     (str (first %) " " (second %)))
                (sort-by first counts))))))))

(defn app-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/div #js {:className "cards"}
                 (om/build card-list {:app app :src :deck :dest :pool})
                 (om/build card-list {:app app :src :pool :dest :deck}))))))


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
                             :deck {:title "Deck"
                                    :counts {}}
                             :pool {:title "Pool"
                                    :counts (into {} (map (juxt :name :count) cards))}})))
           cards)
    (om/root app-view app-state
             {:target (.getElementById js/document "app")}))})
