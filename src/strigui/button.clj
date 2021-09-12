(ns strigui.button
  (:require [strigui.box :as b]
            [strigui.widget :as wdg]))

(defrecord Button [name value args]
  wdg/Widget
  (coord [this canvas] (apply b/box-coord [canvas (:value this) (:args this)])) ;; could be a mapping if the record would look different
  (value [this] (:value this))
  (defaults [this] (assoc-in this [:args :has-border?] true))
  (widget-name [this] (:name this))
  (draw [this canvas]
        (b/box-draw canvas (:value this) (:args this))))

(defmethod wdg/widget-event [strigui.button.Button :key-pressed]
  [_ canvas widget char code]
  (when (= code :enter)
    (let [[x y] (wdg/coord widget canvas)]
      (wdg/handle-clicked x y))))