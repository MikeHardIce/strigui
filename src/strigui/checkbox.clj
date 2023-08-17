(ns strigui.checkbox
  (:require [strigui.widget :as wdg]
            [strigui.box :as b]
            [clojure.set :refer [intersection]]))

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
          h (- (/ (:height props) 2) 5)
          props-square (-> props 
                           (select-keys [:x :y :color :height])
                           (assoc :width 10)
                           (assoc :thickness 1)
                           (update :y #(+ % h)))]
      (b/draw-text window (:text props) (-> props
                                            (update :x + 20)
                                            (update :width - 20)))
      (if (= (:type props) :radio)
        (b/draw-circle window props-square (:value this))
        (b/draw-square window props-square (:value this)))
      this))
  (after-drawing [this] this))

(defmethod wdg/widget-event [strigui.checkbox.Checkbox :mouse-clicked]
  [_ wdgs widget _ _]
  (let [wdgs (if (= (-> widget :props :type) :radio)
               (let [groups (set (-> widget :props :group))
                     radio (filter #(= (-> % :props :type) :radio) (vals wdgs))
                     radio (map :name (filter #(seq (intersection (set (-> % :props :group)) groups)) radio))]
                 (loop [radio-keys radio
                        widgets wdgs]
                   (if (seq radio-keys)
                     (recur (rest radio-keys) (assoc-in widgets [(first radio-keys) :value] false))
                     widgets)))
               wdgs)]
    (update-in wdgs [(:name widget) :value] not)))