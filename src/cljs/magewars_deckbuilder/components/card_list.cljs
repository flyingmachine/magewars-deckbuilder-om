(ns magewars-deckbuilder.components.card-list
  (:require [magewars-deckbuilder.filtering :as f]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

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
