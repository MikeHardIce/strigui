(ns strigui.core
  (:require [strigui.button :as btn]
            [strigui.label :as lbl]
            [strigui.input :as inp]
            [strigui.stacks :as st]
            [strigui.window :as wnd]
            [strigui.widget :as wdg]))

(defn find-by-name 
  [name]
  (first (filter #(= (wdg/widget-name %) name) @wdg/widgets)))

(defn remove! 
  [name]
  (when-let [box-to-remove (find-by-name name)]
    (wdg/unregister (:canvas @wnd/context) box-to-remove)))

(defn update! 
"name - name of the widget
 key - either single key or vector of keys
 value - the new property value"
  [name key value]
  (when-let [w (find-by-name name)]
    (wdg/unregister (:canvas @wnd/context) w )
      (let [keys (if (seqable? key) key (vector key))]
        (wdg/register (:canvas @wnd/context) (assoc-in w keys value)))))

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
  (wdg/register (:canvas @wnd/context) (btn/button (:canvas @wnd/context) name text args)))

(defn label
   "name - name of the element
   text - text displayed inside the button
   args - map of:
     x - x coordinate of top left corner
     y - y coordinate of top left corner
     color - vector consisting of [font-color]
     font-style - vector consisting of either :bold :italic :italic-bold
     font-size - number"
  [name text args]
  (wdg/register (:canvas @wnd/context) (lbl/create (:canvas @wnd/context) name text args)))

(defn input
  "name - name of the element
  text - text displayed inside the button
  args - map of:
    x - x coordinate of top left corner
    y - y coordinate of top left corner
    color - vector consisting of [background-color font-color]
    min-width - the minimum width"
  [name text args]
  (wdg/register (:canvas @wnd/context) (inp/input (:canvas @wnd/context) name text args)))

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
  (wdg/register (:canvas @wnd/context) (st/create (:canvas @wnd/context) name item-list args)))

(defn info 
  [text]
  (wnd/display-info @wnd/context text))