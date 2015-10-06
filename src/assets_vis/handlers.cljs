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
    (get-data)
    (-> db
        (add-dom-listener :resize dispatch-resize)
        (assoc :window-size (window-size)))))

(register-handler
  :resize-window
  (fn [db [_ size]]
    (let [size (or size (window-size))]
      (assoc db :window-size size))))

(register-handler
  :downloaded-data
  (fn [db [_ data]]
    (-> db
        (assoc :data data)
        (assoc :initialized? true))))
