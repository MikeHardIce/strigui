(ns strigui.label
  (:require [clojure2d.core :as c2d]))

(def labels (atom []))

(defn- create-label [canvas text {:keys [x y color align font-style font-size]}]
  (let [font-color (if (empty? color) :black (first color))
        alignment (if (empty? align) :left (first align))
        style (if (empty? font-style) :normal (first font-style))
        size (if (number? font-size) font-size 11)]
    (c2d/with-canvas-> canvas
      (c2d/set-font-attributes size style)
      (c2d/set-color font-color)
      (c2d/text text x y alignment))))

(defn label [context name text {:keys [x y color align font-style font-size] :as arg}]
  (let [args [(:canvas context) text arg]
        func create-label
        coord (apply func args)]
     (swap! labels conj {:coord coord :func func :args args :name name})))