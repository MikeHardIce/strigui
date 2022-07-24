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

(defn swap-widgets!
  [f]
  "Used to add or modify widgets"
  ;; TODO: Some checks
  (wdg/swap-widgets! f))

(defn find-widgets-by-group-name
  "Retuns a vector of widgets by group name"
  [widgets name]
  (let [get-seq (fn [x] (if (string? x) (vector x) x))
        filter-crit (fn [x] (some #(= name %) (-> x val :args :group get-seq)))]
    (vals (filter filter-crit widgets))))

(defn remove-widget-group
  "Removes all widgets assigned to the given group"
  [widgets name]
  (apply dissoc widgets (map :name (find-widgets-by-group-name widgets name))))

(defn attach-event 
  "Attach an event to a widget by widget name
   widgets - current collection of widgets
   name - name of the widget to attach the event
   event - one of :mouse-clicked :mouse-moved :key-pressed :widget-focus-in :widget-focus-out
   f - fn to handle the event with the following args: 
   :mouse-clicked -> widgets widget
   :mouse-moved -> widgets widget x y
   :key-pressed -> widgets widget key-code
   :widget-focus-in -> widgets widget x y
   :widget-focus-out -> widgets widget x y"
  [widgets name event f]
  (assoc-in widgets [name :events event] f))

(defn arrange
  "Arrange size and position of selected widgets 
   via their name using a options map consisting of
   {:from [x y]
   :to [x y]
   :align with :left :right or :center}
   Example: 
   (-> wdgs
   ...
   (arrange {:from [100 50]} \"label1\" \"button1\" \"button2\"))
   "
  ([widgets options] (apply arrange widgets options (keys widgets)))
  ([widgets {:keys [from to align] :or {from [0 0]
                                           to [0 0]
                                           align :left} :as options} & names]
  widgets))


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
   (window! x y width height title Color/white {java.awt.RenderingHints/KEY_ANTIALIASING java.awt.RenderingHints/VALUE_ANTIALIAS_ON}))
  ([x y width height title color]
   (window! x y width height title color {java.awt.RenderingHints/KEY_ANTIALIASING java.awt.RenderingHints/VALUE_ANTIALIAS_ON}))
   ([x y width height title color rendering-hints]
    (swap! wdg/state assoc :context (c/create-window x y width height title (eval color)))
    (swap! wdg/state assoc-in [:context :canvas :rendering] rendering-hints)
    (swap! wdg/state update-in [:context :canvas] c/attach-buffered-strategy 2)
    (:context @wdg/state)))
  
(defn add 
  "Adds the given widget to the map of widgets and runs defaults and dimension adjusting function"
  [widgets ^strigui.widget.Widget widget]
  (let [canvas (-> @wdg/state :context :canvas)
        widget (update widget :args merge wdg/widget-default-args (->> widget
                                                                       (wdg/adjust-dimensions canvas)
                                                                       (wdg/defaults)
                                                                       :args))]
    (assoc widgets (:name widget) widget)))

;; (defmacro add-multiple
;;   [type & names]
;;   `(fn [wdgs#]
;;     (-> wdgs#
;;         ~@(for [pair (partition 2 names)]
;;             `(add (clojure.lang.Reflector/invokeConstructor ~type (into-array Object [(first '~pair) (second '~pair) {:x 0 :y 0}])))))))

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
   args - map of:
     x - x coordinate of top left corner
     y - y coordinate of top left corner
     color - vector consisting of [background-color font-color]
     min-width - the minimum width"
  [widgets name text args]
  (add widgets (Button. name text args)))

(defn add-label
  "Adds a label widget to the given map of widgets.
    widgets - map of widgets
    name - name of the element
    text - text displayed inside the button
    args - map of:
     x - x coordinate of top left corner
     y - y coordinate of top left corner
     color - vector consisting of [font-color]
     font-style - vector consisting of either :bold :italic :italic-bold
     font-size - number"
  [widgets name text args]
  (add widgets (Label. name text args)))

(defn add-input
  "Adds a imput widget to the map of widgets.
   widgets - map of widgets
   name - name of the element
   text - text displayed inside the button
   args - map of:
    x - x coordinate of top left corner
    y - y coordinate of top left corner
    color - vector consisting of [background-color font-color]
    min-width - the minimum width"
  [widgets name text args]
  (add widgets (inp/Input. name text args)))

(defn add-list
  "Adds a list widget holding on a vector of items to the map of widgets.
   widgets - map of widgets
   name - name of the list
   items - vector of items [{:value bla} {:value bla} ...]
           items can be maps and should at least contain a :value"
  [widgets name items args]
  (add widgets (List. name items args)))

(defn from-map!
  "Initializes the window and the widgets from a map"
  [strigui-map]
  (when-let [window-args (:window strigui-map)]
    (apply window! window-args))
  (let [exprs (for [widget-key (filter #(not= % :window) (keys strigui-map))]
                (for [widget-args (map identity (widget-key strigui-map))]
                  (str "(apply " (namespace widget-key) "/->" (name widget-key) " " (vec widget-args) ")")))
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
                       current-widgets (map #(assoc-in % [:args :color] (extract-rgb-constructors (str (-> % :args :color)))) current-widgets)]
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