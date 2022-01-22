(ns strigui.box
  (:require [capra.core :as c]
            [strigui.widget :as wdg])
  (:import [java.awt Color]))

(set! *warn-on-reflection* true)

(def ^:private default-font-size 15.0)

(defn box-coord 
  "Computes the full box coordinates.
  Returns the vector [x y border-width border-heigth]"
  [canvas text {:keys [^long x ^long y ^long height ^long width font-size]}]
  (let [size (if (number? font-size) font-size default-font-size)
        text-box (c/get-text-dimensions canvas text size)
        text-width (first text-box)
        text-heigth  (second text-box)
        btn-w (* text-width 1.8)
        btn-h (* text-heigth 1.8)
        border-width (if (and (number? width) (< btn-w width)) width btn-w)
        border-heigth (if (and (number? height) (< btn-h height)) height btn-h)]
    (println "box-coord: " [x y border-width border-heigth text-width text-heigth] " text: " text)
      [x y border-width border-heigth text-width text-heigth]))

(defn box-draw-text 
  "Draws the text of the box"
  [canvas text {:keys [^long x ^long y color ^long width font-style font-size] :as args}]
  (let [style (if (empty? font-style) :bold (first font-style))
        size (if (number? font-size) font-size default-font-size)
        [_ _ border-width border-heigth text-width text-heigth] (box-coord canvas text args)
        foreground-color (if (> (count color) 1) (nth color 1) Color/black)
        x-offset (/ (- border-width text-width) 2)
        y-offset (/ (- border-heigth text-heigth) 2)]
      (c/draw-> canvas
        (c/text (+ x x-offset) (+ y y-offset (* 0.8 text-heigth)) text foreground-color size style))))

(defn box-draw
  "canvas - java.awt canvas
  text - text displayed inside the input
  x - x coordinate of top left corner
  y - y coordinate of top left corner
  color - vector consisting of [background-color font-color]
  min-width - the minimum width"
  ([args] (apply box-draw args))
  ([canvas text args]
  (let [{:keys [^long x ^long y color ^long min-width]} args
        [_ _ border-width border-heigth] (box-coord canvas text args)
        background-color (if (> (count color) 0) (first color) Color/black)]
    (c/draw-> canvas
      (c/rect x y border-width border-heigth background-color true))
    (box-draw-text canvas text args)
    [x y border-width border-heigth])))

(defn box-border 
  ([canvas color strength x y w h] 
    (box-border canvas color strength x y w h true))
  ([canvas color strength x y w h no-fill]
  (when (> strength 0)
      (c/draw-> canvas
        (c/rect x y w h color (not no-fill) strength)))))

(defn box-draw-border 
  ([box canvas] (box-draw-border box canvas :black 1))
  ([box canvas color] (box-draw-border box canvas color 1))
  ([box canvas color strength] (box-draw-border box canvas color strength false))
  ([box canvas color strength fill]
  (let [[x y w h] (wdg/coord box canvas)]
    (box-border canvas color strength x y w h (not fill)))))