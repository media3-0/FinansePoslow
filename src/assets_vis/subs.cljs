(ns assets-vis.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]))

(register-sub
  :app-initalized?
  (fn [db]
    (reaction (:initialized? @db))))

(register-sub
  :window-size
  (fn [db]
    (reaction (:window-size @db))))

(register-sub
  :data
  (fn [db]
    (reaction (:data @db))))
