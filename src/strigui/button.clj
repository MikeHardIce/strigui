(ns strigui.button
  (:require [strigui.box :as b]
            [strigui.widget :as wdg]))

(set! *warn-on-reflection* true)

(defrecord Button [name value props]
  wdg/Widget
  (coord [this canvas] (apply b/box-coord [canvas (:value this) (:props this)]))
  (defaults [this] (assoc-in this [:props :highlight] [:border :alpha]))
  (before-drawing [this] this)
  (draw [this canvas]
        (b/box-draw canvas (:value this) (:props this)))
  (after-drawing [this] 
                 this))

(defmethod wdg/widget-event [strigui.button.Button :key-pressed]
  [_ widgets widget _ code _]
  (if-let [window (wdg/widget->window-key widgets (:name widget))]
    (if (= code 10) ;;enter
      (let [canvas (-> window :context :canvas)
            [x y] (wdg/coord widget canvas)]
        (wdg/handle-clicked canvas widgets x y))
      widgets)
    widgets))