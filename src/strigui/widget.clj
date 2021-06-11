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

(defn within?
  "Checks wheter the point (x y) is within the given coord
   coord - vector [x-coord y-coord width height]
   x - x-coord of point to check
   y - y-coord of point to check"
  ([coord x y]
  (and (>= x (first coord))
       (>= y (nth coord 1))
       (<= x (+ (first coord) (nth coord 2)))
       (<= y (+ (nth coord 1) (nth coord 3)))))
  ([coord1 coord2]
   (let [[x2 y2 w2 h2] coord2
         x2+w2 (+ x2 w2)
         y2+h2 (+ y2 h2)]
     (or (within? coord1 x2 y2)
         (within? coord1 x2+w2 y2+h2)
         (within? coord1 x2 y2+h2)
         (within? coord1 x2+w2 y2)))))

(defn intersect?
  [coord1 coord2]
  (or (within? coord1 coord2) 
      (within? coord2 coord1)))

(defn hide 
  [^strigui.widget.Widget widget canvas]
  (let [[x y w h] (coord widget canvas)]
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
    [(class widget) action]))

(defmethod widget-event :default [action canvas widget] nil)

(defmulti widget-global-event
  (fn [action canvas & args] action))

(defmethod widget-global-event :default [_ canvas & args] nil)

(defmethod c2d/mouse-event ["main-window" :mouse-moved] [event state]
  (let [context @wnd/context
        canvas (:canvas context)
        window (:window context)
        widget (first (filter #(wnd/within? (coord % canvas) (c2d/mouse-x window) (c2d/mouse-y window)) @widgets))
        widgets (sort-by #(-> % :args :z) @widgets-to-redraw)]
    (println (map #(str " " (:name %)) widgets))
      (let [redrawn-buttons (map #(redraw % canvas) widgets)]
        (swap! widgets-to-redraw #(s/difference %1 (set %2))  redrawn-buttons))
    (when (seq widget)  ;; TODO
      (let [neighbouring-widgets (set (filter #(intersect? (coord widget canvas) (coord % canvas)) @widgets))]
        (swap! widgets-to-redraw s/union neighbouring-widgets))
      (widget-event :mouse-moved canvas widget)
      (trigger-custom-event :mouse-moved widget)))
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
        canvas (:canvas @wnd/context)]
    (widget-global-event :key-pressed canvas char code)
    (loop [widgets @widgets]
      (when (seq widgets)
        (trigger-custom-event :key-pressed (first widgets) code)
        (recur (rest widgets)))))
  state)