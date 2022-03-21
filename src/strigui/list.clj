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
           index 0]
      (when (seq items)
        (let [color (:color args)
              item (first items)
              color (if (or (:hovered? item) (:selected? item)) (update color 0 make-darker-or-brighter) color)]
          (b/box-draw canvas (-> items first :value) (merge 
                                                      {:y (+ (* index item-height) (:y args)) :max-width (:width args) :color color}
                                                      (select-keys args [:width :x])))
          (when (:selected? item)
            (c/draw-> canvas
                      (c/line (inc (:x args)) (+ (* index item-height) (:y args)) 
                              (inc (:x args)) (+ (* (inc index) item-height) (:y args)) 
                              (get color 1 java.awt.Color/green) 3))))
        (recur (rest items) (inc index)))))

(defrecord List [name items args ]
  wdg/Widget
  (coord [this _] (let [{:keys [x y width height]} (:args this)]
                         [x y width height]))
  (defaults [this] this)
  (draw [this canvas]
        (let [{:keys [x y width height color] :as args} (:args this)]
          (c/draw-> canvas
                    (c/rect x y width height (first color) false))
          (draw-list canvas (:items this) args))
        this))

(defn clear-out 
  [items state]
  (let [cleared-items (mapv #(merge % {state false}) items)]
    cleared-items))

(defn activate!
  [widget y property]
  (let [index (get-index-at widget y)
        items (:items widget)]
    (if (seq (get items index))
      (-> widget
          (update :items clear-out property)
          (assoc-in [:items index property] true))
      widget)))

(defmethod wdg/widget-event [strigui.list.List :mouse-clicked]
 [_ canvas widgets widget _ y]
 (update widgets (:name widget) activate! y :selected?))

(defmethod wdg/widget-event [strigui.list.List :mouse-moved]
  [_ canvas widgets widget _ y]
  (update widgets (:name widget) activate! y :hovered?))