(ns strigui.playground
  (:require [strigui.core :as gui]
            [strigui.widget :as wdg])
  (:import [java.awt Color]))

(defonce color-for-button {:background (Color. 220 220 150)
                           :text (Color/black)
                           :focus (Color/black)})

(defn create-start-btn
  [widgets]
  (-> widgets
      (gui/add-button "btn-start" "Start" {:x 400 :y 300 :z 3 :width 250 :color color-for-button :highlight [:border] :font-size 28 :group "menu"})
      (gui/attach-event "btn-start" :mouse-clicked (fn [wdgs _]
                                                     (-> wdgs
                                                         (gui/remove-widgets-by-group "menu"))))))

(defn create-quit-btn
  [widgets]
  (-> widgets
      (gui/add-button "btn-quit" "Quit" {:x 400 :y 500 :z 3 :width 250 :color color-for-button :highlight [:border] :font-size 28 :group "menu"})
      (gui/attach-event "btn-quit" :mouse-clicked (fn [_ _]
                                                    (gui/close-window!)))))

(defn main []
  ;;(gui/window! 200 300 1500 600 "My Window" (java.awt.Color. 44 44 44) ;{java.awt.RenderingHints/KEY_ANTIALIASING java.awt.RenderingHints/VALUE_ANTIALIAS_ON}
               
  (gui/from-file! "gui-test.edn") 

  
  
  (gui/swap-widgets! (fn [wdgs]
                       (-> wdgs
                           (gui/attach-event "click" :mouse-clicked (fn [_ _]
                                                                      (gui/close-window!)))
                           (assoc-in ["click" :props :x] 100)
                           (assoc-in ["click" :props :y] 400)
                           (gui/add-button "btnBla" "Don't Click Me" {:x 50 :y 100 :width 600 :height 800 :z -1 :color {:background (java.awt.Color. 147 220 218) 
                                                                                            :text (java.awt.Color. 247 247 247)
                                                                                            :focus (java.awt.Color. 77 150 148)
                                                                                            :select (java.awt.Color. 77 150 148)
                                                                                            :border (java.awt.Color. 27 100 98)}
                                                                     :highlight [] :can-tab? true})
                           (update-in ["test-list" :items] conj {:value "10"} {:value "11"} {:value "12"} {:value "13"} {:value "14"} {:value "15"}))))
  

  (gui/swap-widgets! #(-> %
                          create-start-btn
                          create-quit-btn)))

(defmethod wdg/widget-global-event :mouse-clicked [_ widgets x y]
  (println "Mouse clicked x:" x " y:" y)
  widgets)
                                                