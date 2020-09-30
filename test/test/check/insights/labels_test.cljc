(ns test.check.insights.labels-test
  (:require [clojure.test :refer [deftest testing is]]
            [test.check.insights.labels :as sut]))

(deftest init-labels
  (testing "Initializes the structure to store labels in"
    (let [label-classifications-category-1
          {:negative {:test.check.insights/classify (fn [x] (< x 0))}
           :positive {:test.check.insights/classify (fn [x] (>= x 0))}
           :ones     {:test.check.insights/classify (fn [x] (= 1 x))}}
          label-classifications-category-2
          {:more-neg {:test.check.insights/classify (fn [x] (< x -100))}
           :less-neg {:test.check.insights/classify
                      (fn [x] (and (> x -100) (< x 0)))}}]
      (is (= [{:test.check.insights/label-classifications label-classifications-category-1
               :test.check.insights/labels
               {:test.check.insights/labled   []
                :test.check.insights/unlabled #{}
                :negative                     []
                :positive                     []
                :ones                         []}}
              {:test.check.insights/label-classifications label-classifications-category-2
               :test.check.insights/labels
               {:test.check.insights/labled   []
                :test.check.insights/unlabled #{}
                :more-neg                     []
                :less-neg                     []}}]
             (sut/init-labels
              [label-classifications-category-1
               label-classifications-category-2]))))))

(deftest update-labels
  (testing "Label classifications should be updated given arguments"
    (let [label-classifications-category-1
          {:negative {:test.check.insights/classify (fn [x] (< x 0))}
           :positive {:test.check.insights/classify (fn [x] (>= x 0))}
           :ones     {:test.check.insights/classify (fn [x] (= 1 x))}}
          label-classifications-category-2
          {:more-neg {:test.check.insights/classify (fn [x] (< x -100))}
           :less-neg {:test.check.insights/classify
                      (fn [x] (and (> x -100) (< x 0)))}}
          labels [{:test.check.insights/label-classifications
                   label-classifications-category-1
                   :test.check.insights/labels
                   {:test.check.insights/labled   [[0] [-1]]
                    :test.check.insights/unlabled #{["a"]}
                    :negative                     [[-1]]
                    :positive                     [[0]]
                    :ones                         []}}
                  {:test.check.insights/label-classifications
                   label-classifications-category-2
                   :test.check.insights/labels
                   {:test.check.insights/labled   []
                    :test.check.insights/unlabled #{}
                    :more-neg                     []
                    :less-neg                     []}}]]
      (is (= [{:test.check.insights/label-classifications
               label-classifications-category-1
               :test.check.insights/labels
               {:test.check.insights/labled   [[0] [-1] [1] [1]]
                :test.check.insights/unlabled #{["a"]}
                :negative                     [[-1]]
                :positive                     [[0] [1]]
                :ones                         [[1]]}}
              {:test.check.insights/label-classifications
               label-classifications-category-2
               :test.check.insights/labels
               {:test.check.insights/labled   []
                :test.check.insights/unlabled #{[1]}
                :more-neg                     []
                :less-neg                     []}}]
             (sut/update-labels labels [1])))))
  )


