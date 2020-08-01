(ns strigui.core
  (:require [clojure2d.core :as c2d]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;{:coord [] :func :args []}
(def buttons (atom []))

(def canvas (c2d/canvas 600 600))

(def window (c2d/show-window canvas "main-window"))

(defn- create-button
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
      (c2d/text text (+ x x-offset) (- y (* text-y 1.5))))
    [x y btn-width btn-heigth]))

(defn button
  "canvas - clojure2d canvas
   text - text displayed inside the button
   x - x coordinate of top left corner
   y - y coordinate of top left corner
   color - vector consisting of [background-color font-color]
   min-width - the minimum width"
  [canvas text {:keys [x y color min-width]}]
  (let [args [canvas text {:x x :y y :color color :min-width min-width}]
        func create-button
        coord (apply func args)]
    (swap! buttons conj {:coord coord :func func :args args})))

(defn within? 
  "Checks wheter the point (x y) is within the given coord
   coord - vector [x-coord y-coord width height]
   x - x-coord of point to check
   y - y-coord of point to check"
  [coord x y]
  (and (>= x (first coord)) 
       (>= y (nth coord 1))
       (<= x (+ (first coord) (nth coord 2)))
       (<= y (+ (nth coord 1) (nth coord 3)))))

(defn display-bottom [canvas text]
  (let [height (c2d/height canvas)
        width (c2d/width canvas)
        [_ _ _ h] (c2d/with-canvas-> canvas
                    (c2d/text-bounding-box text))]
    (c2d/with-canvas-> canvas
      (c2d/set-color :white)
      (c2d/rect 0 (- height h) width h)
      (c2d/set-font-attributes 15)
      (c2d/set-color :black)
      (c2d/text (str text) 0 height))))

;; create window
(defn add-buttons [canvas]
  (button canvas "Hello World!" {:x 50 :y 50 :color [:green :red]})
  (button canvas "How are you?" {:x 50 :y 100 :color [:red :blue]})
  (button canvas "Blah" {:x 50 :y 150 :color [:blue :yellow] :min-width 100})
  (button canvas "Bye" {:x 50 :y 200 :color [:yellow :green] :min-width 100}))

(defmethod c2d/mouse-event ["main-window" :mouse-moved] [event state]
  (let [btn-hits (filter #(within? (:coord %) (c2d/mouse-x window) (c2d/mouse-y window)) @buttons)]
    (display-bottom canvas (str (c2d/mouse-pos window) " " (first btn-hits))))
  state)