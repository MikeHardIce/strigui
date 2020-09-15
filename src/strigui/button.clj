(ns strigui.button
  (:require [clojure2d.core :as c2d]
            [strigui.box :as b]
            [strigui.events :as e])
    (:import [strigui.box Box])
  )

(defrecord Button [name coord create-func args])

 (extend-protocol b/render
  Button
  (draw-hover [this canvas] (b/box-draw-hover this canvas))
  (draw-clicked [this canvas] (apply b/box-border (conj [canvas :green 2] (:coord this)))
                                 this)
  (redraw [this canvas] (box-redraw this canvas)))

 (extend-protocol b/events
  Button
   (clicked [this] (e/button-clicked this)))

(defn create-button
  [name coord create-func args]
  (Button. name coord create-func args))

(defn button
  "context - map consiting of clojure2d canvas and clojure2d window
   text - text displayed inside the button
   x - x coordinate of top left corner
   y - y coordinate of top left corner
   color - vector consisting of [background-color font-color]
   min-width - the minimum width"
  [context name text args]
  (b/box (:canvas context) name text args create-button))