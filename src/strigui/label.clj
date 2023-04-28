(ns strigui.label
  (:require [capra.core :as c]
            [strigui.widget :as wdg]
            [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defn- create-label [context text {:keys [x y align color font-style font-size]} line-size]
  (let [font-color (if (empty? color) :black (:text color))
        ;; TODO: implement style and alignment after capra gets that feature
        alignment (if (empty? align) :left (first align))
        style (if (empty? font-style) :normal (first font-style))
        size (if (number? font-size) font-size 11)]
    (loop [lines (str/split-lines (str/replace text #"  " ""))
           mlt 0]
      (when (seq lines)
        (c/draw-> context
          (c/text x (+ 20 y (* mlt line-size)) (first lines) font-color size style))
          (recur (rest lines) (inc mlt))))))

(defn coord-label
  [lbl context]
  (let [text (str/split-lines (str/replace (:value lbl) #"  " ""))
        largest-line (->> text (sort-by count) reverse first)
        font-size (:font-size (:props lbl)) 
        {:keys [width height]} (:props lbl)
        size (if (number? font-size) font-size 11)
        [text-width text-height] (c/get-text-dimensions context largest-line size)
        [text-width text-height] [(* text-width 1.22) text-height]
        [width height] [(if (> width text-width) width text-width) (if (> height text-height) height text-height)]]
    [(-> lbl :props :x)
     (-> lbl :props :y)
     width height text-height]))

(defn draw-label
  [lbl context]
  (let [[x y _ _ line-height] (coord-label lbl context)]
    (create-label context (:value lbl) (:props lbl) line-height)
    lbl))

(defrecord Label [name value props]
  wdg/Widget
  (coord [this context] (coord-label this context))
  (defaults [this] this)
  (before-drawing [this] this)
  (draw [this context] (draw-label this context))
  (after-drawing [this] this))