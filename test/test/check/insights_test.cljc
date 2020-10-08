(ns test.check.insights-test
  (:require [clojure.test :refer [deftest testing is]]
            [test.check.insights :as sut]
            [clojure.test.check.generators :as gen]))

(def property
  (sut/for-all
   {:test.check.insights/coverage
    [{:negative {:test.check.insights/classify (fn [x] (< x 0))
                 :test.check.insights/cover    50}
      :positive {:test.check.insights/classify (fn [x] (>= x 0))
                 :test.check.insights/cover    50}
      :ones     {:test.check.insights/classify (fn [x] (= x 1))
                 :test.check.insights/cover    1.2}}
     {:more-neg {:test.check.insights/classify (fn [x] (< x -100))
                 :test.check.insights/cover    10}
      :less-neg {:test.check.insights/classify (fn [x] (and (> x -100) (< x 0)))
                 :test.check.insights/cover    10}}]
    :test.check.insights/labels
    [{:negative {:test.check.insights/classify (fn [x] (< x 0))}
      :positive {:test.check.insights/classify (fn [x] (>= x 0))}
      :ones     {:test.check.insights/classify (fn [x] (= 1 x))}}
     {:more-neg {:test.check.insights/classify (fn [x] (< x -100))}
      :less-neg {:test.check.insights/classify (fn [x] (and (> x -100) (< x 0)))}}]
    :test.check.insights/collect
    [{:test.check.insights/collector (fn [x] (identity x))}]}
   [x gen/int]
   (= x x)))

(declare expected-test-result)
(declare expected-humanized-report)

(deftest quick-check
  (is (= expected-test-result
         (sut/quick-check 10 property :seed 1))))

(deftest humanize-report
  (is (= expected-humanized-report
         (sut/humanize-report expected-test-result))))


(def expected-test-result
  {:result          true
   :pass?           true
   :num-tests       10
   :time-elapsed-ms 1
   :seed            1,
   :test.check.insights/labels
   [{:test.check.insights/labled
     [[0] [0] [-2] [-2] [3] [0] [-3] [-2] [2] [2]],
     :test.check.insights/unlabled #{}
     :negative                     [[-2] [-2] [-3] [-2]]
     :positive                     [[0] [0] [3] [0] [2] [2]]
     :ones                         []}
    {:test.check.insights/labled   [[-2] [-2] [-3] [-2]],
     :test.check.insights/unlabled #{[3] [0] [2]},
     :more-neg                     []
     :less-neg                     [[-2] [-2] [-3] [-2]]}]
   :test.check.insights/coverage
   [{:negative
     #:test.check.insights.coverage
     {:coverage-count          4
      :target-coverage-%       50
      :sufficiently-covered?   false
      :insufficiently-covered? false}
     :positive
     #:test.check.insights.coverage
     {:coverage-count          6
      :target-coverage-%       50
      :sufficiently-covered?   false
      :insufficiently-covered? false}
     :ones
     #:test.check.insights.coverage
     {:coverage-count          0
      :target-coverage-%       1.2
      :sufficiently-covered?   false
      :insufficiently-covered? false}}
    {:more-neg
     #:test.check.insights.coverage
     {:coverage-count          0
      :target-coverage-%       10
      :sufficiently-covered?   false
      :insufficiently-covered? false}
     :less-neg
     #:test.check.insights.coverage
     {:coverage-count          4
      :target-coverage-%       10
      :sufficiently-covered?   false
      :insufficiently-covered? false}}]
   :test.check.insights/collect
   [{0  [[0] [0] [0]]
     -2 [[-2] [-2] [-2]]
     3  [[3]]
     -3 [[-3]]
     2  [[2] [2]]}]})

(def expected-humanized-report
  {:result          true,	  
   :pass?           true,
   :num-tests       10,
   :time-elapsed-ms 1,
   :seed            1,
   :test.check.insights/labels
   [{:negative 40.0, :positive 60.0, :ones 0.0}
    {:more-neg 0.0, :less-neg 100.0}],
   :test.check.insights/coverage
   [{:negative
     #:test.check.insights{:coverage 40.0, :target-coverage 50},
     :positive
     #:test.check.insights{:coverage 60.0, :target-coverage 50},
     :ones #:test.check.insights{:coverage 0.0, :target-coverage 1.2},
     :test.check.insights/statistically-failed
     [:negative :positive :ones]}
    {:more-neg                                 #:test.check.insights{:coverage 0.0, :target-coverage 10},
     :less-neg
     #:test.check.insights{:coverage 100.0, :target-coverage 10},
     :test.check.insights/statistically-failed [:more-neg :less-neg]}],
   :test.check.insights/collect
   [{0 30.0, -2 30.0, 3 10.0, -3 10.0, 2 20.0}]})
