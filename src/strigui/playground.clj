(ns strigui.playground
  (:require [strigui.core :as gui]
             [strigui.widget :as wdg]))

(defn main []
  ;; (gui/window! 600 600 "Strigui")
  ;; (gui/label! "welcome" "Welcome to Strigui" {:x 190 :y 100
  ;;                                             :color [:green]
  ;;                                             :font-size 20 :font-style [:bold]
  ;;                                             :can-move? true})
  ;; (strigui.core/button! "click" "Click me" {:x 400 :y 250 :z 10 :color [:white :black] :can-tab? true})
  ;; (gui/input! "input" "" {:x 100 :y 150 :color [:white :red] :min-width 420 :selected? true :can-tab? true})
  ;; (gui/input! "input1" "" {:x 100 :y 200 :color [:white :red] :min-width 420 :can-tab? true})
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
  
  (gui/update! "input1" [:events :mouse-clicked] (fn [wdg]
                                                   (gui/remove! "input1")))
  
  (gui/update! "click" [:args :x] 100 [:args :y] 400))


(defmethod wdg/widget-global-event :mouse-clicked [_ canvas x y]
  (println "Mouse clicked x:" x " y:" y))
                                                