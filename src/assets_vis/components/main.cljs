(ns assets-vis.components.main
  (:require-macros [assets-vis.utils :refer [with-subs]]
                   [cljs-log.core :refer [info]])
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [assets-vis.css :refer [css]]
            [assets-vis.components.styles :as styles]
            [clojure.string :as s]
            [goog.string :refer [format]]
            [goog.string.format]
            [assets-vis.animate :refer [animate-component]]))

(defn format-cash [string]
  (let [splited (s/split string ".")
        cash (first splited)]
    (str (->> cash
              reverse
              (partition 3 3 nil)
              (map s/join)
              (s/join " ")
              (s/reverse))
         ","
         (last splited))))

(defn bar
  [{:keys [id name height x y max-value column-width cash highlight?] :as data}]
  (let [margin 6
        font-size (:font-size-small styles/consts)
        cash (format "%.2f" cash)]
    [:g {:on-mouse-over #(dispatch [:highlight-id id])
         :on-mouse-out #(dispatch [:highlight-id nil])
         :style (css {:transform (str "translate(" x "px," y "px)")})}
     [:rect {:height (- height margin)
             :width column-width
             :fill (if highlight? (:gray-dark styles/colors) (:gray styles/colors))
             :x 0
             :y 0}]
     [:rect {:height (- height margin)
             :width (* (/ cash max-value) column-width)
             :fill (:blue styles/colors)
             :x 0
             :y 0}]
     [:text {:x margin
             :y (+ font-size 5)
             :font-size font-size
             :fill (:main-text-color styles/colors)}
      name]
     [:text {:x margin
             :y (+ (* font-size 2) 8)
             :font-size (- font-size 2)
             :fill (:main-text-color styles/colors)
             :style styles/bar-cash}
      (str (format-cash cash) " zł")]]))

(defn column
  [{:keys [all-ma-ids]}]
  (let [animated-bars (into {} (map
                                 (fn [ma-id] [ma-id (animate-component bar [:cash :y :max-value])])
                                 all-ma-ids))]
    (fn [{:keys [x year data selector column-width highlight-id]}]
      [:g
       (for [[i [ma-id ma-data]] (map-indexed vector (:sorted-values data))]
         ^{:key ma-id}
         [(get animated-bars ma-id)
          (conj ma-data
                {:id ma-id
                 :height (:bar-height styles/consts)
                 :x x
                 :y (* i (:bar-height styles/consts))
                 :cash (get-in (get ma-data selector) [year :sum])
                 :column-width column-width
                 :highlight? (= ma-id highlight-id)
                 :max-value (:max-value data)})])])))

(defn columns
  [{:keys [years data column-width column-margin selector highlight-id]}]
  (let [all-ma-ids (->> data
                        (map (fn [[_ v]] (keys (-> v :cash :sorted-values))))
                        flatten
                        distinct)]
    [:g
     (for [[i year] (map-indexed vector years)]
       ^{:key i}
       [column {:x (* i (+ column-width column-margin))
                :column-width column-width
                :highlight-id highlight-id
                :year year
                :selector selector
                :all-ma-ids all-ma-ids
                :data (get-in data [year selector])}])]))

(defn indices
  [pred coll]
  (keep-indexed #(when (pred %2) %1) coll))

(defn connection
  [{:keys [year-1 year-2 x column-margin highlight-id members-count]}]
  (let [line-width (:line-width styles/consts)
        bar-height (:bar-height styles/consts)
        lines (map
                (fn [[i-1 [ma-1-id year-1-data]]]
                  (let [i-2 (indices (fn [[ma-2-id _]] (= ma-2-id ma-1-id)) year-2)]
                    [i-1 (first i-2) ma-1-id]))
                (map-indexed vector year-1))
        lines (filter (fn [[i-1 i-2 _]]
                        (and (not (nil? i-1))
                             (not (nil? i-2))))
                      lines)]
    [:g
     (for [[i-1 i-2 id] lines]
       ^{:key id}
       [:line {:x1 (- x (/ line-width 2))
               :y1 (+ (* i-1 bar-height) (/ bar-height 2))
               :x2 (+ x column-margin (/ line-width 2))
               :y2 (+ (* i-2 bar-height) (/ bar-height 2))
               :stroke (if (= id highlight-id) (:gray-dark styles/colors) (:gray styles/colors))
               :stroke-width line-width
               :on-mouse-over #(dispatch [:highlight-id id])
               :on-mouse-out #(dispatch [:highlight-id nil])}])]))

(defn connections
  [{:keys [years data selector column-width column-margin highlight-id members-count]}]
  [:g
   (for [[i [year-1 year-2]] (map-indexed vector (partition 2 1 years))]
     ^{:key i}
     [connection {:year-1 (get-in data [year-1 selector :sorted-values])
                  :year-2 (get-in data [year-2 selector :sorted-values])
                  :x (+ (* i (+ column-width column-margin)) column-width)
                  :column-margin column-margin
                  :highlight-id highlight-id
                  :members-count members-count}])])

(defn header-year-select
  [{:keys [available-years selected-year column-index column-width column-margin]}]
  [:g
   (for [[i year] (map-indexed vector available-years)]
     (let [font-size (if (= year selected-year)
                       (:font-size-big styles/consts)
                       (:font-size-small styles/consts))]
       ^{:key i} [:text {:x (+ (* (+ i 0.5) (/ column-width (count available-years)))
                               (* column-index (+ column-width column-margin)))
                         :y 50
                         :font-size font-size
                         :fill (:main-text-color styles/colors)
                         :text-anchor "middle"
                         :on-click #(dispatch [:graph-year-clicked column-index year])
                         :style (css {:cursor "pointer"})}
                  year]))])

(defn header
  [{:keys [available-years years column-width column-margin]}]
  [:g
   (for [[i year] (map-indexed vector years)]
     ^{:key i} [header-year-select {:available-years available-years
                                    :selected-year year
                                    :column-index i
                                    :column-width column-width
                                    :column-margin column-margin }])])

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
          column-width (/ width 3)
          column-margin (/ width 3)
          header-height 60
          available-years (map str (range 2011 2016))]
      [:svg {:height (* (:bar-height styles/consts) members-count)
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
                      :highlight-id highlight-id
                      :members-count members-count}]
        [columns {:years graph-years
                  :data data
                  :selector selector
                  :column-width column-width
                  :column-margin column-margin
                  :highlight-id highlight-id}]]])))

(defn data-selector
  []
  (with-subs [selector [:data-selector]]
    [:div
     [:div {:style (merge (styles/choose-button (= selector :other-income))
                          styles/rounded-corners-left)
            :on-click #(dispatch [:data-selector-change :other-income])}
      "inne dochody"]
     [:div {:style (merge (styles/choose-button (= selector :cash))
                          styles/rounded-corners-right)
            :on-click #(dispatch [:data-selector-change :cash])}
      "zasoby pieniężne"]]))

(defn header-text
  []
  (with-subs [width [:header-width]]
    [:div {:style (css {:width width
                        :display "inline-block"
                        :text-align "justify"})}
     [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus feugiat quam justo, vel egestas urna eleifend vitae. Proin a massa at eros fringilla sodales et non risus. Integer vitae mattis orci. Aliquam dignissim neque ante, vel pulvinar metus facilisis ut. Fusce posuere at nunc ac rutrum. Sed dictum ac nisl id sodales. Maecenas mollis tincidunt lacus, quis tincidunt turpis semper at. Pellentesque aliquet massa vel est mollis blandit. Aenean malesuada ligula ligula, nec condimentum urna egestas eu. Sed in feugiat nisi. Etiam a facilisis sapien, id viverra tortor. Nunc nec ex vel orci posuere lobortis ut sit amet sem. Phasellus porta est eget mi hendrerit tincidunt."]
     [:p "Mauris ultricies aliquam arcu, id sagittis leo volutpat et. Suspendisse nibh justo, efficitur a turpis eget, ultrices posuere tellus. Morbi porta enim a convallis mollis. Etiam lectus magna, pulvinar vitae tempus nec, hendrerit eu lectus. Morbi efficitur orci vitae arcu fringilla consequat. Donec porttitor felis eu ligula pellentesque tempor. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Sed sed tincidunt felis. Suspendisse iaculis, ante pretium porta volutpat, risus sapien aliquet ligula, in pulvinar ipsum diam et nisl."]]))

(defn graph-wrapper
  []
  [:div {:style (css {:text-align "center"})}
   [header-text]
   [data-selector]
   [graph]])

(defn display-number [{:keys [n const]}]
  [:div "n: " n " " const])

(defn timer-component
  []
  (let [seconds-elapsed (reagent/atom 0)
        animated (animate-component display-number [:n] {:anim-time 500})]
    (fn []
      (js/setTimeout #(swap! seconds-elapsed inc) 1000)
      (let [n (* 100 @seconds-elapsed)]
        [animated {:n n :const "CCC"}]))))

(defn main
  []
  [:div {:style styles/main}
   #_[timer-component]
   [graph-wrapper]])
