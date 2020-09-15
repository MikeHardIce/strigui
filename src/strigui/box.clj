(ns strigui.box
  (:require [clojure2d.core :as c2d]
            [clojure.set :as s]
            [strigui.window :as wnd]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defprotocol render 
  "collection of functions around redrawing boxes, managing the border etc. ..."
  (draw-hover [this canvas] "")
  (draw-clicked [this canvas] "")
  (redraw [this canvas] ""))

(defn box-border [canvas color ^long strength [^long x ^long y ^long w ^long h]]
  (when (> strength 0)
      (c2d/with-canvas-> canvas
      ;(c2d/set-stroke strength :butt 0 0)
        (c2d/set-color color)
        (c2d/rect (- x strength) (- y strength) (+ w (* 2 strength)) (+ h (* 2 strength)) true))
      (box-border canvas color (- strength 1) [x y w h])))

(defn box-draw-hover 
  [box canvas] (apply box-border (conj [canvas :black 2] (:coord box)))
  box)

(defn box-redraw 
  [box canvas] 
  (let [coord (:coord box)]
    (when (not-empty coord)
      (apply box-border (conj [canvas :white 2] coord))
      (apply box-border (conj [canvas :black 1] coord))
      box)))

(defrecord Box [name coord create-func args]
  render
  (draw-hover [this canvas] (box-draw-hover this canvas))
   (draw-clicked [this canvas] this)
   (redraw [this canvas] (box-redraw this canvas)))

 (defprotocol events
   "collection of functions to hook into events"
   (clicked [this] ""))

;;{:coord [] :func :args [] :name ""}
(def boxes (atom ()))

(def boxes-to-redraw (atom #{}))

(def boxes-clicked (atom #{}))
  
(defn create-box
  "canvas - clojure2d canvas
   text - text displayed inside the input
   x - x coordinate of top left corner
   y - y coordinate of top left corner
   color - vector consisting of [background-color font-color]
   min-width - the minimum width"
  [canvas text {:keys [^long x ^long y color ^long min-width]}]
  (let [text-box (c2d/with-canvas-> canvas
                   (c2d/text-bounding-box text))
        text-width (nth text-box 2)
        text-heigth  (nth text-box 3)
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
   min-width - the minimum width
   create-func - function that creates a specific record of a component"
  [canvas name text {:keys [x y color min-width]} create-func]
  (let [args [canvas text {:x x :y y :color color :min-width min-width}]
        func create-box
        coord (apply func args)]
    (apply box-border (conj [canvas :black 1] coord))
    (swap! boxes conj (create-func name coord func args))))

  ;; TODO: maybe its not necessary to go to @wnd/context directly
(defmethod c2d/mouse-event ["main-window" :mouse-moved] [event state]
  (let [context @wnd/context
        canvas (:canvas context)
        window (:window context)
        btn-hits (first (filter #(wnd/within? (:coord %) (c2d/mouse-x window) (c2d/mouse-y window)) @boxes))
        btns @boxes-to-redraw]
    (wnd/display-info context (str (c2d/mouse-pos window) " " @boxes-to-redraw))
    (if (empty? btn-hits)
      (let [redrawn-buttons (map #(redraw % canvas) btns)]
        (swap! boxes-to-redraw #(s/difference %1 (set %2))  redrawn-buttons))
      (do 
        (draw-hover btn-hits canvas)
        (swap! boxes-to-redraw  #(conj %1 %2) btn-hits))))
  state)

;; TODO: maybe its not necessary to go to @wnd/context directly
(defmethod c2d/mouse-event ["main-window" :mouse-pressed] [event state]
  (let [context @wnd/context
        canvas (:canvas context)
        window (:window context)
        btn (first (filter #(wnd/within? (:coord %) (c2d/mouse-x window) (c2d/mouse-y window)) @boxes))]
    (when (not-empty btn)
      (draw-clicked btn canvas)
      (swap! boxes-clicked #(conj %1 %2) btn)
      (println btn)
      ;;(e/button-clicked btn)
      (clicked btn)))
  state)

(defmethod c2d/mouse-event ["main-window" :mouse-released] [event state]
  (map #(draw-hover %1 (:canvas @wnd/context)) @boxes-clicked)
  (reset! boxes-clicked  #{})
  state)