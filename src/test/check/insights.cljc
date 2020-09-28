(ns test.check.insights
  (:require [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen]
            [clojure.test.check :as tc]
            [test.check.insights.coverage :as cv]))

(defn power-of-2
  [x]
  (int (Math/pow 2 x)))

;; TODO - adapt to vector for apply and evaluate
(defn coverage-check
  [{:keys [::property ::coverage]} n]
  (loop [i 0]
    (let [test-count         (* (power-of-2 i) n)
          report-db          (atom [])
          qc-result
          (tc/quick-check
           test-count
           property
           :reporter-fn
           (fn [m]
             (when (= (:type m) :trial)
               (swap! report-db conj (:args m)))))
          cv-result          (cv/apply-coverage (first coverage) @report-db)
          eval-result        (cv/evaluate-coverage (first coverage) cv-result test-count)
          sufficent          (cv/filter-sufficient eval-result)
          all-sufficient?    (= (count sufficent) (count coverage))
          insufficient       (cv/filter-insufficient eval-result)
          some-insufficient? (boolean (seq insufficient))
          report             (-> {}
                                 ;;(assoc :report @report-db)
                                 (assoc :coverage cv-result)
                                 (assoc :eval-result eval-result)
                                 ;;(assoc :sufficient sufficent)
                                 ;;(assoc :all-sufficient? all-sufficient?)
                                 ;;(assoc :insufficient insufficient)
                                 ;;(assoc :some-insufficient? some-insufficient?)
                                 ;;(assoc :coverage-report {:test-count test-count})
                                 )]
      ;;(println test-count)
      (cond
        all-sufficient?      (assoc qc-result :coverage
                                    (assoc report :status :success))
        some-insufficient?   (assoc qc-result :coverage
                                    (-> report
                                        (assoc :status :failed)
                                        (assoc :insufficient
                                               (mapv
                                                (fn [in]
                                                  (let [k (first in)]
                                                    [k (get-in coverage [k :cover])]))
                                                insufficient))))
        (> test-count 10000000)
        (assoc qc-result :coverage (assoc report :status :gave-up))
        :else                (recur (inc i))))))


(defmacro for-all
  [insights bindings & body]
  (merge
   {::property
    `(prop/for-all
      ~bindings
      ~@body)}
   insights))

(comment
  (defn sut
    [x]
    (inc x))

  (def property
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
     (= (inc x) (sut x))))

  (coverage-check property 100)
  )
