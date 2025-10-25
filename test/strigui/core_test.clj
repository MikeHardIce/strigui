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

(deftest test-arrange-horizontally
  (testing "Test the arrangement a set of widgets horizontally"))
