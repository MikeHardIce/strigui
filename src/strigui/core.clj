(ns strigui.core
  (:require
   [strigui.button]
   [strigui.label]
   [strigui.input :as inp]
   [strigui.window :as wnd]
   [strigui.widget :as wdg])
  (:import [strigui.button Button]
           [strigui.label Label]
           [strigui.input Input]))

(defn find-by-name 
  "Find and return an widget by its name"
  [name]
  (first (filter #(= (wdg/widget-name %) name) (:widgets @wdg/state))))

(defn remove! 
  "Remove an widget by its name"
  [name]
  (when-let [box-to-remove (find-by-name name)]
    (wdg/unregister (:canvas (:context @wdg/state)) box-to-remove)))

(defn update! 
"Update any property of a widget via the widget name.
 name - name of the widget
 key - either single key or vector of keys
 value - the new property value"
  [name key value]
  (when-let [w (find-by-name name)]
    (wdg/unregister (:canvas (:context @wdg/state)) w )
      (let [keys (if (seqable? key) key (vector key))]
        (wdg/register (:canvas (:context @wdg/state)) (assoc-in w keys value)))))

(defn window!
  "Initializes a new window or reuses an existing one
   wind - an already existing windows instance (experimental)
   width
   height
   fps -  frames per second
   quality - rendering quality :low :mid :high :highest"
  ([wind] (wnd/init-window wind))
  ([width height title]
   (swap! wdg/state assoc :context (wnd/init-window width height title)))
  ([width height title ^Integer fps quality]
   {:pre [(> fps 0) (some #(= % quality) [:low :mid :high :highest])]}
   (swap! wdg/state assoc :context (wnd/init-window width height title fps quality))))
  
(defn create! 
  "Register and show a custom widget"
  [^strigui.widget.Widget widget]
  (wdg/register (:canvas (:context @wdg/state)) widget))

(defn close-window
  "Closes the current active window."
  []
  (wnd/close-window (:window (:context @wdg/state))))

(defn button!
  "Create a simple button on screen.
   name - name of the element
   text - text displayed inside the button
   args - map of:
     x - x coordinate of top left corner
     y - y coordinate of top left corner
     color - vector consisting of [background-color font-color]
     min-width - the minimum width"
  [name text args]
  (create! (Button. name text args {})))

(defn label!
   "Create a simple label on screen.
    name - name of the element
    text - text displayed inside the button
    args - map of:
     x - x coordinate of top left corner
     y - y coordinate of top left corner
     color - vector consisting of [font-color]
     font-style - vector consisting of either :bold :italic :italic-bold
     font-size - number"
  [name text args]
  (create! (Label. name text args)))

(defn input!
  "Create a simple imput field on screen.
   name - name of the element
   text - text displayed inside the button
   args - map of:
    x - x coordinate of top left corner
    y - y coordinate of top left corner
    color - vector consisting of [background-color font-color]
    min-width - the minimum width"
  [name text args]
  (create! (inp/input (:canvas (:context @wdg/state)) name text args)))

(defn info 
  [text]
  (wnd/display-info (:context @wdg/state) text))