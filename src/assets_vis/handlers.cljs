(ns assets-vis.handlers
  (:require-macros [cljs-log.core :refer [info warn]])
  (:require [assets-vis.localstorage :as localstorage]
            [cognitect.transit :as transit]
            [re-frame.core :refer [register-handler dispatch]]
            [ajax.core :refer [GET raw-response-format]]))

(defn window-size []
  [(.-innerWidth js/window)
   (.-innerHeight js/window)])

(defn dispatch-resize [e]
  (dispatch [:resize-window (window-size)]))

(defn add-dom-listener [db id fn]
  (if-not (db id)
    (do (.addEventListener js/window (name id) fn)
        (assoc db id fn))
    db))

(defn cached-request [url handler]
  (if-let [data (localstorage/get-item url)]
    (handler data)
    (GET url {:handler handler
              :response-format (raw-response-format)})))

(defn get-data []
  (cached-request "data.transit.json"
                  (fn [data]
                    (let [reader (transit/reader :json)
                          parsed (transit/read reader data)]
                      (dispatch [:downloaded-data parsed])))))

(register-handler
  :init-app
  (fn [db _]
    (let [db (-> db
                 (add-dom-listener :resize dispatch-resize)
                 (assoc :graph-years ["2011" "2015"]
                        :data-selector :cash
                        :window-size (window-size)))]
      (get-data)
      db)))

(register-handler
  :resize-window
  (fn [db [_ size]]
    (let [size (or size (window-size))]
      (assoc db :window-size size))))

(defn year-data
  [data selector year]
  (let [members 100
        max-value (apply max (map #(get-in % [1 selector year :sum]) data))
        sorted-values (take members (reverse (sort-by #(get-in % [1 selector year :sum]) data)))]
    {:max-value max-value
     :sorted-values sorted-values}))

(register-handler
  :downloaded-data
  (fn [db [_ data]]
    (let [data (into {}
                     (map (fn [year] [year {:cash (year-data data :cash year)
                                            :other-income (year-data data :other-income year)}])
                          (map str (range 2011 2016))))]
      (assoc db
             :data data
             :initialized? true))))

(register-handler
  :graph-year-clicked
  (fn [db [_ index year]]
    (assoc-in db [:graph-years index] year)))

(register-handler
  :data-selector-change
  (fn [db [_ selector]]
    (assoc db :data-selector selector)))

(register-handler
  :highlight-id
  (fn [db [_ id]]
    (assoc db :highlight-id id)))
