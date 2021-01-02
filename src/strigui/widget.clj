(ns strigui.widget
    (:require [clojure2d.core :as c2d]
              [clojure.set :as s]
              [strigui.window :as wnd]))

(defprotocol Widget 
    "collection of functions around redrawing widgets, managing the border etc. ..."
    (coord [this canvas] "gets the coordinates of the widget")
    (value [this] "the current text of the widget")
    (args [this] "the current args of the widget")
    (widget-name [this] "name of the widget")
    (draw [this canvas] "draw the widget, returns the widget on success")
    (redraw [this canvas] "redraw the widget"))

(def widgets (atom ()))

(def ^:private widgets-to-redraw (atom #{}))

(defn hide 
  [^strigui.widget.Widget widget canvas]
  (let [[x y w h] (coord widget canvas)]
    (println (str "x: " x " y: " y " w: " w " h: " h))
    (c2d/with-canvas-> canvas
      (c2d/set-color :white)
      (c2d/rect (- x 5) (- y 5) (+ w 8) (+ h 8)))))

(defn register 
  [canvas ^strigui.widget.Widget widget]
  (when (draw widget canvas)
    (swap! widgets conj widget)))

(defn unregister
  [canvas ^strigui.widget.Widget widget]
  (when (hide widget canvas)
    (swap! widgets #(filter (fn [item] (not= item %2)) %1) widget)
    (swap! widgets-to-redraw #(s/difference %1 #{widget}))))

(defn trigger-custom-event 
  [action ^strigui.widget.Widget widget & args]
  (when-let [event-fn (-> widget :events action)]
    (apply event-fn widget args)))

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
        btn (first (filter #(wnd/within? (coord % canvas) (c2d/mouse-x window) (c2d/mouse-y window)) @widgets))
        btns @widgets-to-redraw]
    (wnd/display-info context (str (c2d/mouse-pos window) " " @widgets-to-redraw))
    (if (empty? btn)
      (let [redrawn-buttons (map #(redraw % canvas) btns)]
        (swap! widgets-to-redraw #(s/difference %1 (set %2))  redrawn-buttons))
      (do
        (widget-event :mouse-moved canvas btn)
        (swap! widgets-to-redraw  #(conj %1 %2) btn)
        (trigger-custom-event :mouse-moved btn))))
  state)
  
;; TODO: maybe its not necessary to go to @wnd/context directly
(defmethod c2d/mouse-event ["main-window" :mouse-pressed] [event state]
  (let [context @wnd/context
        canvas (:canvas context)
        window (:window context)
        btn (first (filter #(wnd/within? (coord % canvas) (c2d/mouse-x window) (c2d/mouse-y window)) @widgets))]
    (if (not-empty btn)
      (do 
        (widget-event :mouse-clicked canvas btn)
        (trigger-custom-event :mouse-clicked btn))
      (widget-global-event :mouse-pressed-on-empty-space canvas)))
  state)

(defmethod c2d/mouse-event ["main-window" :mouse-released] [event state]
  (widget-global-event :mouse-released (:canvas @wnd/context))
  state)

(defmethod c2d/key-event ["main-window" :key-pressed] [event state]
  (let [char (c2d/key-char event)
        code (c2d/key-code event)
        window (:window @wnd/context)
        canvas (:canvas @wnd/context)]
    (widget-global-event :key-pressed canvas char code)
    (when-let [wdg (first (filter #(wnd/within? (coord % canvas) (c2d/mouse-x window) (c2d/mouse-y window)) @widgets))]
      (trigger-custom-event :key-pressed wdg code)))
  state)