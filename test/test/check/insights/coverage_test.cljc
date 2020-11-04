(ns test.check.insights.coverage-test
  (:require [clojure.test :refer [deftest testing is]]
            [test.check.insights.coverage :as sut]))

(deftest filter-in-sufficient
  (testing "Filter sufficiently coverad values from evaluation result"
    (is (= [[:negative {::sut/sufficiently-covered? true}]]
           (sut/filter-sufficient
            {:negative {::sut/sufficiently-covered? true}
             :positive {::sut/sufficiently-covered? false}}))))
  (testing "Filter insuffcientyly covered values from evaluation result"
    (is (= [[:negative {::sut/insufficiently-covered? true}]]
           (sut/filter-insufficient
            {:negative {::sut/insufficiently-covered? true}
             :positive {::sut/insufficiently-covered? false}})))))

(deftest apply-coverage
  (testing "Returns count of classifications"
    (is (= {:negative {::sut/count 2}
            :positive {::sut/count 2}}
           (sut/apply-coverage
            {:negative {:test.check.insights/classify (fn [x] (< x 0))}
             :positive {:test.check.insights/classify (fn [x] (>= x 0))}}
            [[0] [1] [-1] [-2]]))))
  (testing "Count is 0 if there are no classifications"
    (is (= {:negative {::sut/count 0}}
           (sut/apply-coverage
            {:negative {:test.check.insights/classify (fn [x] (< x 0))}}
            [[0] [1]])))))

(deftest evaluate-coverage
  (testing "Coverage is inconclusive [false, false]"
    (is (= {:negative {::sut/sufficiently-covered?   false,
                       ::sut/insufficiently-covered? false
                       ::sut/count          2
                       ::sut/target-%       5}}
           (sut/evaluate-coverage
            {:negative {:test.check.insights/cover 5}}
            {:negative {::sut/count 2}}
            100))))
  (testing "Coverage is sufficient [true, false]"
    (is (= {:negative {::sut/sufficiently-covered?   true,
                       ::sut/insufficiently-covered? false
                       ::sut/count          20
                       ::sut/target-%       5}}
           (sut/evaluate-coverage
            {:negative {:test.check.insights/cover 5}}
            {:negative {::sut/count 20}}
            100))))
  (testing "Coverage is insufficient [false, true]"
    (is (= {:negative {::sut/sufficiently-covered?   false,
                       ::sut/insufficiently-covered? true
                       ::sut/count          0
                       ::sut/target-%       5}}
           (sut/evaluate-coverage
            {:negative {:test.check.insights/cover 5}}
            {:negative {::sut/count 0}}
            1000))))
  (testing "Multiple covage classifications"
    (is (= {:negative {::sut/sufficiently-covered?   false,
                       ::sut/insufficiently-covered? false
                       ::sut/count          2
                       ::sut/target-%       5}
            :positive {::sut/sufficiently-covered?   false,
                       ::sut/insufficiently-covered? false
                       ::sut/count          20
                       ::sut/target-%       50}}
           (sut/evaluate-coverage
            {:negative {:test.check.insights/cover 5}
             :positive {:test.check.insights/cover 50}}
            {:negative {::sut/count 2}
             :positive {::sut/count 20}}
            100))))
  (testing "Should handle 0 number of tests and examples"
    (is (= {:negative {::sut/sufficiently-covered?   false,
                       ::sut/insufficiently-covered? false
                       ::sut/count          0
                       ::sut/target-%       5}
            :positive {::sut/sufficiently-covered?   false,
                       ::sut/insufficiently-covered? false
                       ::sut/count          0
                       ::sut/target-%       50}}
           (sut/evaluate-coverage
            {:negative {:test.check.insights/cover 5}
             :positive {:test.check.insights/cover 50}}
            {:negative {::sut/count 0}
             :positive {::sut/count 0}}
            0)))))

(deftest report-coverage
  (testing "Report the coverage result of multiple coverage categories"
    (let [coverage-categories
          [{:negative {:test.check.insights/classify (fn [x] (< x 0))
                       :test.check.insights/cover    50}
            :positive {:test.check.insights/classify (fn [x] (>= x 0))
                       :test.check.insights/cover    50}
            :ones     {:test.check.insights/classify (fn [x] (= x 1))
                       :test.check.insights/cover    5}}
           {:more-neg {:test.check.insights/classify (fn [x] (< x -100))
                       :test.check.insights/cover    10}
            :less-neg {:test.check.insights/classify (fn [x] (and (> x -100) (< x 0)))
                       :test.check.insights/cover    10}}]]
      (is (= [{:negative
               {::sut/sufficiently-covered?   false
                ::sut/insufficiently-covered? false
                ::sut/count                   3
                ::sut/target-%                50}
               :positive
               {::sut/sufficiently-covered?   false
                ::sut/insufficiently-covered? false
                ::sut/count                   2
                ::sut/target-%                50}
               :ones
               {::sut/sufficiently-covered?   false
                ::sut/insufficiently-covered? false
                ::sut/count                   1
                ::sut/target-%                5}}
              {:more-neg
               {::sut/sufficiently-covered?   false
                ::sut/insufficiently-covered? false
                ::sut/count                   1
                ::sut/target-%                10}
               :less-neg
               {::sut/sufficiently-covered?   false
                ::sut/insufficiently-covered? false
                ::sut/count                   2
                ::sut/target-%                10}}]
             (sut/report-coverage
              coverage-categories
              [[1] [-1] [-2] [-150] [10]])))))
  (testing "Report should handle 0 arguments"
    (let [coverage-categories
          [{:negative {:test.check.insights/classify (fn [x] (< x 0))
                       :test.check.insights/cover    50}}]]
      (is (= [{:negative	  
	       {::sut/sufficiently-covered?   false
                ::sut/insufficiently-covered? false
                ::sut/count                   0
                ::sut/target-%                50}}]
             (sut/report-coverage coverage-categories []))))))

(deftest humanize-coverage-report
  (testing "Humanize the format of the reported coverage"
    (let [coverage-report
          [{:negative
            {::sut/sufficiently-covered?   true
             ::sut/insufficiently-covered? false
             ::sut/count                   4
             ::sut/target-%                50}
            :positive
            {::sut/sufficiently-covered?   false
             ::sut/insufficiently-covered? false
             ::sut/count                   2
             ::sut/target-%                50}
            :ones
            {::sut/sufficiently-covered?   true
             ::sut/insufficiently-covered? false
             ::sut/count                   4
             ::sut/target-%                5}}
           {:more-neg
            {::sut/sufficiently-covered?   false
             ::sut/insufficiently-covered? false
             ::sut/count                   1
             ::sut/target-%                10}
            :less-neg
            {::sut/sufficiently-covered?   false
             ::sut/insufficiently-covered? true
             ::sut/count                   9
             ::sut/target-%                10}}]]
      (is (= [{:negative
               #:test.check.insights{:coverage 40.0 :target-coverage 50}
               :positive
               #:test.check.insights{:coverage 20.0 :target-coverage 50}
               :ones
               #:test.check.insights{:coverage 40.0 :target-coverage 5}
               :test.check.insights/statistically-failed #{:positive}}
              {:more-neg
               #:test.check.insights{:coverage 10.0 :target-coverage 10}
               :less-neg
               #:test.check.insights{:coverage 90.0 :target-coverage 10}
               :test.check.insights/statistically-failed #{:more-neg :less-neg}}] 
             (sut/humanize-report coverage-report)))))
  (testing "failed should only be included if coverage actually failed"
    (is (= [{:negative
             #:test.check.insights{:coverage 100.0 :target-coverage 50}}]
           (sut/humanize-report
            [{:negative
              {::sut/sufficiently-covered?   true
               ::sut/insufficiently-covered? false
               ::sut/count                   4
               ::sut/target-%                50}}])))))


;; Make sure constants are not changed by mistake
(deftest constanst-should-be-correct
  (let [a1 -3.969683028665376e+01
        a2 2.209460984245205e+02
        a3 -2.759285104469687e+02
        a4 1.383577518672690e+02
        a5 -3.066479806614716e+01
        a6 2.506628277459239e+00]
    (is (= a1 sut/a1))
    (is (= a2 sut/a2))
    (is (= a3 sut/a3))
    (is (= a4 sut/a4))
    (is (= a5 sut/a5))
    (is (= a6 sut/a6)))
  (let [b1 -5.447609879822406e+01
        b2 1.615858368580409e+02
        b3 -1.556989798598866e+02
        b4 6.680131188771972e+01
        b5 -1.328068155288572e+01]
    (is (= b1 sut/b1))
    (is (= b2 sut/b2))
    (is (= b3 sut/b3))
    (is (= b4 sut/b4))
    (is (= b5 sut/b5)))
  (let [c1 -7.784894002430293e-03
        c2 -3.223964580411365e-01
        c3 -2.400758277161838e+00
        c4 -2.549732539343734e+00
        c5 4.374664141464968e+00
        c6 2.938163982698783e+00]
    (is (= c1 sut/c1))
    (is (= c2 sut/c2))
    (is (= c3 sut/c3))
    (is (= c4 sut/c4))
    (is (= c5 sut/c5))
    (is (= c6 sut/c6)))
  (let [d1 7.784695709041462e-03
        d2 3.224671290700398e-01
        d3 2.445134137142996e+00
        d4 3.754408661907416e+00]
    (is (= d1 sut/d1))
    (is (= d2 sut/d2))
    (is (= d3 sut/d3))
    (is (= d4 sut/d4)))
  (let [p_low  0.02425
        p_high (- 1 p_low)]
    (is (= p_low sut/p-low))
    (is (= p_high sut/p-high))))
