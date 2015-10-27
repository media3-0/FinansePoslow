(ns assets-vis.components.styles
  (:require [assets-vis.css :refer [css]]))

(def colors
  {:main-text-color "#444"
   :gray "#ddd"
   :gray-light "#eee"
   :gray-dark "#ccc"
   :blue "#7DB5D7"})

(def consts
  {:border-radius 6
   :font-size 16
   :font-size-small 14
   :font-size-big 20
   :bar-height 50
   :line-width 4})

(def main
  (css {:position "absolute"
        :left 0
        :right 0
        :top 0
        :color (:main-text-color colors)
        :font-size (:font-size consts)
        :font-family "'Open Sans', sans-serif"}))

(def rounded-corners-left
  (css {:border-top-left-radius (:border-radius consts)
        :border-bottom-left-radius (:border-radius consts)}))

(def rounded-corners-right
  (css {:border-top-right-radius (:border-radius consts)
        :border-bottom-right-radius (:border-radius consts)}))

(defn choose-button [selected?]
  (css {:display "inline-block"
        :padding 8
        :width 120
        :border (str "1px solid " (:gray colors))
        :background (if selected? (:gray-light colors) "#fff")
        :font-weight (if selected? "bold" "normal")
        :font-size 12
        :margin-left -1
        :color (:main-text-color colors)
        :cursor "pointer"}))

(def bar-cash
  (css {:font-weight "bold"}))

(def a-href
  (css {:color (:main-text-color colors)
        :text-decoration "none"
        :border-bottom (str "1px solid " (:gray-dark colors))}))
