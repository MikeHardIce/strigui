(ns strigui.label
  (:require [capra.core :as c]
            [strigui.widget :as wdg]
            [clojure.string :as str]))

(defn- create-label [canvas text {:keys [x y align color font-style font-size]} line-size]
  (let [font-color (if (empty? color) :black (:text color))
        ;; TODO: implement style and alignment after capra gets that feature
        alignment (if (empty? align) :left (first align))
        style (if (empty? font-style) :normal (first font-style))
        size (if (number? font-size) font-size 11)]
    (loop [lines (str/split-lines (str/replace text #"  " ""))
           mlt 0]
      (when (seq lines)
        (c/draw-> canvas
          (c/text x (+ y (* mlt line-size)) (first lines) font-color size))
          (recur (rest lines) (inc mlt))))))

(defn coord-label
  [lbl canvas]
  (let [text (str/split-lines (str/replace (:value lbl) #"  " ""))
        largest-line (->> text (sort-by count) reverse first)
        font-size (:font-size (:props lbl))
        size (if (number? font-size) font-size 11)
        [width height] (c/get-text-dimensions canvas largest-line size)] 
    [(-> lbl (:props) (:x))                                                                      
     (-> lbl (:props) (:y) (- height))
     (* width 1.22) (* height 1.05 (count text)) height]))

(defn draw-label
  [lbl canvas]
  (let [[x y _ _ line-height] (coord-label lbl canvas)]
    (create-label canvas (:value lbl) (:props lbl) line-height)
    lbl))

(defrecord Label [name value props]
  wdg/Widget
  (coord [this canvas] (coord-label this canvas))
  (defaults [this] this)
  (before-drawing [this] this)
  (draw [this canvas] (draw-label this canvas))
  (after-drawing [this] this))