(ns assets-vis.core
  (:require-macros [assets-vis.utils :refer [with-subs]])
  (:require [reagent.core :refer [render]]
            [re-frame.core :refer [dispatch]]
            [assets-vis.handlers]
            [assets-vis.subs]
            [assets-vis.components.main :refer [main]]))

(defn home []
  (with-subs [initialized? [:app-initalized?]]
    (if initialized?
      main
      [:div "loading..."])))

(defn init []
  (dispatch [:init-app])
  (render [home] (.getElementById js/document "main")))

(init)
