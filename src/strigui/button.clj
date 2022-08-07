(ns strigui.button
  (:require [strigui.box :as b]
            [strigui.widget :as wdg]))

(defrecord Button [name value props]
  wdg/Widget
  (coord [this canvas] (apply b/box-coord [canvas (:value this) (:props this)]))
  (defaults [this] (assoc-in this [:props :has-border?] true))
  (before-drawing [this] this)
  (draw [this canvas]
        (b/box-draw canvas (:value this) (:props this)))
  (after-drawing [this] this))

(defmethod wdg/widget-event [strigui.button.Button :key-pressed]
  [_ canvas widgets widget char code]
  (if (= code 10) ;;enter
    (let [[x y] (wdg/coord widget canvas)]
      (wdg/handle-clicked canvas widgets x y))
    widgets))