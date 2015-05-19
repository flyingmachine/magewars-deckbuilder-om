(ns magewars-deckbuilder.components.filter-selection
  (:require [magewars-deckbuilder.filtering
             :as f :refer [filter-options card-index filter-facet-counts
                           attribute-filter-types empty-filters]]))

(def filter-attributes-ordered
  (reduce #(assoc % (second %2) (first %2))
          {}
          (map-indexed vector
                       [:type :subtypes :school :level
                        :targets :secondary-targets :range
                        :casting-cost :effects :traits
                        :speed :armor :life :channeling :defenses
                        :attack-dice :damage-types :ranged-melee :attack-speed
                        :slots])))

(defn ->str
  [x]
  (str (if (keyword? x) (name x) x)))

(defn toggle-filter!
  [selected-filters attr val]
  (let [f (if (get-in @selected-filters [attr val]) disj conj)]
    (swap! selected-filters #(merge-with f % {attr val}))))

(defn vset?
  [attr]
  (= (attr attribute-filter-types) :vset))

(defn filter-display
  [facet-count display]
  [:span (str display " " facet-count)])

(defn filter-li
  [selected-filters facet-counts attr val display]
  ^{:key (str attr val)}
  [:li
   [:label
    {:on-click #(toggle-filter! selected-filters attr val)
     :class (if ((or (attr @selected-filters) #{}) val) "selected")}
    [filter-display (get-in facet-counts [attr val]) (->str display)]]])

(defn filter-val
  [selected-filters facet-counts attr val]
  (if (vset? attr)
    (let [[root leaves] val
          root-li (filter-li selected-filters facet-counts attr [root] root)]
      (if (empty? leaves)
        root-li
        (conj root-li [:ul (doall (map #(filter-li selected-filters facet-counts attr [root %] %) (sort-by ->str leaves)))])))
    (filter-li selected-filters facet-counts attr val val)))

(defn foptions
  [filter-attributes cards]
  (sort-by #(get filter-attributes (first %))
           (filter-options cards)))

(defn filter-attribute
  [selected-filters facet-counts attr vals]
  ^{:key (str "filter-attribute-" attr)}
  [:div
   [:h3 (name attr)]
   [:ul (doall (map (partial filter-val selected-filters facet-counts attr)
                    (sort-by ->str vals)))]])

(defn filter-list
  [cards cindex selected-filters]
  (let [facet-counts (f/filter-facet-counts cards cindex @selected-filters)]
    [:div {:class "filters"}
     [:h2 "Filters"]
     [:div (doall (map (fn [[attr vals]]
                         (filter-attribute selected-filters facet-counts attr vals))
                       (foptions filter-attributes-ordered cards)))]]))
