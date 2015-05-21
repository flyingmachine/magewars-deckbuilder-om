(ns magewars-deckbuilder.filtering
  (:require [magewars-deckbuilder.utils :refer [mapval]]
            [clojure.set :as s]))

;; TODO put this in cards, consolidate filter-options to go in cards
;; and be returned as big state thing
(def attribute-filter-types
  {:type :keyword
   :subtypes :keyword
   :casting-cost :number
   :speed :keyword
   :armor :number
   :defenses :boolean
   :life :number
   :channeling :number
   :traits :vset
   :attack-dice :number
   :damage-types :keyword
   :effects :vset
   :ranged-melee :keyword
   :attack-speed :keyword
   :targets :vset
   :secondary-targets :vset
   :slots :vset
   :school :keyword
   :level :number})

(def filter-attributes-ordered
  (into {}
        (map-indexed #(vector %2 %1)
                     [:type :subtypes :school :level
                      :targets :secondary-targets :range
                      :casting-cost :effects :traits
                      :speed :armor :life :channeling :defenses
                      :attack-dice :damage-types :ranged-melee :attack-speed
                      :slots])))

(def filter-type-attributes
  (mapval #(set (map first %)) (group-by second attribute-filter-types)))

(defn empty-filters
  [attrs]
  (mapval (constantly #{}) attrs))

(defn only-non-empty
  [filters]
  (filter #(not (empty? (second %))) filters))

(defn leaves
  "In sets of [x] [x y], return only [x y]. Assumes only two level"
  [s]
  (:leaves
   (reduce (fn [final x]
             (if ((:flat final) (first x))
               final
               {:flat (into (:flat final) x)
                :leaves (conj (:leaves final) x)}))
           {:flat #{}
            :leaves #{}}
           (reverse (sort-by :count s)))))

(defn prep-filters
  "For vset filters, only keep leaves. Otherwise set intersection
  returns more results than it should."
  [filters]
  (merge filters
         (mapval
          (fn [v] (leaves v))
          (select-keys filters (:vset filter-type-attributes)))))

(defn filter-match
  "Does a card's attribute intersect with the given filter val set?"
  [attr fvals card]
  (->> (attr card)
       (s/intersection fvals)
       (not-empty)))

(defn filter-card
  [filters card]
  (every? (fn [[attribute values]] (filter-match attribute values (:search card)))
          (only-non-empty filters)))

(defn filter-cards
  [cards filters]
  (filter (partial filter-card (prep-filters filters)) cards))

(defn _filter-vals
  [cards]
  (->> (map :search cards)
       (reduce (partial merge-with into))
       (mapval #(set (filter identity %)))))
(def filter-vals (memoize _filter-vals))

(defn _filter-options
  [cards]
  (let [fvals (filter-vals cards)]
    (merge fvals
           (mapval
            (fn [v]
              (mapval
               (fn [w] (set (filter identity (flatten (map rest w)))))
               (group-by first v)))
            (select-keys fvals (:vset filter-type-attributes))))))
(def filter-options (memoize _filter-options))


(defn _filter-cards-indexed
  "Does a set union on all filters for the same attribute, and a set
  intersection against different attributes

  e.g. '(type is attack or conjuration) AND (subtype is acid)"
  [cards filters index]
  (if (empty? filters)
    cards
    (apply s/intersection
           cards
           (map (fn [[attr fvals]]
                  (apply s/union (map #(get index [attr %]) fvals)))
                (only-non-empty filters)))))
(def filter-cards-indexed (memoize _filter-cards-indexed))

(defn card-index
  [cards]
  (apply merge-with s/union
         (mapcat (fn [card]
                   (mapcat (fn [[attr vals]]
                             (map (fn [val] {[attr val] #{card}}) vals))
                           (:search card)))
                 cards)))

(defn _filter-facet-counts
  [all-cards cindex selected-filters]
  (let [filtered-cards (filter-cards-indexed all-cards selected-filters cindex)
        fvals (filter-vals all-cards)]
    (reduce merge
            (map (fn [[attr fvals]]
                   (let [cf (if (empty? (attr selected-filters))
                              #(s/intersection filtered-cards (get cindex [attr %]))
                              #(filter-cards-indexed all-cards (merge selected-filters {attr #{%}}) cindex))]
                     {attr (reduce merge (map (fn [v] {v (count (cf v))}) fvals))}))
                 fvals))))
(def filter-facet-counts (memoize _filter-facet-counts))

;; filters: {:attr set :attr2 set}
;; filter: (attr fvals)
;; attr: :type
;; fvals: set
;; target: cardvals
