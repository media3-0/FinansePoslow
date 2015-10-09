(ns assets-vis.components.main
  (:require-macros [assets-vis.utils :refer [with-subs]]
                   [cljs-log.core :refer [info]])
  (:require [reagent.core :refer [create-class dom-node]]
            [re-frame.core :refer [subscribe dispatch]]
            [assets-vis.css :refer [css]]
            [goog.string :as s]
            [goog.string.format]))

(defn bar
  [{:keys [id name height x y year max-value column-width selector highlight?] :as data}]
  (let [margin 4
        font-size 12
        cash (s/format "%.2f" (get-in (get data selector) [year :sum]))]
    [:g {:on-mouse-over #(dispatch [:highlight-id id])
         :on-mouse-out #(dispatch [:highlight-id nil])}
     [:rect {:height (- height margin)
             :width column-width
             :fill (if highlight? "#eee" "#ddd")
             :x x
             :y y}]
     [:rect {:height (- height margin)
             :width (* (/ cash max-value) column-width)
             :fill (if highlight? "#bbb" "#999")
             :x x
             :y y}]
     [:text {:x (+ x margin)
             :y (+ y (/ height 2) (/ margin 2))
             :font-family "Helvetica"
             :font-size font-size
             :fill "#333"}
      (str name ": " cash " zł")]]))

(defn column
  [{:keys [x bar-height year data selector column-width highlight-id]}]
  (let [sorted-values (:sorted-values data)
        max-value (:max-value data)]
    [:g
     (for [[i [ma-id ma-data]] (map-indexed vector sorted-values)]
       ^{:key ma-id}
       [bar (conj ma-data {:id ma-id
                           :height bar-height
                           :x x
                           :y (* i bar-height)
                           :selector selector
                           :column-width column-width
                           :year year
                           :highlight? (= ma-id highlight-id)
                           :max-value max-value})])]))

(defn columns
  [{:keys [years data column-width column-margin bar-height selector highlight-id]}]
  [:g
   (for [[i year] (map-indexed vector years)]
     ^{:key (str i "-" year)}
     [column {:x (* i (+ column-width column-margin))
              :bar-height bar-height
              :column-width column-width
              :highlight-id highlight-id
              :year year
              :selector selector
              :data (get-in data [year selector])}])])

(defn indices
  [pred coll]
  (keep-indexed #(when (pred %2) %1) coll))

(defn connection-column
  [{:keys [year-1 year-2 x bar-height column-margin highlight-id members-count]}]
  (let [lines (map
                (fn [[i-1 [ma-1-id year-1-data]]]
                  (let [i-2 (indices (fn [[ma-2-id _]] (= ma-2-id ma-1-id)) year-2)]
                    [i-1 (first i-2) ma-1-id]))
                (map-indexed vector year-1))
        lines (filter (fn [[i-1 i-2 _]]
                        (and (not (nil? i-1))
                             (not (nil? i-2))))
                      lines)]
    [:g
     (for [[i-1 i-2 ma-id] lines]
       ^{:key ma-id}
       [:line {:x1 x
               :y1 (+ (* i-1 bar-height) (/ bar-height 2))
               :x2 (+ x column-margin)
               :y2 (+ (* i-2 bar-height) (/ bar-height 2))
               :fill "none"
               :stroke (if (= ma-id highlight-id) "#eee" "#ddd")
               :stroke-width 3}])]))

(defn connections
  [{:keys [years data selector column-width column-margin bar-height highlight-id members-count]}]
  [:g
   (for [[i [year-1 year-2]] (map-indexed vector (partition 2 1 years))]
     ^{:key i}
     [connection-column {:year-1 (get-in data [year-1 selector :sorted-values])
                         :year-2 (get-in data [year-2 selector :sorted-values])
                         :x (+ (* i (+ column-width column-margin)) column-width)
                         :bar-height bar-height
                         :column-margin column-margin
                         :highlight-id highlight-id
                         :members-count members-count}])])

(defn header-year-select
  [{:keys [available-years selected-year column-index column-width column-margin margin font-size-big font-size-small]}]
  [:g
   (for [[i year] (map-indexed vector available-years)]
     (let [font-size (if (= year selected-year) font-size-big font-size-small)]
       ^{:key i} [:text {:x (+ (* (+ i 0.5) (/ column-width (count available-years)))
                               (* column-index (+ column-width column-margin)))
                         :y 44
                         :font-size font-size
                         :font-family "Helvetica"
                         :fill "#999"
                         :text-anchor "middle"
                         :on-click #(dispatch [:graph-year-clicked column-index year])
                         :style (css {:cursor "pointer"})}
                  year]))])

(defn header
  [{:keys [available-years years column-width column-margin]}]
  (let [font-size-big 24
        font-size-small 18
        margin 10]
    [:g
     (for [[i year] (map-indexed vector years)]
       ^{:key i} [header-year-select {:available-years available-years
                                      :selected-year year
                                      :column-index i
                                      :column-width column-width
                                      :column-margin column-margin
                                      :font-size-big font-size-big
                                      :font-size-small font-size-small
                                      :margin margin}])]))

(defn graph
  []
  (with-subs [data [:data]
              width [:graph-width]
              graph-years [:graph-years]
              selector [:data-selector]
              highlight-id [:highlight-id]]
    (let [margin 20
          members-count (count (-> data first last :cash :sorted-values))
          width (- width margin)
          bar-height 30
          column-width (/ width 3)
          column-margin (/ width 3)
          header-height 60
          available-years (map str (range 2011 2016))]
      [:svg {:height (* bar-height members-count)
             :width (- (* (+ column-width column-margin) (count graph-years)) column-margin)}
       [header {:available-years available-years
                :years graph-years
                :column-width column-width
                :column-margin column-margin}]
       [:g {:transform (str "translate(0," header-height ")")}
        [connections {:years graph-years
                      :data data
                      :selector selector
                      :column-width column-width
                      :column-margin column-margin
                      :bar-height bar-height
                      :highlight-id highlight-id
                      :members-count members-count}]
        [columns {:years graph-years
                  :data data
                  :selector selector
                  :column-width column-width
                  :column-margin column-margin
                  :bar-height bar-height
                  :highlight-id highlight-id}]]])))

(defn data-selector
  []
  (with-subs [selector [:data-selector]]
    [:div
     [:div {:style (css {:cursor "pointer"
                         :color (if (= selector :other-income) "#222" "#bbb")})
            :on-click #(dispatch [:data-selector-change :other-income])}
      "inne dochody"]
     [:div {:style (css {:cursor "pointer"
                         :color (if (= selector :cash) "#222" "#bbb")})
            :on-click #(dispatch [:data-selector-change :cash])}
      "zasoby pieniężne"]]))

(defn graph-wrapper
  []
  [:div {:style (css {:text-align "center"})}
   [data-selector]
   [graph]])

(defn main
  []
  [:div {:style (css {:position "absolute"
                      :left 0
                      :right 0
                      :top 0})}
   [graph-wrapper]])
