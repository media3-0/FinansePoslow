(defproject assets-vis "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.122"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [com.cognitect/transit-cljs "0.8.225"]
                 [reagent "0.5.1"]
                 [re-frame "0.4.1"]
                 [cljs-log "0.2.2"]
                 [camel-snake-kebab "0.3.2"]
                 [cljs-ajax "0.3.14"]]
  :plugins [[lein-cljsbuild "1.1.0"]
            [lein-figwheel "0.4.0"]]
  :source-paths ["src"]
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src" "dev"]
                        :compiler {:main assets-vis.dev
                                   :asset-path "js/compiled/out"
                                   :output-to "resources/public/js/assets_vis.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :source-map true
                                   :source-map-timestamp true }}
                       {:id "prod"
                        :source-paths ["src"]
                        :compiler {:output-to "resources/public/js/assets_vis.js"
                                   :main assets-vis.core
                                   :optimizations :advanced
                                   :pretty-print false}}]}
  :figwheel {:server-port 3000})

