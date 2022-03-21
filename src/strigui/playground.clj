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
                           (assoc-in ["click" :args :y] 400)
                           (gui/add-button "btnBla" "Don't Click Me" {:x 100 :y 300 :color [java.awt.Color/green java.awt.Color/red] :can-tab? true})
                           (update-in ["test-list" :items] conj {:value "item4"} {:value "item5"} {:value "item6"} {:value "item7"} {:value "item8"} {:value "item9"})))))

(defmethod wdg/widget-global-event :mouse-clicked [_ widgets x y]
  (println "Mouse clicked x:" x " y:" y)
  widgets)
                                                