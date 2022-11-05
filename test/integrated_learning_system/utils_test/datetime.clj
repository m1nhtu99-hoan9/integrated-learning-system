(ns integrated-learning-system.utils-test.datetime
  (:require [clojure.test :refer :all]
            [integrated-learning-system.utils.datetime :as dt :refer :all])
  (:import (java.text SimpleDateFormat)
           (java.time LocalDate)))


(deftest string->date--test
  (let [expected-datetime (->> "20/10/2022 02:00:00"
                               (.parse (SimpleDateFormat. "dd/M/yyyy hh:mm:ss")))
        expected-dateonly (->> "20/10/2022"
                               (.parse (SimpleDateFormat. "dd/M/yyyy")))]
    (testing "with valid datetime strings"
      (doseq [datetime-input '("20/10/2022T02:00:00", "20/10/2022 02:00:00", "20/10/2022 2:0:0", "2022-10-20 02:00:00")]
        (is (= (string->date datetime-input)
               expected-datetime))))
    (testing "with valid date-only strings"
      (doseq [dateonly-input '("20/10/2022" "2022-10-20")]
        (is (= (string->date dateonly-input)
               expected-dateonly))))
    (testing "with invalid datetime strings"
      (doseq [invalid-input '("20-10-2022" "2022/10/20" "20-10-2022 2:0:0" "2022/10/20")]
        (is (= (string->date invalid-input) nil))))))

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
