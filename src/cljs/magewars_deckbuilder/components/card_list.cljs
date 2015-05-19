(ns magewars-deckbuilder.components.card-list
  (:require [magewars-deckbuilder.filtering :as f]))

(defn remove-card
  [src c]
  (let [name (:name c)
        count (get src name)]
    (if (= 1 count)
      (dissoc src name)
      (update-in src [name] dec))))

(defn add-card
  [dest c]
  (update-in dest [(:name c)] (fnil inc 0)))

(defn move-card
  [card src dest]
  (swap! src remove-card card)
  (swap! dest add-card card))

(defn card-list
  [cards cards-by-name this-count other-count]
  (map (fn [card]
         ^{:key (str "card-list-" (:name card))}
         [:li {:on-click #(move-card card this-count other-count)}
          (:name card) " " (:count card)])
       (sort-by :name
                (filter identity
                        (map (fn [[name count]]
                               (let [card (get cards-by-name name)]
                                 (if (get cards card)
                                   (merge card {:count count}))))
                             @this-count)))))

(defn card-list-container
  [heading all-cards cards-by-name selected-filters cindex this-list other-list]
  (let [cards (f/filter-cards-indexed all-cards @selected-filters cindex)]
    [:div
     [:h2 heading]
     [:ul (card-list cards cards-by-name this-list other-list)]]))
