(ns magewars-deckbuilder.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-sync.core :refer [om-sync]]
            [om-sync.util :refer [tx-tag edn-xhr]]
            [magewars-deckbuilder.filtering :as f]))

(enable-console-print!)

(def app-state
  (atom {:selected-filters (f/empty-filters f/attribute-filter-types)}))

(defn remove-card
  [card-name src]
  (let [count (get-in src [:counts card-name])]
    (if (= 1 count)
      (assoc src :counts (dissoc (:counts src) card-name))
      (update-in src [:counts card-name] dec))))

(defn add-card
  [card-name dest]
  (update-in dest [:counts card-name] (fnil inc 0)))

(defn move-card!
  [card-name src dest]
  (om/transact! src #(remove-card card-name %))
  (om/transact! dest #(add-card card-name %)))

(defn filter-card-list
  [cards-by-name filtered-cards counts]
  (filter identity
          (map (fn [[name count]]
                 (let [card (get cards-by-name name)]
                   (if (get filtered-cards card)
                     [name count])))
               counts)))

(defn card-list-view [{:keys [app src dest]} owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [title counts]} src
            {:keys [cards-by-name cindex cards selected-filters]} app
            filtered-cards (f/filter-cards-indexed cards selected-filters cindex)]
        (dom/div nil
          (dom/h2 nil title)
          (apply
           dom/ul nil
           (map #(dom/li
                     #js {:onClick (fn [e] (move-card! (first %) src dest))}
                     (str (first %) " " (second %)))
                (sort-by first
                         (filter identity
                                 (map (fn [[name count]]
                                        (let [card (get cards-by-name name)]
                                          (if (get filtered-cards card)
                                            [name count])))
                                      counts))))))))))

;; Filters
(defprotocol FilterVal
  (display [x])
  (sort-val [x]))

(extend-protocol FilterVal
  cljs.core.Keyword
  (display [x] (name x))
  (sort-val [x] x))

(extend-type default
  FilterVal
  (display [x] (str x))
  (sort-val [x] x))

(defn foptions
  [filter-attributes cards]
  (sort-by #(get filter-attributes (first %))
           (f/filter-options cards)))


(defn filter-val-view [{:keys [attr val count selected]} owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [toggle-filter]}]
      (dom/li
          #js {:onClick (fn [e] (put! toggle-filter [attr val]))
               :className (if selected "selected")}
          (str (display val) " " count)))))

(defn filter-attribute-view [{:keys [attr vals facet-counts selected-filters]} owner]
  (reify
    om/IInitState
    (init-state [_]
      {:start-time (atom nil)
       :toggle-filter (chan)})
    om/IWillMount
    (will-mount [_]
      (reset! (om/get-state owner :start-time) (.now js/Date))
      (let [toggle-filter (om/get-state owner :toggle-filter)]
        (go (loop []
              (let [fav (<! toggle-filter)]
                (om/transact! selected-filters
                  (fn [xs]
                    (let [f (if (get-in xs fav) disj conj)]
                      (merge-with f xs (apply hash-map fav))))))
              (recur)))))
    om/IDidMount
    (did-mount [_]
      (println "Render time:" (- (.now js/Date) @(om/get-state owner :start-time))))
    om/IWillUpdate
    (will-update [_ _ _]
      (reset! (om/get-state owner :start-time) (.now js/Date)))
    om/IDidUpdate
    (did-update [_ _ _]
      (println "Render time:" (- (.now js/Date) @(om/get-state owner :start-time))))
    om/IRenderState
    (render-state [_ {:keys [toggle-filter]}]
      (dom/div nil
        (dom/h3 nil (name attr))
        (apply dom/ul nil
               (map #(om/build filter-val-view
                               {:attr attr
                                :val %
                                :count (get-in facet-counts [attr %])
                                :selected (get-in selected-filters [attr %])}
                               {:init-state {:toggle-filter toggle-filter}})
                    (sort-by str vals)))))))

(defn filter-list [{:keys [selected-filters cards cindex]} owner]
  (reify
    om/IInitState
    (init-state [_] {:start-time (atom nil)})
    om/IWillMount
    (will-mount [_]
      (reset! (om/get-state owner :start-time) (.now js/Date)))
    om/IDidMount
    (did-mount [_]
      (println "FL Render time:" (- (.now js/Date) @(om/get-state owner :start-time))))
    om/IWillUpdate
    (will-update [_ _ _]
      (reset! (om/get-state owner :start-time) (.now js/Date)))
    om/IDidUpdate
    (did-update [_ _ _]
      (println "FL Render time:" (- (.now js/Date) @(om/get-state owner :start-time))))
    
    om/IRender
    (render [_]
      (let [facet-counts (f/filter-facet-counts cards cindex selected-filters)
            filter-attributes (foptions f/filter-attributes-ordered cards)]
        (dom/div #js {:className "filters"}
                 (dom/h2 nil "Filters")
                 (apply dom/div nil
                        (map (fn [[attr vals]]
                               (om/build filter-attribute-view
                                         {:attr attr
                                          :vals vals
                                          :facet-counts facet-counts
                                          :selected-filters selected-filters}))
                             filter-attributes)))))))


(defn app-view [{:keys [deck pool] :as app} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (om/build filter-list app)
        (dom/div #js {:className "cards"}
                 (om/build card-list-view {:app app :src deck :dest pool})
                 (om/build card-list-view {:app app :src pool :dest deck}))))))


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
    (om/root app-view app-state
             {:target (.getElementById js/document "app")}))})
