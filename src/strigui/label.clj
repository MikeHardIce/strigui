(ns strigui.label
  (:require [clojure2d.core :as c2d]
            [strigui.widget :as wdg]
            [clojure.string :as str]))

(defn- create-label [canvas text {:keys [x y align color font-style font-size]} line-size]
  (let [font-color (if (empty? color) :black (first color))
        alignment (if (empty? align) :left (first align))
        style (if (empty? font-style) :normal (first font-style))
        size (if (number? font-size) font-size 11)]
    (loop [lines (str/split-lines (str/replace text #"  " ""))
           mlt 0]
      (when (seq lines)
          (c2d/with-canvas-> canvas
            (c2d/set-font-attributes size style)
            (c2d/set-color font-color)
            (c2d/text (first lines) x (+ y (* mlt line-size)) alignment))
          (recur (rest lines) (inc mlt))))))

(defn coord-label
  [lbl canvas]
  (let [text (str/split-lines (str/replace (:value lbl) #"  " ""))
        largest-line (->> text (sort-by count) reverse first)
        font-size (:font-size (:args lbl))
        size (if (number? font-size) font-size 11)
        [_ _ width height line-height] (c2d/with-canvas-> canvas
                             (c2d/set-font-attributes size)
                             (c2d/text-bounding-box largest-line))]
                         [(-> lbl (:args) (:x))
                          (-> lbl (:args) (:y) (- height))
                          (* width 1.22) (* height 1.05 (count text)) height]))

(defn draw-label
  [lbl canvas]
  (let [[x y _ _ line-height] (coord-label lbl canvas)]
    (create-label canvas (:value lbl) (:args lbl) line-height)
    lbl))

(defrecord Label [name value args]
  wdg/Widget
  (coord [this canvas] (coord-label this canvas))
  (defaults [this] this)
  (draw [this canvas] (draw-label this canvas)))