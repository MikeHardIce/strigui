(ns strigui.stacks
    (:require [clojure2d.core :as c2d]))

(def stack-data (atom ()))

(defn draw-item-lines 
  [canvas val x y]
  (loop [y-offset (- y 5)
        curr-val val]
    (when (> curr-val 0)
      (c2d/with-canvas-> canvas
        (c2d/set-color :green)
        (c2d/set-stroke 3)
        (c2d/line x y-offset (+ x 30) y-offset))
       (recur (- y-offset 6) (- curr-val 1)))))

(defn draw-stack
  [canvas val x y h]
  (let [height (+ h 20)
        x-offset (+ x 35)]
    (c2d/with-canvas-> canvas
        (c2d/set-color :black)
        (c2d/set-stroke 2)
        (c2d/line x y x (+ y height))
        (c2d/line x-offset y x-offset (+ y height)))
    (draw-item-lines canvas val (+ x 3) (+ y height))))

(defn draw-stacks
  [canvas stack-vals x y]
  (let [items (apply + stack-vals)
        height (* 3 (+ items 6))]
    (loop [x-offset x
            cur-index 0]
            (when (< cur-index (count stack-vals))
              (draw-stack canvas (nth stack-vals cur-index) x-offset y height)
              (recur (+ x-offset 45) (inc cur-index))))))

(defn stacks
  [context name args]
  (let [canvas (:canvas context)
        {:keys [x y]} args]
        ;; TODO: maybe the list can simply be passed, instead of using an
        ;; atom later
        (draw-stacks canvas '(5 4 1 6 3 0) x y)))