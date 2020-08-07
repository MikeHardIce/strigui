(ns strigui.core
  (:require [strigui.button :as btn]
            [strigui.window :as wnd]))

(defn window [width height]
  (wnd/create-window width height))

(defn button
  "canvas - clojure2d canvas
   text - text displayed inside the button
   x - x coordinate of top left corner
   y - y coordinate of top left corner
   color - vector consisting of [background-color font-color]
   min-width - the minimum width"
  [name text {:keys [x y color min-width]}]
  (btn/button wnd/canvas name text {:keys [x y color min-width]}))

(defn info [text]
  (wnd/display-info wnd/canvas text))
