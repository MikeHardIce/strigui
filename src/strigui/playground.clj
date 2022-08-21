(ns strigui.playground
  (:require [strigui.core :as gui]
            [strigui.widget :as wdg]))

(defn main []
  ;;(gui/window! 200 300 1500 600 "My Window" (java.awt.Color. 44 44 44) ;{java.awt.RenderingHints/KEY_ANTIALIASING java.awt.RenderingHints/VALUE_ANTIALIAS_ON}
               
  (gui/from-file! "gui-test.edn") 

  (gui/swap-widgets! (fn [wdgs]
                       (-> wdgs
                           (gui/attach-event "click" :mouse-clicked (fn [_ _]
                                                                      (gui/close-window!)))
                           (gui/attach-event "input" :key-pressed (fn [widgets name _ code _]
                                                                    (if (= code 10)
                                                                      (-> widgets
                                                                          (assoc-in [name :props :selected?] nil)
                                                                          (assoc-in ["input1" :props :selected?] true))
                                                                      widgets)))
                           (gui/attach-event "input1" :key-pressed (fn [widgets name _ code _]
                                                                    (if (= code 10)
                                                                      (-> widgets
                                                                          (assoc-in [name :props :selected?] nil)
                                                                          (assoc-in ["input" :props :selected?] true))
                                                                      widgets)))
                           (assoc-in ["click" :props :x] 100)
                           (assoc-in ["click" :props :y] 400)
                           (gui/add-button "btnBla" "Don't Click Me" {:x 100 :y 300 :color {:background (java.awt.Color. 47 120 118) 
                                                                                            :text (java.awt.Color. 247 247 247)
                                                                                            :focus (java.awt.Color. 77 150 148)
                                                                                            :select (java.awt.Color. 77 150 148)
                                                                                            :border (java.awt.Color. 27 100 98)}
                                                                     :highlight [:alpha] :can-tab? true})
                           (update-in ["test-list" :items] conj {:value "10"} {:value "11"} {:value "12"} {:value "13"} {:value "14"} {:value "15"})))))

(defmethod wdg/widget-global-event :mouse-clicked [_ widgets x y]
  (println "Mouse clicked x:" x " y:" y)
  widgets)
                                                