(ns strigui.input
  (:require [clojure2d.core :as c2d]
            [strigui.window :as wnd]
            [strigui.box :as b]
            [strigui.events :as e]))

;;{:coord [] :func :args [] :name ""}
; (defrecord Input [name coord create-func args]
;   b/Box
;   (box-coord [this] (:coord this)) ;; could be a mapping if the record would look different
;   (draw-hover [this canvas] (b/box-draw-hover this canvas))
;   (draw-clicked [this canvas] (apply b/box-border (conj [canvas :green 2] (:coord this)))
;                                  this)
;   (redraw [this canvas] (b/box-redraw this canvas)))

;  (extend-protocol b/Actions
;   Input
;    (clicked [this] (e/button-clicked this)))

; (defn create-input 
;   [name coord create-func args]
;   (Input. name coord create-func args))

; (defn input
;   "canvas - clojure2d canvas
;    text - text displayed inside the button
;    x - x coordinate of top left corner
;    y - y coordinate of top left corner
;    color - vector consisting of [background-color font-color]
;    min-width - the minimum width"
;   [canvas name text {:keys [x y color min-width]}]
;   (b/box canvas name text {x y color min-width} create-input))