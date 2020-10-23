(ns strigui.widget
    (:require [clojure2d.core :as c2d]))

(defprotocol Widget 
    "collection of functions around redrawing widgets, managing the border etc. ..."
    (coord [this] "gets the coordinates of the widget")
    (value [this] "the current text of the widget")
    (args [this] "the current args of the widget")
    (widget-name [this] "name of the widget")
    (draw [this canvas] "draw the widget, returns the widget on success")
    (redraw [this canvas] "redraw the widget")
    (hide [this canvas] "removes the widget from the canvas"))

(def widgets (atom ()))

(def ^:private boxes-to-redraw (atom #{}))

(defmulti widget-event 
  (fn [action canvas widget] [(:class widget) action]))

(defmethod widget-event :default [_ canvas widget] nil)

(defmulti widget-global-event
  (fn [action canvas & args] action))

(defmethod widget-global-event :default [_ canvas & args] nil)

(defmethod c2d/mouse-event ["main-window" :mouse-moved] [event state]
  (let [context @wnd/context
        canvas (:canvas context)
        window (:window context)
        btn-hits (first (filter #(wnd/within? (coord %) (c2d/mouse-x window) (c2d/mouse-y window)) @boxes))
        btns @boxes-to-redraw]
    (wnd/display-info context (str (c2d/mouse-pos window) " " @boxes-to-redraw))
    (if (empty? btn-hits)
      (let [redrawn-buttons (map #(wdg/redraw % canvas) btns)]
        (swap! boxes-to-redraw #(s/difference %1 (set %2))  redrawn-buttons))
      (do 
        (widget-event btn-hits :mouse-moved canvas)
        ;;(draw-hover btn-hits canvas)
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
        (widget-event btn :mouse-clicked canvas)
        ;;(clicked btn)
        ;;(draw-clicked btn canvas)
        (swap! boxes-clicked #(conj %1 %2) btn))
      (reset! boxes-focused #{})))
  state)

(defmethod c2d/mouse-event ["main-window" :mouse-released] [event state]
  (widget-global-event :mouse-released canvas)
  ;(map #(draw-hover %1 (:canvas @wnd/context)) @boxes-clicked)
  ;(reset! boxes-clicked  #{})
  state)

(defmethod c2d/key-event ["main-window" :key-pressed] [event state]
  (let [char (c2d/key-char event)
        code (c2d/key-code event)
        new-focused-inputs (doall (map #(key-pressed %1 char code) @boxes-focused))]
    (widget-global-event :key-pressed canvas char code)
    (when (not-empty new-focused-inputs)
    (doall (map #(unregister-box! %1) @boxes-focused))
    (doall (map #(register-box! %1) new-focused-inputs))
    (doall (map #(box-draw-text (:canvas @wnd/context) (wdg/value %1) (wdg/args %1)) new-focused-inputs))
    (reset! boxes-focused (set new-focused-inputs))))
  state)