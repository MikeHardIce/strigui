(ns strigui.input
  (:require [clojure2d.core :as c2d]
            [strigui.window :as wnd]
            [strigui.box :as b]
            [strigui.events :as e]))

;;{:coord [] :func :args [] :name ""}
(def inputs (atom {}))

;; TODO: put this together with the button func, its exatly the same

(defn input
  "canvas - clojure2d canvas
   text - text displayed inside the button
   x - x coordinate of top left corner
   y - y coordinate of top left corner
   color - vector consisting of [background-color font-color]
   min-width - the minimum width"
  [canvas name text {:keys [x y color min-width]}]
  (b/box canvas name text {x y color min-width}))