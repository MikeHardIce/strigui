(ns strigui.core
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string]
   [strigui.button]
   [strigui.label]
   [strigui.list]
   [strigui.input :as inp]
   [capra.core :as c]
   [strigui.widget :as wdg])
  (:import [strigui.button Button]
           [strigui.label Label]
           [strigui.list List]
           [strigui.input Input]
           [java.awt Color RenderingHints]))

(set! *warn-on-reflection* true)

(defn swap-widgets!
  [f]
  "Used to add or modify widgets"
  ;; TODO: Some checks
  (wdg/swap-widgets! f))

(defn get-widget-names-by-group
  "Retuns a vector of widgets by group name"
  [widgets name]
  (let [get-seq (fn [x] (if (string? x) (vector x) x))
        filter-crit (fn [x] (some #(= name %) (-> x val :props :group get-seq)))]
    (map :name (vals (filter filter-crit widgets)))))

(defn remove-widgets-by-group
  "Removes all widgets assigned to the given group"
  [widgets name]
  (apply dissoc widgets (get-widget-names-by-group widgets name)))

(defn assoc-property
  "Assoc a value to a particular widget property for multiple widgets given by widget name"
  [widgets property-key property-value & widget-names]
  (loop [names widget-names
         widgets widgets]
    (if-not (seq names)
      widgets
      (recur (rest names)
             (assoc-in widgets [(first names) :props property-key] property-value)))))

(defn attach-event 
  "Attach an event to a widget by widget name
   widgets - current collection of widgets
   name - name of the widget to attach the event
   event - one of :mouse-clicked :mouse-moved :key-pressed :widget-focus-in :widget-focus-out
   f - fn to handle the event with the following props: 
   :mouse-clicked -> widgets widget
   :mouse-moved -> widgets widget x y
   :key-pressed -> widgets widget key-code
   :widget-focus-in -> widgets widget x y
   :widget-focus-out -> widgets widget x y"
  [widgets name event f]
  (assoc-in widgets [name :events event] f))

(defn arrange
  "Arranges the widgets given via widget names 
   by the given grid
   skip-after - indicates how many items should be displayed per row
   {:from [x y]
   :space [between-space-horizontal between-space-vertical]
   :align :left :center :right
   Example: 2 by 3 grid that starts at x: 100 y: 50
   (-> wdgs
   ...
   (arrange 2 {:from [100 50]} \"label1\" \"button1\" \"label2\" \"button2\" \"label3\" \"button3\"))"
  ([widgets names] (apply arrange widgets (count names) {} names))
  (^{:pre [#(> % 0)]} 
   [widgets skip-after {:keys [from space align] :or {from [0 0]
                                                     space [35 15]
                                                      align :left}} & names] 
   (let [name-groups (partition-all skip-after (->> (select-keys widgets names)
                                                    vals
                                                    (map (fn [widget]
                                                           (vec (cons (:name widget) (vals (select-keys (:props widget) [:x :y :width :height]))))))))
         max-width (apply max (flatten (for [group name-groups]
                                         (+ (apply + (map (comp second reverse) group)) (* (count group) (first space))))))]
     (loop [name-groups name-groups
            widgets widgets
            height (second from)]
       (if-not (seq name-groups)
         widgets
         (let [row-width (apply + (map (comp second reverse) (first name-groups)))
               start-on-x (case align 
                            :left (first from)
                            :right (- (+ (first from) max-width) row-width)
                            :center (- (+ (first from) (/ max-width 2)) (/ row-width 2)))
               coords (reduce (fn [prev [name _ _ w h]]
                                (let [[_ x0 y0 w0 _] (last prev)]
                                  (concat prev [[name (+ x0 w0 (first space))
                                                 y0 w h]])))
                              (let [[name _ _ w h] (-> name-groups first first)]
                                [[name start-on-x height w h]])
                              (-> name-groups first rest))
               max-height (apply max (map last coords))]
           (recur (rest name-groups)
                  (loop [gr coords
                         wdgs widgets]
                    (if-not (seq gr)
                      wdgs
                      (let [[name x y w h] (first gr)]
                        (recur (rest gr)
                               (-> wdgs
                                   (assoc-in [name :props :x] x)
                                   (assoc-in [name :props :y] y)
                                   (assoc-in [name :props :width] w)
                                   (assoc-in [name :props :height] h))))))
                  (+ height max-height (last space)))))))))

(defn window!
  "Initializes a new window or reuses an existing one
   context - an already existing context (experimental)
   x - x position on the screen
   y - y position on the screen
   width - width of the window
   height - height of the window
   title - name displayed in the title bar of the window
   color - java.awt.Color of the windows background color
   rendering-hints - map of java.awt.RenderingHints key value combinations to configure the rendering quality
   of any widget drawn within the window"
  ([context] 
   (swap! wdg/state assoc :context context))
  ([x y width height title]
   (window! x y width height title (java.awt.Color. 44 44 44) {java.awt.RenderingHints/KEY_ANTIALIASING java.awt.RenderingHints/VALUE_ANTIALIAS_ON}))
  ([x y width height title color]
   (window! x y width height title color {java.awt.RenderingHints/KEY_ANTIALIASING java.awt.RenderingHints/VALUE_ANTIALIAS_ON}))
   ([x y width height title color rendering-hints]
    (swap! wdg/state assoc :context (c/create-window x y width height title (eval color)))
    (swap! wdg/state assoc-in [:context :canvas :rendering] rendering-hints)
    (swap! wdg/state update-in [:context :canvas] c/attach-buffered-strategy 2)
    (:context @wdg/state)))
  
(defn add 
  "Adds the given widget to the map of widgets and runs defaults and dimension adjusting function"
  ([widgets ^strigui.widget.Widget widget] (add widgets widget (:color wdg/widget-default-props)))
  ([widgets ^strigui.widget.Widget widget color-profile]
  (let [canvas (-> @wdg/state :context :canvas)
        widget (update widget :props merge wdg/widget-default-props
                       (-> widget (assoc-in [:props :color] color-profile) :props)
                       (-> widget :props))
        widget (->> widget
                    (wdg/adjust-dimensions canvas)
                    (wdg/defaults))]
    (assoc widgets (:name widget) widget))))

(defmacro add-multiple
  [wdgs type & names]
  `(-> ~wdgs
         ~@(for [pair (partition 2 names)]
             `(add (clojure.lang.Reflector/invokeConstructor ~type (into-array Object [(first '~pair) (second '~pair) {:x 0 :y 0}]))))))

(defn close-window!
  "Closes the current active window."
  []
  (c/close-window (-> @wdg/state :context :window)))

(defn add-button
  "Adds a button widget to the given map of widgets.
   widgets - map of widgets
   name - name of the element
   text - text displayed inside the button
   props - map of:
     x - x coordinate of top left corner
     y - y coordinate of top left corner
     color - vector consisting of [background-color font-color]
     min-width - the minimum width"
  [widgets name text props]
  (add widgets (Button. name text props)))

(defn add-label
  "Adds a label widget to the given map of widgets.
    widgets - map of widgets
    name - name of the element
    text - text displayed inside the button
    props - map of:
     x - x coordinate of top left corner
     y - y coordinate of top left corner
     color - vector consisting of [font-color]
     font-style - vector consisting of either :bold :italic :italic-bold
     font-size - number"
  [widgets name text props]
  (add widgets (Label. name text props)))

(defn add-input
  "Adds a imput widget to the map of widgets.
   widgets - map of widgets
   name - name of the element
   text - text displayed inside the button
   props - map of:
    x - x coordinate of top left corner
    y - y coordinate of top left corner
    color - vector consisting of [background-color font-color]
    min-width - the minimum width"
  [widgets name text props]
  (add widgets (inp/Input. name text props)))

(defn add-list
  "Adds a list widget holding on a vector of items to the map of widgets.
   widgets - map of widgets
   name - name of the list
   items - vector of items [{:value bla} {:value bla} ...]
           items can be maps and should at least contain a :value"
  [widgets name items props]
  (add widgets (List. name items props)))

(defn from-map!
  "Initializes the window and the widgets from a map"
  [strigui-map]
  (when-let [window-props (:window strigui-map)]
    (apply window! window-props))
  (let [exprs (for [widget-key (filter #(not= % :window) (keys strigui-map))]
                (for [widget-props (map identity (widget-key strigui-map))]
                  (str "(apply " (namespace widget-key) "/->" (name widget-key) " " (vec widget-props) ")")))
        widgets (->> exprs
                     (mapcat identity)
                     (map #(-> %
                               read-string
                               eval)))]
    (swap-widgets! (fn [wdgs]
                     (loop [to-be widgets
                            wdgs wdgs]
                       (if (seq to-be)
                         (recur (rest to-be) (add wdgs (first to-be)))
                         wdgs))))))

(defn from-file!
  "Initializes the window and the widgets from a edn file"
  [file-name]
  (when (.exists (io/file file-name))
    (->> (slurp file-name)
         edn/read-string
         from-map!)))

(defn extract-rgb-constructors
  [rgb-string]
  (when (seq rgb-string)
    (let [colors (re-seq #"r=\d{1,3},g=\d{1,3},b=\d{1,3}" rgb-string)
          colors (map #(re-seq #"\d{1,3}" %) colors)]
      (vec (map (fn [[r g b]]
                  `(java.awt.Color. ~(Integer/parseInt r) ~(Integer/parseInt g) ~(Integer/parseInt b)))
                colors)))))

(defn to-map
  "converts the current state to a map that could be stored in a file"
  []
  (let [{:keys [x y width height name color]} (c/properties (-> @wdg/state :context))
        strigui-map {:window [x y width height name (first (extract-rgb-constructors (str color)))]}
        widgets-grouped (group-by #(class %) (vals (-> @wdg/state :widgets)))
        widget-types (keys widgets-grouped)
        widget-types (map #(let [parts (clojure.string/split (clojure.string/replace-first (str %) #"class " "") #"\.")
                                 cl (last parts)
                                 n-space (filter (fn [part] (not= part cl)) parts)]
                             [% (keyword (str (clojure.string/join "." n-space) "/" cl))]) widget-types)
        widget-map (loop [w-types widget-types
                    w-map {}]
               (if (seq w-types)
                 (let [cur-key (first w-types)
                       current-widgets (get widgets-grouped (first cur-key))
                       current-widgets (map #(assoc-in % [:props :color] (extract-rgb-constructors (str (-> % :props :color)))) current-widgets)]
                   (recur (rest w-types) (merge w-map {(second cur-key)
                                                       (mapv #(vec (vals (select-keys % (filter (fn [k] (not= k :events)) (keys %))))) current-widgets)})))
                 w-map))
        strigui-map (merge strigui-map widget-map)]
    strigui-map))

(defn to-file
  "Writes the current state of strigui into a edn file"
  [file-name]
  (when (.exists (io/file file-name))
    (io/delete-file file-name))
  (->> (to-map)
       str
       (#(clojure.string/replace % #"]]," "]]\n"))
       (#(clojure.string/replace % #"," ""))
       (spit file-name)))