(ns strigui.playground
  (:require [strigui.core :as gui]
             [strigui.widget :as wdg]))

(defn main []
  (gui/from-file "gui-test.edn")

  (gui/swap-widgets! (fn [wdgs]
                       (-> wdgs
                           (gui/attach-event "click" :mouse-clicked (fn [_ _]
                                                                      (gui/close-window)))
                           (gui/attach-event "input" :key-pressed (fn [widgets name code]
                                                                    (when (= code :enter)
                                                                      (-> widgets
                                                                          (assoc-in [name :args :selected?] nil)
                                                                          (assoc-in ["input1" :args :selected?] true)))))
                           (gui/attach-event "input" :key-pressed (fn [widgets name code]
                                                                    (when (= code :enter)
                                                                      (-> widgets
                                                                          (assoc-in [name :args :selected?] nil)
                                                                          (assoc-in ["input" :args :selected?] true)))))
                           (assoc-in ["click" :args :x] 100)
                           (assoc-in ["click" :args :y] 400))))
  

  ;; (gui/update! "click" [:events :mouse-clicked] (fn [wdg]
  ;;                                                 (gui/close-window)))
  
  ;; (gui/update! "input" [:events :key-pressed] (fn [wdg code]
  ;;                                               (when (= code :enter)
  ;;                                                 (gui/update! "input" [:args :selected?] nil)
  ;;                                                 (gui/update! "input1" [:args :selected?] true))))
  ;; (gui/update! "input1" [:events :key-pressed] (fn [wdg code]
  ;;                                                (when (= code :enter)
  ;;                                                  (gui/update! "input1" [:args :selected?] nil)
  ;;                                                  (gui/update! "input" [:args :selected?] true))))
  ;; (gui/update! "click" [:args :x] 100 [:args :y] 400))
)

(defmethod wdg/widget-global-event :mouse-clicked [_ canvas x y]
  (println "Mouse clicked x:" x " y:" y))
                                                