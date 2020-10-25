(ns strigui.window
  (:require [clojure2d.core :as c2d]))

(def context (atom {:canvas nil :window nil}))

(defn create-window [width height]
  (let [canvas (c2d/canvas width height)
        new-context {:canvas canvas
                     :window (c2d/show-window canvas "main-window")}]
    (swap! context merge new-context)
    new-context))

(defn display-info [context text]
  (let [canvas (:canvas context)
        height (c2d/height canvas)
        width (c2d/width canvas)
        [_ _ _ h] (c2d/with-canvas-> canvas
                    (c2d/text-bounding-box text))]
    (c2d/with-canvas-> canvas
      (c2d/set-color :white)
      (c2d/rect 0 (- height h) width h)
      (c2d/set-font-attributes 15)
      (c2d/set-color :black)
      (c2d/text (str text) 0 height))))

(defn within?
  "Checks wheter the point (x y) is within the given coord
   coord - vector [x-coord y-coord width height]
   x - x-coord of point to check
   y - y-coord of point to check"
  [coord x y]
  (println coord)
  (and (>= x (first coord))
       (>= y (nth coord 1))
       (<= x (+ (first coord) (nth coord 2)))
       (<= y (+ (nth coord 1) (nth coord 3)))))