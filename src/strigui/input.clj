(ns strigui.input
  (:require [clojure2d.core :as c2d]
            [clojure.set :as s]
            [strigui.window :as wnd]
            [strigui.box :as b]
            [strigui.events :as e]))

;;
(def has-focus (atom #{}))

(defrecord Input [name coordinates args]
  b/Box
  (coord [this] (:coordinates this)) ;; could be a mapping if the record would look different
  (draw-hover [this canvas] 
    (when (not (contains? @has-focus this))
      (b/box-draw-hover this canvas)))
  (draw-clicked [this canvas] (apply b/box-border (conj [canvas :blue 2] (:coordinates this)))
                                  this)
  (redraw [this canvas] 
    (when (not (contains? @has-focus this))
      (b/box-redraw this canvas)))
  (draw [this canvas]
    (b/box-draw-border this canvas) 
    (b/box-draw (:args this))))

  (extend-protocol b/Actions
  Input
    (clicked [this] (let [focus @has-focus
                          new (if (contains? focus this)
                                (s/difference focus #{this})
                                (s/union focus #{this}))] 
                      (reset! has-focus new))))

(defn input
  "context - map consisting of clojure2d canvas and clojure2d window
    name - name of the input element
    text - text displayed inside the input element
    args - map of properties:
      x - x coordinate of top left corner
      y - y coordinate of top left corner
      color - vector consisting of [background-color font-color]
      min-width - the minimum width"
  [context name text args]
  (let [canvas (:canvas context)
        arg [canvas text args]
        coord (apply b/box-coord arg)
        inp (Input. name coord arg)]
    (b/draw inp canvas)
    (b/register-box inp)))
  
;; (= code "back_space")
(defn adjust-text [text char code]
  (if (and (= code :back_space) (> (count text) 0)) 
    (subs text 0 (- (count text) 1))
    (str text char)))

(defmethod c2d/key-event ["main-window" :key-pressed] [event state]
  (let [char-added (c2d/key-char event)
        char-code (c2d/key-code event)
        new-focused-inputs (doall (map #(assoc-in %1 [:args 1] (adjust-text (nth (:args %1) 1) char-added char-code)) @has-focus))]
    (println (str char-added ":" char-code))
    (doall (map #(b/unregister-box %1) @has-focus))
    (doall (map #(b/register-box %1) new-focused-inputs))
    (doall (map #(apply b/box-draw-text (:args %1)) new-focused-inputs))
    (reset! has-focus (set new-focused-inputs))
    (doall (map #(e/input-modified %1) new-focused-inputs)))
  state)