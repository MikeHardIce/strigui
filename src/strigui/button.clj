(ns strigui.button
  (:require [clojure2d.core :as c2d]
            [clojure.set :as s]
            [strigui.window :as wnd]
            [strigui.box :as b]
            [strigui.events :as e]))

(def buttons-to-redraw (atom #{}))

(def buttons-clicked (atom #{}))

(defn button
  "canvas - clojure2d canvas
   text - text displayed inside the button
   x - x coordinate of top left corner
   y - y coordinate of top left corner
   color - vector consisting of [background-color font-color]
   min-width - the minimum width"
  [canvas name text {:keys [x y color min-width]}]
  (b/box canvas name test {x y color min-width}))

(defn- draw-hover
  "Draws the hover effect of the given button on the given canvas"
  [canvas btn]
  (when (not-empty btn)
    (apply b/button-border (conj [canvas :black 2] (:coord btn)))
    btn))

(defn- draw-clicked
  "Draws the click effect of the given button on the given canvas"
  [canvas btn]
  (when (not-empty btn)
    (apply b/button-border (conj [canvas :green 2] (:coord btn)))
    btn))

(defn- redraw-button
  "Redraws the default border of the given button on the given canvas"
  [canvas btn]
  (let [coord (:coord btn)]
    (when (not-empty coord)
      (apply b/button-border (conj [canvas :white 2] coord))
      (apply b/button-border (conj [canvas :black 1] coord))
      btn)))

(defmethod c2d/mouse-event ["main-window" :mouse-moved] [event state]
  (let [btn-hits (first (filter #(wnd/within? (:coord %) (c2d/mouse-x @wnd/window) (c2d/mouse-y @wnd/window)) @b/boxes))
        btns @buttons-to-redraw]
    (wnd/display-info wnd/canvas (str (c2d/mouse-pos @wnd/window) " " @buttons-to-redraw))
    (if (empty? btn-hits)
      (let [redrawn-buttons (map #(redraw-button wnd/canvas %) btns)]
        (swap! buttons-to-redraw #(s/difference %1 (set %2))  redrawn-buttons))
      (do 
        (draw-hover wnd/canvas btn-hits)
        (swap! buttons-to-redraw  #(conj %1 %2) btn-hits))))
  state)

(defmethod c2d/mouse-event ["main-window" :mouse-pressed] [event state]
  (let [btn (first (filter #(wnd/within? (:coord %) (c2d/mouse-x @wnd/window) (c2d/mouse-y @wnd/window)) @box/boxes))]
    (when (not-empty btn)
      (draw-clicked wnd/canvas btn)
      (swap! buttons-clicked #(conj %1 %2) btn)
      (e/button-clicked btn)))
  state)

(defmethod c2d/mouse-event ["main-window" :mouse-released] [event state]
  (map #(draw-hover wnd/canvas %1) @buttons-clicked)
  (reset! buttons-clicked  #{})
  state)