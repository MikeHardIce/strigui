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
           [java.awt Color]))

(defn swap-widgets!
  [f]
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
  (apply dissoc widgets (find-widgets-by-group-name widgets name)))

(defn attach-event 
  [widgets name event f]
  (assoc-in widgets [name :events event] f))

;; (defn update-group!
;;   "Update all widgets that are part of the given group.
;;    name - name of the group
;;    key - either simple key or vector of keys
;;    value - the new value of the key/key path"
;;   [group-name key value & kvs]
;;   (when-let [widgets (find-by-group group-name)]
;;     (loop [widgets widgets]
;;       (when (seq widgets)
;;         (update-widget-multiple-keys! false (first widgets) key value (when kvs @kvs))
;;         (recur (rest widgets))))
;;     widgets))

;; (defn update-group-skip-redraw!
;;   "Update all widgets that are part of the given group but skip redrawing the widgets of that group.
;;    name - name of the group
;;    key - either simple key or vector of keys
;;    value - the new value of the key/key path"
;;   [name key value & kvs]
;;   (when-let [widgets (find-by-group name)]
;;     (loop [widgets widgets]
;;       (when (seq widgets)
;;         (update-widget-multiple-keys! true (first widgets) key value (when kvs @kvs))
;;         (recur (rest widgets))))
;;     widgets))

(defn window!
  "Initializes a new window or reuses an existing one
   wind - an already existing windows instance (experimental)
   width
   height
   fps -  frames per second
   quality - rendering quality :low :mid :high :highest"
  ([context] 
   ;;(swap! wdg/state assoc :context (wnd/init-window wind)))
   (swap! wdg/state assoc :context context))
  ([x y width height title]
   ;;(swap! wdg/state assoc :context (wnd/init-window width height title)))
   (window! x y width height title Color/white))
   ([x y width height title color]
      (swap! wdg/state assoc :context (c/create-window x y width height title (eval color)))))
  
(defn add 
  "Adds the given widget to the widgets map and runs defaults and dimension adjusting function"
  [widgets ^strigui.widget.Widget widget]
  (let [canvas (-> @wdg/state :context :canvas)
        widget (->> widget
                    (wdg/adjust-dimensions canvas)
                    (wdg/defaults))]
    (assoc widgets (:name widget) widget)))

(defn close-window
  "Closes the current active window."
  []
  (c/close-window (-> @wdg/state :context :window)))

(defn add-button
  [widgets name text args]
  (add widgets (Button. name text args)))

(defn add-label
  [widgets name text args]
  (add widgets (Label. name text args)))

(defn add-input
  [widgets name text args]
  (add widgets (inp/Input. name text args)))

(defn add-list
  [widgets name items args]
  (add widgets (List. name items args)))

(defn create! 
  "Register and show a custom widget.
   Registering a component with the same name will replace the existing component with the new one."
  [^strigui.widget.Widget widget]
  ;;(remove! (:name widget))
  (let [canvas (-> @wdg/state :context :canvas)
        widget (wdg/adjust-dimensions canvas widget)
        widget (wdg/defaults widget)
        neighbours (wdg/all-neighbouring-widgets canvas widget (->> @wdg/state :widgets vals) >)
        neighbours (sort-by #(-> % :args :z) neighbours)]
    (apply wdg/redraw! canvas neighbours)
    (wdg/register! canvas widget)))

(defn button!
  "Create a simple button on screen.
   name - name of the element
   text - text displayed inside the button
   args - map of:
     x - x coordinate of top left corner
     y - y coordinate of top left corner
     color - vector consisting of [background-color font-color]
     min-width - the minimum width"
  [name text args]
  (create! (Button. name text args)))

(defn label!
   "Create a simple label on screen.
    name - name of the element
    text - text displayed inside the button
    args - map of:
     x - x coordinate of top left corner
     y - y coordinate of top left corner
     color - vector consisting of [font-color]
     font-style - vector consisting of either :bold :italic :italic-bold
     font-size - number"
  [name text args]
  (create! (Label. name text args)))

(defn input!
  "Create a simple imput field on screen.
   name - name of the element
   text - text displayed inside the button
   args - map of:
    x - x coordinate of top left corner
    y - y coordinate of top left corner
    color - vector consisting of [background-color font-color]
    min-width - the minimum width"
  [name text args]
  (create! (inp/input name text args)))

(defn list! 
  "Create a simple list of items
   name - name of the list
   items - vector of items [{:value bla} {:value bla} ...]
           items can be maps and should at least contain a :value"
  [name items args]
  (create! (List. name items args)))

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