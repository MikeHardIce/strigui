(ns strigui.button
  (:require [clojure2d.core :as c2d]
            [strigui.box :as b]
            [strigui.widget :as wdg]))

(defrecord Button [name value coordinates args events]
  wdg/Widget
  (coord [this] (:coordinates this)) ;; could be a mapping if the record would look different
  (value [this] (:value this))
  (args [this] (:args this))
  (widget-name [this] (:name this))
  (redraw [this canvas] (b/box-redraw this canvas))
  (draw [this canvas]
    (b/box-draw-border this canvas) 
    (b/box-draw canvas (:value this) (:args this))))

(extend-protocol b/Box
  Button
  (draw-hover [this canvas] (b/box-draw-hover this canvas))
  (draw-clicked [this canvas] 
    (b/box-draw-border this canvas :blue 2)
    this))

(defn button
  "canvas - clojure2d canvas
   name - name of the button
   text - text displayed inside the button
   args - map of properties:
      x - x coordinate of top left corner
      y - y coordinate of top left corner
      color - vector consisting of [background-color font-color]
      min-width - the minimum width
    events - map of event functions
      supported event atm :clicked"
  ([canvas name text args] (button canvas name text args {}))
  ([canvas name text args events]
  (let [arg [canvas text args]
        coord (apply b/box-coord arg)]
    (Button. name text coord args events))))

(defmethod wdg/widget-event [strigui.button.Button :mouse-moved] 
  [_ canvas widget]
  (b/draw-hover widget canvas))

(defmethod wdg/widget-event [strigui.button.Button :mouse-clicked]
  [_ canvas widget]
  (b/draw-clicked widget canvas)
  (swap! b/boxes-clicked #(conj %1 %2) widget))