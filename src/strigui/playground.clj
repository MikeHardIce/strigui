(ns strigui.playground
  (:require [strigui.core :as gui]
            [strigui.widget :as wdg]))

(defn main []
  (gui/from-file! "gui-test.edn")

  ;; (gui/window! 200 300 600 600 "My Window" (java.awt.Color. 255 200 133))
  
  ;; (gui/swap-widgets! (fn [wdgs]
  ;;                      (-> wdgs
  ;;                          (gui/add-label "welcome" "Welcome to Strigui" {:x 190 :y 100
  ;;                                                                         :color [(Color. 255 31 0)]
  ;;                                                                         :font-size 20 :font-style [:bold]})
  ;;                          (gui/add-button "click" "Click me" {:x 400 :y 200 :color [Color/white Color/black]})
  ;;                          (gui/add-input "input" "" {:x 100 :y 150 :width 420 :color [Color/white Color/red] :min-width 420}))))
  
  ;; (gui/swap-widgets! (fn [wdgs]
  ;;                      (gui/attach-event wdgs "click" :mouse-clicked (fn [_ _] 
  ;;                                                                      (gui/close-window!)))))
  

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
                           (update-in ["test-list" :items] conj {:value "10"} {:value "11"} {:value "12"} {:value "13"} {:value "14"} {:value "15"})))))

(defmethod wdg/widget-global-event :mouse-clicked [_ widgets x y]
  (println "Mouse clicked x:" x " y:" y)
  widgets)
                                                