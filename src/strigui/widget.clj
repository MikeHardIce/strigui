(ns strigui.widget
  (:require [clojure2d.core :as c2d]
            [clojure.set :as s]))

(defprotocol Widget
  "collection of functions around redrawing widgets, managing the border etc. ..."
  (coord [this canvas] "gets the coordinates of the widget")
  (defaults [this] "attach default values")
  (draw [this canvas] "draw the widget, returns the widget on success"))

(defprotocol Hide
  (hide [this canvas] "to provide a custom implementation for hiding a widget per widget type"))

(def state (atom {:widgets ()
                  :widgets-to-redraw #{}
                  :previous-mouse-position nil
                  :previously-tabbed #{}
                  :context {:canvas nil :window nil}}))

(defn on-border?
  [[x y w h] x0 y0]
  (let [thickness 10
        bottom-start (+ y h)
        right-start (+ x w)]
    (or (and (<= (- x thickness) x0 (+ right-start thickness))
             (or (<= (- y thickness) y0 (+ y thickness))
                 (<= (- bottom-start thickness) y0 (+ bottom-start thickness))))
        (and (<= (- y thickness) y0 (+ bottom-start thickness))
                 (or (<= (- x thickness) x0 (+ x thickness))
                     (<= (- right-start thickness) x0 (+ right-start thickness)))))))

(defn within?
  "Checks wheter the point (x y) is within the given coord
   coord - vector [x-coord y-coord width height]
   x - x-coord of point to check
   y - y-coord of point to check"
  ([coord x y]
   (and (>= (+ x 5) (first coord))
        (>= (+ y 5) (nth coord 1))
        (<= (- x 5) (+ (first coord) (nth coord 2)))
        (<= (- y 5) (+ (nth coord 1) (nth coord 3)))))
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

(defn- draw-border-rec
  ([canvas color strength x y w h no-fill]
   (when (> strength 0)
     (c2d/with-canvas-> canvas
       (c2d/set-color color)
       (c2d/rect (- x strength) (- y strength) (+ w (* 2 strength)) (+ h (* 2 strength)) no-fill))
     (draw-border-rec canvas color (- strength 1) x y w h no-fill))))

(defn- draw-border
  ([box canvas] (draw-border box canvas :black 1))
  ([box canvas color] (draw-border box canvas color 1))
  ([box canvas color strength] (draw-border box canvas color strength false))
  ([box canvas color strength fill]
   (let [[x y w h] (coord box canvas)]
     (draw-border-rec canvas color strength x y w h (not fill)))))

(defn draw-widget-border
  [^strigui.widget.Widget widget canvas]
  (when (-> widget :args :has-border?)
    (cond
      (-> widget :args :resizing?) (draw-border widget canvas :orange 2)
      (-> widget :args :selected?) (draw-border widget canvas :blue 2)
      (-> widget :args :focused?) (draw-border widget canvas :black 2)
      :else (draw-border widget canvas :black 1))))

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
  (let [widgets-can-tab (filter #(-> % :args :can-tab?) widgets)
        prev-tab-widgets (filter #(some (fn [x] (= (:name %) x)) previously-tabbed) widgets-can-tab)
        not-tabbed (s/difference (set widgets-can-tab) (set prev-tab-widgets) (when (seq selected-widget) (set '(selected-widget))))
        to-be-tabbed (if (seq not-tabbed) not-tabbed widgets-can-tab)
        coord-widget (if (seq selected-widget) (coord selected-widget canvas) [0 0])
        dist (map #(merge {:widget %} {:dist (distance-x coord-widget (coord % canvas))}) to-be-tabbed)
        dist (sort-by :dist < dist)
        dist (filter #(> (:dist %) 0) dist)
        new-selected (:widget (first dist))
        new-selected (when (seq new-selected) (assoc-in new-selected [:args :selected?] true))
        widgets (filter #(and (not= (:name %) (:name selected-widget))
                              (not= (:name %) (:name new-selected))) widgets)
        new-widgets (if (seq new-selected) (conj widgets new-selected)
                        widgets)
        new-widgets (if (seq selected-widget) (conj new-widgets (assoc-in selected-widget [:args :selected?] nil)) 
                        new-widgets)]
    (when new-widgets
      {:widgets new-widgets
       :previously-tabbed (when (seq not-tabbed) (s/union previously-tabbed #{(:name new-selected)}))})))

(defmulti hide! (fn [widget _]
                  (cond 
                    (extends? Hide (class widget)) :custom)))

(defmethod hide! :custom
  [widget canvas]
  (hide widget canvas))

(defmethod hide! :default
 [widget canvas] 
  (let [[x y w h] (coord widget canvas)]
    (c2d/with-canvas-> canvas
      (c2d/set-color :white)
      (c2d/rect (- x 5) (- y 5) (+ w 8) (+ h 8)))))

(defn redraw!
  [canvas & widgets]
  (when (seq widgets)
    (let [widget (first widgets)]
      (hide! widget canvas)
      (draw widget canvas)
      (draw-widget-border widget canvas)
      (recur canvas (rest widgets)))))

(defn adjust-dimensions 
  [canvas ^strigui.widget.Widget widget]
  (let [[_ _ w h] (coord widget canvas)
        {{width :width height :height :as args} :args} widget
        width (if (and (number? width) (>= width w)) width w)
        height (if (and (number? height) (>= height h)) height h)
        args (assoc args :width width :height height)]
    (assoc widget :args args)))

(defn register!
  [canvas ^strigui.widget.Widget widget]
  (when (draw widget canvas)
    (draw-widget-border widget canvas)
    (swap! state update :widgets conj widget)))

(defn unregister!
  [canvas ^strigui.widget.Widget widget]
  (when (hide! widget canvas)
    (swap! state update :widgets #(filter (fn [item] (not= item %2)) %1) widget)
    (swap! state update :widgets-to-redraw #(s/difference %1 #{widget}))))

(defn replace!
  [canvas old-widget new-widget]
  (unregister! canvas old-widget)
  (register! canvas new-widget))

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

(defn- handle-widget-resizing
  [^strigui.widget.Widget widget [x y]]
  (when-let [old-position (:previous-mouse-position @state)]
    (let [dx (- x (first old-position))
          dy (- y (second old-position))
          new-width (+ (-> widget :args :width) dx)
          new-height (+ (-> widget :args :height) dy)]
      (update widget :args #(merge % {:width new-width :height new-height})))))

(defn handle-clicked
  [x-pos y-pos]
  (let [context (:context @strigui.widget/state)
        canvas (:canvas context)
        widgets (:widgets @strigui.widget/state)
        widget (first (filter #(within? (coord % canvas) x-pos y-pos) widgets))
        selected (filter #(and (-> % :args :selected?) (not= % widget)) widgets)]
    (when (seq widget)
      (when (not (-> widget :args :resizing?))
        (widget-event :mouse-clicked canvas widget)
        (trigger-custom-event :mouse-clicked widget)
        (replace! canvas widget (assoc-in widget [:args :selected?] true))))
    (when (seq selected)
      (loop [sel selected]
        (when (seq sel)
          (replace! canvas (first sel) (assoc-in (first sel) [:args :selected?] nil))
          (recur (rest sel)))))))

(defn handle-mouse-moved
  []
  (let [context (:context @state)
        canvas (:canvas context)
        window (:window context)
        widgets (:widgets @state)
        [x y] [(c2d/mouse-x window) (c2d/mouse-y window)]
        widget (first (filter #(within? (coord % canvas) x y) (sort-by #(-> % :args :z) widgets)))
        widget-mut (atom widget)]
    (when (seq widget)
      (let [widget-coords (coord widget canvas)
            neighbouring-widgets (set (filter #(and (intersect? widget-coords (coord % canvas))
                                                    (not= widget %)) widgets))
            neighbouring-widgets (sort-by #(-> % :args :z) neighbouring-widgets)]
        (apply redraw! canvas neighbouring-widgets)
        (widget-event :mouse-moved canvas widget)
        (trigger-custom-event :mouse-moved widget))
        (swap! widget-mut assoc-in [:args :resizing?] (and (-> widget :args :can-resize) (on-border? (coord widget canvas) x y)))
      ;; handle widget focusing
      (when (not (-> widget :args :focused?))
        (swap! widget-mut assoc-in [:args :focused?] true)
        (widget-event :widget-focus-in canvas widget)
        (trigger-custom-event :widget-focus-in widget))
      (when (c2d/mouse-pressed? window)
        (cond 
          (and (-> widget :args :can-resize) (on-border? (coord widget canvas) x y)) (swap! widget-mut merge (handle-widget-resizing widget [x y]))
          (-> widget :args :can-move?) (do
                                         (swap! widget-mut merge (handle-widget-dragging widget [x y]))
                                         (widget-event :widget-moved canvas widget)
                                         (trigger-custom-event :widget-moved widget)))))
    ;; reset all previously focused widgets
    (loop [prev-widgets (filter #(and (-> % :args :focused?) (not= widget %)) widgets)]
      (when (seq prev-widgets)
        (let [prev-widget (first prev-widgets)
              prev-new-widget (assoc-in prev-widget [:args :focused?] nil)
              prev-new-widget (assoc-in prev-new-widget [:args :resizing?] nil)]
        (replace! canvas prev-widget prev-new-widget)
        (widget-event :widget-focus-out canvas prev-widget)
        (trigger-custom-event :widget-focus-out prev-widget)
        (recur (rest prev-widgets)))))
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
  (let [context (:context @strigui.widget/state)]
    (handle-clicked (c2d/mouse-x (:window context)) (c2d/mouse-y (:window context))))
  state)

(defmethod c2d/mouse-event ["main-window" :mouse-released] [event state]
  (widget-global-event :mouse-released (:canvas (:context @strigui.widget/state)))
  (swap! strigui.widget/state assoc :previous-mouse-position nil)
  state)

(defmethod c2d/key-event ["main-window" :key-pressed] [event state]
  (let [char (c2d/key-char event)
        code (c2d/key-code event)
        canvas (:canvas (:context @strigui.widget/state))
        state (atom (select-keys @strigui.widget/state [:widgets :previously-tabbed]))
        widget (first (filter #(-> % :args :selected?) (:widgets @state)))]
    (when (= code :tab)
      (when-let [new-widget-map (next-tabbed-widget-map canvas (:widgets @state) (:previously-tabbed @state) widget)]
        (swap! state merge new-widget-map)
        (swap! strigui.widget/state merge @state)
        (apply redraw! canvas (:widgets new-widget-map))))
    (widget-global-event :key-pressed canvas char code)
    (when-let [widget (first (filter #(-> % :args :selected?) (:widgets @state)))]
      (widget-event :key-pressed canvas widget char code)
      (trigger-custom-event :key-pressed widget code)))
  state)