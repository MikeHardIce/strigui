(ns strigui.button
  (:require [strigui.box :as b]
            [strigui.widget :as wdg]))

(set! *warn-on-reflection* true)

(defrecord Button [name value props]
  wdg/Widget
  (coord [this context] (apply b/box-coord [context (:value this) (:props this)]))
  (defaults [this] (assoc-in this [:props :highlight] [:border :alpha]))
  (before-drawing [this] this)
  (draw [this context]
        (b/box-draw context (:value this) (:props this)))
  (after-drawing [this] 
                 this))

(defmethod wdg/widget-event [strigui.button.Button :key-pressed]
  [_ widgets widget _ code _]
  (if-let [window (wdg/widget->window-key widgets (:name widget))]
    (if (= code 10) ;;enter
      (let [context (-> window :context)
            [x y] (wdg/coord widget context)]
        (wdg/handle-clicked context widgets x y))
      widgets)
    widgets))