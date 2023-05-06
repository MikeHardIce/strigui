(ns strigui.checkbox
  (:require [strigui.widget :as wdg]
            [strigui.box :as b]
            [capra.core :as c]))

(defrecord Checkbox [name value props]
  wdg/Widget
  (coord [this window] (let [props (-> this 
                                       :props
                                       (update :x + 30)
                                       (update :width - 30))]
                         (b/coord-for-box-with-text window (:text props) props)))
  (defaults [this] this)
  (before-drawing [this] this)
  (draw [this window] 
    (let [props (:props this)
          props-square (-> props 
                           (select-keys [:x :y :color])
                           (assoc :width 25)
                           (assoc :thickness 1))]
      (b/draw-text window (:text props) (-> props
                                            (update :x + 30)
                                            (update :width - 30)
                                            (update :y - 5)))
      (b/draw-square window props-square (:value this))
      this))
  (after-drawing [this] this))