(ns strigui.box
  (:require [clojure2d.core :as c2d]
            [strigui.widget :as wdg]))

(set! *warn-on-reflection* true)

(def ^:private default-font-size 15)

(defn box-coord 
  "Computes the full box coordinates.
  Returns the vector [x y border-width border-heigth]"
  [canvas text {:keys [^long x ^long y ^long height ^long width font-size]}]
  (let [size (if (number? font-size) font-size default-font-size)
        text-box (c2d/with-canvas-> canvas
                   (c2d/set-font-attributes size)
                   (c2d/text-bounding-box text))
        text-width (nth text-box 2)
        text-heigth  (nth text-box 3)
        btn-w (* text-width 1.8)
        btn-h (* text-heigth 1.8)
        border-width (if (and (number? width) (< btn-w width)) width btn-w)
        border-heigth (if (and (number? height) (< btn-h height)) height btn-h)]
      [x y border-width border-heigth]))

(defn box-draw-text 
  "Draws the text of the box"
  [canvas text {:keys [^long x ^long y color ^long width font-style font-size] :as args}]
  (let [style (if (empty? font-style) :bold (first font-style))
        size (if (number? font-size) font-size default-font-size)
        [_ _ border-width border-heigth] (box-coord canvas text args)
        [_ text-y text-width _] (c2d/with-canvas-> canvas
                          (c2d/set-font-attributes size style)
                          (c2d/text-bounding-box text))
        background-color (if (> (count color) 0) (first color) :grey)
        foreground-color (if (> (count color) 1) (nth color 1) :black)
        x-offset (if (and (number? width) (>= border-width width))
                   (/ (- border-width text-width) 2.0)
                   (* border-width 0.12))]
      (c2d/with-canvas-> canvas
        (c2d/set-color background-color)
        (c2d/rect x y border-width border-heigth)
        (c2d/set-font-attributes size style)
        (c2d/set-color foreground-color)
        (c2d/text text (+ x x-offset) (- y (* text-y 1.5))))))

(defn box-draw
  "canvas - clojure2d canvas
  text - text displayed inside the input
  x - x coordinate of top left corner
  y - y coordinate of top left corner
  color - vector consisting of [background-color font-color]
  min-width - the minimum width"
  ([args] (apply box-draw args))
  ([canvas text args]
  (let [{:keys [^long x ^long y color ^long min-width]} args
        [_ _ border-width border-heigth] (box-coord canvas text {:x x :y y :min-width min-width})
        background-color (if (> (count color) 0) (first color) :grey)]
    (c2d/with-canvas-> canvas
      (c2d/set-color background-color)
      (c2d/rect x y border-width border-heigth))
    (box-draw-text canvas text args)
    [x y border-width border-heigth])))

(defn box-border 
  ([canvas color strength x y w h] 
    (box-border canvas color strength x y w h true))
  ([canvas color strength x y w h no-fill]
  (when (> strength 0)
      (c2d/with-canvas-> canvas
        (c2d/set-color color)
        (c2d/rect (- x strength) (- y strength) (+ w (* 2 strength)) (+ h (* 2 strength)) no-fill))
      (box-border canvas color (- strength 1) x y w h no-fill))))

(defn box-draw-border 
  ([box canvas] (box-draw-border box canvas :black 1))
  ([box canvas color] (box-draw-border box canvas color 1))
  ([box canvas color strength] (box-draw-border box canvas color strength false))
  ([box canvas color strength fill]
  (let [[x y w h] (wdg/coord box canvas)]
    (box-border canvas color strength x y w h (not fill)))))