(ns assets-vis.animate
  (:require-macros [assets-vis.utils :refer [with-subs]]
                   [reagent.ratom :refer [reaction]]
                   [cljs-log.core :refer [info]])
  (:require [assets-vis.tick :as tick]
            [reagent.core :as reagent]
            [reagent.impl.util :as reagent-util]
            [re-frame.core :refer [register-sub subscribe dispatch]]))

(defn register-animations
  [db-root]
  (register-sub
    :animation
    (fn [db _]
      (reaction (get-in @db [db-root]))))
  (dispatch
    [:add-tick-handlers
     {:update-animation
      (reify tick/PTickHandler
        (init-state [_ db]
          (assoc db db-root {:start (.getTime (js/Date.))
                             :last (.getTime (js/Date.))}))
        (tick [_ db]
          (let [now (.getTime (js/Date.))]
            (update db db-root assoc :last now))))}]))

(defn lerp
  [k v1 v2]
  (+ (* v1 k)
     (* v2 (- 1 k))))

(defn props-vec->props-map
  [props-vec]
  (into {}
        (map
          (fn [prop] [prop nil])
          props-vec)))

(defn animate-props
  [anim-targets anim-props props-vec time-diff]
  (into {} (map
             (fn [prop]
               [prop (if (nil? (get anim-props prop))
                       (get anim-targets prop)
                       (lerp time-diff
                             (get anim-targets prop)
                             (get anim-props prop)))])
             props-vec)))

(defn animate-component
  [component props-vec & [options]]
  (let [start-time (atom (.getTime (js/Date.)))
        anim-time (or (:anim-time options) 1000)
        anim-props (atom (props-vec->props-map props-vec))
        anim-targets (atom (props-vec->props-map props-vec))
        anim-subs (subscribe [:animation])]
    (fn [props]
      (when (not= (select-keys props props-vec) @anim-targets)
        (reset! anim-targets (select-keys props props-vec))
        (reset! start-time (.getTime (js/Date.))))
      (let [time-diff (min 1.0 (/ (- (:last @anim-subs) @start-time) anim-time))]
        (reset! anim-props (animate-props @anim-targets @anim-props props-vec time-diff))
        [component (merge props @anim-props)]))))
