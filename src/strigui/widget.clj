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

(def ^:private previous-mouse-position (atom nil))

(def widget-props (atom nil))

(defn selected?
  [wdg]
  (= wdg (:selected @widget-props)))

(defn focused?
  [wdg]
  (= wdg (:selected @widget-props)))

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
    (swap! widgets conj widget)
    (when (-> widget :args :selected?)
      (swap! widget-props assoc :selected widget)
      (redraw widget canvas))))

(defn unregister
  [canvas ^strigui.widget.Widget widget]
  (when (hide widget canvas)
    (swap! widgets #(filter (fn [item] (not= item %2)) %1) widget)
    (swap! widgets-to-redraw #(s/difference %1 #{widget}))
    (when (= (:selected @widget-props) widget)
      (swap! widget-props assoc :selected nil))))

(defn trigger-custom-event 
  [action ^strigui.widget.Widget widget & args]
  (when-let [event-fn (-> widget :events action)]
    (apply event-fn widget args)))

(defmulti widget-event 
  (fn [action canvas widget & args] 
    [(class widget) action]))

(defmethod widget-event :default [action canvas widget & args] nil)

(defmulti widget-global-event
  (fn [action canvas & args] action))

(defmethod widget-global-event :default [_ canvas & args] nil)

(defn- handle-widget-dragging 
  [canvas ^strigui.widget.Widget widget [x y]]
  (when-let [old-position @previous-mouse-position]
    (let [dx (- x (first old-position))
          dy (- y (second old-position))
          new-x (+ (-> widget :args :x) dx)
          new-y (+ (-> widget :args :y) dy)]
      (unregister canvas widget)
      (register canvas (update widget :args #(merge % {:x new-x :y new-y}))))))

(defn handle-mouse-moved 
  []
  (let [context @wnd/context
        canvas (:canvas context)
        window (:window context)
        widget (first (filter #(wnd/within? (coord % canvas) (c2d/mouse-x window) (c2d/mouse-y window)) (sort-by #(-> % :args :z) @widgets)))
        redraw-widgets (sort-by #(-> % :args :z) @widgets-to-redraw)]
    (let [redrawn-buttons (map #(redraw % canvas) redraw-widgets)]
      (swap! widgets-to-redraw #(s/difference %1 (set %2))  redrawn-buttons))
      (if (seq widget)
        (when (not (focused? widget))
          (swap! widget-props assoc :focused widget)
          (widget-event :widget-focus-in canvas widget)
          (trigger-custom-event :widget-focus-in widget))
        (do
          (widget-event :widget-focus-out canvas (:focused @widget-props))
          (trigger-custom-event :widget-focus-out (:focused @widget-props))
          (when (not= (:focused @widget-props) (:selected @widget-props))
            (swap! widget-props assoc :focused nil))))
    (when (seq widget)
      (when (and (c2d/mouse-pressed? window) (-> widget :args :can-move?))
        (handle-widget-dragging canvas widget [(c2d/mouse-x window) (c2d/mouse-y window)])
        (widget-event :widget-moved canvas widget)
        (trigger-custom-event :widget-moved widget))
      (let [widget-coords (coord widget canvas)
            neighbouring-widgets (set (filter #(and (intersect? widget-coords (coord % canvas))
                                                    (not= widget %)) @widgets))]
        (swap! widgets-to-redraw s/union neighbouring-widgets))
      (widget-event :mouse-moved canvas widget)
      (trigger-custom-event :mouse-moved widget))))

(defmethod c2d/mouse-event ["main-window" :mouse-dragged] [event state]
  (handle-mouse-moved)
  (let [context @wnd/context
        window (:window context)]
    (reset! previous-mouse-position [(c2d/mouse-x window) (c2d/mouse-y window)]))
  state)

(defmethod c2d/mouse-event ["main-window" :mouse-moved] [event state]
  (handle-mouse-moved)
  state)
  
;; TODO: maybe its not necessary to go to @wnd/context directly
(defmethod c2d/mouse-event ["main-window" :mouse-pressed] [event state]
  (let [context @wnd/context
        canvas (:canvas context)
        window (:window context)
        widget (first (filter #(wnd/within? (coord % canvas) (c2d/mouse-x window) (c2d/mouse-y window)) @widgets))]
    (if (seq widget)
      (do
        (widget-event :mouse-clicked canvas widget)
        (trigger-custom-event :mouse-clicked widget))
      (when (:selected @widget-props)
        (redraw (:selected @widget-props) canvas)))
    (swap! widget-props assoc :selected widget))
  state)

(defmethod c2d/mouse-event ["main-window" :mouse-released] [event state]
  (widget-global-event :mouse-released (:canvas @wnd/context))
  (reset! previous-mouse-position nil)
  state)

(defmethod c2d/key-event ["main-window" :key-pressed] [event state]
  (let [char (c2d/key-char event)
        code (c2d/key-code event)
        canvas (:canvas @wnd/context)
        widget (:selected @widget-props)]
    (widget-global-event :key-pressed canvas char code)
    (println "class: " (class widget))
    (println "widget: " widget)
    (when (seq widget)
      (widget-event :key-pressed canvas widget char code)
      (trigger-custom-event :key-pressed widget code)))
  state)