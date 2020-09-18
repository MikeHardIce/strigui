(ns strigui.input
  (:require [clojure2d.core :as c2d]
            [clojure.set :as s]
            [strigui.window :as wnd]
            [strigui.box :as b]
            [strigui.events :as e]))

;;
(def has-focus (atom #{}))

(defrecord Input [name coordinates args]
  b/Box
  (coord [this] (:coordinates this)) ;; could be a mapping if the record would look different
  (draw-hover [this canvas] 
    (when (not (contains? @has-focus this))
      (b/box-draw-hover this canvas)))
  (draw-clicked [this canvas] (apply b/box-border (conj [canvas :blue 2] (:coordinates this)))
                                  this)
  (redraw [this canvas] 
    (when (not (contains? @has-focus this))
      (b/box-redraw this canvas)))
  (draw [this canvas]
    (b/box-draw-border this canvas) 
    (b/box-draw (:args this))))

  (extend-protocol b/Actions
  Input
    (clicked [this] (let [new (if (contains? @has-focus this)
                                (s/difference @has-focus (set this))
                                (s/union @has-focus (set this))] 
                      (reset! has-focus new))))

(defn input
  "context - map consiting of clojure2d canvas and clojure2d window
    name - name of the input element
    text - text displayed inside the input element
    args - map of properties:
      x - x coordinate of top left corner
      y - y coordinate of top left corner
      color - vector consisting of [background-color font-color]
      min-width - the minimum width"
  [context name text args]
  (let [canvas (:canvas context)
        arg [canvas text args]
        coord (apply b/box-coord arg)
        inp (Input. name coord arg)]
    (b/draw inp canvas)
    (b/register-box canvas inp)))