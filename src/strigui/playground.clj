(ns strigui.playground
  (:require [strigui.core :as gui]
            [strigui.widget :as wdg])
  (:import [java.awt Color]))

(defonce color-for-button {:background (Color. 220 220 150)
                           :text (Color/black)
                           :focus (Color/black)})

(defonce color-profiles (atom [{:background (java.awt.Color. 44 44 44)
                                :background-widgets (java.awt.Color. 47 120 118)
                                :text (java.awt.Color. 247 247 247)
                                :focus (java.awt.Color. 117 190 188)
                                :select (java.awt.Color. 117 190 188)
                                :border (java.awt.Color. 27 100 98)
                                :resize (java.awt.Color. 247 247 247)}
                               {:background (java.awt.Color. 250 250 250)
                                :background-widgets (java.awt.Color. 44 44 44)
                                :text (java.awt.Color. 161 161 161)
                                :focus (java.awt.Color. 117 190 188)
                                :select (java.awt.Color. 117 190 188)
                                :border (java.awt.Color. 27 100 98)
                                :resize (java.awt.Color. 247 247 247)}]))

(defn moving-by-button
  []
  (gui/swap-widgets! #(gui/add-window % "main-window" 50 50 700 500 "Main Window" {:on-close gui/exit :resizable? true :color (-> @color-profiles first)}))

  (gui/swap-widgets! #(-> %
                          (gui/add-window "sub-window" 100 100 700 500 "Sub Window" {:on-close gui/hide :resizable? true :color (-> @color-profiles first)})
                          (gui/add-button "sub-window" "btnBla" "Change Theme" {:x 100 :y 300 :width 200})
                          (gui/attach-event "btnBla" :mouse-clicked (fn [wdgs _]
                                                                      (-> wdgs
                                                                          (update-in ["main-window" :props :x] (partial + 50))
                                                                          (gui/change-color-profile "sub-window" (first (swap! color-profiles reverse)))))))))

(defn default-stuff
  []
  #_(gui/from-file! "gui-test-simple.edn")
  (gui/from-file! "bla-test.edn")
  #_(gui/swap-widgets! #(-> %
                          (gui/add-window "main" 100 100 1200 800 "Welcome to Strigui" {})
                          (gui/add-button "main" "btnOk" "Ok" {:x 150 :y 700}))))

(defn main []
  
  #_(moving-by-button)
  (default-stuff)
  (gui/swap-widgets! #(-> %
                          (gui/add-checkbox "main-window" "cb-none" nil {:x 10 :y 0 :text "Without a tick" :align-text :left})
                          (gui/add-checkbox "main-window" "cb-tick" true {:x 10 :y 50 :text "With a tick" :align-text :left})
                          (gui/add-radio "main-window" "cb-radio" nil {:x 10 :y 100 :text "Radio empty (X)" :align-text :left :group ["001"]})
                          (gui/add-radio "main-window" "cb-radio-full" true {:x 10 :y 150 :text "Radio full (X)" :align-text :left :group ["001"]})
                          (gui/add-radio "main-window" "cb-radio1" nil {:x 10 :y 250 :text "Radio empty (Y)" :align-text :left :group ["002"]})
                          (gui/add-radio "main-window" "cb-radio1-full" true {:x 10 :y 300 :text "Radio full (Y)" :align-text :left :group ["002"]})))
  
  ;;(gui/window! 200 300 1500 600 "My Window" (java.awt.Color. 44 44 44)) ;{java.awt.RenderingHints/KEY_ANTIALIASING java.awt.RenderingHints/VALUE_ANTIALIAS_ON}

  
  ;;TODO: cabra with window hide/exit/resize event
  ;; in particular the resizing needs to update the properties

  #_(gui/to-file "bla-test.edn") 
  
  #_(gui/swap-widgets! (fn [wdgs]
                       (-> wdgs
                           (gui/attach-event "click" :mouse-clicked (fn [wdgs_]
                                                                      (gui/close-window! wdgs "main")))
                           (assoc-in ["click" :props :x] 100)
                           (assoc-in ["click" :props :y] 400)
                           (gui/add-button "main" "btnBla" "Change Theme" {:x 100 :y 300 :width 200 :height 42  :color {:background (java.awt.Color. 147 220 218) 
                                                                                            :text (java.awt.Color. 247 247 247)
                                                                                            :focus (java.awt.Color. 77 150 148)
                                                                                            :select (java.awt.Color. 77 150 148)
                                                                                            :border (java.awt.Color. 27 100 98)}
                                                                     :highlight [] :can-tab? true})
                           (gui/attach-event "btnBla" :mouse-clicked (fn [wdgs _]
                                                                       (gui/change-color-profile wdgs "main"(first (swap! color-profiles reverse)))))
                           (update-in ["test-list" :items] conj {:value "10"} {:value "11"} {:value "12"} {:value "13"} {:value "14"} {:value "15"})))))

(defmethod wdg/widget-global-event :mouse-clicked [_ widgets window-name x y]
  (println "Mouse clicked x:" x " y:" y " on window: " window-name)
  widgets)