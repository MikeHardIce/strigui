(ns strigui.playground
  (:require [strigui.core :as gui]
             [strigui.widget :as wdg]))

(defn main []
  (gui/from-file "gui-test.edn")
  (gui/update! "click" [:events :mouse-clicked] (fn [wdg]
                                                  (gui/close-window)))
  
  (gui/update! "input" [:events :key-pressed] (fn [wdg code]
                                                (when (= code :enter)
                                                  (gui/update! "input" [:args :selected?] nil)
                                                  (gui/update! "input1" [:args :selected?] true))))
  (gui/update! "input1" [:events :key-pressed] (fn [wdg code]
                                                 (when (= code :enter)
                                                   (gui/update! "input1" [:args :selected?] nil)
                                                   (gui/update! "input" [:args :selected?] true))))
  (gui/update! "click" [:args :x] 100 [:args :y] 400))


(defmethod wdg/widget-global-event :mouse-clicked [_ canvas x y]
  (println "Mouse clicked x:" x " y:" y))
                                                