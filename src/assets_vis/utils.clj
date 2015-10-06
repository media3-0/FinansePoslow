(ns assets-vis.utils)

; stolen from https://github.com/Day8/re-frame/wiki/Macros--WIP

(defn- to-sub
  [[binding sub]]
  `[~binding (re-frame.core/subscribe ~sub)])

(defn- to-deref
  [binding]
  `[~binding (deref ~binding)])

(defmacro with-subs
  [bindings & body]
  `(let [~@(mapcat to-sub (partition 2 bindings))]
     (fn []
       (let [~@(mapcat to-deref (take-nth 2 bindings))]
         ~@body))))
