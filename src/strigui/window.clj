(ns strigui.window
  (:require [strigui.widget :as wdg]
            [capra.core :as c])
  (:import [javax.swing JFrame]))

(defrecord Window [name context props]
 wdg/Widget
  (coord [this _] this)
  (defaults [this] this)
  (before-drawing [this] this)
  (draw [this _] this)
  (after-drawing [this] this))

(defn window
  "Initializes a new window or reuses an existing one
   context - an already existing context (experimental)
   x - x position on the screen
   y - y position on the screen
   width - width of the window
   height - height of the window
   title - name displayed in the title bar of the window
   color - java.awt.Color of the windows background color
   rendering-hints - map of java.awt.RenderingHints key value combinations to configure the rendering quality
   of any widget drawn within the window" 
  ([x y width height title]
   (window x y width height title (java.awt.Color. 44 44 44) {java.awt.RenderingHints/KEY_ANTIALIASING java.awt.RenderingHints/VALUE_ANTIALIAS_ON}))
  ([x y width height title color]
   (window x y width height title color {java.awt.RenderingHints/KEY_ANTIALIASING java.awt.RenderingHints/VALUE_ANTIALIAS_ON
                                          java.awt.RenderingHints/KEY_RENDERING java.awt.RenderingHints/VALUE_RENDER_SPEED}))
  ([x y width height title color rendering-hints]
   (let [context (c/create-window x y width height title (eval color))
         context (assoc-in context [:canvas :rendering] rendering-hints)
         context (update context :canvas c/attach-buffered-strategy 2)]
     (Window. title context {:x x :y y :width width :height height :color {:background color}}))))
