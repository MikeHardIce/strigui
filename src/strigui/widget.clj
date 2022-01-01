(ns strigui.widget
  (:require [capra.core :as c]
            [clojure.set :as s]
            [clojure.string])
  (:import [java.awt Color]))

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

(def state (atom {:widgets {}
                  :widgets-to-redraw #{}
                  :previous-mouse-position nil
                  :previously-tabbed #{}
                  :previously-selected nil
                  :context {:canvas nil :window nil}}))

(defmulti widget-event
  (fn [action canvas widget & args]
    [(class widget) action]))

(defmethod widget-event :default [action canvas widget & args] nil)

(defmulti widget-global-event
  (fn [action canvas & args] action))

(defmethod widget-global-event :default [_ canvas & args] nil)

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
     (c/draw-> canvas
        (c/rect x y w h color (not no-fill) strength)) ;;TODO: clean this up double negation
     (draw-border-rec canvas color (- strength 1) x y w h no-fill))))

(defn- draw-border
  ([box canvas] (draw-border box canvas Color/black 1))
  ([box canvas color] (draw-border box canvas color 1))
  ([box canvas color strength] (draw-border box canvas color strength false))
  ([box canvas color strength fill]
   (let [[x y w h] (coord box canvas)]
     (draw-border-rec canvas color strength x y w h (not fill)))))

(def-action "hide" (fn [widget canvas]
                     (let [[x y w h] (coord widget canvas)]
                       (c/draw-> canvas
                         (c/rect (- x 5) (- y 5) (+ w 8) (+ h 8) Color/white true)))))

(def-action "draw-resizing" (fn [widget canvas]
                              (draw-border widget canvas Color/orange 2)))

(def-action "draw-selected" (fn [widget canvas]
                              (draw-border widget canvas Color/blue 2)))

(def-action "draw-focused" (fn [widget canvas]
                              (draw-border widget canvas Color/black 2)))

(defn draw-widget-border
  [^strigui.widget.Widget widget canvas]
  (when (-> widget :args :has-border?)
    (cond
      (-> widget :args :resizing?) (draw-resizing! widget canvas)
      (-> widget :args :selected?) (draw-selected! widget canvas)
      (-> widget :args :focused?) (draw-focused! widget canvas)
      :else (draw-border widget canvas Color/black 1))))

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

(defn get-with-property
  "Returns the widget names for widgets that contain the specific key value pair 
   or a truthy value for the specific key from a sequence of widgets."
  ([widgets key value]
   (->> widgets
        (filter #(= (-> % :args key) value))
        (map :name)))
  ([widgets key]
   (->> widgets
        (filter #(-> % :args key))
        (map :name))))

(defn set-with-property
  ""
  [widgets key value]
  (for [widget widgets]
    (assoc-in widget [:args key] value)))

(defn next-widget-to-tab
  [canvas widgets previously-tabbed ^strigui.widget.Widget selected-widget]
  (let [widgets-can-tab (get-with-property (vals widgets) :can-tab?)
        previously-tabbed (conj previously-tabbed (:name selected-widget))
        not-tabbed (s/difference (set widgets-can-tab) (set previously-tabbed))
        ;; In case not-tabbed is empty, start new
        to-be-tabbed (if (seq not-tabbed) not-tabbed widgets-can-tab)
        coord-widget (if (seq selected-widget) (coord selected-widget canvas) [0 0])
        widgets-to-be-tabbed (select-keys widgets to-be-tabbed)
        dist (map #(merge {:widget %} {:dist (distance-x coord-widget (coord % canvas))}) (vals widgets-to-be-tabbed))
        dist (sort-by :dist < dist)
        dist (filter #(> (:dist %) 0) dist)
        new-selected (:widget (first dist))]
    new-selected))

(defn redraw!
  "redraws the given sequence of widgets"
  [canvas & widgets]
  (loop [widgets (sort-by #(-> % :args :z) widgets)]
    (when (seq widgets)
      (let [^strigui.widget.Widget widget (first widgets)]
        (hide! widget canvas)
        (draw widget canvas)
        (draw-widget-border widget canvas)
        (recur (rest widgets))))))

(defn neighbouring-widgets
  "get all neighbours of the given widget. Neighbours are sorted by their :z coordinate
   in ascending order"
  [canvas ^strigui.widget.Widget widget widgets]
  (let [widget-coords (coord widget canvas)
              neighbours (set (filter #(and (intersect? widget-coords (coord % canvas))
                                                      ) widgets)) ;;(not= widget %)  
              neighbours (sort-by #(-> % :args :z) neighbours)]
    neighbours))

(defn all-neighbouring-widgets
  "Get all neighbouring widgets by following the neighouring chain.
   operator-f is a optional order with truthy return (fn [neighbour current-widget] ....) that will be used in a filter , 
    > - all widgets that are covering the current widget
    < - all widgets that are covered by the current widget
   If no order function is given, then take all widgets that touch the current widget."
  ([canvas ^strigui.widget.Widget widget widgets] (all-neighbouring-widgets canvas widget widgets nil))
  ([canvas ^strigui.widget.Widget widget widgets operator-f]
   (loop [neighbours #{widget}
          visited #{}]
     (if (< (count visited) (count neighbours))
       (let [not-visited (s/difference neighbours visited)
             next-widget (first not-visited)
             z-of-next (-> next-widget :args :z)
             new-neighbours (neighbouring-widgets canvas next-widget widgets)
             new-neighbours (if operator-f 
                              (filter #(operator-f (-> % :args :z) z-of-next) new-neighbours)
                              new-neighbours)]
         (recur (s/union neighbours (set new-neighbours)) (s/union visited #{next-widget})))
       neighbours))))

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
  "register the widget and draw it to the canvas. Can skip redraw via ski-redraw?"
  ([canvas ^strigui.widget.Widget widget]
   (register! canvas widget false))
  ([canvas ^strigui.widget.Widget widget skip-redraw?]
   (let [border-and-swap-f (fn []
                             (draw-widget-border widget canvas)
                             (swap! state assoc-in [:widgets (:name widget)] widget))]
     (if skip-redraw?
       (border-and-swap-f)
       (when (draw widget canvas)
         (border-and-swap-f))))
   (get-in @state [:widgets (:name widget)])))

(defn unregister!
  "unregister the widget and hide it. "
  ([canvas ^strigui.widget.Widget widget]
   (unregister! canvas widget false))
  ([canvas ^strigui.widget.Widget widget skip-hide?]
   (let [remove-widget-f (fn []
                           (swap! state update :widgets dissoc (:name widget))
                           (swap! state update :widgets-to-redraw #(s/difference %1 #{widget}))
                          ;;  (swap! state update :previously-tabbed #(s/difference % #{(:name widget)}))
                          ;;  (when (= widget (:previously-selected @state))
                          ;;    (swap! state assoc :previously-selected nil))
                           )]
     (if skip-hide?
       (remove-widget-f)
       (when (hide! widget canvas)
         (remove-widget-f))))))

(defn replace!
  "Replaces the current widget of the given widget name with the
   new widget, if the current widget actually exists, otherwise it ignores it."
  ([canvas old-widget-name ^strigui.widget.Widget new-widget]
   (replace! canvas old-widget-name new-widget false))
  ([canvas old-widget-name ^strigui.widget.Widget new-widget skip-redraw?]
  (when-let [old-widget (first (-> @state :widgets (select-keys [old-widget-name])))]
    (unregister! canvas (val old-widget) skip-redraw?)
    (register! canvas new-widget skip-redraw?))))

(defn trigger-custom-event
  [action ^strigui.widget.Widget widget & args]
  (when-let [event-fn (-> widget :events action)]
    (apply event-fn widget args)))

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
  [x y]
  (let [context (:context @strigui.widget/state)
        canvas (:canvas context)
         ;; get the first widget that is on top close to the mouse position
        widget (first (reverse (sort-by #(-> % :args :z) (filter #(within? (coord % canvas) x y) (->> @state :widgets vals)))))
        selected (get-with-property (->> @state :widgets vals) :selected?)
        selected (sort-by #(-> % val :args :z) (vals (select-keys (->> @state :widgets) selected)))]
    (when (seq selected)
      (loop [unselected (set-with-property selected :selected? nil)]
        (when (seq unselected)
          (replace! canvas (-> unselected first :name) (-> unselected first) (-> unselected first :args :skip-redrawing :on-unselect))
          (recur (rest unselected)))))
    (when (seq widget)
      (when (not (-> widget :args :resizing?))
        (replace! canvas (:name widget) (assoc-in widget [:args :selected?] true) (-> widget :args :skip-redrawing :on-click))
        (widget-event :mouse-clicked canvas widget x y)
        (widget-global-event :mouse-clicked canvas x y)
        (trigger-custom-event :mouse-clicked widget)))))

(defn handle-mouse-moved
  [action x y]
  ;; this functions needs a rework, i think it can be optimized a lot.
  ;; need to find a better strategy to redraw widgets
  (let [context (:context @state)
        canvas (:canvas context)
        widget (first (reverse (sort-by #(-> % :args :z) (filter #(within? (coord % canvas) x y) (->> @state :widgets vals)))))
        widget-mut (atom widget)
        was-focused (and (seq widget) (= (:name widget) (-> @state :previously-selected :name)) (not= :mouse-dragged action))
        pending-redraw (atom #{})
        skip-redrawing true]
    (when (seq widget)
      (let [at-border (on-border? (coord widget canvas) x y)]
        (when-not was-focused
          (let [neighbours (all-neighbouring-widgets canvas widget (->> @state :widgets vals) (when was-focused >))]
            (swap! pending-redraw s/union (set (map :name neighbours)))
            ;;(apply redraw! canvas (set neighbours))
            (swap! state assoc :previously-selected widget)))
        (widget-event :mouse-moved canvas widget x y)
        (trigger-custom-event :mouse-moved widget x y)
        (swap! widget-mut assoc-in [:args :resizing?] (and (-> widget :args :can-resize) at-border))
      ;; handle widget focusing
        (when (not (-> widget :args :focused?))
          (swap! widget-mut assoc-in [:args :focused?] true)
          (widget-event :widget-focus-in canvas widget x y)
          (trigger-custom-event :widget-focus-in widget x y))
        (when (= action :mouse-dragged)
          (cond
            (and (-> widget :args :can-resize) at-border) (swap! widget-mut merge (handle-widget-resizing widget [x y]))
            (-> widget :args :can-move?) (do
                                           (swap! widget-mut merge (handle-widget-dragging widget [x y]))
                                           (widget-event :widget-moved canvas widget x y)
                                           (trigger-custom-event :widget-moved widget x y))))))
    ;; reset all previously focused widgets
    (let [previous-widgets (filter #(and (-> % :args :focused?) (not= widget %)) (->> @state :widgets vals))
          previous-widgets (map #(all-neighbouring-widgets canvas % (->> @state :widgets vals) >) previous-widgets)
          previous-widgets (mapcat identity previous-widgets)
          previous-widgets (sort-by #(-> % :args :z) (set previous-widgets))]
      (let [greater (if (seq widget) 
                           (filter #(>= (-> % :args :z) (-> widget :args :z)) previous-widgets)
                             previous-widgets)]
        (swap! pending-redraw s/union (set (map :name greater))))
      (loop [prev-widgets previous-widgets]
        (when (seq prev-widgets)
          (let [prev-widget (first prev-widgets)
                prev-new-widget (assoc-in prev-widget [:args :focused?] nil)
                prev-new-widget (assoc-in prev-new-widget [:args :resizing?] nil)]
            (replace! canvas (:name prev-widget) prev-new-widget skip-redrawing)
            (widget-event :widget-focus-out canvas prev-widget x y)
            (trigger-custom-event :widget-focus-out prev-widget x y)
            (recur (rest prev-widgets))))))
    (when (not= @widget-mut widget)
      (replace! canvas (:name widget) @widget-mut skip-redrawing)
      (when (= :mouse-dragged action)
        (hide! widget canvas))
      (let [covering-widgets (all-neighbouring-widgets canvas @widget-mut (->> @state :widgets vals) >)
            covering-widgets (filter #(not= @widget-mut %) covering-widgets)]
        (swap! pending-redraw s/union (set (map :name covering-widgets)))))
    (when-let [widgets (->> @pending-redraw
                            (select-keys (:widgets @state))
                            vals
                            (filter #(not (-> % :args :skip-redrawing :on-hover))))] ;; (vals (select-keys (:widgets @state) @pending-redraw))
      (apply redraw! canvas widgets))))

(defmethod c/handle-event :mouse-dragged [_ {:keys [x y]}]
  (handle-mouse-moved :mouse-dragged x y)
  (let [context (:context @strigui.widget/state)]
    (swap! strigui.widget/state assoc :previous-mouse-position [x y])
    (widget-global-event :mouse-dragged (:canvas context) x y)))

(defmethod c/handle-event :mouse-moved [_ {:keys [x y]}]
  (handle-mouse-moved :mouse-moved x y)
  (let [context (:context @strigui.widget/state)]
    (widget-global-event :mouse-moved (:canvas context) x y)))

(defmethod c/handle-event :mouse-pressed [_ {:keys [x y]}]
  (let [context (:context @strigui.widget/state)]
    (handle-clicked x y)
    (widget-global-event :mouse-clicked (:canvas context) x y)))

(defmethod c/handle-event :mouse-released [_ {:keys [x y]}]
  (widget-global-event :mouse-released (:canvas (:context @strigui.widget/state)) x y)
  (swap! strigui.widget/state assoc :previous-mouse-position nil))

(defmethod c/handle-event :key-pressed [_ {:keys [char code]}]
  (let [canvas (:canvas (:context @state))
        widget (first (reverse (sort-by #(-> % :args :z) (filter #(-> % :args :selected?) (->> @state :widgets vals)))))
        previously-tabbed (:previously-tabbed @state)
        previously-tabbed (when-not (= (set (get-with-property (->> @state :widgets vals) :can-tab?))
                                   (set previously-tabbed))
                            previously-tabbed)]
    (when (= code 9) ;;tab
      (when-let [new-widget (:name (next-widget-to-tab canvas (->> @state :widgets) previously-tabbed widget))]
        (swap! state assoc-in [:widgets new-widget :args :selected?] true)
        (loop [prev (sort-by #(-> % :args :z) (filter #(-> % :args :selected?) (->> @state :widgets vals)))]
          (when (seq prev)
            (replace! canvas (:name (first prev)) (assoc-in (first prev) [:args :selected?] (= (-> prev first :name) new-widget)))
            (recur (rest prev))))
        (when-not (seq previously-tabbed)
          (swap! state assoc :previously-tabbed #{}))
        (swap! state update :previously-tabbed s/union #{new-widget})))
    (widget-global-event :key-pressed canvas char code)
    (when-let [widget (first (reverse (sort-by #(-> % :args :z) (filter #(-> % :args :selected?) (->> @state :widgets vals)))))]
      (widget-event :key-pressed canvas widget char code)
      (trigger-custom-event :key-pressed widget code))))