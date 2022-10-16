(ns strigui.window
  (:require [strigui.widget :as wdg]
            [capra.core :as c])
  (:import [java.awt Dimension]))

(defrecord Window [name context props]
 wdg/Widget
  (coord [this _] this)
  (defaults [this] this)
  (before-drawing [this] (let [{:keys [window canvas]} (:context this)
                               {:keys [x y width height title rendering-hints color on-close resizable?]} (:props this)
                               canvas (assoc canvas :rendering rendering-hints)
                               window (doto window
                                        (.setLocation x y)
                                        (.setSize (Dimension. width height))
                                        (.setTitle title)
                                        (.setBackground ^java.awt.Color (:background color))
                                        (.setDefaultCloseOperation on-close)
                                        (.setResizable resizable?))]
                           (assoc this :context {:window window :canvas canvas})))
  (draw [this _] 
        this)
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
  [name x y width height title {:keys [color rendering-hints on-close icon-path :resizable?] :or {color (java.awt.Color. 44 44 44)
                                                                                                  rendering-hints {java.awt.RenderingHints/KEY_ANTIALIASING java.awt.RenderingHints/VALUE_ANTIALIAS_ON
                                                                                                                   java.awt.RenderingHints/KEY_RENDERING java.awt.RenderingHints/VALUE_RENDER_SPEED}
                                                                                                  on-close c/exit
                                                                                                  icon-path nil
                                                                                                  resizable? false}}]
   (let [context (c/create-window name x y width height title {:color color :on-close on-close :icon-path icon-path :resizable? resizable?})
         context (assoc-in context [:canvas :rendering] rendering-hints)
         context (update context :canvas c/attach-buffered-strategy 2)]
     (Window. name context {:title title :x x :y y :width width :height height :color {:background color} :rendering-hints rendering-hints 
                            :on-close on-close :icon-path icon-path :resizable? resizable?})))