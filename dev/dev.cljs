(ns assets-vis.dev
  (:require [assets-vis.core]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(figwheel/start {:websocket-url "ws://localhost:3000/figwheel-ws"
                 :build-id "dev"})
