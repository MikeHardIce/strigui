(ns strigui.playground
  (:require [strigui.core :as gui]
             [strigui.widget :as wdg]))

(defn main []
  (gui/from-file! "gui-test.edn")

  (gui/swap-widgets! (fn [wdgs]
                       (-> wdgs
                           (gui/attach-event "click" :mouse-clicked (fn [_ _]
                                                                      (gui/close-window!)))
                           (gui/attach-event "input" :key-pressed (fn [widgets name code]
                                                                    (if (= code 10)
                                                                      (-> widgets
                                                                          (assoc-in [name :args :selected?] nil)
                                                                          (assoc-in ["input1" :args :selected?] true))
                                                                      widgets)))
                           (gui/attach-event "input1" :key-pressed (fn [widgets name code]
                                                                    (if (= code 10)
                                                                      (-> widgets
                                                                          (assoc-in [name :args :selected?] nil)
                                                                          (assoc-in ["input" :args :selected?] true))
                                                                      widgets)))
                           (assoc-in ["click" :args :x] 100)
                           (assoc-in ["click" :args :y] 400)))))

(defmethod wdg/widget-global-event :mouse-clicked [_ canvas widgets x y]
  (println "Mouse clicked x:" x " y:" y)
  widgets)
                                                