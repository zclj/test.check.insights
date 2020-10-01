(ns test.check.insights.collect-test
  (:require [clojure.test :refer [deftest testing is]]
            [test.check.insights.collect :as sut]))

(deftest collect-arguments
  (is (= [{4 [[[1 2 3 4]]]
           2 [[[1 2]] [[3 3]]]}	  
          {true [[[1 2 3 4]] [[1 2]]]
           nil [[[3 3]]]}]
         (sut/collect
          [{:test.check.insights/collector (fn [x] (count x))}
           {:test.check.insights/collector (fn [x] (some even? x))}]
          [[[1 2 3 4]] [[1 2]] [[3 3]]])))
  )

