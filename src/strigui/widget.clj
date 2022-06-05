(ns strigui.widget
  (:require [capra.core :as c]
            [clojure.set :as s]
            [clojure.string])
  (:import [java.awt Color]))

(def ^:private border-thickness 10)

(defprotocol Widget
  "collection of functions around redrawing widgets, managing the border etc. ..."
  (coord [this canvas] "gets the coordinates of the widget")
  (defaults [this] "attach default values once the widget gets created")
  (before-drawing [this] "modify the widget each time before it gets drawn")
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

(def previously (atom {:tabbed #{}
                       :mouse-position nil}))

(def state (atom {:widgets {}
                  :context {:canvas nil :window nil}}))

(defmulti widget-event
  (fn [action canvas widgets widget & args]
    [(class widget) action]))

(defmethod widget-event :default [action canvas widgets widget & args] widgets)

(defmulti widget-global-event
  (fn [action widgets & args] action))

(defmethod widget-global-event :default [_ widgets & args] widgets)

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
        (c/rect (+ x strength) (+ y strength) (- w strength) (- h strength) color (not no-fill) strength)) ;;TODO: clean this up double negation
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
                       (c/draw-> (dissoc canvas :rendering)
                           (c/clear-rect (- x 5) (- y 5) (+ w 8) (+ h 8))))))

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

(defn assoc-arg-for-all
  [widgets key value]
  (reduce merge {} (map #(merge {} {(:name %) %}) (for [w (vals widgets)] (assoc-in w [:args key] value)))))

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

(defn draw-widgets! 
  [canvas widgets]
  (loop [widgets (sort-by #(-> % :args :z) widgets)]
    (when (seq widgets)
      (let [^strigui.widget.Widget widget (first widgets)]
        (draw widget canvas)
        (draw-widget-border widget canvas)
        (recur (rest widgets))))))

(defn widget->neighbour
  "get all neighbours of the given widget. Neighbours are sorted by their :z coordinate
   in ascending order"
  [canvas ^strigui.widget.Widget widget widgets]
  (let [widget-coords (coord widget canvas)
              neighbours (set (filter #(intersect? widget-coords (coord % canvas))
                                      widgets)) ;;(not= widget %)  
              neighbours (sort-by #(-> % :args :z) neighbours)]
    neighbours))

(defn widget->neighbours
  "Get all neighbouring widgets by following the neighouring chain.
   operator-f is a optional order with truthy return (fn [neighbour current-widget] ....) that will be used in a filter , 
    - > all widgets that are covering the current widget
    - < all widgets that are covered by the current widget
   If no order function is given, then take all widgets that touch the current widget."
  ([canvas ^strigui.widget.Widget widget widgets] (widget->neighbours canvas widget widgets nil))
  ([canvas ^strigui.widget.Widget widget widgets operator-f]
   (loop [neighbours #{widget}
          visited #{}]
     (if (< (count visited) (count neighbours))
       (let [not-visited (s/difference neighbours visited)
             next-widget (first not-visited)
             z-of-next (-> next-widget :args :z)
             new-neighbours (widget->neighbour canvas next-widget widgets)
             new-neighbours (if operator-f 
                              (filter #(operator-f (-> % :args :z) z-of-next) new-neighbours)
                              new-neighbours)]
         (recur (s/union neighbours (set new-neighbours)) (s/union visited #{next-widget})))
       neighbours))))

(defn widgets->neighbours
  [canvas widgets-to-determine widgets fn-operation]
  (loop [widget (vals widgets-to-determine)
         keys-not-considered (-> widgets keys set)
         neighbour-keys #{}]
    (if (and (seq widget) (seq keys-not-considered))
      (let [neighb (set (map :name (widget->neighbours canvas (first widget) (vals (select-keys widgets keys-not-considered)) fn-operation)))]
        (recur (rest widget) (s/difference keys-not-considered neighb) (s/union neighbour-keys neighb)))
      (select-keys widgets neighbour-keys))))

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

(defn determine-updated-keys
  [before after]
  (let [keys-before (-> before keys set)
        keys-after (-> after keys set)
        added-keys (s/difference keys-after keys-before)
        remaining-keys (s/difference keys-after added-keys)
        keys-with-updates (map :name (filter #(not= % (get before (:name %))) (vals (select-keys after remaining-keys))))]
    keys-with-updates))

(defn determine-widgets-to-redraw
  [before after updated-keys]
  (let [keys-before (-> before keys set)
        keys-after (-> after keys set)
        added-keys (s/difference keys-after keys-before)
        widgets-newly-added (select-keys after added-keys)
        widgets-with-updates (select-keys after updated-keys)]
    (merge widgets-with-updates widgets-newly-added)))

(defn determine-old-widgets-to-hide
  [before after updated-keys]
  (let [bef (for [widget (vals (select-keys before updated-keys))]
              {(:name widget) (select-keys (:args widget) [:x :y :width :height])})
        aft (for [widget (vals (select-keys after updated-keys))]
              {(:name widget) (select-keys (:args widget) [:x :y :width :height])})
        bef (apply merge-with into bef)
        aft (apply merge-with into aft)
        diff (for [b bef]
               (when-let [a (get aft (key b))]
                 (when (some #(not= 0.0 %) (map (comp double -) (->> b val vals) (vals a)))
                   (key b))))]
    (select-keys before (filterv seq diff))))

(defn swap-widgets!
  "Swaps out the widgets using the given function.
   f - function that takes a widgets map and returns a new widgets map"
  [f]
  (try
    (let [canvas (-> @state :context :canvas)
          before (:widgets @state)
          after (f before)
          updated-keys (determine-updated-keys before after)
          to-redraw (determine-widgets-to-redraw before after updated-keys)
          to-hide (determine-old-widgets-to-hide before after updated-keys) 
          ;; maybe only use the neighbouts of widgets whois size or position has changed
          neighbours (merge (widgets->neighbours canvas to-redraw after >) (widgets->neighbours canvas to-hide after <))]
      (doseq [to-hide (vals to-hide)]
        (hide! to-hide canvas))
      (when-let [widgets-to-draw (vals (merge to-redraw neighbours))]
        (let [widgets-to-draw (map before-drawing widgets-to-draw)
              after (merge-with into after (mapcat #(merge {(:name %) %}) widgets-to-draw))]
          (swap! state assoc :widgets after)
          (draw-widgets! canvas widgets-to-draw))))
    (catch Exception e (str "Failed to update widgets, perhaps the given function" \newline
                           "doesn't take or doesn't return a widgets map." \newline
                           "Exception: " (.getMessage e)))))

(defn trigger-custom-event
  [action widgets ^strigui.widget.Widget widget & args]
  (if-let [event-fn (-> widget :events action)]
    (apply event-fn widgets (:name widget) args)
    widgets))

(defn- handle-widget-dragging
  [^strigui.widget.Widget widget x y x-prev y-prev]
    (let [dx (- x x-prev)
          dy (- y y-prev)
          new-x (+ (-> widget :args :x) dx)
          new-y (+ (-> widget :args :y) dy)]
      (update widget :args #(merge % {:x new-x :y new-y}))))

(defn- handle-widget-resizing
  [^strigui.widget.Widget widget x y x-prev y-prev]
    (let [[dx dy] [(- x x-prev) (- y y-prev)]
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
                                      :x x1 :y y1}))))

(defn handle-clicked
  [canvas widgets x y]
  (let [;; get the first widget that is on top close to the mouse position
        widget (first (reverse (sort-by #(-> % :args :z) (filter #(within? (coord % canvas) x y) (vals widgets)))))
        clicked (when (seq widget) (:name widget))
        widgets (assoc-arg-for-all widgets :selected? nil)]
    (if (and clicked (not (-> (get widgets clicked) :args :resizing?)))
        (let [widgets (assoc-in widgets [clicked :args :selected?] true)
              widgets (widget-event :mouse-clicked canvas widgets (get widgets clicked) x y)
              widgets (trigger-custom-event :mouse-clicked widgets (get widgets clicked))]
          widgets)
      widgets)))

(defn handle-mouse-dragged
  [canvas widgets x y x-prev y-prev]
  (if-let [widget (first (reverse (sort-by #(-> % :args :z) (filter #(within? (coord % canvas) x y) (vals widgets)))))]
    (let [widgets (if (-> widget :args :resizing?)
                    (update widgets (:name widget) handle-widget-resizing x y x-prev y-prev)
                    (if (-> widget :args :can-move?)
                      (update widgets (:name widget) handle-widget-dragging x y x-prev y-prev)
                      widgets))]
      (widget-event :mouse-dragged canvas widgets (get widgets (:name widget)) x y x-prev y-prev))
    widgets))

(defn handle-mouse-moved 
  [canvas widgets x y]
  (let [widget (first (reverse (sort-by #(-> % :args :z) (filter #(within? (coord % canvas) x y) (vals widgets)))))
        ;; if the mouse is not on a widget, check previously focused widgets, trigger events and unfocus them
        widgets (if-let [focused-widgets (get-with-property (vals widgets) :focused?)]
                  (loop [remaining-focused focused-widgets
                         widgets widgets]
                    (if (seq remaining-focused)
                      (recur (rest remaining-focused) (let [name (first remaining-focused)
                                                            widgets (assoc-in widgets [name :args :focused?] nil)
                                                            widgets (assoc-in widgets [name :args :resizing?] nil)
                                                            widgets (widget-event :widget-focus-out canvas widgets (get widgets name) x y)
                                                            widgets (trigger-custom-event :widget-focus-out widgets (get widgets name) x y)]
                                                        widgets))
                      widgets))
                  widgets)]
    (if (seq widget)
      ;; if the mouse is on a widget, focus it and trigger events in case it wasn't focused before, check if it should resize
      (let [widgets (widget-event :mouse-moved canvas widgets widget x y)
            widgets (trigger-custom-event :mouse-moved widgets (get widgets (:name widget)) x y)
            widget (get widgets (:name widget))
            widgets (if (-> widget :args :focused?) 
                      widgets
                      (let [name (:name widget)
                            widgets (widget-event :widget-focus-in canvas widgets (get widgets name) x y)]
                        (trigger-custom-event :widget-focus-in widgets (get widgets name) x y)))
            widgets (assoc-in widgets [(:name widget) :args :focused?] true)]
        (assoc-in widgets [(:name widget) :args :resizing?] (and (-> widget :args :can-resize?) 
                                                                 (on-border? (coord (get widgets (:name widget)) canvas) x y))))
      widgets)))

(defmethod c/handle-event :mouse-dragged [_ {:keys [x y]}]
  (let [canvas (-> @state :context :canvas)
        [x-prev y-prev] (-> @previously :mouse-position)]
    (swap-widgets! #(let [widgets (handle-mouse-dragged canvas % x y x-prev y-prev)]
                      (widget-global-event :mouse-dragged widgets x y x-prev y-prev)))
    (swap! previously assoc :mouse-position [x y])))

(defmethod c/handle-event :mouse-moved [_ {:keys [x y]}]
  (let [canvas (-> @state :context :canvas)]
    (swap-widgets! #(let [widgets (handle-mouse-moved canvas % x y)]
                      (widget-global-event :mouse-moved widgets x y)))
    (swap! previously assoc :mouse-position [x y])))

(defmethod c/handle-event :mouse-pressed [_ {:keys [x y]}]
  (let [canvas (-> @strigui.widget/state :context :canvas)]
    (swap-widgets! #(let [widgets (handle-clicked canvas % x y)]
                      (widget-global-event :mouse-clicked widgets x y)))))

(defmethod c/handle-event :mouse-released [_ {:keys [x y]}]
  (swap-widgets! #(let [widgets (widget-global-event :mouse-released % x y)]
                    (widget-global-event :mouse-released widgets x y))))

(defn handle-tabbing
  [canvas widgets widget code]
  (if (= code 9) ;;tab
    (let [previously-tabbed (:tabbed @previously)
          previously-tabbed (if (= (set (get-with-property (vals widgets) :can-tab?))
                                   (set previously-tabbed))
                              #{}
                              previously-tabbed)
          new-widget (:name (next-widget-to-tab canvas widgets previously-tabbed widget))
          widgets (assoc-arg-for-all widgets :selected? nil)]
      (when new-widget
        (let [previously-tabbed (s/union previously-tabbed #{new-widget})]
          (swap! previously assoc :tabbed previously-tabbed)))
      (assoc-in widgets [new-widget :args :selected?] (seq new-widget)))
    widgets))

(defn handle-key-pressed
  [canvas widgets char code]
  (let [widgets (widget-global-event :key-pressed widgets char code)]
    (if-let [widget (first (reverse (sort-by #(-> % :args :z) (filter #(-> % :args :selected?) (vals widgets)))))]
      (let [widgets (handle-tabbing canvas widgets widget code)
            widgets (widget-event :key-pressed canvas widgets (get widgets (:name widget)) char code)]
        (trigger-custom-event :key-pressed widgets (get widgets (:name widget)) code))
      (if-let [tabable (first (get-with-property widgets :can-tab? true))]
        (handle-tabbing canvas widgets (get widgets tabable) code)
        widgets))))

(defmethod c/handle-event :key-pressed [_ {:keys [char code]}]
  (let [canvas (:canvas (:context @state))]
    (swap-widgets! #(handle-key-pressed canvas % char code))))