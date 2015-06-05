(ns magewars-deckbuilder.cards
  (:require [magewars-deckbuilder.utils :refer :all]))

;; "unpack" cards
(def common-attributes
  {:attacks      {:type :attack
                  :speed :quick}
   :conjurations {:type :conjuration
                  :targets #{:zone}
                  :speed :quick
                  :range [0 1]}
   :creatures    {:type :creature
                  :targets #{:zone}
                  :speed :slow}
   :enchantments {:type :enchantment
                  :speed :quick}
   :equipment    {:type :equipment
                  :targets #{[:creature :mage]}
                  :speed :quick
                  :range [0 2]}
   :incantations {:type :incantation}})

(def transformations
  {:attacks [(fn [x]
               (let [attack-keys [:dice :damage-type :effects :traits]]
                 (merge (apply dissoc x attack-keys)
                        {:attacks #{(select-keys x (conj attack-keys :range))}})))]})

(defn count-in-sets
  [card sets]
  (reduce + (map #(get % (:name card) 0) sets)))
(defn merge-count
  [card sets]
  (merge card {:count (count-in-sets card sets)}))

;; create a search view
;; TODO tell myself wtf a vset is. A set of vectors?
(defn vset
  [val]
  (reduce (fn [final x]
            (if (keyword? x)
              (conj final [x])
              (let [[head & tail] x]
                (conj (reduce conj final (map (fn [y] [head y]) tail))
                      [head]))))
        #{}
        val))

(defn ->set [x] (if (set? x) x #{x}))

(defn with-attacks
  [card indexer attr]
  (reduce into
          (indexer (attr card))
          (map (comp indexer attr) (:attacks card))))

(defn attack-set
  [card attr]
  (set (map attr (:attacks card))))

(defn school-pair
  [pair]
  {:school #{(first pair)}
   :level #{(second pair)}})

(defn school-level
  [{school :school}]
  (if (keyword? (first school))
    (school-pair school)
    (reduce (partial merge-with into) (map school-pair school))))

(defn card-views
  [card]
  {:name (:name card)
   :display card
   :search (merge
            (reduce into
                    (map (fn [k] {k (->set (k card))})
                         [:type :subtypes :speed :armor :casting-cost
                          :defenses :life :channeling]))
            {:traits            (with-attacks card vset :traits)
             :attack-dice       (attack-set card :dice)
             :damage-types      (attack-set card :damage-type)
             :effects           (with-attacks card vset :effects)
             :ranged-melee      (attack-set card :ranged-melee)
             :attack-speed      (attack-set card :speed)
             :targets           (with-attacks card vset :targets)
             :secondary-targets (with-attacks card vset :secondary-targets)
             :range             (with-attacks card set :range)
             :slots             (vset (:slots card))}
            (school-level card))})

(defn cards-by-type
  [card-type sets]
  (->> (read-edn "data/cards/" card-type)
       (map (fn [card]
              (-> (card-type common-attributes)
                  (merge card)
                  ((apply comp (card-type transformations [])))
                  (card-views)
                  (merge-count sets))))
       (into #{})))


(defn card-set
  [set-name]
  (read-edn "data/sets/" set-name))

(defn cards
  [& sets]
  (->> (keys common-attributes)
       (map #(cards-by-type % (map card-set sets)))
       (reduce into)))
