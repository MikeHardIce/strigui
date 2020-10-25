(ns strigui.widget
    (:require [clojure2d.core :as c2d]
              [clojure.set :as s]
              [strigui.window :as wnd]))

(defprotocol Widget 
    "collection of functions around redrawing widgets, managing the border etc. ..."
    (coord [this] "gets the coordinates of the widget")
    (value [this] "the current text of the widget")
    (args [this] "the current args of the widget")
    (widget-name [this] "name of the widget")
    (draw [this canvas] "draw the widget, returns the widget on success")
    (redraw [this canvas] "redraw the widget")
    (hide [this canvas] "removes the widget from the canvas"))

(def widgets (atom ()))

(def ^:private widgets-to-redraw (atom #{}))

(defn register 
  [canvas ^strigui.widget.Widget widget]
  (when (draw widget canvas)
    (swap! widgets conj widget)))

(defn unregister
  [canvas ^strigui.widget.Widget widget]
  (when (hide widget canvas)
    (swap! widgets #(filter (fn [item] (not= item %2))) widget)
    (swap! widgets-to-redraw #(s/difference %1 #{widget}))))

(defmulti widget-event 
  (fn [action canvas widget] 
    (println (str "dispatch: " [(class widget) action]))
    [(class widget) action]))

(defmethod widget-event :default [action canvas widget] (println (str "default " action " " (class widget))))

(defmulti widget-global-event
  (fn [action canvas & args] action))

(defmethod widget-global-event :default [_ canvas & args] nil)

(defmethod c2d/mouse-event ["main-window" :mouse-moved] [event state]
  (let [context @wnd/context
        canvas (:canvas context)
        window (:window context)
        btn-hits (first (filter #(wnd/within? (coord %) (c2d/mouse-x window) (c2d/mouse-y window)) @widgets))
        btns @widgets-to-redraw]
    (wnd/display-info context (str (c2d/mouse-pos window) " " @widgets-to-redraw))
    (if (empty? btn-hits)
      (let [redrawn-buttons (map #(redraw % canvas) btns)]
        (swap! widgets-to-redraw #(s/difference %1 (set %2))  redrawn-buttons))
      (do 
        (println "bla")
        (widget-event :mouse-moved canvas btn-hits)
        (swap! widgets-to-redraw  #(conj %1 %2) btn-hits))))
  state)
  
;; TODO: maybe its not necessary to go to @wnd/context directly
(defmethod c2d/mouse-event ["main-window" :mouse-pressed] [event state]
  (let [context @wnd/context
        canvas (:canvas context)
        window (:window context)
        btn (first (filter #(wnd/within? (coord %) (c2d/mouse-x window) (c2d/mouse-y window)) @widgets))]
    (if (not-empty btn)
      (widget-event :mouse-clicked canvas btn)
      (widget-global-event :mouse-pressed-on-empty-space canvas)))
  state)

(defmethod c2d/mouse-event ["main-window" :mouse-released] [event state]
  (widget-global-event :mouse-released (:canvas @wnd/context))
  state)

(defmethod c2d/key-event ["main-window" :key-pressed] [event state]
  (let [char (c2d/key-char event)
        code (c2d/key-code event)]
    (widget-global-event :key-pressed (:canvas @wnd/context) char code))
  state)