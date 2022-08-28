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
  (draw [this canvas] "draw the widget, returns the widget on success")
  (after-drawing [this] "modify the widget each time after it got drawn"))

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

(defonce widget-default-props {:width 150 :height 42
                              :z 0 
                               :border-size 1
                               :highlight []
                               :highlight-border-size 1.5
                               :highlight-alpha-opacity 30})

(def previously (atom {:tabbed #{}
                       :mouse-position nil
                       :key-code nil}))

(def state (atom {:widgets {}
                  :context {:canvas nil :window nil}}))

(defmulti widget-event
  (fn [action canvas widgets widget & props]
    [(class widget) action]))

(defmethod widget-event :default [action canvas widgets widget & props] widgets)

(defmulti widget-global-event
  (fn [action widgets & props] action))

(defmethod widget-global-event :default [_ widgets & props] widgets)

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

(defn draw-highlight [key default widget canvas]
     (when (some #{:border} (-> widget :props :highlight))
       (draw-border widget canvas (get (-> widget :props :color) key default) (get (-> widget :props) :highlight-border-size (:highlight-border-size widget-default-props))))
     (when (some #{:alpha} (-> widget :props :highlight))
       (draw (assoc-in widget [:props :color :background] (let [color ^Color (get (-> widget :props :color) key default)]
                                                            (Color. (.getRed color) (.getGreen color) (.getBlue color) (get (-> widget :props) :highlight-alpha-opacity (:highlight-alpha-opacity widget-default-props)))))
             canvas)))

(def-action "hide" (fn [widget canvas]
                     (let [[x y w h] (coord widget canvas)]
                       (c/draw-> (dissoc canvas :rendering) ;;remove rendering hints when erasing a widget on the canvas
                           (c/clear-rect (- x 5) (- y 5) (+ w 8) (+ h 8))))))

(def-action "draw-resizing" (partial draw-highlight :resize (-> widget-default-props :color :resize)))

(def-action "draw-selected" (partial draw-highlight :select (-> widget-default-props :color :select)))

(def-action "draw-focused" (partial draw-highlight :focus (-> widget-default-props :color :focus)))

(defn draw-widget-border
  [^strigui.widget.Widget widget canvas]
  (when (seq (-> widget :props :highlight))
    (cond
      (-> widget :props :resizing?) (draw-resizing! widget canvas)
      (-> widget :props :selected?) (draw-selected! widget canvas)
      (-> widget :props :focused?) (draw-focused! widget canvas)
      :else (draw-border widget canvas (-> widget :props :color :border) (-> widget :props :border-size)))))

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
        (filter #(= (-> % :props key) value))
        (map :name)))
  ([widgets key]
   (->> widgets
        (filter #(-> % :props key))
        (map :name))))

(defn set-with-property
  ""
  [widgets key value]
  (for [widget widgets]
    (assoc-in widget [:props key] value)))

(defn assoc-arg-for-all
  [widgets key value]
  (reduce merge {} (map #(merge {} {(:name %) %}) (for [w (vals widgets)] (assoc-in w [:props key] value)))))

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
  (loop [widgets (sort-by #(-> % :props :z) widgets)]
    (when (seq widgets)
      (let [^strigui.widget.Widget widget (first widgets)]
        (draw widget canvas)
        (draw-widget-border widget canvas)
        (recur (rest widgets))))))

(defn adjust-dimensions 
  [canvas ^strigui.widget.Widget widget]
  (let [[_ _ w h] (coord widget canvas)
        set-default (fn [value] (if (number? value) value 0))
        {{width :width height :height x :x y :y z :z :as props} :props} widget
        width (if (and (number? width) (>= width w)) width w)
        height (if (and (number? height) (>= height h)) height h)
        [x y z] (map set-default [x y z])
        props (assoc props :width width :height height :x x :y y :z z)]
    (assoc widget :props props)))

(defn updated-widgets->keys
  [before after]
  (let [keys-before (-> before keys set)
        keys-after (-> after keys set)
        added-keys (s/difference keys-after keys-before)
        remaining-keys (s/difference keys-after added-keys)
        keys-with-updates (map :name (filter #(not= % (get before (:name %))) (vals (select-keys after remaining-keys))))]
    (set keys-with-updates)))

(defn added-widgets->keys
  [before after]
  (let [keys-before (-> before keys set)
        keys-after (-> after keys set)]
    (s/difference keys-after keys-before)))

(defn removed-widgets->keys
  [before after]
  (let [keys-before (-> before keys set)
        keys-after (-> after keys set)]
    (s/difference keys-before keys-after)))

(defn select-neighbouring-keys
  "Select all widgets that share pixels with the widget names given
   in the key-selection parameter"
  [canvas widgets key-selection] 
  (loop [neighbours (set key-selection)
         to-be-considered (s/difference (-> widgets keys set) neighbours)
         new-reachable neighbours]
    (if-not (seq new-reachable)
      neighbours
      (let [all-reachable-widgets (set (flatten (for [neighbour (map #(coord % canvas) (vals (select-keys widgets neighbours)))]
                                                  (let [wdgs-to-consider (vals (select-keys widgets to-be-considered))] 
                                                    (map :name (filterv #(intersect? neighbour (coord % canvas))
                                                                        wdgs-to-consider))))))]
        (recur (s/union neighbours all-reachable-widgets)
               (s/difference to-be-considered all-reachable-widgets)
               all-reachable-widgets)))))

(defn swap-widgets!
  "Swaps out the widgets using the given function.
   f - function that takes a widgets map and returns a new widgets map"
  [f]
  (try
    (let [canvas (-> @state :context :canvas)
          before (:widgets @state)
          after (f before)
          updated-keys (updated-widgets->keys before after)
          added-keys (added-widgets->keys before after)
          removed-keys (removed-widgets->keys before after)
          neighbour-keys (select-neighbouring-keys canvas after (s/union updated-keys added-keys removed-keys))]
      (c/use-buffer-> canvas
                      (doseq [to-hide (vals (select-keys before (s/union updated-keys removed-keys)))];;(vals (select-keys before (s/union updated-keys removed-keys)))]
                        (hide! to-hide canvas))
                      (when-let [widgets-to-draw (vals (select-keys after neighbour-keys))];;(vals (select-keys after (s/union updated-keys added-keys neighbour-keys)))]
                        (let [widgets-to-draw (map before-drawing widgets-to-draw)]
                          (draw-widgets! canvas widgets-to-draw)
                          (let [widgets-to-draw (map after-drawing widgets-to-draw)
                                after (merge-with into after (mapcat #(merge {(:name %) %}) widgets-to-draw))]
                            (swap! state assoc :widgets after))))))
    (catch Exception e (str "Failed to update widgets, perhaps the given function" \newline
                           "doesn't take or doesn't return a widgets map." \newline
                           "Exception: " (.getMessage e)))))

(defn trigger-custom-event
  [action widgets ^strigui.widget.Widget widget & props]
  (if-let [event-fn (-> widget :events action)]
    (apply event-fn widgets (:name widget) props)
    widgets))

(defn- handle-widget-dragging
  [^strigui.widget.Widget widget x y x-prev y-prev]
    (let [dx (- x x-prev)
          dy (- y y-prev)
          new-x (+ (-> widget :props :x) dx)
          new-y (+ (-> widget :props :y) dy)]
      (update widget :props #(merge % {:x new-x :y new-y}))))

(defn- handle-widget-resizing
  [^strigui.widget.Widget widget x y x-prev y-prev]
    (let [[dx dy] [(- x x-prev) (- y y-prev)]
          [x0 y0 w h] [(-> widget :props :x) (-> widget :props :y) (-> widget :props :width) (-> widget :props :height)]
          position-f (fn [m0 m] (<= (- m0 border-thickness) m (+ m0 border-thickness)))
          [left? top? right? bottom?] [(position-f x0 x) (position-f y0 y) (position-f (+ x0 w) x) (position-f (+ y0 h) y)]
          [x1 y1 w1 h1] (cond 
                          left? [(+ x0 dx) y0 (- w dx) h]
                          top? [x0 (+ y0 dy) w (- h dy)]
                          right? [x0 y0 (+ w dx) h]
                          bottom? [x0 y0 w (+ h dy)]
                          :else [x0 y0 w h])]
      (update widget :props #(merge % {:width w1 :height h1
                                      :x x1 :y y1}))))

(defn handle-clicked
  [canvas widgets x y]
  (swap! previously assoc :key-code nil)
  (let [;; get the first widget that is on top close to the mouse position
        widget (first (reverse (sort-by #(-> % :props :z) (filter #(within? (coord % canvas) x y) (vals widgets)))))
        clicked (when (seq widget) (:name widget))
        widgets (assoc-arg-for-all widgets :selected? nil)]
    (if (and clicked (not (-> (get widgets clicked) :props :resizing?)))
        (let [widgets (assoc-in widgets [clicked :props :selected?] true)
              widgets (widget-event :mouse-clicked canvas widgets (get widgets clicked) x y)
              widgets (trigger-custom-event :mouse-clicked widgets (get widgets clicked))]
          widgets)
      widgets)))

(defn handle-mouse-dragged
  [canvas widgets x y x-prev y-prev]
  (if-let [widget (first (reverse (sort-by #(-> % :props :z) (filter #(within? (coord % canvas) x y) (vals widgets)))))]
    (let [widgets (if (-> widget :props :resizing?)
                    (update widgets (:name widget) handle-widget-resizing x y x-prev y-prev)
                    (if (-> widget :props :can-move?)
                      (update widgets (:name widget) handle-widget-dragging x y x-prev y-prev)
                      widgets))]
      (widget-event :mouse-dragged canvas widgets (get widgets (:name widget)) x y x-prev y-prev))
    widgets))

(defn handle-mouse-moved 
  [canvas widgets x y]
  (let [widget (first (reverse (sort-by #(-> % :props :z) (filter #(within? (coord % canvas) x y) (vals widgets)))))
        ;; if the mouse is not on a widget, check previously focused widgets, trigger events and unfocus them
        widgets (if-let [focused-widgets (get-with-property (vals widgets) :focused?)]
                  (loop [remaining-focused focused-widgets
                         widgets widgets]
                    (if (seq remaining-focused)
                      (recur (rest remaining-focused) (let [name (first remaining-focused)
                                                            widgets (assoc-in widgets [name :props :focused?] nil)
                                                            widgets (assoc-in widgets [name :props :resizing?] nil)
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
            widgets (if (-> widget :props :focused?) 
                      widgets
                      (let [name (:name widget)
                            widgets (widget-event :widget-focus-in canvas widgets (get widgets name) x y)]
                        (trigger-custom-event :widget-focus-in widgets (get widgets name) x y)))
            widgets (assoc-in widgets [(:name widget) :props :focused?] true)]
        (assoc-in widgets [(:name widget) :props :resizing?] (and (-> widget :props :can-resize?) 
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
  (let [canvas (-> @state :context :canvas)]
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
        (assoc-in widgets [new-widget :props :selected?] (seq new-widget)))
    widgets))

(defn handle-key-pressed
  [canvas widgets char code]
  (let [previous-code (-> @previously :key-code)
        widgets (widget-global-event :key-pressed widgets char code previous-code)]
    (swap! previously assoc :key-code code)
    (if-let [widget (first (reverse (sort-by #(-> % :props :z) (filter #(-> % :props :selected?) (vals widgets)))))]
      (let [widgets (handle-tabbing canvas widgets widget code)
            widgets (widget-event :key-pressed canvas widgets (get widgets (:name widget)) char code previous-code)]
        (trigger-custom-event :key-pressed widgets (get widgets (:name widget)) code char previous-code))
      (if-let [tabable (first (get-with-property (vals widgets) :can-tab? true))]
          (handle-tabbing canvas widgets (get widgets tabable) code)
        widgets))))

(defmethod c/handle-event :key-pressed [_ {:keys [char code]}]
  (let [canvas (:canvas (:context @state))]
    (swap-widgets! #(handle-key-pressed canvas % char code))))
