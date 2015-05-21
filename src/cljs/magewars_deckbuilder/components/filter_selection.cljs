(ns magewars-deckbuilder.components.filter-selection
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! chan <!]]
            [magewars-deckbuilder.filtering :as f]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defprotocol FilterVal
  (display [x])
  (sort-val [x]))

(extend-protocol FilterVal
  cljs.core.Keyword
  (display [x] (name x))
  (sort-val [x] x)

  cljs.core/PersistentVector
  (display [x] (display (first x)))
  (sort-val [x] (sort-val (first x))))

(extend-type default
  FilterVal
  (display [x] (str x))
  (sort-val [x] x))

(defn vset?
  [attr]
  (= (attr f/attribute-filter-types) :vset))

(defn foptions
  [filter-attributes cards]
  (sort-by #(get filter-attributes (first %))
           (f/filter-options cards)))

(defn prepare-vals
  [attr vals vset-parent facet-counts selected-filters]
  (sort-by
   (comp str :val)
   (cond
     ;; vset roots
     (and (vset? attr) (not vset-parent))
     (map (fn [v]
            (let [[val leaves] v
                  root [val]]
              {:attr attr
               :root root
               :val val
               :count (get-in facet-counts [attr root])
               :selected (get-in selected-filters [attr root])
               :children (prepare-vals attr leaves val facet-counts selected-filters)}))
          vals)

     ;; vset leaves
     vset-parent
     (map (fn [v]
            (let [root [vset-parent v]]
              {:attr attr
               :root root
               :val v
               :count (get-in facet-counts [attr root])
               :selected (get-in selected-filters [attr root])}))
          vals)

     ;; non-vset
     :else
     (map (fn [v] {:attr attr
                  :root v
                  :val v
                  :count (get-in facet-counts [attr v])
                  :selected (get-in selected-filters [attr v])})
          vals))))

(defn val-li
  [{:keys [attr root val count selected children]} owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [toggle-filter] :as state}]
      (dom/li #js {:onClick (fn [e] (put! toggle-filter [attr root]))}
              (dom/label #js {:className (if selected "selected")}
                         (str (display val) " " count))
              (if children (om/build val-list children {:init-state {:toggle-filter toggle-filter}}))))))

(defn val-list
  [vals owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [toggle-filter] :as state}]
      (apply dom/ul nil
             (map (fn [val] (om/build val-li val {:init-state {:toggle-filter toggle-filter}}))
                  vals)))))

(defn filter-attribute-view [{:keys [attr vals facet-counts selected-filters]} owner]
  (reify
    om/IInitState
    (init-state [_] {:toggle-filter (chan)})
    om/IWillMount
    (will-mount [_]
      (let [toggle-filter (om/get-state owner :toggle-filter)]
        (go (loop []
              (let [fav (<! toggle-filter)]
                (om/transact! selected-filters
                  (fn [xs]
                    (let [f (if (get-in xs fav) disj conj)]
                      (merge-with f xs (apply hash-map fav))))))
              (recur)))))
    om/IRenderState
    (render-state [_ {:keys [toggle-filter]}]
      (let [prepped-vals (prepare-vals attr vals nil facet-counts selected-filters)]
        (dom/div nil
          (dom/h3 nil (name attr))
          (om/build val-list prepped-vals
                    {:init-state {:toggle-filter toggle-filter}}))))))

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
