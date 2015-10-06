(ns assets-vis.components.main
  (:require-macros [assets-vis.utils :refer [with-subs]]
                   [cljs-log.core :refer [info]])
  (:require [reagent.core :refer [create-class dom-node]]
            [re-frame.core :refer [dispatch]]
            [assets-vis.css :refer [css]]))

(defn bar [{:keys [id name height x y year max-value cash column-width]}]
  (let [margin 4
        font-size 12
        cash (get-in cash [year :sum])]
    [:g
     [:rect {:height (- height margin)
             :width column-width
             :fill "#ddd"
             :x x
             :y y}]
     [:rect {:height (- height margin)
             :width (* (/ cash max-value) column-width)
             :fill "#999"
             :x x
             :y y}]
     [:text {:x (+ x margin)
             :y (+ y (/ height 2) (/ margin 2))
             :font-family "Helvetica"
             :font-size font-size
             :fill "#333"}
      (str "[" id "] " name ": " cash "pln")]]))

(defn column [{:keys [x bar-height year data column-width]}]
  (let [sorted-values (:sorted-values data)
        max-value (:max-value data)]
    [:g
     (for [[i [ma-id ma-data]] (map-indexed vector sorted-values)]
       ^{:key ma-id}
       [bar (conj ma-data {:id ma-id
                           :height bar-height
                           :x x
                           :y (* i bar-height)
                           :column-width column-width
                           :year year
                           :max-value max-value})])]))

(defn columns [{:keys [years-data column-width column-margin bar-height]}]
  [:g
   (for [[i [year year-data]] (map-indexed vector years-data)]
     ^{:key year}
     [column {:x (* i (+ column-width column-margin))
              :bar-height bar-height
              :column-width column-width
              :year year
              :data year-data}])])

(defn indices [pred coll]
  (keep-indexed #(when (pred %2) %1) coll))

(defn connection-column [{:keys [year-1 year-2 x bar-height column-margin]}]
  (let [lines (map
                (fn [[i-1 [ma-1-id year-1-data]]]
                  (let [i-2 (indices (fn [[ma-2-id _]] (= ma-2-id ma-1-id)) year-2)]
                    [i-1 (first i-2)]))
                (map-indexed vector year-1))]
    [:g
     (for [[i-1 i-2] lines]
       ^{:key (str i-1 "-" i-2)}
       [:line {:x1 x
               :y1 (+ (* i-1 bar-height) (/ bar-height 2))
               :x2 (+ x column-margin)
               :y2 (+ (* i-2 bar-height) (/ bar-height 2))
               :fill "none"
               :stroke "#ddd"
               :stroke-width 3}])]))

(defn connections [{:keys [years years-data column-width column-margin bar-height]}]
  [:g
   (for [[i [year-1 year-2]] (map-indexed vector (partition 2 1 years))]
     ^{:key i}
     [connection-column {:year-1 (get-in years-data [year-1 :sorted-values])
                         :year-2 (get-in years-data [year-2 :sorted-values])
                         :x (+ (* i (+ column-width column-margin)) column-width)
                         :bar-height bar-height
                         :column-margin column-margin}])])

(defn header [{:keys [years column-width column-margin]}]
  (let [font-size 32
        margin 10]
    [:g
     (for [[i year] (map-indexed vector years)]
       ^{:key i}
       [:text {:x (+ (* i (+ column-width column-margin)) (/ column-width 2))
               :y (+ font-size margin)
               :font-size font-size
               :font-family "Helvetica"
               :fill "#999"
               :text-anchor "middle"}
        year])]))

(defn year-data [data year]
  (let [max-value (apply max (map #(get-in % [1 :cash year :sum]) data))
        sorted-values (reverse (sort-by #(get-in % [1 :cash year :sum]) data))]
    {:max-value max-value
     :sorted-values sorted-values}))

(defn graph []
  (with-subs [data [:data]]
    (let [bar-height 30
          column-width 300
          column-margin 300
          header-height 60
          years (map str (range 2011 2016))
          years-data (into {} (map (fn [year] [year (year-data data year)]) years))]
      [:svg {:height (* bar-height (count data))
             :width (- (* (+ column-width column-margin) (count years)) column-margin)}
       [header {:years years
                :column-width column-width
                :column-margin column-margin}]
       [:g {:transform (str "translate(0," header-height ")")}
        [connections {:years years
                      :years-data years-data
                      :column-width column-width
                      :column-margin column-margin
                      :bar-height bar-height}]
        [columns {:years-data years-data
                  :column-width column-width
                  :column-margin column-margin
                  :bar-height bar-height}]]])))

(defn main [] [:div [graph]])
