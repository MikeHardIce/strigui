(ns strigui.window
  (:require [strigui.widget :as wdg]
            [capra.core :as c]
            [clojure.stacktrace])
  (:import [java.awt Dimension Color]))

(defrecord Window [name context props]
 wdg/Widget
  (coord [this _] this)
  (defaults [this] this)
  (before-drawing [this] 
                  (if (-> this :props :source-object-changed?)
                    (assoc-in this [:props :source-object-changed?] false)
                    (let [{:keys [frame canvas]} (:context this)
                          {:keys [x y width height title rendering-hints color on-close resizable? visible?]
                           :or {x 0 y 0 width 0 height 0 title "" rendering-hints {} color (:background Color/white) on-close c/exit resizable? false visible? true}} (:props this)
                          canvas (assoc canvas :rendering rendering-hints)
                          canvas (assoc canvas :canvas (doto (:canvas canvas)
                                                         (.setBackground ^java.awt.Color (:background color))))
                          window (doto frame
                                   (.setLocation x y)
                                   (.setSize (Dimension. width height))
                                   (.setBackground ^java.awt.Color (:background color))
                                   (.setTitle title)
                                   (.setDefaultCloseOperation on-close)
                                   (.setResizable resizable?)
                                   (.setVisible visible?))]
                      (assoc this :context {:frame window :canvas canvas}))))
  (draw [{:keys [name context props] :as this} window] 
        (let [{:keys [width height color]} props]
          (c/draw-> window
                    (c/rect 0 0 width height (:background color) true)))
        this)
  (after-drawing [this] this))

(defn window
  "Creates and renders the frame of a new window
   name - widget name
   x - x position on the screen
   y - y position on the screen
   width - width of the window
   height - height of the window
   title - name displayed in the title bar of the window
   property map with keys like:
        :color - java.awt.Color of the windows background color
        :rendering-hints - map of java.awt.RenderingHints key value combinations to configure the rendering quality
        of any widget drawn within the window
        :icon-path - file system path for the window icon" 
  [name x y width height title {:keys [color rendering-hints on-close icon-path resizable? visible?] :or {rendering-hints {java.awt.RenderingHints/KEY_ANTIALIASING java.awt.RenderingHints/VALUE_ANTIALIAS_ON
                                                                                                                             java.awt.RenderingHints/KEY_RENDERING java.awt.RenderingHints/VALUE_RENDER_SPEED}
                                                                                                            on-close c/exit
                                                                                                            icon-path nil
                                                                                                            resizable? false
                                                                                                            visible? true}}] 
   (let [context (c/create-window name x y width height title {:color (:background color) :on-close on-close :icon-path icon-path :resizable? resizable? :visible? visible?})
         context (assoc-in context [:canvas :rendering] rendering-hints)
         context (c/attach-buffered-strategy context 2)]
     (Window. name context {:title title :x x :y y :width width :height height :color color :rendering-hints rendering-hints 
                            :on-close on-close :icon-path icon-path :resizable? resizable? :visible? visible?})))

(defn window-from-context
  "Recreates a new window but uses the existing context without creating the frame again.
   context - an already existing context
   name - widget name
   x - x position on the screen
   y - y position on the screen
   width - width of the window
   height - height of the window
   title - name displayed in the title bar of the window
   property map with keys like:
        :color - java.awt.Color of the windows background color
        :rendering-hints - map of java.awt.RenderingHints key value combinations to configure the rendering quality
        of any widget drawn within the window
        :icon-path - file system path for the window icon"
  [context name x y width height title {:keys [color rendering-hints on-close icon-path resizable? visible?] :or {rendering-hints {java.awt.RenderingHints/KEY_ANTIALIASING java.awt.RenderingHints/VALUE_ANTIALIAS_ON
                                                                                                                           java.awt.RenderingHints/KEY_RENDERING java.awt.RenderingHints/VALUE_RENDER_SPEED}
                                                                                                          on-close c/exit
                                                                                                          icon-path nil
                                                                                                          resizable? false
                                                                                                          visible? true}}]
  (let [context (assoc-in context [:canvas :rendering] rendering-hints)
        context (c/attach-buffered-strategy context 2)]
    (Window. name context {:title title :x x :y y :width width :height height :color color :rendering-hints rendering-hints
                           :on-close on-close :icon-path icon-path :resizable? resizable? :visible? visible?})))

(defmethod c/handle-event :window-hidden [_ {:keys [window-name]}] 
  (wdg/swap-widgets! #(-> %
                          (assoc-in [window-name :props :visible?] false)
                          (assoc-in [window-name :props :source-object-changed?] true))))

(defmethod c/handle-event :window-shown [_ {:keys [window-name]}] 
  (wdg/swap-widgets! #(-> %
                          (assoc-in [window-name :props :visible?] true)
                          (assoc-in [window-name :props :source-object-changed?] true))))

(defmethod c/handle-event :window-resized [_ {:keys [x y width height window-name]}]
  (wdg/swap-widgets! #(-> %
                          (assoc-in [window-name :props :width] width)
                          (assoc-in [window-name :props :height] height)
                          (assoc-in [window-name :props :source-object-changed?] true))))

(defmethod c/handle-event :window-moved [_ {:keys [x y width height window-name]}]
  (wdg/swap-widgets! #(-> %
                          (assoc-in [window-name :props :x] x)
                          (assoc-in [window-name :props :y] y)
                          (assoc-in [window-name :props :source-object-changed?] true))))

(defmethod  c/handle-event :window-closed [_ {:keys [window-name]}]
  )