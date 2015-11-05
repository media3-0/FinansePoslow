(ns assets-vis.components.main
  (:require-macros [assets-vis.utils :refer [with-subs]]
                   [cljs-log.core :refer [info]])
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [assets-vis.css :refer [css]]
            [assets-vis.components.styles :as styles]
            [clojure.string :as s]
            [clojure.set :refer [union]]
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
  [{:keys [years data column-width column-margin selector highlight-id all-ma-ids]}]
  [:g
   (for [[i year] (map-indexed vector years)]
     ^{:key i}
     [column {:x (* i (+ column-width column-margin))
              :column-width column-width
              :highlight-id highlight-id
              :year year
              :selector selector
              :all-ma-ids all-ma-ids
              :data (get-in data [year selector])}])])

(defn indices
  [pred coll]
  (keep-indexed #(when (pred %2) %1) coll))

(defn lines-for-years
  [year-1 year-2]
  (let [lines (map
                (fn [[i-1 [ma-1-id year-1-data]]]
                  (let [i-2 (indices (fn [[ma-2-id _]] (= ma-2-id ma-1-id)) year-2)]
                    [i-1 (first i-2) ma-1-id]))
                (map-indexed vector year-1))
        lines (filter (fn [[i-1 i-2 _]]
                        (and (not (nil? i-1))
                             (not (nil? i-2))))
                      lines)]
    lines))

(defn connection
  [{:keys [all-ma-ids]}]
  (let [line-width (:line-width styles/consts)
        bar-height (:bar-height styles/consts)
        animated-lines (into {} (map
                                  (fn [ma-id] [ma-id (animate-component :line [:y1 :y2])])
                                  all-ma-ids))]
    (fn [{:keys [year-1 year-2 x column-margin highlight-id]}]
      [:g
       (for [[i-1 i-2 id] (lines-for-years year-1 year-2)]
         ^{:key id}
         [(get animated-lines id)
          {:x1 (- x (/ line-width 2))
           :y1 (+ (* i-1 bar-height) (/ bar-height 2))
           :x2 (+ x column-margin (/ line-width 2))
           :y2 (+ (* i-2 bar-height) (/ bar-height 2))
           :stroke (if (= id highlight-id) (:gray-dark styles/colors) (:gray styles/colors))
           :stroke-width line-width
           :on-mouse-over #(dispatch [:highlight-id id])
           :on-mouse-out #(dispatch [:highlight-id nil])}])])))

(defn connections
  [{:keys [years data selector column-width column-margin highlight-id all-ma-ids]}]
  [:g
   (for [[i [year-1 year-2]] (map-indexed vector (partition 2 1 years))]
     ^{:key i}
     [connection {:year-1 (get-in data [year-1 selector :sorted-values])
                  :year-2 (get-in data [year-2 selector :sorted-values])
                  :all-ma-ids all-ma-ids
                  :x (+ (* i (+ column-width column-margin)) column-width)
                  :column-margin column-margin
                  :highlight-id highlight-id}])])

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

(defn calculate-all-ids
  [data]
  (->> data
       (map (fn [[_ v]]
              (into '()
                    (union
                      (set (keys (-> v :cash :sorted-values)))
                      (set (keys (-> v :other-income :sorted-values)))))))
       flatten
       distinct))

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
          available-years (map str (range 2011 2016))
          all-ma-ids (calculate-all-ids data)]
      [:svg {:height (* (:bar-height styles/consts) members-count)
             :width (- (* (+ column-width column-margin) (count graph-years)) column-margin)}
       [header {:available-years available-years
                :years graph-years
                :column-width column-width
                :column-margin column-margin}]
       [:g {:transform (str "translate(0," header-height ")")}
        [connections {:available-years available-years
                      :years graph-years
                      :data data
                      :selector selector
                      :column-width column-width
                      :column-margin column-margin
                      :highlight-id highlight-id
                      :all-ma-ids all-ma-ids}]
        [columns {:years graph-years
                  :data data
                  :selector selector
                  :column-width column-width
                  :column-margin column-margin
                  :highlight-id highlight-id
                  :all-ma-ids all-ma-ids}]]])))

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

(defn a-with-text
  [href]
  [:a {:href href :style styles/a-href} href])

(defn header-text
  []
  (with-subs [width [:header-width]
              info-opened? [:info-opened?]]
    [:div {:style (css {:width width
                        :display "inline-block"
                        :text-align "justify"})}
     [:h1 {:style (css {:text-align "center"})} "Prześwietlamy finanse posłów"]
     [:p
      "Postanowiliśmy przyjrzeć się najnowszym oświadczeniom majątkowym na koniec VII kadencji Sejmu. Każdy poseł zobowiązany jest do złożenia oświadczenia o swym stanie majątkowym. "
      "Ze swego obowiązku wywiązuje się poprzez wypełnienie druku "
      [a-with-text "http://www.sejm.gov.pl/poslowie/oswmajatkposla.pdf"]
      " oraz złożenia go w Kancelarii Sejmu. "]
     [:p {:style (if info-opened?
                   {:max-height 1000
                    :transition "max-height 500ms"}
                   {:max-height 0
                    :transition "max-height 500ms"
                    :overflow "hidden"})}
      "W jaki sposób oświadczenia są weryfikowane? Analizą oświadczeń zajmuje się Komisja Etyki Poselskiej, która czasami wzywa posłów do złożenia wyjaśnienia. "
      "Oświadczenia sprawdzać też może Centralne Biuro Antykorupcyjne. Jakakolwiek jednak instytucja, która miałaby zweryfikować prawdziwość oświadczeń będzie miała problem z ustaleniem stanu faktycznego. "
      "A jakie szanse mamy my jako obywatele? Niestety wszystkie oświadczenia wypisywane są ręcznie przez samych posłów. Często zdarza się, że nie da się jednoznacznie określić którejś z cyfr kwoty podanej przez posła. "
      "Karkołomnego zadania próby przepisania wszystkich oświadczeń majątkowych podjął się serwis "
      [a-with-text "http://mamprawowiedziec.pl"]
      ". Wspólnym wysiłkiem udało się przepisać do arkuszy kalkulacyjnych wszystkie oświadczenia majątkowe do aktualnego stanu. Okazało się jednak, że to dopiero początek drogi. "
      "Dane wpisywane przez posłów okazały się strasznie niechlujne, np. nie do końca wiadomo, czy w rubryce o zobowiązaniach finansowych poseł wpisuje pozostałą do spłacenia część kredytu czy też cały zaciągnięty kredyt "
      "(niektórzy posłowie o tym informowali, inni nie, zaś sumy te znacznie różniły się na przestrzeni jednego roku). "
      "Przeprowadziliśmy wiele prób automatycznego \"wyczyszczenia danych\", następnie rozpoczęliśmy prace na wizualizacją. "
      "Na niniejszej stronie możecie porównać dochody pozasejmowe posła na przestrzeni lat oraz zgromadzony majątek. W przypadku znalezienia błędu bardzo prosimy o zgłoszenia na adres: "
      [:a {:href "mailto:ktorzadzi@media30.pl" :style styles/a-href} "ktorzadzi@media30.pl"]]
     [:div {:style (css {:text-align "center"})}
      [:span {:on-click #(dispatch [:toggle-info])
              :style (css (styles/read-more-button info-opened?))}
       (if info-opened?
         "Schowaj informacje"
         "Przeczytaj więcej...")]]
     [:p
      "Autorzy: "
      [:a {:href "http://treesmovethemost.com" :style styles/a-href} "Szymon Kaliski"]
      ", "
      [:a {:href "http://ktorzadzi.pl" :style styles/a-href} "Łukasz Żyła"]]
     [:p
      "Dane: "
      [a-with-text "http://mamprawowiedziec.pl"]]

     [:img {:src "baner.png"
            :style (css {:margin-top 20})}]
     [:div {:style (css {:border-bottom (str "1px solid " (:gray-light styles/colors))
                         :margin-top 20
                         :margin-bottom 20})}]
     [:p {:style (css {:color (:gray-dark styles/colors)
                       :margin-bottom 60
                       :font-size (:font-size-small styles/consts)})}
      "Strona internetowa jest częścią projektu \"Ktorzadzi.pl - dziennikarstwo danych\" prowadzonego przez fundację Media 3.0 realizowanego w ramach programu Obywatele dla Demokracji, finansowanego z Funduszy EOG."]]))

(defn graph-wrapper
  []
  [:div {:style (css {:text-align "center"})}
   [header-text]
   [data-selector]
   [graph]])

(defn main
  []
  [:div {:style styles/main}
   [graph-wrapper]])
