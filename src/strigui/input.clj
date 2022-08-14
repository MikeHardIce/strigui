(ns strigui.input
  (:require [strigui.widget :as wdg]
            [strigui.box :as b])
  (:import [java.awt.datatransfer Clipboard StringSelection DataFlavor]
           [java.awt Toolkit]))

(defrecord Input [name value props]
  wdg/Widget
  (coord [this canvas] (apply b/box-coord [canvas (:value this) (:props this)]))
  (defaults [this] (assoc-in this [:props :highlight] [:border :alpha]))
  (before-drawing [this] this)
  (draw [this canvas]
        (b/box-draw canvas (if (-> this :props :password?)
                             (apply str (repeat (count (:value this)) "*"))
                             (:value this)) (:props this))
        this)
  (after-drawing [this] this))

;; (extend-protocol wdg/Draw-resizing
;;   Input
;;   (draw-resizing [this canvas] (let [[x y w h] (wdg/coord this canvas)]
;;                                  (c2d/with-canvas-> canvas
;;                                    (c2d/set-color :orange)
;;                                    (c2d/rect x y w h))
;;                                  this)))
;; (extend-protocol wdg/Hide
;;  Input
;;   (hide [this canvas] (let [[x y w h] (wdg/coord this canvas)]
;;                         (println "hide call to overriden function!")
;;                         (c2d/with-canvas-> canvas
;;                           (c2d/set-color :white)
;;                           (c2d/rect (- x 5) (- y 5) (+ w 8) (+ h 8))))))

(defn adjust-text [text char code]
  (if (and (= code 8) (> (count text) 0)) 
    (subs text 0 (- (count text) 1))
    (str text char)))

(defn key-pressed [this char code]
  (if (or (and (<= code 28) (not= code 8))
          (and (= code 8) (< (count (:value this)) 1)))
    this
    (assoc this :value (adjust-text (:value this) char code))))

(defn input
  " name - name of the input element
    text - text displayed inside the input element
    props - map of properties:
      x - x coordinate of top left corner
      y - y coordinate of top left corner
      color - vector consisting of [background-color font-color]
      min-width - the minimum width"
  [name text props]
    (let [input (->Input name text props)]
      input))

(defn copy->clipboard!
  "Copies the value of the current input widget to the systems clipboard"
  [widget]
  (let [clip-board ^Clipboard (.getSystemClipboard (Toolkit/getDefaultToolkit))
        selection (StringSelection. ^String (:value widget))]
    (.setContents clip-board selection selection) 
    widget))

(defn clipboard->widget!
  "Paste the clipboard contents to the widgets value"
  [widget]
  (let [clip-board ^Clipboard (.getSystemClipboard (Toolkit/getDefaultToolkit))
        content ^String (.toString (.getTransferData (.getContents clip-board nil) DataFlavor/stringFlavor))]
    (update widget :value #(str % content))))

(defn handle-key-pressed
  [widget char code prev-code] 
  (if (= prev-code 17)
    (case code 
      67 (copy->clipboard! widget)
      86 (clipboard->widget! widget)
      widget)
    (let [box-with-new-input (key-pressed widget char code)]
          ;box-with-new-input (assoc-in box-with-new-input [:props :selected?] (or (not= code 10) ;;enter
          ;                                                                       (not= code 9)))] ;; tab
      box-with-new-input)))

(defmethod wdg/widget-event [strigui.input.Input :key-pressed]
  [_ canvas widgets widget char code prev-code]
  (assoc widgets (:name widget) (handle-key-pressed widget char code prev-code)))