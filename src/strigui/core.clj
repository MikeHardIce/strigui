(ns strigui.core
  (:require [strigui.button :as btn]
            [strigui.label :as lbl]
            [strigui.input :as inp]
            [strigui.window :as wnd]))

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
  (btn/button @wnd/context name text args))

(defn label
   "name - name of the element
   text - text displayed inside the button
   args - map of:
     x - x coordinate of top left corner
     y - y coordinate of top left corner
     color - vector consisting of [font-color]
     align - either :right :center :left"
  [name text args]
  (lbl/label @wnd/context name text args))

(defn input
  "name - name of the element
  text - text displayed inside the button
  args - map of:
    x - x coordinate of top left corner
    y - y coordinate of top left corner
    color - vector consisting of [background-color font-color]
    min-width - the minimum width"
  [name text args]
  (inp/input @wnd/context name text args))

(defn info 
  [text]
  (wnd/display-info @wnd/context text))
