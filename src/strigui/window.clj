(ns strigui.window
  (:require [clojure2d.core :as c2d]))

(def canvas (c2d/canvas 600 600))

(def window (atom ()))

(defn create-window [width height]
  (reset! window (c2d/show-window canvas "main-window")))

(defn display-info [canvas text]
  (let [height (c2d/height canvas)
        width (c2d/width canvas)
        [_ _ _ h] (c2d/with-canvas-> canvas
                    (c2d/text-bounding-box text))]
    (c2d/with-canvas-> canvas
      (c2d/set-color :white)
      (c2d/rect 0 (- height h) width h)
      (c2d/set-font-attributes 15)
      (c2d/set-color :black)
      (c2d/text (str text) 0 height))))