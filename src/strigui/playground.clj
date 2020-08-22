(ns strigui.playground
  (:require [strigui.core :as gui]
            [strigui.events :as e]))

(defn main []
  (gui/window 600 600)
  (gui/label "welcome" "Welcome to Strigui" {:x 300 :y 20 :color [:red] :align [:center]})
  (gui/button "a" "Hello World!" {:x 50 :y 50 :color [:green :red]})
  (gui/button "b" "How are you?" {:x 50 :y 100 :color [:red :blue]})
  (gui/button "c" "Blah" {:x 50 :y 150 :color [:blue :yellow] :min-width 100})
  (gui/button "d" "Bye" {:x 50 :y 200 :color [:yellow :green] :min-width 100}))

(defmethod e/button-clicked "a" [btn]
  (gui/info "Button A pressed"))