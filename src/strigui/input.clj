(ns strigui.input
  (:require [strigui.widget :as wdg]
            [strigui.box :as b]
            [clojure.string :as s]
            [capra.core :as c])
  (:import [java.awt.datatransfer Clipboard StringSelection DataFlavor]
           [java.awt Toolkit]))

(set! *warn-on-reflection* true)

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
  (after-drawing [this] 
                 this))

(defn adjust-text [text canvas char code {:keys [can-multiline? can-scroll? font-size width height] :or {height 42 width 150 font-size 15}}] 
  (if (and (= code 8) (> (count text) 0)) 
    (subs text 0 (- (count text) 1))
    (if (= code 10)
      (str text \newline)
      (let [new-text (str text char)
            split-text (s/split-lines new-text)
            h (* (count split-text) (second (c/get-text-dimensions canvas new-text font-size)))
            [w _] (c/get-text-dimensions canvas (last split-text) font-size)
            should-break-line? (>= (+ 30 (* 1.3 w)) width)
            enough-space? (or can-scroll? (>= height (+ 30 (* 1.4 h))))]
        (if should-break-line?
          (if can-multiline? 
            (if enough-space?
              (str text \newline char)
              (str text))
            (str text))
          new-text)))))

(defn key-pressed [this canvas char code]
  (if (or (and (<= code 28) (not= code 8) (not= code 10))
          (and (= code 10) (not (-> this :props :can-multiline?)))
          (and (= code 8) (< (count (:value this)) 1)))
    this
    (update this :value adjust-text canvas char code (:props this))))

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
  [widget canvas char code prev-code] 
  (if (= prev-code 17)
    (case (int code) 
      67 (copy->clipboard! widget)
      86 (clipboard->widget! widget)
      widget)
    (let [box-with-new-input (key-pressed widget canvas char code)] 
      box-with-new-input)))

(defmethod wdg/widget-event [strigui.input.Input :key-pressed]
  [_ canvas widgets widget char code prev-code]
  (update widgets (:name widget) handle-key-pressed canvas char code prev-code))