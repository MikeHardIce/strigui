(ns strigui.button
  (:require [clojure2d.core :as c2d]
            [strigui.box :as b]
            [strigui.events :as e]))
  ;(:import strigui.box Box))

(defrecord Button [name value coordinates args]
  b/Box
  (coord [this] (:coordinates this)) ;; could be a mapping if the record would look different
  (draw-hover [this canvas] (b/box-draw-hover this canvas))
  (draw-clicked [this canvas] (apply b/box-border (conj [canvas :green 2] (:coordinates this)))
                                 this)
  (redraw [this canvas] (b/box-redraw this canvas))
  (draw [this canvas]
    (b/box-draw-border this canvas) 
    (b/box-draw (:args this))))

 (extend-protocol b/Actions
  Button
   (clicked [this] (e/button-clicked this)))

(defn button
  "context - map consiting of clojure2d canvas and clojure2d window
   name - name of the button
   text - text displayed inside the button
   args - map of properties:
      x - x coordinate of top left corner
      y - y coordinate of top left corner
      color - vector consisting of [background-color font-color]
      min-width - the minimum width"
  [context name text args]
  (let [canvas (:canvas context)
        arg [canvas text args]
        coord (apply b/box-coord arg)
        btn (Button. name text coord arg)]
    (b/draw btn canvas)
    (b/register-box! btn)))