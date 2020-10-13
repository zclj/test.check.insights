(ns test.check.insights.collect-test
  (:require [clojure.test :refer [deftest is]]
            [test.check.insights.collect :as sut]))

(deftest collect-arguments
  (is (= [{4 [[[1 2 3 4]]]
           2 [[[1 2]] [[3 3]]]}	  
          {true [[[1 2 3 4]] [[1 2]]]
           nil  [[[3 3]]]}]
         (sut/collect
          [{:test.check.insights/collector (fn [x] (count x))}
           {:test.check.insights/collector (fn [x] (some even? x))}]
          [[[1 2 3 4]] [[1 2]] [[3 3]]]))))

(deftest humanize-report
  (is (= [{0  20.0
           -2 20.0
           1  10.0
           4  20.0
           5  10.0
           6  10.0
           -6 10.0}	  
          {1 50.0
           2 50.0}]
         (sut/humanize-report
          [{0  [[0] [0]]
            -2 [[-2] [-2]]
            1  [[1]]
            4  [[4] [4]]
            5  [[5]]
            6  [[6]]
            -6 [[-6]]}
           {1 [[1] [1]]
            2 [[2] [2]]}]))))
