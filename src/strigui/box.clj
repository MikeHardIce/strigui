(ns strigui.box
  (:require [clojure2d.core :as c2d]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

;;{:coord [] :func :args [] :name ""}
(def boxes (atom {}))

(defn box-border [canvas color strength [x y w h]]
(when (> strength 0)
    (c2d/with-canvas-> canvas
    ;(c2d/set-stroke strength :butt 0 0)
    (c2d/set-color color)
    (c2d/rect (- x strength) (- y strength) (+ w (* 2 strength)) (+ h (* 2 strength)) true))
    (box-border canvas color (- strength 1) [x y w h])))
  
(defn create-box
  "canvas - clojure2d canvas
   text - text displayed inside the input
   x - x coordinate of top left corner
   y - y coordinate of top left corner
   color - vector consisting of [background-color font-color]
   min-width - the minimum width"
  [canvas text {:keys [x y color min-width]}]
  (let [text-box (c2d/with-canvas-> canvas
                   (c2d/text-bounding-box text))
        text-width (nth text-box 2)
        text-heigth (nth text-box 3)
        text-y (nth text-box 1)
        btn-w (* text-width 1.8)
        border-width (if (and (number? min-width) (< btn-w min-width)) min-width btn-w)
        border-heigth (* text-heigth 1.8)
        background-color (if (> (count color) 0) (first color) :grey)
        foreground-color (if (> (count color) 1) (nth color 1) :black)
        x-offset (if (and (number? min-width) (= min-width border-width))
                   (/ (- border-width text-width) 2.0)
                   (* border-width 0.12))]
    (c2d/with-canvas-> canvas
      (c2d/set-color background-color)
      (c2d/rect x y border-width border-heigth)
      (c2d/set-font-attributes 15 :bold)
      (c2d/set-color foreground-color)
      (c2d/text text (+ x x-offset) (- y (* text-y 1.5))))
    [x y border-width border-heigth]))

(defn box
  "canvas - clojure2d canvas
   text - text displayed inside the button
   x - x coordinate of top left corner
   y - y coordinate of top left corner
   color - vector consisting of [background-color font-color]
   min-width - the minimum width"
  [canvas name text {:keys [x y color min-width]}]
  (let [args [canvas text {:x x :y y :color color :min-width min-width}]
        func create-box
        coord (apply func args)]
    (apply box-border (conj [canvas :black 1] coord))
    (swap! boxes conj {:coord coord :func func :args args :name name})))