(ns strigui.label
  (:require [clojure2d.core :as c2d]
            [strigui.widget :as wdg]))

(defn- create-label [canvas text {:keys [x y color align font-style font-size]}]
  (let [font-color (if (empty? color) :black (first color))
        alignment (if (empty? align) :left (first align))
        style (if (empty? font-style) :normal (first font-style))
        size (if (number? font-size) font-size 11)]
    (c2d/with-canvas-> canvas
      (c2d/set-font-attributes size style)
      (c2d/set-color font-color)
      (c2d/text text x y alignment))))
    
(defrecord Label [name value coordinates args]
  wdg/Widget
    (coord [this] (:coordinates this))
    (value [this] (:value this))
    (args [this] (:args this))
    (widget-name [this] (:name this))
    (draw [this canvas] 
      (let [[x y] (wdg/coord this)]
        (create-label canvas (value this) args)
        this))
    (redraw 
      [this canvas]
      (wdg/draw this canvas)))

(defn create 
  [canvas name text {:keys [x y color align font-style font-size] :as arg}]
  (Label. name test [x y] arg))