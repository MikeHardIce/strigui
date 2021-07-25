(ns strigui.input
  (:require [strigui.widget :as wdg]
            [strigui.box :as b]))

(defonce dont-display [:shift :alt :alt_graph :left :right :up
                       :down :tab])

(defrecord Input [name value args]
  wdg/Widget
  (coord [this canvas] (apply b/box-coord [canvas (:value this) (:args this)]))
  (value [this] (:value this))
  (args [this] (:args this))
  (widget-name [this] (:name this))
  (redraw [this canvas]
     (if (wdg/selected? this)
       (b/box-draw-border this canvas :blue 2)
       (b/box-redraw this canvas))
     (b/box-draw canvas (:value this) (:args this)))
  (draw [this canvas]
    (b/box-draw-border this canvas) 
    (b/box-draw canvas (:value this) (:args this))))

(extend-protocol b/Box
  Input
  (draw-hover [this canvas] 
    (when (not (wdg/selected? this))
      (b/box-draw-hover this canvas)))
  (draw-clicked [this canvas]  
    (b/box-draw-border this canvas :blue 2)
    this))

(defn adjust-text [text char code]
  (if (and (= code :back_space) (> (count text) 0)) 
    (subs text 0 (- (count text) 1))
    (str text char)))

(extend-protocol b/Event
  Input
  (key-pressed [this char code]
    (if (or (some #(= code %) dont-display)
            (and (= code :back_space) (< (count (:value this)) 1)))
      this
      (assoc this :value (adjust-text (:value this) char code)))))

(defn input
  "canvas - clojure2d canvas
    name - name of the input element
    text - text displayed inside the input element
    args - map of properties:
      x - x coordinate of top left corner
      y - y coordinate of top left corner
      color - vector consisting of [background-color font-color]
      min-width - the minimum width"
  [canvas name text args]
    (let [input (->Input name text args)]
      (when (:selected? args)
        (b/draw-clicked input canvas))
      input))

(defmethod wdg/widget-event [strigui.input.Input :widget-focus-in]
  [_ canvas widget]
  (b/draw-hover widget canvas))

(defmethod wdg/widget-event [strigui.input.Input :widget-focus-out]
  [_ canvas widget]
  (b/box-remove-drawn widget canvas))

(defmethod wdg/widget-event [strigui.input.Input :mouse-clicked]
  [_ canvas widget]
  (swap! wdg/state assoc :selected widget)
  (b/draw-clicked widget canvas)
  (swap! b/boxes-clicked #(conj %1 %2) widget))

(defmethod wdg/widget-event [strigui.input.Input :key-pressed]
  [_ canvas widget char code]
  (b/handle-key-pressed canvas widget char code))