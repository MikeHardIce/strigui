(ns strigui.input
  (:require [clojure2d.core :as c2d]
            [clojure.set :as s]
            [strigui.widget :as wdg]
            [strigui.box :as b]
            [strigui.events :as e]))

;; TODO: check out alternatives to records
(defrecord Input [name value coordinates args]
  wdg/Widget
  (coord [this] (:coordinates this))
  (value [this] (:value this))
  (args [this] (:args this))
  (widget-name [this] (:name this))
  (redraw [this canvas] 
    (when (not (b/focused? this))
      (b/box-redraw this canvas)))
  (draw [this canvas]
    (b/box-draw-border this canvas) 
    (b/box-draw canvas (:value this) (:args this))))

(extend-protocol b/Box
  Input
  (draw-hover [this canvas] 
    (when (not (b/focused? this))
      (b/box-draw-hover this canvas)))
  (draw-clicked [this canvas]  
    (b/box-draw-border this canvas :blue 2)
    this))

(defn adjust-text [text char code]
  (if (and (= code :back_space) (> (count text) 0)) 
    (subs text 0 (- (count text) 1))
    (str text char)))

(extend-protocol e/Actions
  Input
  (clicked [this] 
    (b/swap-focused! this))
  (key-pressed [this char code]
    (e/input-modified this)
    (assoc this :value (adjust-text (:value this) char code))))

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
  (let [arg [canvas text args]
        coord (apply b/box-coord arg)]
    (Input. name text coord args)))

(defmethod wdg/widget-event [strigui.input.Input :mouse-moved] 
  [_ canvas widget]
  (println "mouse moved Input")
  (b/draw-hover widget canvas))

(defmethod wdg/widget-event [strigui.input.Input :mouse-clicked]
  [_ canvas widget]
  (println "mouse clicked Input")
  (e/clicked widget)
  (b/draw-clicked widget canvas)
  (swap! b/boxes-clicked #(conj %1 %2) widget))