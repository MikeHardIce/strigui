(ns strigui.widget
    (:require [clojure2d.core :as c2d]
              [clojure.set :as s]))

(defprotocol Widget 
    "collection of functions around redrawing widgets, managing the border etc. ..."
    (coord [this canvas] "gets the coordinates of the widget")
    (value [this] "the current text of the widget")
    (args [this] "the current args of the widget")
    (widget-name [this] "name of the widget")
    (draw [this canvas] "draw the widget, returns the widget on success"))

(def state (atom {:widgets ()
                  :widgets-to-redraw #{}
                  :previous-mouse-position nil
                  :previously-tabbed #{}
                  :context {:canvas nil :window nil}}))

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

(defn distance-x
 "Manhatten distance that is sqashed on the x-axis,
  meaning widgets on similar y positions are treated as
  being closer together." 
  ([canvas ^strigui.widget.Widget widget1 ^strigui.widget.Widget widget2]
  (let [coord1 (coord widget1 canvas)
        coord2 (coord widget2 canvas)]
    (distance-x coord1 coord2)))
  ([[x1 y1] [x2 y2]]
   (+ (* 0.3 (Math/abs (- x1 x2))) (Math/abs (- y1 y2)))))

(defn next-tabbed-widget-map
  [canvas widgets previously-tabbed ^strigui.widget.Widget selected-widget]
  (let [widgets (filter #(-> % :args :can-tab?) widgets)
        not-tabbed (s/difference (set widgets) previously-tabbed (when (seq selected-widget) (set '(selected-widget))))
        to-be-tabbed (if (seq not-tabbed) not-tabbed widgets)
        coord-widget (if (seq selected-widget) (coord selected-widget canvas) [0 0])
        dist (map #(merge {:widget %} {:dist (distance-x coord-widget (coord % canvas))}) to-be-tabbed)
        dist (sort-by :dist < dist)
        new-selected (:widget (first dist))
        new-selected (assoc-in new-selected [:args :selected?] true)]
    {:widgets (conj (filter #(not= % selected-widget) widgets) new-selected)
     :previously-tabbed (if (seq not-tabbed) (s/union previously-tabbed #{new-selected}) #{new-selected})}))

(defn hide 
  [^strigui.widget.Widget widget canvas]
  (let [[x y w h] (coord widget canvas)]
    (c2d/with-canvas-> canvas
      (c2d/set-color :white)
      (c2d/rect (- x 5) (- y 5) (+ w 8) (+ h 8)))))

(defn redraw!
  [canvas & widgets]
  (when (seq widgets)
    (hide (first widgets) canvas)
    (draw (first widgets) canvas)
    (recur canvas (rest widgets))))

(defn register 
  [canvas ^strigui.widget.Widget widget]
  (when (draw widget canvas)
    (swap! state update :widgets conj widget)))

(defn unregister
  [canvas ^strigui.widget.Widget widget]
  (when (hide widget canvas)
    (swap! state update :widgets #(filter (fn [item] (not= item %2)) %1) widget)
    (swap! state update :widgets-to-redraw #(s/difference %1 #{widget}))))

(defn replace! 
  [canvas old-widget new-widget]
  (unregister canvas old-widget)
  (register canvas new-widget))

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
  [^strigui.widget.Widget widget [x y]]
  (when-let [old-position (:previous-mouse-position @state)]
    (let [dx (- x (first old-position))
          dy (- y (second old-position))
          new-x (+ (-> widget :args :x) dx)
          new-y (+ (-> widget :args :y) dy)]
      (update widget :args #(merge % {:x new-x :y new-y})))))

(defn handle-mouse-moved 
  []
  (let [context (:context @state)
        canvas (:canvas context)
        window (:window context)
        widgets (:widgets @state)
        widget (first (filter #(within? (coord % canvas) (c2d/mouse-x window) (c2d/mouse-y window)) (sort-by #(-> % :args :z) widgets)))
        widget-mut (atom widget)]
    (when (seq widget)
      (let [widget-coords (coord widget canvas)
            neighbouring-widgets (set (filter #(and (intersect? widget-coords (coord % canvas))
                                                    (not= widget %)) widgets))
            neighbouring-widgets (sort-by #(-> % :args :z) neighbouring-widgets)]
        (apply redraw! canvas neighbouring-widgets)
        (widget-event :mouse-moved canvas widget)
        (trigger-custom-event :mouse-moved widget))
      ;; handle widget focusing
      (when (not (-> widget :args :focused?))
        ;;(replace! canvas @widget (swap! widget assoc-in [:args :focused?] true))
        (swap! widget-mut assoc-in [:args :focused?] true)
        (widget-event :widget-focus-in canvas widget)
        (trigger-custom-event :widget-focus-in widget))
      ;; handle widget dragging
      (when (and (c2d/mouse-pressed? window) (-> widget :args :can-move?))
        (swap! widget-mut merge (handle-widget-dragging widget [(c2d/mouse-x window) (c2d/mouse-y window)]))
        ;;(handle-widget-dragging canvas @widget [(c2d/mouse-x window) (c2d/mouse-y window)])
        (widget-event :widget-moved canvas widget)
        (trigger-custom-event :widget-moved widget)))
    ;; reset all previously focused widgets
    (loop [prev-widgets (filter #(and (-> % :args :focused?) (not= widget %)) widgets)]
      (when (seq prev-widgets)
        (replace! canvas (first prev-widgets) (assoc-in (first prev-widgets) [:args :focused?] nil))
        (widget-event :widget-focus-out canvas (first prev-widgets))
        (trigger-custom-event :widget-focus-out (first prev-widgets))
        (recur (rest prev-widgets))))
    (when (not= @widget-mut widget)
      (replace! canvas widget @widget-mut))))

(defmethod c2d/mouse-event ["main-window" :mouse-dragged] [event state]
  (handle-mouse-moved)
  (let [context (:context @strigui.widget/state)
        window (:window context)]
    (swap! strigui.widget/state assoc :previous-mouse-position [(c2d/mouse-x window) (c2d/mouse-y window)]))
  state)

(defmethod c2d/mouse-event ["main-window" :mouse-moved] [event state]
  (handle-mouse-moved)
  state)
  
(defmethod c2d/mouse-event ["main-window" :mouse-pressed] [event state]
  (let [context (:context @strigui.widget/state)
        canvas (:canvas context)
        window (:window context)
        widgets (:widgets @strigui.widget/state)
        widget (first (filter #(within? (coord % canvas) (c2d/mouse-x window) (c2d/mouse-y window)) widgets))
        selected (filter #(and (-> % :args :selected?) (not= % widget)) widgets)]
    (when (seq widget)
      (widget-event :mouse-clicked canvas widget)
      (trigger-custom-event :mouse-clicked widget)
      (replace! canvas widget (assoc-in widget [:args :selected?] true)))
    (when (seq selected)
      (loop [sel selected]
        (when (seq sel)
          (replace! canvas (first sel) (assoc-in (first sel) [:args :selected?] nil))
          (recur (rest sel))))))
  state)

(defmethod c2d/mouse-event ["main-window" :mouse-released] [event state]
  (widget-global-event :mouse-released (:canvas (:context @strigui.widget/state)))
  (swap! strigui.widget/state assoc :previous-mouse-position nil)
  state)

;; TODO bug when tabbing
(defmethod c2d/key-event ["main-window" :key-pressed] [event state]
  (let [char (c2d/key-char event)
        code (c2d/key-code event)
        canvas (:canvas (:context @strigui.widget/state))
        widgets (:widgets @strigui.widget/state)
        widget (first (filter #(-> % :args :selected?) widgets))
        prev-tabbed (:previously-tabbed @strigui.widget/state)]
    (when (= code :tab)
      (let [new-widget-map (next-tabbed-widget-map canvas widgets prev-tabbed widget)]
        (swap! strigui.widget/state merge new-widget-map)
        (apply redraw! canvas (:previously-tabbed new-widget-map))))
    (widget-global-event :key-pressed canvas char code)
    (when (seq widget)
      (widget-event :key-pressed canvas widget char code)
      (trigger-custom-event :key-pressed widget code)))
  state)