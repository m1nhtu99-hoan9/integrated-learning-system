(ns integrated-learning-system.utils-test.datetime
  (:require [clojure.test :refer :all]
            [integrated-learning-system.utils.datetime :as dt :refer :all])
  (:import [java.time LocalDate]))


(deftest string->local-date--test
  (let [expected (LocalDate/of 2022 10 20)]
    (testing "with valid input"
      (doseq [input (vector "20/10/2022" "2022-10-20")]
        (is (= (string->local-date input)
               expected))))
    (testing "with invalid input"
      (doseq [invalid-input (vector "20-Oct-2022" "20-10-2022" "2022/10/20")]
        (is (contains? (string->local-date invalid-input)
                       ::dt/error)))))

  (testing "with tricky edge-case input"
    (let [arranged (LocalDate/of 2022 02 28)]
      (doseq [tricky-input (vector "30/02/2022" "2022-02-30")]
        (let [actual (string->local-date tricky-input)]
          (is (not= actual arranged))
          (is (contains? actual ::dt/error)))))))
