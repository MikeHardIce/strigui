(ns strigui.button
  (:require [clojure2d.core :as c2d]
            [strigui.box :as b]
            [strigui.events :as e])
  ;;(:import [strigui.box render events])
  )

(defrecord Button [name coord create-func args]
  b/render
    (draw-hover [this canvas] (apply b/box-border (conj [canvas :black 2] (:coord this)))
                                this)
    (draw-clicked [this canvas] (apply b/box-border (conj [canvas :green 2] (:coord this)))
                                this)
    (redraw [this canvas] (let [coord (:coord this)]
                            (when (not-empty coord)
                              (apply b/box-border (conj [canvas :white 2] coord))
                              (apply b/box-border (conj [canvas :black 1] coord))
                              this))))
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