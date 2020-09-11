(ns strigui.label
  (:require [clojure2d.core :as c2d]))

(def labels (atom []))

(defn- create-label [canvas text {:keys [x y color align]}]
  (let [font-color (if (empty? color) :black (first color))
        alignment (if (empty? align) :left (first align))]
    (c2d/with-canvas-> canvas
      (c2d/set-color font-color)
      (c2d/text text x y alignment))))

(defn label [context name text {:keys [x y color align]}]
  (let [args [(:canvas context) text {:x x :y y :color color :align align}]
        func create-label
        coord (apply func args)]
     (swap! labels conj {:coord coord :func func :args args :name name})))