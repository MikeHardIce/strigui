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

(defmethod wdg/widget-event [strigui.button.Button :key-pressed]
  [_ canvas widget char code]
  (when (= code :enter)
    (let [[x y] (wdg/coord widget canvas)]
      (wdg/handle-clicked x y))))