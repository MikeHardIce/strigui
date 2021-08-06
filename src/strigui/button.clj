(ns strigui.button
  (:require [strigui.box :as b]
            [strigui.widget :as wdg]))

(defrecord Button [name value args events]
  wdg/Widget
  (coord [this canvas] (apply b/box-coord [canvas (:value this) (:args this)])) ;; could be a mapping if the record would look different
  (value [this] (:value this))
  (args [this] (:args this))
  (widget-name [this] (:name this))
  (draw [this canvas]
        (cond
          (-> this :args :selected?) (b/box-draw-border this canvas :blue 2)
          (-> this :args :focused?) (b/box-draw-border this canvas :black 2)
          :else (b/box-draw-border this canvas :black 1))
        (b/box-draw canvas (:value this) (:args this))))

(extend-protocol b/Box
  Button
  (draw-hover [this canvas] (b/box-draw-hover this canvas))
  (draw-clicked [this canvas] 
    (b/box-draw-border this canvas :blue 2)
    this))

;; (defmethod wdg/widget-event [strigui.button.Button :widget-focus-in] 
;;   [_ canvas widget]
;;   (b/draw-hover widget canvas))

;; (defmethod wdg/widget-event [strigui.button.Button :widget-focus-out]
;;   [_ canvas widget]
;;   (b/box-remove-drawn widget canvas))

;; (defmethod wdg/widget-event [strigui.button.Button :mouse-clicked]
;;   [_ canvas widget]
;;   (b/draw-clicked widget canvas)
;;   (swap! b/boxes-clicked #(conj %1 %2) widget))