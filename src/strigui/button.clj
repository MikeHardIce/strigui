(ns strigui.button
  (:require [strigui.box :as b]
            [strigui.widget :as wdg]
            [capra.core :as c]))

(defrecord Button [name value args]
  wdg/Widget
  (coord [this canvas] (apply b/box-coord [canvas (:value this) (:args this)]))
  (defaults [this] (assoc-in this [:args :has-border?] true))
  (draw [this canvas]
        (b/box-draw canvas (:value this) (:args this))))

(defmethod wdg/widget-event [strigui.button.Button :key-pressed]
  [_ canvas widget char code]
  (when (= code 10) ;;enter
    (let [[x y] (wdg/coord widget canvas)]
      (c/handle-event :mouse-pressed nil {:x x :y y})))
  widget)