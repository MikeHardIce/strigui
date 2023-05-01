(ns strigui.box
  (:require [capra.core :as c]
            [clojure.string :as s])
  (:import [java.awt Color]))

(set! *warn-on-reflection* true)

(def ^:private default-font-size 15.0)

(defn coord-for-box-with-text
  "Computes the full box coordinates.
  Returns the vector [x y border-width border-heigth]"
  [context text {:keys [^long x ^long y ^long height ^long width font-size] :or {x 0 y 0 height 42 width 150 font-size 15}}]
  (let [size (if (number? font-size) font-size default-font-size)
        text-box (c/get-text-dimensions context text size)
        text-width (first text-box)
        text-heigth (second text-box)]
      [x y width height text-width text-heigth]))

(defn draw-text
  "Draws the text of the box"
  [context text {:keys [^long x ^long y color ^long height font-style font-size can-multiline?] :as props}]
  (let [style (if (empty? font-style) :bold (first font-style))
        size (if (number? font-size) font-size default-font-size)
        [_ _ border-width border-heigth text-width text-heigth] (coord-for-box-with-text context text props)
        text-color (get color :text (java.awt.Color. 10 10 10))
        x-offset (/ (- border-width text-width) 2)
        y-offset (/ (- border-heigth text-heigth) 2)]
    (if can-multiline?
      (loop [text (s/split-lines text)
             height-off 30.0] ;; <-- this is basically the top padding ;;(+ y-offset (* 0.8 text-heigth))
        (when (and (seq text) (<= height-off height))
          (c/draw-> context
                    (c/text (+ x 30.0) (+ y height-off) (first text) text-color size style))  ;; (+ x 30.0) 30 is basically the left padding
          (recur (rest text) (+ height-off (* 0.8 text-heigth) 10.0))))
      (c/draw-> context
                (c/text (+ x x-offset) (+ y y-offset (* 0.8 text-heigth)) text text-color size style)))))

(defn draw-box-with-text
  "context - capra {:frame ... :canvas ...} map 
  text - text displayed inside the input
  x - x coordinate of top left corner
  y - y coordinate of top left corner
  color - vector consisting of [background-color font-color]
  min-width - the minimum width
   max-width - the maximum width"
  [context text props]
  (let [{:keys [^long x ^long y color]} props
        [_ _ border-width border-heigth] (coord-for-box-with-text context text props)
        background-color (get color :background (java.awt.Color. 250 250 250))]
      (c/draw-> context 
                (c/rect x y border-width border-heigth background-color true)) 
    (draw-text context text props)
    [x y border-width border-heigth]))

(defn draw-square
  [context {:keys [x y width color thickness]}]
  (let [background-color (get color :background (java.awt.Color. 250 250 250))
        border-color (get color :border (java.awt.Color. 27 100 98))]
    (c/draw-> context
              (c/rect x y width width background-color true)
              (c/rect x y width width border-color false thickness))))

(defn draw-tick 
  [context {:keys [x y width color thickness]} shape]
  (c/draw-> context
            (case shape
              :cross (let [y+w (+ y width)
                           x+w (+ x width)]
                       (c/line x y x+w y+w color thickness)
                       (c/line x y+w x+w y color thickness))
              :circle (c/ellipse x y width width color false thickness)
              :ok (let [y2 (+ y (/ width 2))
                        x2 (+ x (/ width 2))
                        y+w (+ y width)
                        x+w (+ x width)] 
                    (c/line x y2 x2 y+w color thickness)
                    (c/line x2 y+w x+w y color thickness))
              nil)))