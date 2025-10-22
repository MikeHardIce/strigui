(ns strigui.core-test
  (:require [clojure.test :refer :all]
            [strigui.core :refer :all]))

(deftest simple-window-button-test
  (testing "Simple test for a window with button"
    (swap-widgets! #(-> %
                        (add-window "test-window" 50 50 500 500 "This is a test window" {})
                        (add-button "test-window" "test-button" "This is a test button" {:x 100 :y 50})))
    (Thread/sleep 500)
    (let [widgets (inspect-widgets)
          window (get widgets "test-window")
          button (get widgets "test-button")]
      (is (= "This is a test window" (-> window :props :title)) "Test Window must exist and contain the title")
      (is (= "This is a test button" (-> button :value)) "Test Button must exist and contain the title")
      (is (= 100 (-> button :props :x)) "Test button must have unchanged x coordinate")
      (is (= 50 (-> button :props :y)) "Test button must have unchanged y coordinate"))))

(deftest simple-button-with-event-test
  (testing "Simple test for a window with button"
    (let [event-fn (fn [wdgs _]
                     nil)]
      (swap-widgets! #(-> %
                          (add-window "test-window" 50 50 500 500 "This is a test window" {})
                          (add-button "test-window" "test-button" "This is a test button" {:x 100 :y 50})
                          (attach-event "test-button" :mouse-clicked event-fn)))
      (Thread/sleep 500)
      (let [widgets (inspect-widgets)
            mouse-clicked-fn (-> (get widgets "test-button") :events :mouse-clicked)]
        (is (= event-fn mouse-clicked-fn) "The mouse clicked test function must be attached and must be the same unmodified funtion as handed over")))
    ))
