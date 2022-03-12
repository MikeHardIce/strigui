(ns strigui.input
  (:require [strigui.widget :as wdg]
            [strigui.box :as b]))

(defrecord Input [name value args]
  wdg/Widget
  (coord [this canvas] (apply b/box-coord [canvas (:value this) (:args this)]))
  (defaults [this] (assoc-in this [:args :has-border?] true))
  (draw [this canvas]
        (b/box-draw canvas (:value this) (:args this))
        this))

;; (extend-protocol wdg/Draw-resizing
;;   Input
;;   (draw-resizing [this canvas] (let [[x y w h] (wdg/coord this canvas)]
;;                                  (c2d/with-canvas-> canvas
;;                                    (c2d/set-color :orange)
;;                                    (c2d/rect x y w h))
;;                                  this)))
;; (extend-protocol wdg/Hide
;;  Input
;;   (hide [this canvas] (let [[x y w h] (wdg/coord this canvas)]
;;                         (println "hide call to overriden function!")
;;                         (c2d/with-canvas-> canvas
;;                           (c2d/set-color :white)
;;                           (c2d/rect (- x 5) (- y 5) (+ w 8) (+ h 8))))))

(defn adjust-text [text char code]
  (if (and (= code 8) (> (count text) 0)) 
    (subs text 0 (- (count text) 1))
    (str text char)))

(defn key-pressed [this char code]
  (if (or (and (<= code 28) (not= code 8))
          (and (= code 8) (< (count (:value this)) 1)))
    this
    (assoc this :value (adjust-text (:value this) char code))))

(defn input
  " name - name of the input element
    text - text displayed inside the input element
    args - map of properties:
      x - x coordinate of top left corner
      y - y coordinate of top left corner
      color - vector consisting of [background-color font-color]
      min-width - the minimum width"
  [name text args]
    (let [input (->Input name text args)]
      input))

(defn handle-key-pressed
  [canvas widget char code]
    (let [box-with-new-input (key-pressed widget char code)
          box-with-new-input (assoc-in box-with-new-input [:args :selected?] (or (not= code 10) ;;enter
                                                                                 (not= code 9)))] ;; tab
      (if (and (-> widget :args :selected?) box-with-new-input)
        (do
          ;;(wdg/replace! canvas (:name widget) box-with-new-input)
          (b/box-draw-text canvas (:value box-with-new-input) (:args box-with-new-input))
          box-with-new-input)
        widget)))

(defmethod wdg/widget-event [strigui.input.Input :key-pressed]
  [_ canvas widget char code]
  (handle-key-pressed canvas widget char code))