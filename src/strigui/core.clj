(ns strigui.core
  (:require [strigui.button :as btn]
            [strigui.label :as lbl]
            [strigui.input :as inp]
            [strigui.stacks :as st]
            [strigui.window :as wnd]))

(def ^:private widgets (atom ()))

(defn- register 
  [^strigui.widget.Widget widget canvas]
  (when (draw widget canvas)
      (swap! widgets conj widget)))

(defn- unregister
  [^strigui.widget.Widget widget canvas]
  (when (hide widget canvas)
      (swap! widgets #(filter (fn [item] (not= item %2))) widget)))

(defn find-by-name 
  [name]
  (first (filter #(= (widget-name %) name) @widgets)))

(defn remove! 
  [name]
  (when-let [box-to-remove (find-by-name name)]
    (unregister box-to-remove (:canvas @wnd/context))))

(defn update! 
  [name key value]
  (when-let [w (find-by-name name)
             widget (assoc widget key value)]
            (unregister w (:canvas @wnd/context)
            (register widget (:canvas @wnd/context)))))

(defn window [width height]
  (wnd/create-window width height))

(defn button
  "name - name of the element
  text - text displayed inside the button
  args - map of:
     x - x coordinate of top left corner
     y - y coordinate of top left corner
     color - vector consisting of [background-color font-color]
     min-width - the minimum width"
  [name text args]
  (register (btn/button name text args)))

(defn label
   "name - name of the element
   text - text displayed inside the button
   args - map of:
     x - x coordinate of top left corner
     y - y coordinate of top left corner
     color - vector consisting of [font-color]
     align - vector consisting of either :right :center :left
     font-style - vector consisting of either :bold :italic :italic-bold
     font-size - number"
  [name text args]
  (register (lbl/create name text args)))

(defn input
  "name - name of the element
  text - text displayed inside the button
  args - map of:
    x - x coordinate of top left corner
    y - y coordinate of top left corner
    color - vector consisting of [background-color font-color]
    min-width - the minimum width"
  [name text args]
  (register (inp/input name text args)))

(defn stacks
  "name - name of the elemnt
  item-list - list consisting of the number of
              items on each stack for example:
              (5 4 5) for 3 stacks with 5 on the 1st,
                4 on the 2nd and 5 and the last stack
  args - map of:
    x - x coordinate of top left corner
    y - y coordinate of top left corner"
  [name item-list args]
  (register (st/create name item-list args)))

(defn info 
  [text]
  (wnd/display-info @wnd/context text))