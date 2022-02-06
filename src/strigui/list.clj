(ns strigui.list
  (:require [strigui.box :as b]
             [strigui.widget :as wdg]
             [capra.core :as c]))

(defonce item-height 25)

(defn make-darker-or-brighter
  [^java.awt.Color color]
  (let [hsl (java.awt.Color/RGBtoHSB (.getRed color) (.getGreen color) (.getBlue color) nil)]
    (if (> (get hsl 2) 0.8) (.darker color) (-> color (.brighter) (.brighter) (.brighter) (.brighter) (.brighter)))))

(defn get-index-at 
  [widget y]
  (let [index (int (/ (- y (-> widget :args :y)) item-height))]
    index))

(defn draw-list
  [canvas items args]
    (loop [items items
           ind 0]
      (when (seq items)
        (let [color (:color args)
              color (if (:hovered? (first items)) (update color 0 make-darker-or-brighter) color)]
          (b/box-draw canvas (-> items first :value) (merge {:y (+ (* ind item-height) (:y args)) :max-width (:width args) :color color}
                                                            (select-keys args [:width :x]))))
        (recur (rest items) (inc ind)))))

(defrecord List [name items args ]
  wdg/Widget
  (coord [this canvas] (let [{:keys [x y width height]} (:args this)]
                         [x y width height]))
  (defaults [this]
      (when (not (instance? clojure.lang.Atom (:items this)))
        (let [ref-value (atom (:items this))]
          (assoc this :items ref-value))))
  (draw [this canvas]
        (let [{:keys [x y width height color] :as args} (:args this)]
          (c/draw-> canvas
                    (c/rect x y width height (first color) false))
          (draw-list canvas @(:items this) args))
        this))

(defn clear-out 
  [items state]
  (let [cleared-items (mapv #(merge % {state false}) items)]
    cleared-items))

(defmethod wdg/widget-event [strigui.list.List :mouse-clicked]
 [_ canvas widget x y]
 (println ":mouse-clicked  Widget: " (:name widget) " clicked at [" x " " y "]"))

(defmethod wdg/widget-event [strigui.list.List :mouse-moved]
  [_ canvas widget x y]
  (let [index (get-index-at widget y)]
    (when-let [item (get @(:items widget) index)]
      (swap! (:items widget) clear-out :hovered?)
      (swap! (:items widget) assoc index (assoc item :hovered? true)))))