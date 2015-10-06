(ns assets-vis.css
  (:require [camel-snake-kebab.core :refer [->camelCase]]))

(defn css [values]
  (into {}
        (for [[k v] values]
          [(-> k name ->camelCase) v])))
