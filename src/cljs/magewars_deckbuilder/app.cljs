(ns magewars-deckbuilder.app
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :as ax]
            [magewars-deckbuilder.components.card-list :as cl]
            [magewars-deckbuilder.components.filter-selection :as fs]
            [magewars-deckbuilder.filtering :as f]))

(enable-console-print!)

(defonce deck   (atom #{}))
(defonce pool   (atom #{}))
(def selected-filters (atom (f/empty-filters f/attribute-filter-types)))


(defn read-card-response
  [res]
  (let [cards-with-count (into #{}
                               (map #(assoc % :key (:name %))
                                    (cljs.reader/read-string res)))
        starting-pool-count (into {} (map (juxt :name :count) cards-with-count))
        cards (into #{} (map #(dissoc % :count)) cards-with-count)]
    {:starting-pool-count starting-pool-count
     :starting-deck-count {}
     :cards cards
     :cards-by-name (into {} (map (juxt :name identity) cards))}))

(defn init
  [after]
  (ax/GET "/cards"
          {:handler (fn [res]
                      (let [{:keys [cards
                                    cards-by-name
                                    starting-pool-count
                                    starting-deck-count]}
                            (read-card-response res)]
                        (when (empty? @pool) (reset! pool starting-pool-count))
                        (when (empty? @deck) (reset! deck starting-deck-count))
                        (after cards cards-by-name (f/card-index cards))))}))

(defn timing-wrapper [f]
  (let [start-time (atom nil)
        render-time (atom nil)
        now #(.now js/Date)
        start #(reset! start-time (now))
        stop #(reset! render-time (- (now) @start-time))
        timed-f (with-meta f
                  {:component-will-mount start
                   :component-will-update start
                   :component-did-mount stop
                   :component-did-update stop})]
    (fn []
      [:div
       [:p [:em "render time: " @render-time "ms"]]
       [timed-f]])))

(defn builder
  [cards cards-by-name cindex]
  [:div
   [(timing-wrapper (fn [] (fs/filter-list cards cindex selected-filters)))]
   [:div {:class "cards"}
    [cl/card-list-container "Deck" cards cards-by-name selected-filters cindex deck pool]
    [cl/card-list-container "Pool" cards cards-by-name selected-filters cindex pool deck]]])

(defn ^:export run []
  (init (fn [cards cards-by-name cindex]
          (println "inited")
          (reagent/render [builder cards cards-by-name cindex]
                  (js/document.getElementById "app")))))

(run)
