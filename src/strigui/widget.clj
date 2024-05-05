(ns strigui.widget
  (:require [capra.core :as c]
            [clojure.set :as s]
            [clojure.string]
            [clojure.stacktrace])
  (:import [java.awt Color]))

(set! *warn-on-reflection* true)
(def ^:private border-thickness 10)

(defprotocol Widget
  "collection of functions around redrawing widgets, managing the border etc. ..."
  (coord [this window] "gets the coordinates of the widget")
  (defaults [this] "attach default values once the widget gets created")
  (before-drawing [this] "modify the widget each time before it gets drawn")
  (draw [this window] "draw the widget, returns the widget on success")
  (after-drawing [this] "modify the widget each time after it got drawn"))

(System/setProperty "sun.java2d.opengl" "True")

(defonce widget-default-props {:width 150 :height 42
                              :z 0 
                               :border-size 1
                               :highlight []
                               :highlight-border-size 1.5
                               :highlight-alpha-opacity 30
                               :can-hide? true
                               :color {:background (java.awt.Color. 250 250 250)
                                       :text (java.awt.Color. 10 10 10)
                                       :focus (java.awt.Color. 117 190 188)
                                       :select (java.awt.Color. 117 190 188)
                                       :border (java.awt.Color. 27 100 98)
                                       :resize (java.awt.Color. 147 220 118)}})

(def previously (atom {:tabbed #{}
                       :mouse-position nil
                       :key-code nil}))

(def state (agent {:widgets {}}))

(defmulti widget-event
  (fn [widgets event-attr]
    [(class (-> event-attr :widget)) (-> event-attr :action)]))

(defmethod widget-event :default [widgets _] widgets)

(defmulti widget-global-event
  (fn [widgets event-attr] 
    (-> event-attr :action)))

(defmethod widget-global-event :default [widgets _] widgets)

(defn window-key->widgets
  "Returns a vector of widget names of widgets that are part of the window with the given window key"
  [widgets window-key] 
  (mapv :name (vals (filter (comp #(= % window-key) :window :props val) widgets))))

(defn widget->window-key
  "Returns the window key the given widget is displayed on"
  [widgets widget-name]
  (when-let [widget (get widgets widget-name)]
    (-> widget :props :window)))

(defn widget->window
  "Returns the window the given widget is displayed on"
  [widgets widget-name]
  (when-let [window-key (widget->window-key widgets widget-name)]
    (get widgets window-key)))

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
  ([coord1 coord2]
  (or (within? coord1 coord2)
      (within? coord2 coord1)))
  ([[x1 y1 w1 h1] [x2 y2 w2 h2] offset]
   (let [coord1 [(- x1 offset) (- y1 offset) (+ (* 2 offset) w1) (+ (* 2 offset) h1)]
         coord2 [(- x2 offset) (- y2 offset) (+ (* 2 offset) w2) (+ (* 2 offset) h2)]]
     (intersect? coord1 coord2))))

(defn- draw-border-rec
  ([context color strength x y w h no-fill]
   (when (> strength 0)
     (c/draw-> context
               (c/rect (+ x strength) (+ y strength) (- w strength) (- h strength) color (not no-fill) strength)) ;;TODO: clean this up double negation
     (draw-border-rec context color (- strength 1) x y w h no-fill))))

(defn- draw-border
  ([box context] (draw-border box context Color/black 1))
  ([box context color] (draw-border box context color 1))
  ([box context color strength] (draw-border box context color strength false))
  ([box context color strength fill]
   (let [[x y w h] (coord box context)]
     (draw-border-rec context color strength x y w h (not fill)))))

(defn draw-highlight [key default ^strigui.widget.Widget widget context]
  (when (some #{:border} (-> widget :props :highlight))
    (draw-border widget context (get (-> widget :props :color) key default) (get (-> widget :props) :highlight-border-size (:highlight-border-size widget-default-props))))
  (when (some #{:alpha} (-> widget :props :highlight))
      (draw (assoc-in widget [:props :color :background] (let [color ^Color (get (-> widget :props :color) key default)]
                                                           (Color. ^int (.getRed color) ^int (.getGreen color) ^int (.getBlue color) ^int (get (-> widget :props) :highlight-alpha-opacity (:highlight-alpha-opacity widget-default-props)))))
            context)))

(def hide! (fn [widget context]
                     (let [[x y w h] (coord widget context)]
                       (c/draw-> (dissoc context :rendering) ;;remove rendering hints when erasing a widget on the canvas
                           (c/clear-rect (- x 5) (- y 5) (+ w 8) (+ h 8))))))

(def draw-resizing! (partial draw-highlight :resize (-> widget-default-props :color :resize)))

(def draw-selected! (partial draw-highlight :select (-> widget-default-props :color :select)))

(def draw-focused! (partial draw-highlight :focus (-> widget-default-props :color :focus)))

(defn draw-widget-border
  [^strigui.widget.Widget widget context]
  (when (seq (-> widget :props :highlight))
    (cond
      (-> widget :props :resizing?) (draw-resizing! widget context)
      (-> widget :props :selected?) (draw-selected! widget context)
      (-> widget :props :focused?) (draw-focused! widget context)
      :else (draw-border widget context (-> widget :props :color :border) (-> widget :props :border-size)))))

(defn distance-x
  "Manhatten distance that is sqashed on the x-axis,
  meaning widgets on similar y positions are treated as
  being closer together."
  ([context ^strigui.widget.Widget widget1 ^strigui.widget.Widget widget2]
   (let [coord1 (coord widget1 context)
         coord2 (coord widget2 context)]
     (distance-x coord1 coord2)))
  ([[^double x1 ^double y1] [^double x2 ^double y2]]
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
  [widgets key value]
  (for [widget widgets]
    (assoc-in widget [:props key] value)))

(defn assoc-arg-for-all
  [widgets key value]
  (reduce merge {} (map #(merge {} {(:name %) %}) (for [w (vals widgets)] (assoc-in w [:props key] value)))))

(defn next-widget-to-tab
  [context widgets previously-tabbed ^strigui.widget.Widget selected-widget] 
  (let [widgets-can-tab (get-with-property (vals widgets) :can-tab?)
        previously-tabbed (conj previously-tabbed (:name selected-widget))
        not-tabbed (s/difference (set widgets-can-tab) (set previously-tabbed))
        ;; In case not-tabbed is empty, start new
        to-be-tabbed (if (seq not-tabbed) not-tabbed widgets-can-tab)
        coord-widget (if (seq selected-widget) (coord selected-widget context) [0 0])
        widgets-to-be-tabbed (select-keys widgets to-be-tabbed)
        dist (map #(merge {:widget %} {:dist (distance-x coord-widget (coord % context))}) (vals widgets-to-be-tabbed))
        dist (sort-by :dist < dist)
        dist (filter #(> (:dist %) 0) dist)
        new-selected (:widget (first dist))]
    new-selected))

(defn draw-widgets! 
  [context widgets]
  (loop [widgets (sort-by #(-> % :props :z) widgets)]
    (when (seq widgets)
      (let [^strigui.widget.Widget widget (first widgets)]
        (draw widget context)
        (draw-widget-border widget context)
        (recur (rest widgets))))))

(defn adjust-dimensions 
  [context ^strigui.widget.Widget widget]
  (let [[_ _ w h] (coord widget context)
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
  [context widgets key-selection] 
  (loop [neighbours (set key-selection)
         to-be-considered (s/difference (-> widgets keys set) neighbours)
         new-reachable neighbours]
    (if-not (seq new-reachable)
      neighbours
      (let [all-reachable-widgets (set (flatten (for [neighbour (mapv #(coord % context) (vals (select-keys widgets neighbours)))]
                                                  (let [wdgs-to-consider (vals (select-keys widgets to-be-considered))] 
                                                    (mapv :name (filterv #(intersect? neighbour (coord % context) 10)
                                                                        wdgs-to-consider))))))]
        (recur (s/union neighbours all-reachable-widgets)
               (s/difference to-be-considered all-reachable-widgets)
               all-reachable-widgets)))))

(defn- filter-by-window
  [window-key widgets]
  (into {} (filter (comp #(= % window-key) :window :props val) widgets)))

(defn- get-windows
  [widgets]
  (into {} (filter (comp :rendering-hints :props val) widgets)))

(defn- refresh-window!
  [window-key before after]
  (let [window-before (get before window-key)
        window-after (get after window-key)
        window-after (if (and (seq window-after) (not= window-before window-after))
                       (before-drawing window-after)
                       window-after)
        after (if (seq window-after) (assoc after window-key window-after) after)]
    (if-let [window (get after window-key)]
      (try
        (let [context (-> window :context)
              before (filter-by-window window-key before)
              widgets-after (atom (filter-by-window window-key after))
              updated-keys (if (and (seq window-after) (not= window-before window-after))
                             (keys @widgets-after)
                             (updated-widgets->keys before @widgets-after))
              added-keys (added-widgets->keys before @widgets-after)
              removed-keys (removed-widgets->keys before @widgets-after)
              neighbour-keys (select-neighbouring-keys context @widgets-after (s/union updated-keys added-keys removed-keys))]
          (c/use-buffer-> context
                          (when (and (seq window-after) (not= window-before window-after))
                            (draw window context))
                          (doseq [to-hide (vals (select-keys before (s/union updated-keys removed-keys)))]
                            (when (-> to-hide :props :can-hide?)
                              (hide! to-hide context)))
                          (when-let [widgets-to-draw (vals (select-keys @widgets-after neighbour-keys))]
                            (let [widgets-to-draw (map before-drawing widgets-to-draw)]
                              (draw-widgets! context widgets-to-draw)
                              (let [widgets-to-draw (map after-drawing widgets-to-draw)
                                    wdgs-after (merge-with into @widgets-after (mapcat #(merge {(:name %) %}) widgets-to-draw))]
                                (reset! widgets-after wdgs-after)))))
          (merge after @widgets-after))
        (catch Exception e
          (println "Failed to update widgets, perhaps the given function" \newline
                   "doesn't take or doesn't return a widgets map." \newline
                   "Exception: " (.getMessage e) \newline
                   (clojure.stacktrace/print-stack-trace e))
          after))
      after)))

(defn- refresh-windows!
  [before f]
  (try
   (let [after (f before)]
     (loop [windows (get-windows after)
            widgets after]
       (if (seq windows)
         (recur (rest windows) (refresh-window! (-> windows first key) before widgets))
         widgets))) 
    (catch Exception e
      (clojure.stacktrace/print-stack-trace e)
      before)))

(defn swap-widgets!
  "Swaps out the widgets using the given function.
   f - function that takes a widgets map and returns a new widgets map"
  [f]
  (send state update :widgets refresh-windows! f))

(defn trigger-custom-event
  [widgets {:keys [action widget] :as event-attr}]
  (if-let [event-fn (-> widget :events action)]
    (event-fn widgets event-attr)
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
  [widgets window-name x y]
  (swap! previously assoc :key-code nil)
  (if-let [window (get widgets window-name)]
    (let [;; get the first widget that is on top close to the mouse position
          context (-> window :context)
          widget (first (reverse (sort-by #(-> % :props :z) (filter #(within? (coord % context) x y) (vals (select-keys widgets (window-key->widgets widgets window-name)))))))
          clicked (when (seq widget) (:name widget))
          widgets (assoc-arg-for-all widgets :selected? nil)]
      (if (and clicked (not (-> (get widgets clicked) :props :resizing?)))
        (let [widgets (assoc-in widgets [clicked :props :selected?] true)
              widgets (widget-event widgets {:action :mouse-clicked :widget (get widgets clicked) :x x :y y :window-name window-name})
              widgets (trigger-custom-event widgets {:action :mouse-clicked :widget (get widgets clicked) :x x :y y :window-name window-name})]
          widgets)
        widgets))
    widgets))

(defn handle-mouse-dragged
  [widgets window-name x y x-prev y-prev]
  (if-let [window (get widgets window-name)]
    (if-let [widget (first (reverse (sort-by #(-> % :props :z) (filter #(within? (coord % (-> window :context)) x y) (vals (select-keys widgets (window-key->widgets widgets window-name)))))))]
      (let [widgets (if (-> widget :props :resizing?)
                      (update widgets (:name widget) handle-widget-resizing x y x-prev y-prev)
                      (if (-> widget :props :can-move?)
                        (update widgets (:name widget) handle-widget-dragging x y x-prev y-prev)
                        widgets))
            widgets (widget-event widgets {:action :mouse-dragged :widget (get widgets (:name widget)) :x x :y y :x-prev x-prev :y-prev y-prev :window-name window-name})]
        (trigger-custom-event widgets {:action :mouse-dragged :widget (get widgets (:name widget)) :x x :y y :x-prev x-prev :y-prev y-prev :window-name window-name}))
      widgets)
    widgets))

(defn handle-mouse-moved 
  [widgets window-name x y]
  (if-let [current-widgets (seq (window-key->widgets widgets window-name))]
    (let [context (-> (get widgets window-name) :context)
          widget (first (reverse (sort-by #(-> % :props :z) (filter #(within? (coord % context) x y) (vals (select-keys widgets current-widgets))))))
        ;; if the mouse is not on a widget, check previously focused widgets, trigger events and unfocus them
          widgets (if-let [focused-widgets (get-with-property (vals widgets) :focused?)]
                            (loop [remaining-focused focused-widgets
                                   widgets widgets]
                              (if (seq remaining-focused)
                                (recur (rest remaining-focused) (let [name (first remaining-focused)
                                                                      widgets (assoc-in widgets [name :props :focused?] nil)
                                                                      widgets (assoc-in widgets [name :props :resizing?] nil)
                                                                      widgets (widget-event widgets {:action :widget-focus-out :widget (get widgets name) :x x :y y :window-name window-name})
                                                                      widgets (trigger-custom-event widgets {:action :widget-focus-out :widget (get widgets name) :x x :y y :window-name window-name})]
                                                                  widgets))
                                widgets))
                            widgets)]
      (if (seq widget)
      ;; if the mouse is on a widget, focus it and trigger events in case it wasn't focused before, check if it should resize
        (let [widgets (widget-event widgets {:action :mouse-moved :widget widget :x x :y y :window-name window-name})
              widgets (trigger-custom-event widgets {:action :mouse-moved :widget (get widgets (:name widget)) :x x :y y :window-name window-name})
              widget (get widgets (:name widget))
              widgets (if (-> widget :props :focused?)
                        widgets
                        (let [name (:name widget)
                              widgets (widget-event widgets {:action :widget-focus-in :widget (get widgets name) :x x :y y :window-name window-name})]
                          (trigger-custom-event widgets {:action :widget-focus-in :widget (get widgets name) :x x :y y :window-name window-name})))
              widgets (assoc-in widgets [(:name widget) :props :focused?] true)]
          (assoc-in widgets [(:name widget) :props :resizing?] (and (-> widget :props :can-resize?)
                                                                    (on-border? (coord (get widgets (:name widget)) (:context (get widgets (widget->window-key widgets (:name widget))))) x y))))
        widgets))
    widgets))

(defmethod c/handle-event :mouse-dragged [_ {:keys [x y window-name]}]
  (println "dragging")
  (let [[x-prev y-prev] (-> @previously :mouse-position (get window-name [0 0]))]
    (swap-widgets! #(let [widgets (handle-mouse-dragged % window-name x y x-prev y-prev)]
                      (widget-global-event widgets {:action :mouse-dragged :window-name window-name :x x :y y :x-prev x-prev :y-prev y-prev})))
    (swap! previously assoc-in [:mouse-position window-name] [x y])))

(defmethod c/handle-event :mouse-moved [_ {:keys [x y window-name]}]
  (swap-widgets! #(let [widgets (handle-mouse-moved % window-name x y)]
                    (widget-global-event widgets {:action :mouse-moved :window-name window-name :x x :y y})))
  (swap! previously assoc-in [:mouse-position window-name] [x y]))

(defmethod c/handle-event :mouse-pressed [_ {:keys [x y window-name]}]
  (println "pressed")
  (swap-widgets! #(let [widgets (handle-clicked % window-name x y)]
                    (widget-global-event widgets {:action :mouse-clicked :window-name window-name :x x :y y}))))

(defmethod c/handle-event :mouse-released [_ {:keys [x y window-name]}]
  (println "released")
  (swap-widgets! #(widget-global-event % {:action :mouse-released :window-name window-name :x x :y y})))

(defn handle-tabbing
  [widgets widget code]
  (if-let [window (widget->window widgets (:name widget))]
    (if (= code 9) ;;tab
      (let [context (-> window :context)
            previously-tabbed (:tabbed @previously)
            previously-tabbed (if (= (set (get-with-property (vals widgets) :can-tab?))
                                     (set previously-tabbed))
                                #{}
                                previously-tabbed)
            new-widget (:name (next-widget-to-tab context widgets previously-tabbed widget))
            widgets (assoc-arg-for-all widgets :selected? nil)]
        (when new-widget
          (let [previously-tabbed (s/union previously-tabbed #{new-widget})]
            (swap! previously assoc :tabbed previously-tabbed)))
        (assoc-in widgets [new-widget :props :selected?] (seq new-widget)))
      widgets)
    widgets))

(defn handle-key-pressed
  [widgets window-name char code]
  (let [previous-code (-> @previously :key-code)
        widgets (widget-global-event widgets {:action :key-pressed :window-name window-name :char char :code code :previous-code previous-code})]
    (swap! previously assoc :key-code code)
    (if-let [widget (first (reverse (sort-by #(-> % :props :z) (filter #(-> % :props :selected?) (vals (select-keys widgets (window-key->widgets widgets window-name)))))))]
      (let [widgets (handle-tabbing widgets widget code)
            widgets (widget-event widgets {:action :key-pressed :widget (get widgets (:name widget)) :char char :code code :previous-code previous-code :window-name window-name})]
        (trigger-custom-event widgets {:action :key-pressed :widget (get widgets (:name widget)) :char char :code code :previous-code previous-code :window-name window-name}))
      (if-let [tabable (first (get-with-property (vals widgets) :can-tab? true))]
        (handle-tabbing widgets (get widgets tabable) code) 
        widgets))))

(defmethod c/handle-event :key-pressed [_ {:keys [char code window-name]}]
  (swap-widgets! #(handle-key-pressed % window-name char code)))
