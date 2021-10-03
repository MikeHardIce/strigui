(ns strigui.widget
  (:require [clojure2d.core :as c2d]
            [clojure.set :as s]
            [clojure.string]))

(def ^:private border-thickness 10)

(defprotocol Widget
  "collection of functions around redrawing widgets, managing the border etc. ..."
  (coord [this canvas] "gets the coordinates of the widget")
  (defaults [this] "attach default values")
  (draw [this canvas] "draw the widget, returns the widget on success"))

(defmacro def-action
  [name default-fn]
  (let [symb-big (symbol (clojure.string/capitalize name))
        symb-small (symbol (clojure.string/lower-case name))
        symb-mult (symbol (clojure.string/lower-case (str name "!")))]
    `(do (defprotocol ~symb-big
             (~symb-small [this# canvas#] ""))
          (defmulti ~symb-mult (fn [widget# _#]
                                  (cond
                                    (extends? ~symb-big (class widget#)) :custom)))
         (defmethod ~symb-mult :custom 
           [this# canvas#]
           (~symb-small this# canvas#))
         (defmethod ~symb-mult :default
           [this# canvas#]
           (~default-fn this# canvas#)))))

(def state (atom {:widgets ()
                  :widgets-to-redraw #{}
                  :previous-mouse-position nil
                  :previously-tabbed #{}
                  :previously-selected nil
                  :context {:canvas nil :window nil}}))

(defn on-border?
  [[x y w h] x0 y0]
  (let [bottom-start (+ y h)
        right-start (+ x w)]
    (or (and (<= (- x border-thickness) x0 (+ right-start border-thickness))
             (or (<= (- y border-thickness) y0 (+ y border-thickness))
                 (<= (- bottom-start border-thickness) y0 (+ bottom-start border-thickness))))
        (and (<= (- y border-thickness) y0 (+ bottom-start border-thickness))
                 (or (<= (- x border-thickness) x0 (+ x border-thickness))
                     (<= (- right-start border-thickness) x0 (+ right-start border-thickness)))))))

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

(def-action "hide" (fn [widget canvas]
                     (let [[x y w h] (coord widget canvas)]
                       (c2d/with-canvas-> canvas
                         (c2d/set-color :white)
                         (c2d/rect (- x 5) (- y 5) (+ w 8) (+ h 8))))))

(def-action "draw-resizing" (fn [widget canvas]
                              (draw-border widget canvas :orange 2)))

(def-action "draw-selected" (fn [widget canvas]
                              (draw-border widget canvas :blue 2)))

(def-action "draw-focused" (fn [widget canvas]
                              (draw-border widget canvas :black 2)))

(defn draw-widget-border
  [^strigui.widget.Widget widget canvas]
  (when (-> widget :args :has-border?)
    (cond
      (-> widget :args :resizing?) (draw-resizing! widget canvas)
      (-> widget :args :selected?) (draw-selected! widget canvas)
      (-> widget :args :focused?) (draw-focused! widget canvas)
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

(defn redraw!
  [canvas & widgets]
  (when (seq widgets)
    (let [widget (first widgets)]
      (hide! widget canvas)
      (draw widget canvas)
      (draw-widget-border widget canvas)
      (recur canvas (rest widgets)))))

(defn neighbouring-widgets
  "get all neighbours of the given widget. Neighbours are sorted by their :z coordinate
   in ascending order"
  [canvas ^strigui.widget.Widget widget widgets]
  (let [widget-coords (coord widget canvas)
              neighbours (set (filter #(and (intersect? widget-coords (coord % canvas))
                                                      ) widgets)) ;;(not= widget %)  
              neighbours (sort-by #(-> % :args :z) neighbours)]
    neighbours))

(defn adjust-dimensions 
  [canvas ^strigui.widget.Widget widget]
  (let [[_ _ w h] (coord widget canvas)
        set-default (fn [value] (if (number? value) value 0))
        {{width :width height :height x :x y :y z :z :as args} :args} widget
        width (if (and (number? width) (>= width w)) width w)
        height (if (and (number? height) (>= height h)) height h)
        [x y z] (map set-default [x y z])
        args (assoc args :width width :height height :x x :y y :z z)]
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
    (let [[dx dy] [(- x (first old-position)) (- y (second old-position))]
          [x0 y0 w h] [(-> widget :args :x) (-> widget :args :y) (-> widget :args :width) (-> widget :args :height)]
          position-f (fn [m0 m] (<= (- m0 border-thickness) m (+ m0 border-thickness)))
          [left? top? right? bottom?] [(position-f x0 x) (position-f y0 y) (position-f (+ x0 w) x) (position-f (+ y0 h) y)]
          [x1 y1 w1 h1] (cond 
                          left? [(+ x0 dx) y0 (- w dx) h]
                          top? [x0 (+ y0 dy) w (- h dy)]
                          right? [x0 y0 (+ w dx) h]
                          bottom? [x0 y0 w (+ h dy)]
                          :else [x0 y0 w h])]
      (update widget :args #(merge % {:width w1 :height h1
                                      :x x1 :y y1})))))

(defn handle-clicked
  [x-pos y-pos]
  (let [context (:context @strigui.widget/state)
        canvas (:canvas context)
        widgets (:widgets @strigui.widget/state)
        widget (first (filter #(within? (coord % canvas) x-pos y-pos) widgets))
        selected (filter #(and (-> % :args :selected?) (not= % widget)) widgets)
        selected (sort-by #(-> % :args :z) selected)]
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
  [action]
  ;; this functions needs a rework, i think it can be optimized a lot.
  ;; need to find a better strategy to redraw widgets
  (let [context (:context @state)
        canvas (:canvas context)
        window (:window context)
        widgets (:widgets @state)
        [x y] [(c2d/mouse-x window) (c2d/mouse-y window)]
        widget (first (reverse (sort-by #(-> % :args :z) (filter #(within? (coord % canvas) x y) widgets))))
        widget-mut (atom widget)]
    (when (seq widget)
      (let [at-border (on-border? (coord widget canvas) x y)
            was-focused (and (= (:name widget) (-> @state :previously-selected :name)) (not= :mouse-dragged action))]
          (let [neighbours (neighbouring-widgets canvas widget widgets)
                neighbours (if was-focused 
                             (filter #(> (-> % :args :z) (-> widget :args :z)) neighbours)
                             neighbours)]
            (apply redraw! canvas neighbours)
            (swap! state assoc :previously-selected widget))
        (widget-event :mouse-moved canvas widget)
        (trigger-custom-event :mouse-moved widget)
        (swap! widget-mut assoc-in [:args :resizing?] (and (-> widget :args :can-resize) at-border))
      ;; handle widget focusing
        (when (not (-> widget :args :focused?))
          (swap! widget-mut assoc-in [:args :focused?] true)
          (widget-event :widget-focus-in canvas widget)
          (trigger-custom-event :widget-focus-in widget))
        (when (c2d/mouse-pressed? window)
          (cond
            (and (-> widget :args :can-resize) at-border) (swap! widget-mut merge (handle-widget-resizing widget [x y]))
            (-> widget :args :can-move?) (do
                                           (swap! widget-mut merge (handle-widget-dragging widget [x y]))
                                           (widget-event :widget-moved canvas widget)
                                           (trigger-custom-event :widget-moved widget))))))
    ;; reset all previously focused widgets
    (let [previous-widgets (filter #(and (-> % :args :focused?) (not= widget %)) widgets)
          previous-widgets (map #(neighbouring-widgets canvas % widgets) previous-widgets)
          previous-widgets (mapcat identity previous-widgets)
          previous-widgets (sort-by #(-> % :args :z) (set previous-widgets))]
      (loop [prev-widgets previous-widgets]
        (when (seq prev-widgets)
          (let [prev-widget (first prev-widgets)
                prev-new-widget (assoc-in prev-widget [:args :focused?] nil)
                prev-new-widget (assoc-in prev-new-widget [:args :resizing?] nil)]
            (replace! canvas prev-widget prev-new-widget)
            (widget-event :widget-focus-out canvas prev-widget)
            (trigger-custom-event :widget-focus-out prev-widget)
            (recur (rest prev-widgets))))))
    (when (not= @widget-mut widget)
      (replace! canvas widget @widget-mut))))

(defmethod c2d/mouse-event ["main-window" :mouse-dragged] [event state]
  (handle-mouse-moved :mouse-dragged)
  (let [context (:context @strigui.widget/state)
        window (:window context)]
    (swap! strigui.widget/state assoc :previous-mouse-position [(c2d/mouse-x window) (c2d/mouse-y window)]))
  state)

(defmethod c2d/mouse-event ["main-window" :mouse-moved] [event state]
  (handle-mouse-moved :mouse-moved)
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
        widget (first (reverse (sort-by #(-> % :args :z) (filter #(-> % :args :selected?) (:widgets @state)))))]
    (when (= code :tab)
      (when-let [new-widget-map (next-tabbed-widget-map canvas (:widgets @state) (:previously-tabbed @state) widget)]
        (swap! state merge new-widget-map)
        (swap! strigui.widget/state merge @state)
        (apply redraw! canvas (sort-by #(-> % :args :z) (:widgets new-widget-map)))))
    (widget-global-event :key-pressed canvas char code)
    (when-let [widget (first (filter #(-> % :args :selected?) (:widgets @state)))]
      (widget-event :key-pressed canvas widget char code)
      (trigger-custom-event :key-pressed widget code)))
  state)