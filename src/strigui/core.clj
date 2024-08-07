(ns strigui.core
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string] 
   [strigui.button]
   [strigui.checkbox]
   [strigui.image]
   [strigui.label]
   [strigui.list]
   [strigui.input :as inp]
   [strigui.window :as wnd]
   [capra.core :as c]
   [strigui.widget :as wdg]
   [clojure.set :as s]) 
  (:import [strigui.button Button]
           [strigui.checkbox Checkbox]
           [strigui.label Label]
           [strigui.list List]
           [strigui.input Input]
           [strigui.image Image]
           [java.awt Color RenderingHints]))

(set! *warn-on-reflection* true)

(defonce ^:const exit c/exit)
(defonce ^:const hide c/hide)

(defn inspect-widgets
  "Returns the entire widget map."
  []
  (:widgets (@wdg/state)))

(defn swap-widgets!
  [f]
  "Used to add or modify widgets"
  ;; TODO: Some checks
  (wdg/swap-widgets! f))

(defn get-widget-names-by-group
  "Retuns a vector of widget names of widgets that belong to the given group name"
  [widgets group-name]
  (let [get-seq (fn [x] (if (string? x) (vector x) x))
        filter-crit (fn [x] (some #(= group-name %) (-> x val :props :group get-seq)))]
    (mapv :name (vals (filter filter-crit widgets)))))

(defn get-widget-names-by-window
  "Returns a vector of widget names of widgets that are part of the window with the given window key"
  [widgets window-key]
  (wdg/window-key->widgets widgets window-key))

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
  (if (get widgets name)
    (assoc-in widgets [name :events event] f)
    (println "Cannot attach event " event " | Widget with name \"" name "\" doesn't exist")))

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

#_(defn align-horizontal
  "Aligns a widget or widgets relative to another widget horizontally, only changing its x-coordinate.
   For example:
   (align wdgs \"main-window\" :center \"a\" \"b\")
   would align the widget \"a\" and \"b\" to the horizontal center of the main window"
  [widgets reference-widget-name alignment & widget-names]
  widgets)

#_(defn align-vertical
  "Aligns a widget relative to another widget vertically, only changing its y-coordinate
   For example:
   (align-vertical wdgs \"main-window\" :center \"a\" \"b\")
   would align the widget \"a\" and \"b\" to the vertical center of the main window"
  [widgets reference-widget-name alignment & widget-names]
  widgets)

(defn change-color-profile
  "Changes the color profile of the window and all widgets
   color-profile is a map with keys:
  :background :background-widgets :text :focus :select :resize :border"
  [widgets window-name color-profile]
  (let [color-prof (dissoc (merge color-profile {:background (:background-widgets color-profile)}) :background-widgets)
        widgets (loop [wdgs widgets
                       wdgs-keys (wdg/window-key->widgets wdgs window-name)]
                  (if-not (seq wdgs-keys)
                    wdgs
                    (recur (assoc-in wdgs [(first wdgs-keys) :props :color] color-prof) (rest wdgs-keys))))]
    (assoc-in widgets [window-name :props :color] color-profile)))

(defn add-window
  "Creates a new window widget with the parameters:
   widgets - current map of widgets
   name - widget name
   x - x position on the screen
   y - y position on the screen
   width - width of the window
   height - height of the window
   title - name displayed in the title bar of the window
   props - map of properties like color
   color - java.awt.Color of the windows background color
   rendering-hints - map of java.awt.RenderingHints key value combinations to configure the rendering quality
   of any widget drawn within the window"
  [widgets name x y width height title props]
  (let [props (update props :color merge (:color props)
                      {:background (java.awt.Color. 250 250 250) :background-widgets (java.awt.Color. 250 250 250 250)})
        window-exists? (get widgets name)]
    (assoc widgets name (if window-exists?
                          (wnd/window-from-context (:context window-exists?) name x y width height title props)
                          (wnd/window name x y width height title props))))) 

(defn add 
  "Adds the given widget to the map of widgets and runs defaults and dimension adjusting function"
  ([widgets window-key ^strigui.widget.Widget widget] (add widgets window-key widget (:color wdg/widget-default-props)))
  ([widgets window-key ^strigui.widget.Widget widget color-profile]
  (let [window (get widgets window-key)
        context (-> window :context)
        window-color-profile (-> window :props :color)
        window-color-profile (-> window-color-profile
                                 ((fn [p]
                                    (assoc p :background (:background-widgets p))))
                                 (dissoc :background-widgets))
        widget (update-in widget [:props :color] merge
                          (:color wdg/widget-default-props)
                          color-profile
                          window-color-profile
                          (-> widget :props :color))
        widget (update widget :props merge wdg/widget-default-props (:props widget))
        widget (assoc-in widget [:props :window] window-key)
        widget (->> widget
                    (wdg/adjust-dimensions context)
                    (wdg/defaults))]
    (assoc widgets (:name widget) widget))))

(defmacro add-multiple
  [wdgs window-key type & names]
  `(-> ~wdgs
         ~@(for [pair (partition 2 names)]
             `(add ~window-key (clojure.lang.Reflector/invokeConstructor ~type (into-array Object [(first '~pair) (second '~pair) {:x 0 :y 0}]))))))

(defn close-window!
  "Closes the given window."
  [widgets window-key] 
  (if-let [widget (get widgets window-key)]
    (c/close-window (:context widget))
    (println "Cannot close window | Window with name \"" window-key "\" doesn't exist")) 
  widgets)

(defn add-checkbox 
  [widgets window-key name status props]
  (add widgets window-key (Checkbox. name status props)))

(defn add-radio
  [widgets window-key name status props]
  (add widgets window-key (Checkbox. name status (assoc props :type :radio))))

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
  [widgets window-key name text props]
  (add widgets window-key (Button. name text props)))

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
  [widgets window-key name text props]
  (add widgets window-key (Label. name text props)))

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
  [widgets window-key name text props]
  (add widgets window-key (inp/Input. name text props)))

(defn add-list
  "Adds a list widget holding on a vector of items to the map of widgets.
   widgets - map of widgets
   name - name of the list
   items - vector of items [{:value bla} {:value bla} ...]
           items can be maps and should at least contain a :value"
  [widgets window-key name items props]
  (add widgets window-key (List. name items props)))

(defn add-image
  ""
  [widgets window-key name path props]
  (add widgets window-key (Image. name path props)))

(defn from-map!
  "Initializes the window and the widgets from a map"
  [strigui-map]
  (when-let [windows (:window strigui-map)]
    (doseq [window-props windows]
      (let [[name {:keys [x y width height title] :as props}] window-props]
        (swap-widgets! #(add-window % name x y width height title (eval props))))))
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
                         (recur (rest to-be) (add wdgs (-> to-be first :props :window) (first to-be)))
                         wdgs))))))

(defn- resolve-window-operation [key]
  (case key
    hide hide
    exit exit))

(defn from-file!
  "Initializes the window and the widgets from a edn file"
  [file-name]
  (when (.exists (io/file file-name))
    (->> (slurp file-name)
         (edn/read-string {:readers {'window resolve-window-operation}})
         from-map!)))

(defn java-color->rgb-constructors
  "Converts a java color object like #object[java.awt.Color 0x19bd601 \"java.awt.Color[r=250,g=250,b=250]\"]
   into a constructure that can be called from within clojure like (java.awt.Color. 250 250 250)"
  [java-color]
  (let [rgb-string (str java-color)]
    (when (seq rgb-string)
      (let [colors (re-seq #"r=\d{1,3},g=\d{1,3},b=\d{1,3}" rgb-string)
            colors (map #(re-seq #"\d{1,3}" %) colors)
            [[r g b]] colors]
        `(java.awt.Color. ~(Integer/parseInt r) ~(Integer/parseInt g) ~(Integer/parseInt b))))))

(defn extract-color-map
  "Converts a strigui color map consisting of java color objects into a color map consisting of rgb constructors"
  [colors]
  (loop [colors colors
         color-keys (keys colors)]
    (if-not (seq color-keys)
      colors
      (let [next-key (first color-keys)
            java-color (get colors next-key)]
        (recur (assoc colors next-key (java-color->rgb-constructors java-color))
               (rest color-keys))))))

(defn convert-for-export
  "groups all widgets by their widget type and does some conversion to not print the plain java objects"
  [strigui-map]
  (let [windows (->> strigui-map :widgets vals (filter (fn [wdg] (-> wdg (get :context {}) :canvas))))
        window (for [window windows]
                 (let [window (-> window 
                                  (update-in [:props :color] extract-color-map)
                                  (update :props dissoc :rendering-hints :source-object-changed?)
                                  (update-in [:props :on-close] #(case (int (parse-long (str %)))
                                                                   1 (edn/read-string {:default tagged-literal} "#window hide")
                                                                   3 (edn/read-string {:default tagged-literal} "#window exit"))))]
                   [(:name window) (:props window)]))
        strigui-tmp-map {:window (vec window)}
        widgets-grouped (group-by #(class %) (vals (filter #(empty? (s/intersection #{(-> % val :name)} (set (mapv :name windows)))) (-> strigui-map :widgets))))
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
                             current-widgets (map #(update-in % [:props :color] extract-color-map) current-widgets)]
                         (recur (rest w-types) (merge w-map {(second cur-key)
                                                             (mapv #(vec (vals (select-keys % (filter (fn [k] (not= k :events)) (keys %))))) current-widgets)})))
                       w-map))]
    (merge strigui-tmp-map widget-map)))

(defn to-file
  "Writes the current state of strigui into a edn file"
  [file-name]
  (when (.exists (io/file file-name))
    (io/delete-file file-name))
  (->> @wdg/state
       convert-for-export
       str
       (#(clojure.string/replace % #"] " "]\n"))
       (#(clojure.string/replace % #"]]" "]]\n"))
       (spit file-name))
  #_(pprint (convert-for-export @wdg/state) (clojure.java.io/writer file-name)))

(convert-for-export @wdg/state)