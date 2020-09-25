(ns test.check.insights
  (:require [clojure.test.check.properties :as prop]))

(defmacro for-all
  [insights bindings & body]
  (merge
   {::property
    `(prop/for-all
      ~bindings
      ~@body)}
   insights))

(comment
  (for-all
   {::coverage
    [{:negative {::classify (fn [x] (< x 0))
                 ::cover    50}
      :positive {::classify (fn [x] (>= x 0))
                 ::cover    50}
      :ones     {::classify (fn [x] (= x 1))
                 ::cover    1.2}}]
    ::labels  [{:negative {::classify                  (fn [x] (< x 0))
                           ::future-presentation-thing (fn [x] (str x))}
                :positive (fn [x] (>= x 0))
                :ones     (fn [x] (= 1 x))}
               {:more-neg (fn [x] (< x -100))
                :less-neg (fn [x] (and (> x -100) (< x 0)))}]
    ::collect [{::collector (fn [x] (count x))}]}
   [x gen/int]
   (= (inc x) (sut x)))
  )
