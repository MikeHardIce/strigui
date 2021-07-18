(ns strigui.box
  (:require [clojure2d.core :as c2d]
            [strigui.widget :as wdg]))

(set! *warn-on-reflection* true)
;;(set! *unchecked-math* :warn-on-boxed)

(defprotocol Box 
  "collection of functions around redrawing boxes, managing the border etc. ..."
  (draw-hover [this canvas] "draws the hover effect")
  (draw-clicked [this canvas] "draws the clicked effect"))

(defprotocol Event
  "collection of functions to hook into events"
  (key-pressed [this char code] ""))

(def ^:private default-font-size 15)

(def boxes-clicked (atom #{}))

(defn box-coord 
  "Computes the full box coordinates.
  Returns the vector [x y border-width border-heigth]"
  [canvas text {:keys [^long x ^long y ^long min-width font-size]}]
  (let [size (if (number? font-size) font-size default-font-size)
        text-box (c2d/with-canvas-> canvas
                  (c2d/set-font-attributes size)
                  (c2d/text-bounding-box text))
      text-width (nth text-box 2)
      text-heigth  (nth text-box 3)
      btn-w (* text-width 1.8)
      border-width (if (and (number? min-width) (< btn-w min-width)) min-width btn-w)
      border-heigth (* text-heigth 1.8)]
      [x y border-width border-heigth]))

(defn box-draw-text 
  "Draws the text of the box"
  [canvas text {:keys [^long x ^long y color ^long min-width align font-style font-size]}]
  (let [style (if (empty? font-style) :bold (first font-style))
        size (if (number? font-size) font-size default-font-size)
        [_ _ border-width border-heigth] (box-coord canvas text {:x x :y y 
                                                                 :min-width min-width 
                                                                 :font-size font-size})
        [_ text-y text-width _] (c2d/with-canvas-> canvas
                          (c2d/set-font-attributes size style)
                          (c2d/text-bounding-box text))
        background-color (if (> (count color) 0) (first color) :grey)
        foreground-color (if (> (count color) 1) (nth color 1) :black)
        x-offset (if (and (number? min-width) (= min-width border-width))
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
  ([^strigui.box.Box box canvas] (box-draw-border box canvas :black 1))
  ([^strigui.box.Box box canvas color] (box-draw-border box canvas color 1))
  ([^strigui.box.Box box canvas color strength] (box-draw-border box canvas color strength false))
  ([^strigui.box.Box box canvas color strength fill]
  (let [[x y w h] (wdg/coord box canvas)]
    (box-border canvas color strength x y w h (not fill)))))

(defn box-draw-hover 
  [^strigui.box.Box box canvas] 
  (box-draw-border box canvas :black 2)
  box)

(defn box-redraw 
  [^strigui.box.Box box canvas] 
  (let [coord (wdg/coord box canvas)]
    (when (seq coord)
    (box-draw-border box canvas :white 2)
      (box-draw-border box canvas :black 1)
      (wdg/draw box canvas)
      box)))

(defn box-remove-drawn 
  [^strigui.box.Box box canvas]
  (box-draw-border box canvas :white 2 false)
  (box-draw-border box canvas :black 1 false))

(defmethod wdg/widget-global-event :mouse-released 
  [_ canvas & args]
  (map #(draw-hover %1 canvas) @boxes-clicked)
  (reset! boxes-clicked  #{}))

;; TODO: should remove global events
(defn handle-key-pressed
  [canvas widget char code]
  (when (wdg/selected? widget)
    (let [box-with-new-input (key-pressed widget char code)]
      (when box-with-new-input
        (wdg/unregister canvas widget)
        (wdg/register canvas box-with-new-input)
        (box-draw-text canvas (wdg/value box-with-new-input) (wdg/args box-with-new-input))
        (if (= code :enter)
          (reset! wdg/selected-widget nil)
          (do 
            (reset! wdg/selected-widget box-with-new-input)
            (draw-clicked @wdg/selected-widget canvas)))))))