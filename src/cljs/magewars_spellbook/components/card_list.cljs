(ns magewars-spellbook.components.card-list
  (:require [magewars-spellbook.filtering :as f]
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
        (dom/div #js {:className "card-list"}
          (dom/h3 nil title)
          (apply
           dom/ul nil
           (map (fn [[name count]]
                  (dom/li
                      #js {:className (if (= name (:selected-card app)) "selected")}
                      (dom/label
                          #js {:onClick (fn [e] (om/transact! app :selected-card (constantly name)))}
                          (dom/i #js {:className "fa fa-exchange"
                                      :onClick (fn [e]
                                                 (.stopPropagation e)
                                                 (move-card! name src dest))})
                          (str count " " name))))
                (sort-by first
                         (filter identity
                                 (map (fn [[name count]]
                                        (let [card (get cards-by-name name)]
                                          (if (get filtered-cards card)
                                            [name count])))
                                      counts))))))))))

(defn all-cards-view [{:keys [deck pool] :as app} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (om/build card-list-view {:app app :src deck :dest pool})
        (om/build card-list-view {:app app :src pool :dest deck})))))
