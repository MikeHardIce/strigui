(ns strigui.core
  (:require [strigui.button :as btn]
            [strigui.label :as lbl]
            [strigui.window :as wnd]))

(defn window [width height]
  (wnd/create-window width height))

(defn button
  "canvas - clojure2d canvas
   text - text displayed inside the button
   args - list of:
     x - x coordinate of top left corner
     y - y coordinate of top left corner
     color - vector consisting of [background-color font-color]
     min-width - the minimum width"
  [name text args]
  (btn/button wnd/canvas name text args))

(defn label
  "canvas - clojure2d canvas
   text - text displayed inside the button
   args - list of:
     x - x coordinate of top left corner
     y - y coordinate of top left corner
     color - vector consisting of [font-color]
     align - either :right :center :left"
  [name text args]
  (lbl/label wnd/canvas name text args))

(defn info [text]
  (wnd/display-info wnd/canvas text))
