(ns strigui.list
  (:require [strigui.box :as b]
             [strigui.widget :as wdg]
             [capra.core :as c]))

(defonce item-height 25)
(defonce item-width-right-margin 10)

(defn make-darker-or-brighter
  [^java.awt.Color color]
  (let [hsl (java.awt.Color/RGBtoHSB (.getRed color) (.getGreen color) (.getBlue color) nil)]
    (if (> (get hsl 2) 0.8) (.darker color) (-> color (.brighter) (.brighter) (.brighter) (.brighter) (.brighter)))))

(defn get-index-at 
  [widget y]
  (let [index (int (/ (- y (-> widget :props :y)) item-height))]
    index))

(defn draw-list
  [canvas items props]
    (loop [items (filter :visible? items)
           index 0]
      (when (seq items)
        (let [color (:color props)
              item (first items)
              color (if (or (:hovered? item) (:selected? item)) (update color :background make-darker-or-brighter) color)]
          (b/box-draw canvas (-> items first :value) (merge 
                                                      {:y (+ (* index item-height) (:y props)) :max-width (- (:width props) item-width-right-margin) :color color}
                                                      (select-keys props [:width :x])))
          (when (:selected? item)
            (c/draw-> canvas
                      (c/line (inc (:x props)) (+ (* index item-height) (:y props)) 
                              (inc (:x props)) (+ (* (inc index) item-height) (:y props)) 
                              (get color :text java.awt.Color/green) 3))))
        (recur (rest items) (inc index)))))

(defn clear-out
  [items property]
  (let [cleared-items (mapv #(merge % {property false}) items)]
    cleared-items))

(defn make-visible 
  ([widget]
  (let [max-visible-items (Math/floor (/ (-> widget :props :height) item-height))
        items (:items widget)
        widgets-visible (mapv #(merge % {:visible? true}) (take max-visible-items items))
        widgets-not-visible (take-last (- (count items) max-visible-items) items)]
    (assoc widget :items (apply conj widgets-visible widgets-not-visible))))
  ([widget y]
   (let [max-visible-items (int (/ (-> widget :props :height) item-height))
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
                                (Math/floor (/ (-> this :props :height) item-height)))
                           (make-visible this)
                           this))
  (draw [this canvas]
        (let [{:keys [x y width height color] :as props} (:props this)
              bar-x (+ x (- width item-width-right-margin))
              items (:items this)
              items-visible (filter :visible? items)
              bar-ratio (/ (count items-visible) (if (seq items) (count items) 1))
              first-items-hidden (take-while #(not (:visible? %)) items)
              bar-scroll-height (* bar-ratio height)
              bar-y (+ y (/ (* bar-scroll-height (count first-items-hidden)) (if (seq items-visible) (count items-visible) 1)))]
          (c/draw-> canvas
                    (c/rect x y width height (:background color) false)
                    (c/rect bar-x y item-width-right-margin height (make-darker-or-brighter (:background color)) true)
                    (c/rect bar-x bar-y item-width-right-margin bar-scroll-height (make-darker-or-brighter (:text color)) true))
          (draw-list canvas items props)
          this))
  (after-drawing [this] this))

(defn activate!
  [widget y property]
  (let [index (get-index-at widget y)
        items (:items widget)
        item (get (filterv :visible? items) index)
        index (.indexOf items item)]
    (if (seq item)
      (-> widget
          (update :items clear-out property)
          (assoc-in [:items index property] true))
      widget)))

(defmethod wdg/widget-event [strigui.list.List :mouse-clicked]
 [_ canvas widgets widget x y]
  (let [item-border-x (+ (-> widget :props :x) (- (-> widget :props :width) item-width-right-margin))]
    (if (< item-border-x x)
      (update widgets (:name widget) make-visible y)
      (update widgets (:name widget) activate! y :selected?))))

(defmethod wdg/widget-event [strigui.list.List :mouse-moved]
  [_ canvas widgets widget _ y]
  (update widgets (:name widget) activate! y :hovered?))

(defmethod wdg/widget-event [strigui.list.List :mouse-dragged]
  [_ canvas widgets widget x y _ _]
  (let [item-border-x (+ (-> widget :props :x) (- (-> widget :props :width) item-width-right-margin))]
    (if (< item-border-x x)
      (update widgets (:name widget) make-visible y)
      widgets)))