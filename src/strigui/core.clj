(ns strigui.core
  (:require [strigui.button :as btn]
            [strigui.label :as lbl]
            [strigui.input :as inp]
            [strigui.stacks :as st]
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
  (st/stacks @wnd/context name item-list args))

(defn info 
  [text]
  (wnd/display-info @wnd/context text))
