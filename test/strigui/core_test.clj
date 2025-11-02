(ns strigui.core-test
  (:require [clojure.test :refer :all]
            [clojure.set :as s]
            [strigui.core :refer :all]))

(deftest test-simple-window-button
  (testing "Simple test for a window with button"
    (swap-widgets! #(-> %
                        (add-window "test-window" 50 50 500 500 "simple-window-button-test" {})
                        (add-button "test-window" "test-button" "This is a test button" {:x 100 :y 50})))
    (Thread/sleep 500)
    (let [widgets (inspect-widgets)
          window (get widgets "test-window")
          button (get widgets "test-button")]
      (is (= "simple-window-button-test" (-> window :props :title)) "Test Window must exist and contain the title")
      (is (= "This is a test button" (-> button :value)) "Test Button must exist and contain the title")
      (is (= 100 (-> button :props :x)) "Test button must have unchanged x coordinate")
      (is (= 50 (-> button :props :y)) "Test button must have unchanged y coordinate"))))

(deftest test-simple-button-with-event
  (testing "Simple test for a window with button"
    (let [event-fn (fn [wdgs _]
                     nil)]
      (swap-widgets! #(-> %
                          (add-window "test-window" 50 50 500 500 "simple-button-with-event-test" {})
                          (add-button "test-window" "test-button" "This is a test button" {:x 100 :y 50})
                          (attach-event "test-button" :mouse-clicked event-fn)))
      (Thread/sleep 500)
      (let [widgets (inspect-widgets)
            mouse-clicked-fn (-> (get widgets "test-button") :events :mouse-clicked)]
        (is (= event-fn mouse-clicked-fn) "The mouse clicked test function must be attached and must be the same unmodified funtion as handed over")))))

(deftest test-add-multiple-widgets-at-once
  (testing "Test to add multiple widgets at once"
    (swap-widgets! #(-> %
                        (add-window "test-window" 50 50 500 500 "simple-button-with-event-test" {})
                        (add-multiple "test-window" strigui.button.Button "btn-1" "1" "btn-2" "2" "btn-3" "3")))
    (Thread/sleep 500)
    (let [widgets (inspect-widgets)
          wkeys (keys widgets)
          expected #{"btn-1" "btn-2" "btn-3"}]
      (is (= expected (s/intersection (set wkeys) expected))))))

(deftest test-assoc-property-change-width
  (testing "Test to change the width property of multiple widgets at once"
    (swap-widgets! #(-> %
                        (add-window "test-window" 50 50 500 500 "simple-button-with-event-test" {})
                        (add-multiple "test-window" strigui.button.Button "btn-1" "1" "btn-2" "2" "btn-3" "3")
                        (assoc-property :width 100 "btn-1" "btn-2" "btn-3")))
    (Thread/sleep 500)
    (let [widgets (inspect-widgets)
          buttons (vals (select-keys widgets ["btn-1" "btn-2" "btn-3"]))]
      (doseq [btn-width (map #(-> % :props :width) buttons)]
        (is (= 100 btn-width))))))

(deftest test-arrange
  (testing "Test the arrangement of a set of widgets"
    (swap-widgets! #(-> %
                        (add-window "test-window" 50 50 500 500 "simple-button-with-event-test" {})
                        (add-multiple "test-window" strigui.button.Button "btn-1" "1" "btn-2" "2" "btn-3" "3"
                                      "btn-4" "4" "btn-5" "5" "btn-6" "6"
                                      "btn-7" "7" "btn-8" "8" "btn-9" "9")
                        (assoc-property :width 150 "btn-1" "btn-2" "btn-3" "btn-4" "btn-5" "btn-6" "btn-7" "btn-8" "btn-9")
                        (assoc-property :height 50 "btn-1" "btn-2" "btn-3" "btn-4" "btn-5" "btn-6" "btn-7" "btn-8" "btn-9")
                        (arrange "test-window" {:x 0 :y 0 :width 500 :height 500 :spacing-horizontally 20 :spacing-vertically 30} "btn-1" "btn-2" "btn-3" 
                                 "btn-4" "btn-5" "btn-6" "btn-7" "btn-8" "btn-9")))
    (Thread/sleep 500)
    (let [widgets (inspect-widgets)
          buttons (sort-by :name (vals (select-keys widgets ["btn-1" "btn-2" "btn-3" "btn-4" "btn-5" "btn-6" "btn-7" "btn-8" "btn-9"])))
          actual (mapv (fn [wdg]
                           (vec (cons (-> wdg :name) (cons (-> wdg :props :window) (vals (select-keys (-> wdg :props) [:x :y :width :height])))))) buttons)
          expected [["btn-1" "test-window" 0 0 150 50] ["btn-2" "test-window" 170 0 150 50] ["btn-3" "test-window" 340 0 150 50]
                    ["btn-4" "test-window" 0 80 150 50] ["btn-5" "test-window" 170 80 150 50] ["btn-6" "test-window" 340 80 150 50]
                    ["btn-7" "test-window" 0 160 150 50] ["btn-8" "test-window" 170 160 150 50] ["btn-9" "test-window" 340 160 150 50]]]
      (is (= expected actual)))))
