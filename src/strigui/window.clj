(ns strigui.window
  (:require [clojure2d.core :as c2d])
  (:import [javax.swing JFrame]))

(defn init-window
  "Creates a window based on the canvas size. Alternatively, also accepts an already
   existing window."
  ([^clojure2d.core.Window window]
   (let [title (:window-name window)
         window (assoc window :window-name "main-window")]
     ;; change the title of the underlying frame, so it doesn't mess with the
     ;; window name used for the clojure2d events
     (.setTitle (:frame window) title)))
  ([width height ^String title] (init-window width height title 10 :mid))
  ([width height ^String title fps quality]
   (let [canvas (c2d/canvas width height quality)
         window (c2d/show-window canvas "main-window" width height fps)
         new-context {:canvas canvas
                      :window window}]
     ;; change the title of the underlying frame, so it doesn't mess with the
     ;; window name used for the clojure2d events
     (.setTitle (:frame window) title)
     (.setDefaultCloseOperation (:frame window) (. JFrame EXIT_ON_CLOSE))
     new-context)))

(defn close-window
  [^clojure2d.core.Window window]
  (c2d/close-window window))

(defn display-info [context text]
  (let [canvas (:canvas context)
        height (c2d/height canvas)
        width (c2d/width canvas)
        [_ _ _ h] (c2d/with-canvas-> canvas
                    (c2d/text-bounding-box text))]
    (c2d/with-canvas-> canvas
      (c2d/set-color :white)
      (c2d/rect 0 (- height h) width h)
      (c2d/set-font-attributes 15)
      (c2d/set-color :black)
      (c2d/text (str text) 0 height))))