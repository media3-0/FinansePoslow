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

(register-sub
  :graph-width
  (fn [db]
    (reaction (max (min ((:window-size @db) 0) 1200)
                   800))))

(register-sub
  :graph-years
  (fn [db]
    (reaction (:graph-years @db))))

(register-sub
  :data-selector
  (fn [db]
    (reaction (:data-selector @db))))

(register-sub
  :highlight-id
  (fn [db]
    (reaction (:highlight-id @db))))

(register-sub
  :header-width
  (fn [db]
    (reaction (min ((:window-size @db) 0) 800))))
