(ns strigui.checkbox
  (:require [strigui.widget :as wdg]
            [strigui.box :as b]))

(defrecord Checkbox [name value props]
  wdg/Widget
  (coord [this window] (let [props (-> this 
                                       :props
                                       (update :x + 25)
                                       (update :width - 30))
                             [x y width height] (b/coord-for-box-with-text window (:text props) props)]
                         [(- x 25) y (+ width 30) height]))
  (defaults [this] this)
  (before-drawing [this] this)
  (draw [this window] 
    (let [props (:props this)
          props-square (-> props 
                           (select-keys [:x :y :color])
                           (assoc :width 20)
                           (assoc :thickness 1))]
      (b/draw-text window (:text props) (-> props
                                            (update :x + 30)
                                            (update :width - 30)
                                            (update :y - 10)))
      (if (= (:type props) :radio)
        (b/draw-circle window props-square (:value this))
        (b/draw-square window props-square (:value this)))
      this))
  (after-drawing [this] this))

(defmethod wdg/widget-event [strigui.checkbox.Checkbox :mouse-clicked]
  [_ wdgs widget x y]
  (update-in wdgs [(:name widget) :value] not))