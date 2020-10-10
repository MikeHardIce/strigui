(ns strigui.box
  (:require [clojure2d.core :as c2d]
            [clojure.set :as s]
            [strigui.window :as wnd]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

;; not sure if thats the right way
(defprotocol Box 
  "collection of functions around redrawing boxes, managing the border etc. ..."
  (coord [this] "gets the coordinates of the box")
  (draw-hover [this canvas] "")
  (draw-clicked [this canvas] "")
  (redraw [this canvas] "")
  (draw [this canvas] ""))

(defprotocol Actions
  "collection of functions to hook into events"
  (clicked [this] "")
  (key-pressed [this char code] ""))

(def ^:private boxes (atom ()))

(def ^:private boxes-to-redraw (atom #{}))

(def ^:private boxes-clicked (atom #{}))

(def ^:private boxes-focused (atom #{}))

(defn box-coord 
  "Computes the full box coordinates.
  Returns the vector [x y border-width border-heigth]"
  [canvas text {:keys [^long x ^long y ^long min-width]}]
  (let [text-box (c2d/with-canvas-> canvas
                  (c2d/text-bounding-box text))
      text-width (nth text-box 2)
      text-heigth  (nth text-box 3)
      btn-w (* text-width 1.8)
      border-width (if (and (number? min-width) (< btn-w min-width)) min-width btn-w)
      border-heigth (* text-heigth 1.8)]
      [x y border-width border-heigth]))

(defn box-draw-text 
  "Draws the text of the box"
  [canvas text {:keys [^long x ^long y color ^long min-width align font-style font-size]}]
  (let [style (if (empty? font-style) :bold (first font-style))
        size (if (number? font-size) font-size 15)
        [_ _ border-width border-heigth] (box-coord canvas text {:x x :y y :min-width min-width})
        [_ text-y text-width _] (c2d/with-canvas-> canvas
                          (c2d/set-font-attributes size style)
                          (c2d/text-bounding-box text))
        background-color (if (> (count color) 0) (first color) :grey)
        foreground-color (if (> (count color) 1) (nth color 1) :black)
        x-offset (if (and (number? min-width) (= min-width border-width))
                   (/ (- border-width text-width) 2.0)
                   (* border-width 0.12))]
      (c2d/with-canvas-> canvas
        (c2d/set-color background-color)
        (c2d/rect x y border-width border-heigth)
        (c2d/set-font-attributes size style)
        (c2d/set-color foreground-color)
        (c2d/text text (+ x x-offset) (- y (* text-y 1.5))))))

(defn box-draw
  "canvas - clojure2d canvas
  text - text displayed inside the input
  x - x coordinate of top left corner
  y - y coordinate of top left corner
  color - vector consisting of [background-color font-color]
  min-width - the minimum width"
  ([args] (apply box-draw args))
  ([canvas text args]
  (let [{:keys [^long x ^long y color ^long min-width]} args
        [_ _ border-width border-heigth] (box-coord canvas text {:x x :y y :min-width min-width})
        background-color (if (> (count color) 0) (first color) :grey)]
    (c2d/with-canvas-> canvas
      (c2d/set-color background-color)
      (c2d/rect x y border-width border-heigth))
    (box-draw-text canvas text args)
    [x y border-width border-heigth])))

(defn box-border [canvas color ^long strength [^long x ^long y ^long w ^long h]]
  (when (> strength 0)
      (c2d/with-canvas-> canvas
      ;(c2d/set-stroke strength :butt 0 0)
        (c2d/set-color color)
        (c2d/rect (- x strength) (- y strength) (+ w (* 2 strength)) (+ h (* 2 strength)) true))
      (box-border canvas color (- strength 1) [x y w h])))

(defn find-by-name 
  [name]
  (first (filter #(= (:name %) name) @boxes)))

(defn register-box!
  "Registers a box component"
  [^strigui.box.Box box]
  (swap! boxes conj box))

(defn unregister-box! 
  "Unregisters a box component"
  [^strigui.box.Box box]
    (swap! boxes #(filter (fn [el] (not= %2 el)) %1) box)
    (swap! boxes-to-redraw #(s/difference %1 #{box})))

(defn box-draw-border 
  ([^strigui.box.Box box canvas] (box-draw-border box canvas :black))
  ([^strigui.box.Box box canvas color]
  (apply box-border (conj [canvas color 1] (coord box)))))

(defn box-draw-hover 
  [^strigui.box.Box box canvas] 
  (apply box-border (conj [canvas :black 2] (coord box)))
  box)

(defn box-redraw 
  [^strigui.box.Box box canvas] 
  (let [coord (coord box)]
    (when (not-empty coord)
      (apply box-border (conj [canvas :white 2] coord))
      (apply box-border (conj [canvas :black 1] coord))
      box)))

(defn box-remove-drawn 
  [^strigui.box.Box box canvas]
  (let [empty-box (-> box
                    (assoc-in [:args :color] [:white :white])
                    (assoc-in [:args :text] ""))]
    (draw empty-box canvas)
    (box-draw-border empty-box canvas :white)))

(defn redraw-all 
  [canvas]
  (doall (map box-redraw @boxes)))

(defn swap-focused!
  [box]
  (let [focus @boxes-focused
        new (if (contains? focus box)
                (s/difference focus #{box})
                (s/union focus #{box}))] 
    (reset! boxes-focused new)))

(defn focused? 
  [box]
  (contains? @boxes-focused box))

  ;; TODO: maybe its not necessary to go to @wnd/context directly
(defmethod c2d/mouse-event ["main-window" :mouse-moved] [event state]
  (let [context @wnd/context
        canvas (:canvas context)
        window (:window context)
        btn-hits (first (filter #(wnd/within? (coord %) (c2d/mouse-x window) (c2d/mouse-y window)) @boxes))
        btns @boxes-to-redraw]
    (wnd/display-info context (str (c2d/mouse-pos window) " " @boxes-to-redraw))
    (if (empty? btn-hits)
      (let [redrawn-buttons (map #(redraw % canvas) btns)]
        (swap! boxes-to-redraw #(s/difference %1 (set %2))  redrawn-buttons))
      (do 
        (draw-hover btn-hits canvas)
        (swap! boxes-to-redraw  #(conj %1 %2) btn-hits))))
  state)

;; TODO: maybe its not necessary to go to @wnd/context directly
(defmethod c2d/mouse-event ["main-window" :mouse-pressed] [event state]
  (let [context @wnd/context
        canvas (:canvas context)
        window (:window context)
        btn (first (filter #(wnd/within? (coord %) (c2d/mouse-x window) (c2d/mouse-y window)) @boxes))]
    (if (not-empty btn)
      (do 
        (clicked btn)
        (draw-clicked btn canvas)
        (swap! boxes-clicked #(conj %1 %2) btn))
      (reset! boxes-focused #{})))
  state)

(defmethod c2d/mouse-event ["main-window" :mouse-released] [event state]
  (map #(draw-hover %1 (:canvas @wnd/context)) @boxes-clicked)
  (reset! boxes-clicked  #{})
  state)

(defmethod c2d/key-event ["main-window" :key-pressed] [event state]
  (let [char (c2d/key-char event)
        code (c2d/key-code event)
        new-focused-inputs (doall (map #(key-pressed %1 char code) @boxes-focused))]
    (when (not-empty new-focused-inputs)
      (doall (map #(unregister-box! %1) @boxes-focused))
      (doall (map #(register-box! %1) new-focused-inputs))
      (doall (map #(apply box-draw-text (:args %1)) new-focused-inputs))
      (reset! boxes-focused (set new-focused-inputs))))
  state)