(ns strigui.core
  (:require [clojure2d.core :as c2d]
            [clojure.set :as s]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;{:coord [] :func :args [] :name ""}
(def buttons (atom []))

(def buttons-to-redraw (atom #{}))

(def buttons-clicked (atom #{}))

(def canvas (c2d/canvas 600 600))

(def window (c2d/show-window canvas "main-window"))

(defn- button-border [canvas color strength [x y w h]]
  (when (> strength 0)
    (c2d/with-canvas-> canvas
      ;(c2d/set-stroke strength :butt 0 0)
      (c2d/set-color color)
      (c2d/rect (- x strength) (- y strength) (+ w (* 2 strength)) (+ h (* 2 strength)) true))
    (button-border canvas color (- strength 1) [x y w h])))

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
  [canvas name text {:keys [x y color min-width]}]
  (let [args [canvas text {:x x :y y :color color :min-width min-width}]
        func create-button
        coord (apply func args)]
    (apply button-border (conj [canvas :black 1] coord))
    (swap! buttons conj {:coord coord :func func :args args :name name})))

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

(defn display-info [canvas text]
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
  (button canvas "a" "Hello World!" {:x 50 :y 50 :color [:green :red]})
  (button canvas "b" "How are you?" {:x 50 :y 100 :color [:red :blue]})
  (button canvas "c" "Blah" {:x 50 :y 150 :color [:blue :yellow] :min-width 100})
  (button canvas "d" "Bye" {:x 50 :y 200 :color [:yellow :green] :min-width 100}))

(defn- draw-hover 
  "Draws the hover effect of the given button on the given canvas"
  [canvas btn]
  (when (not-empty btn)
    (apply button-border (conj [canvas :black 2] (:coord btn)))
    (swap! buttons-to-redraw #(set (conj %1 %2)) btn)))

(defn- draw-clicked 
  "Draws the click effect of the given button on the given canvas"
  [canvas btn]
  (when (not-empty btn)
    (apply button-border (conj [canvas :green 2] (:coord btn)))
    (swap! buttons-clicked  #(set (conj %1 %2)) btn)))

(defn- redraw-button
  "Redraws the default border of the given button on the given canvas" 
  [canvas btn]
  (let [coord (:coord btn)]
    (when (not-empty coord) 
            (apply button-border (conj [canvas :white 2] coord))
            (apply button-border (conj [canvas :black 1] coord)))))

(defmethod c2d/mouse-event ["main-window" :mouse-moved] [event state]
  (let [btn-hits (first (filter #(within? (:coord %) (c2d/mouse-x window) (c2d/mouse-y window)) @buttons))
        btns @buttons-to-redraw]
    (display-info canvas (str (c2d/mouse-pos window) " " @buttons-to-redraw))
    (if (empty? btn-hits)
      (let [redrawn-buttons (map #(redraw-button canvas %) btns)]
        (swap! buttons-to-redraw #(s/difference %1 (set %2))  redrawn-buttons))
      (draw-hover canvas btn-hits)))
  state)

(defmulti button-clicked :name)

(defmethod button-clicked "" []
  )

(defmethod c2d/mouse-event ["main-window" :mouse-pressed] [event state]
  (let [btn (first (filter #(within? (:coord %) (c2d/mouse-x window) (c2d/mouse-y window)) @buttons))]
    (draw-clicked canvas btn)
    (button-clicked (:name btn)))
  state)

(defmethod c2d/mouse-event ["main-window" :mouse-released] [event state]
  (let [btn (first @buttons-clicked)]
    (draw-hover canvas btn))
  state)

