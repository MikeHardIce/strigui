(ns strigui.list
  (:require [strigui.box :as b]
             [strigui.widget :as wdg]
             [capra.core :as c]))

(defonce min-height 25)

(defn draw-list
  [canvas items args]
  (println "draw-list: " items)
    (loop [items items
           ind 0]
      (when (seq items)
        (b/box-draw canvas (-> items first :value) (merge {:y (+ (* ind min-height) (:y args))} (select-keys args [:width :x :color])))
        (recur (rest items) (inc ind)))))

(defrecord List [name items args ]
  wdg/Widget
  (coord [this canvas] (let [{:keys [x y width height]} (:args this)]
                         [x y width height]))
  (defaults [this]
    (println "defaults: " this)
    (when (not (instance? clojure.lang.Atom (:items this)))
      (let [ref-value (atom (:items this))]
        (assoc this :items ref-value))))
  (draw [this canvas]
        (let [{:keys [x y width height color] :as args} (:args this)]
          (c/draw-> canvas
                    (c/rect x y width height (first color) false))
          (draw-list canvas @(:items this) args))))

