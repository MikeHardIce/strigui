(ns my-app.core
  (:require [strigui.core :as gui])
  (:import [java.awt Color])
  (:gen-class))

(defn -main
  []
  (gui/swap-widgets! #(-> %
                          (gui/add-window "first-window" 50 50 600 450 "My First Window" {})
                          (gui/add-label "first-window" "lbl-hello" "Hello!" {:x 100 :y 100 :font-size 24})
                          (gui/add-button "first-window" "btn-dragging" "Disabled" {:x 400 :y 200})
                          (gui/attach-event "btn-dragging" :mouse-clicked (fn [wdgs _]
                                                                            (-> wdgs
                                                                                (update-in ["lbl-hello" :props]
                                                                                           (fn [props]
                                                                                             (merge props
                                                                                                    (if (:can-move? props)
                                                                                                      {:can-move? false :color {:text Color/black}}
                                                                                                      {:can-move? true :color {:text Color/orange}}))))
                                                                                (update-in ["btn-dragging" :value] (fn [text]
                                                                                                                     (case text
                                                                                                                       "Disabled" "Enabled"
                                                                                                                       "Disabled")))))))))
  
  
