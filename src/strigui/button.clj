(ns strigui.button
  (:require [strigui.box :as b]
            [strigui.widget :as wdg]))

(set! *warn-on-reflection* true)

(defrecord Button [name value props]
  wdg/Widget
  (coord [this context] (b/coord-for-box-with-text context (:value this) (:props this)))
  (defaults [this] (assoc-in this [:props :highlight] [:border :alpha]))
  (before-drawing [this] this)
  (draw [this context]
        (b/draw-box-with-text context (:value this) (:props this)))
  (after-drawing [this] 
                 this))

(defmethod wdg/widget-event [strigui.button.Button :key-pressed]
  [widgets {:keys [widget code]}]
  (if-let [window (wdg/widget->window widgets (:name widget))]
    (if (= code 10) ;;enter
      (let [context (-> window :context)
            [x y] (wdg/coord widget context)]
        (wdg/handle-clicked widgets (:name window) x y))
      widgets)
    widgets))