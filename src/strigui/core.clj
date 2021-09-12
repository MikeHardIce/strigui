(ns strigui.core
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
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

(defn find-by-group
  "Retuns a vector of widgets by group name"
  [name]
  (let [get-seq (fn [x] (if (string? x) (vector x) x))
        filter-crit (fn [x] (some #(= name %) (-> x :args :group get-seq)))]
    (filter filter-crit (:widgets @wdg/state))))

(defn remove! 
  "Remove an widget by its name"
  [name]
  (when-let [box-to-remove (find-by-name name)]
    (wdg/unregister! (:canvas (:context @wdg/state)) box-to-remove)))

(defn remove-group!
  "Removes all widgets assigned to the given group"
  [name]
  (when-let [widgets (find-by-group name)]
    (loop [widgets widgets]
      (when (seq widgets)
        (wdg/unregister! (:canvas (:context @wdg/state)) (first widgets))
        (recur (rest widgets))))
    widgets))

(defn- update-widget!
  [widget key value]
  (when (seq widget)
    (wdg/unregister! (:canvas (:context @wdg/state)) widget)
    (let [keys (if (seqable? key) key (vector key))]
      (wdg/register! (:canvas (:context @wdg/state)) (assoc-in widget keys value)))))

(defn update! 
"Update any property of a widget via the widget name.
 name - name of the widget
 key - either single key or vector of keys
 value - the new property value"
  [name key value]
  (when-let [w (find-by-name name)]
    (update-widget! w key value)))

(defn update-group!
  [name key value]
  (when-let [widgets (find-by-group name)]
    (loop [widgets widgets]
      (when (seq widgets)
        (update-widget! (first widgets) key value)
        (recur (rest widgets))))
    widgets))

(defn window!
  "Initializes a new window or reuses an existing one
   wind - an already existing windows instance (experimental)
   width
   height
   fps -  frames per second
   quality - rendering quality :low :mid :high :highest"
  ([wind] 
   (swap! wdg/state assoc :context (wnd/init-window wind)))
  ([width height title]
   (swap! wdg/state assoc :context (wnd/init-window width height title)))
  ([width height title ^Integer fps quality]
   {:pre [(> fps 0) (some #(= % quality) [:low :mid :high :highest])]}
   (swap! wdg/state assoc :context (wnd/init-window width height title fps quality))))
  
(defn create! 
  "Register and show a custom widget.
   Registering a component with the same name will replace the existing component with the new one."
  [^strigui.widget.Widget widget]
  (remove! (:name widget))
  (let [canvas (-> @wdg/state :context :canvas)
        widget (wdg/adjust-dimensions canvas widget)]
    (wdg/register! canvas widget)))

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
  (create! (Button. name text args)))

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

(defn from-map
  "Initializes the window and the widgets from a map"
  [strigui-map]
  (when-let [window-args (:window strigui-map)]
    (apply window! window-args))
  (let [exprs (for [widget-key (filter #(not= % :window) (keys strigui-map))]
                (for [widgets-args (map identity (widget-key strigui-map))]
                  (str "(strigui.core/create! (apply " (namespace widget-key) "/->" (name widget-key) " " widgets-args "))")))]
    (loop [exp (mapcat identity exprs)]
      (when (seq exp)
        (eval (read-string (first exp)))
        (recur (rest exp))))))

(defn from-file
  "Initializes the window and the widgets from a edn file"
  [file-name]
  (when (.exists (io/file file-name))
    (->> (slurp file-name)
         edn/read-string
         from-map)))