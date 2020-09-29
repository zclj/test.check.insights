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
    (is (= {:negative 2, :positive 2}
           (sut/apply-coverage
            {:negative {:test.check.insights/classify (fn [x] (< x 0))}
             :positive {:test.check.insights/classify (fn [x] (>= x 0))}}
            [[0] [1] [-1] [-2]]))))
  (testing "Count is 0 if there are no classifications"
    (is (= {:negative 0}
           (sut/apply-coverage
            {:negative {:test.check.insights/classify (fn [x] (< x 0))}}
            [[0] [1]])))))

(deftest evaluate-coverage
  (testing "Coverage is inconclusive [false, false]"
    (is (= {:negative {::sut/sufficiently-covered?   false,
                       ::sut/insufficiently-covered? false}}
           (sut/evaluate-coverage
            {:negative {:test.check.insights/cover 5}}
            {:negative 2}
            100))))
  (testing "Coverage is sufficient [true, false]"
    (is (= {:negative {::sut/sufficiently-covered?   true,
                       ::sut/insufficiently-covered? false}}
           (sut/evaluate-coverage
            {:negative {:test.check.insights/cover 5}}
            {:negative 20}
            100))))
  (testing "Coverage is insufficient [false, true]"
    (is (= {:negative {::sut/sufficiently-covered?   false,
                       ::sut/insufficiently-covered? true}}
           (sut/evaluate-coverage
            {:negative {:test.check.insights/cover 5}}
            {:negative 0}
            1000)))))

;; Make sure constants are not changed by misstake
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
