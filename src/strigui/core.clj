(ns strigui.core
  (:require [clojure2d.core :as c2d]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

;; define canvas
(def test-canvas (c2d/canvas 600 600))

(defn button
  "canvas - clojure2d canvas
   text - text displayed inside the button
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
        btn-width (if (and (number? min-width) (< btn-w min-width)) min-width btn-w)
        btn-heigth (* text-heigth 1.8)
        background-color (if (> (count color) 0) (first color) :grey)
        foreground-color (if (> (count color) 1) (nth color 1) :black)
        x-offset (if (and (number? min-width) (= min-width btn-width)) 
                   (/ (- btn-width text-width) 2.0)
                   (* btn-width 0.12))]
    (c2d/with-canvas-> canvas
      (c2d/set-color :black)
      (c2d/rect (- x 1) (- y 1) (+ btn-width 2) (+ btn-heigth 2))
      (c2d/set-color background-color)
      (c2d/rect x y btn-width btn-heigth)
      (c2d/set-font-attributes 15 :bold)
      (c2d/set-color foreground-color)
      (c2d/text text (+ x x-offset) (- y (* text-y 1.5))))))

;; create window
(defn show-window []
 (let [canvas (c2d/canvas 600 600)]
   (c2d/show-window test-canvas "Hello World!")
   (button test-canvas "Hello World!" {:x 50 :y 50 :color [:green :red]})
   (button test-canvas "How are you?" {:x 50 :y 100 :color [:red :blue]})
   (button test-canvas "Blah" {:x 50 :y 150 :color [:blue :yellow] :min-width 100})
   (button test-canvas "Bye" {:x 50 :y 200 :color [:yellow :green] :min-width 100})))