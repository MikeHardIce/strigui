(ns strigui.label
  (:require [clojure2d.core :as c2d]
            [strigui.widget :as wdg]))

(defn- create-label [canvas text {:keys [x y align color font-style font-size]}]
  (let [font-color (if (empty? color) :black (first color))
        alignment (if (empty? align) :left (first align))
        style (if (empty? font-style) :normal (first font-style))
        size (if (number? font-size) font-size 11)]
    (c2d/with-canvas-> canvas
      (c2d/set-font-attributes size style)
      (c2d/set-color font-color)
      (c2d/text text x y alignment))))

(defn coord-label
  [lbl canvas]
  (let [font-size (:font-size (:args lbl))
                             size (if (number? font-size) font-size 11)
                             [_ _ width height] (c2d/with-canvas-> canvas
                                                  (c2d/set-font-attributes size)
                                                  (c2d/text-bounding-box (:value lbl)))]
                         [(-> lbl (:args) (:x))
                          (-> lbl (:args) (:y) (- height))
                          (* width 1.15) (* height 1.05)]))

(defn draw-label
  [lbl canvas]
  (let [[x y] (coord-label lbl canvas)]
    (create-label canvas (:value lbl) (:args lbl))
    lbl))

(defrecord Label [name value args]
  wdg/Widget
  (coord [this canvas] (coord-label this canvas))
  (value [this] (:value this))
  (defaults [this] this)
  (widget-name [this] (:name this))
  (draw [this canvas] (draw-label this canvas)))