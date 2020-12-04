(ns strigui.playground
  (:require [strigui.core :as gui]
            [strigui.events :as e]))

(defn main []
  (gui/window 600 600)
  (gui/label "welcome" "Welcome to Strigui" {:x 300 :y 20 
                                              :color [:red] :align [:center]
                                              :font-size 20 :font-style [:bold]})
  (gui/button "a" "Hello World!" {:x 50 :y 50 :color [:green :red]})
  (gui/button "b" "How are you?" {:x 50 :y 100 :color [:red :blue] 
                                  :font-size 20 :font-style [:bold]})
  (gui/button "c" "Blah" {:x 50 :y 150 :color [:blue :yellow] :min-width 100})
  (gui/button "d" "Bye" {:x 50 :y 200 :color [:yellow :green] :min-width 100})
  (gui/input "inp1" "" {:x 250 :y 50 :color [:white :red] :min-width 100})
  (gui/input "inp2" "" {:x 250 :y 100 :color [:white :red] :min-width 100})
  (gui/input "inp3" "last" {:x 250 :y 150 :color [:white :red] :min-width 100})
  (gui/stacks "stacks" '(5 1 8 2 0 3 0 5 7) {:x 150 :y 400})
  (gui/find-by-name "inp2")
  (gui/remove! "inp1")
  (gui/update! "inp3" :value "Hello")
  (gui/update! "a" :events {:mouse-clicked (fn [wdg]
                                                (gui/info "Button A pressed"))}))
