(ns strigui.list
  (:require [strigui.box :as b]
             [strigui.widget :as wdg]
             [capra.core :as c]))

(set! *warn-on-reflection* true)
(defonce item-height 25)
(defonce item-width-right-margin 10)

;; (defn make-darker-or-brighter
;;   [^java.awt.Color color]
;;   (let [hsl (java.awt.Color/RGBtoHSB (.getRed color) (.getGreen color) (.getBlue color) nil)]
;;     (if (> (get hsl 2) 0.8) (.darker color) (-> color (.brighter))))) ;; (.brighter) (.brighter) (.brighter) (.brighter)

(defn get-index-at 
  " index >= 0 for items, < 0 for header"
  [widget y]
  (let [index (int (/ (- y (-> widget :props :y)) item-height))]
    (if (-> widget :props :header)
      (dec index)
      index)))

(defn draw-list
  [canvas items props ^Integer max-columns]
  (let [items (filter :visible? items)]
    (loop [items items
           index 0]
      (when (seq items)
        (let [color (:color props)
              item (first items)
              color (if (or (:focused? item) (:selected? item)) (assoc color :background (:focus color)) color)]
          (if (vector? (:value item))
            (let [columns (count (:value item))
                  width (/ (- (:width props) item-width-right-margin) max-columns)
                  cells (map (fn [it ind] [it ind]) (vec (concat (:value item) (repeat (- max-columns columns) ""))) (range 0 max-columns))]
              (doseq [cell cells]
                (b/box-draw canvas (str (first cell)) {:x (+ (:x props) (* (second cell) width)) :y (+ (* index item-height) (:y props)) :width width :height item-height :color color})))
            (b/box-draw canvas (str (-> items first :value)) (merge
                                                              {:y (+ (* index item-height) (:y props)) :width (- (:width props) item-width-right-margin) :height item-height :color color}
                                                              (select-keys props [:width :x]))))
          (when (:selected? item)
            (c/draw-> canvas
                      (c/line (inc (:x props)) (+ (* index item-height) (:y props))
                              (inc (:x props)) (+ (* (inc index) item-height) (:y props))
                              (get color :text java.awt.Color/green) 3))))
        (recur (rest items) (inc index))))))

(defn clear-out
  [items property]
  (let [cleared-items (mapv #(merge % {property false}) items)]
    cleared-items))

(defn make-visible 
  ([widget]
  (let [max-visible-items (- (Math/floor (/ (-> widget :props :height) item-height)) (if (-> widget :props :header)
                                                                                       1
                                                                                       0))
        items (:items widget)
        widgets-visible (mapv #(merge % {:visible? true}) (take max-visible-items items))
        widgets-not-visible (take-last (- (count items) max-visible-items) items)]
    (assoc widget :items (apply conj widgets-visible widgets-not-visible))))
  ([widget y]
   (let [max-visible-items (- (Math/floor (/ (-> widget :props :height) item-height)) (if (-> widget :props :header)
                                                                                        1
                                                                                        0))
         items (:items widget)
         cnt-items (count items)
         index (int (/ (int (- y (-> widget :props :y))) item-height))
         until (+ index max-visible-items)
         diff (if (> until cnt-items) (- until cnt-items) 0)
         index (int (if (< (- index diff) 0) 0 (- index diff)))
         widget (update widget :items (fn [items] (mapv #(merge % {:visible? false}) items)))
         items (loop [items (:items widget)
                      ind 0]
                 (if (< ind max-visible-items)
                     (recur (assoc-in items [(+ index ind) :visible?] true)
                            (inc ind))
                   items))]
     (assoc widget :items items))))


(defrecord List [name items props]
  wdg/Widget
  (coord [this _] (let [{:keys [x y width height]} (:props this)]
                         [x y width height]))
  (defaults [this] (make-visible this))
  (before-drawing [this] (if (< (count (filter :visible? (:items this)))
                                (- (Math/floor (/ (-> this :props :height) item-height)) (if (-> this :props :header)
                                                                                           1
                                                                                           0)))
                           (make-visible this)
                           this))
  (draw [this canvas]
        (let [{:keys [x y width height color header] :as props} (:props this)
              bar-x (+ x (- width item-width-right-margin))
              items (:items this)
              items-visible (filter :visible? items)
              bar-ratio (/ (count items-visible) (if (seq items) (count items) 1))
              first-items-hidden (take-while #(not (:visible? %)) items)
              bar-scroll-height (* bar-ratio height)
              bar-y (+ y (/ (* bar-scroll-height (count first-items-hidden)) (if (seq items-visible) (count items-visible) 1)))]
          (c/draw-> canvas
                    (c/rect x y width height (:background color) false)
                    (c/rect bar-x y item-width-right-margin height (:background color) true)
                    (c/rect bar-x bar-y item-width-right-margin bar-scroll-height (:border color) true)) ;;(make-darker-or-brighter (:text color))
          (if-not header
            (draw-list canvas items props (apply max (map #(-> % :value count) items)))
            (let [columns (count header)]
              (draw-list canvas [{:value (mapv (fn [head]
                                                 (let [name (:value head)
                                                       suffix (case (:action head)
                                                                :sort \u2191
                                                                :sort-asc \u2191 
                                                                :sort-desc \u2193
                                                                "")]
                                                   (str name " " suffix))) header) :visible? true}] props columns)
              (draw-list canvas items (update props :y (partial + item-height)) columns)))
          this))
  (after-drawing [this] this))

(defn activate!
  [widget y property]
  (let [index (get-index-at widget y)]
    (if (> index -1)
      (let [index (get-index-at widget y)
            items (:items widget)
            item (get (filterv :visible? items) index)
            items-before (take-while #(not (:visible? %)) items)
            index (+ (count items-before) index)]
        (if (seq item)
          (-> widget
              (update :items clear-out property)
              (assoc-in [:items index property] true))
          widget))
      widget)))

(defn partition-and-sort-by-numeric-and-non-numeric-chars
  [items index]
  (let [groups (group-by (fn [item]
                           (some #(Character/isLetter ^char %) (-> item :value (get index 0) str))) items)]
    (vec (concat
          (sort-by #(-> % :value (get index 0)) (get groups nil []))
          (sort-by #(-> % :value (get index 0) str) (get groups true []))))))

(defn header-action
  [widget x]
  (let [header (-> widget :props :header)
        width-per-item (/ (-> widget :props :width) (count header))
        x (- x (-> widget :props :x))
        index (int (Math/floor (/ x width-per-item)))
        header (get header index)
        widget (update widget :items (fn [items]
                                       (case (:action header)
                                         :sort (partition-and-sort-by-numeric-and-non-numeric-chars items index)
                                         :sort-asc (partition-and-sort-by-numeric-and-non-numeric-chars items index)
                                         :sort-desc (reverse (partition-and-sort-by-numeric-and-non-numeric-chars items index))
                                         :select-all (let [set-to-selected (not (every? identity (map :selected? items)))]
                                                       (mapv #(assoc % :selected? set-to-selected) items))
                                         items)))
        widget (update-in widget [:props :header index] (fn [head]
                                                          (-> head
                                                              (assoc :action (case (:action head)
                                                                                    :sort :sort-desc
                                                                                    :sort-asc :sort-desc
                                                                                    :sort-desc :sort-asc
                                                                                    (:action head))))))]
    widget))

(defmethod wdg/widget-event [strigui.list.List :mouse-clicked]
 [_ canvas widgets widget x y]
  (let [item-border-x (+ (-> widget :props :x) (- (-> widget :props :width) item-width-right-margin))]
    (if (< item-border-x x)
      (update widgets (:name widget) make-visible y)
      (if (>= (get-index-at widget y) 0)
        (update widgets (:name widget) activate! y :selected?)
        (-> widgets
            (update (:name widget) header-action x)
            (update (:name widget) make-visible y))))))

(defmethod wdg/widget-event [strigui.list.List :mouse-moved]
  [_ canvas widgets widget _ y]
  (update widgets (:name widget) activate! y :focused?))

(defmethod  wdg/widget-event [strigui.list.List :widget-focus-out]
 [_ canvas widgets widget _ _]
 (update-in widgets [(:name widget) :items] (fn [items] (mapv #(dissoc % :focused?) items))))

(defmethod wdg/widget-event [strigui.list.List :mouse-dragged]
  [_ canvas widgets widget x y _ _]
  (let [item-border-x (+ (-> widget :props :x) (- (-> widget :props :width) item-width-right-margin))]
    (if (< item-border-x x)
      (update widgets (:name widget) make-visible y)
      widgets)))