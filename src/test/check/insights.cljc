(ns test.check.insights
  (:require [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen]
            [clojure.test.check :as tc]
            [test.check.insights.coverage :as cv]
            [test.check.insights.labels :as lb]))

(defmacro for-all
  [insights bindings & body]
  (merge
   {::property
    `(prop/for-all
      ~bindings
      ~@body)}
   insights))

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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API

(defn check-coverage
  "Run statistical hypothesis check to establish if the given coverage of values produced by a property can be achieved"
  [{:keys [::property ::coverage]} n])


(comment
  (defn y
    [& {:keys [a b]}]
    [a b])
  
  (defn x
    [stuff & opts]
    (let [{:keys [filter-fn] :or {filter-fn :default}} opts]
      [(apply y opts) filter-fn]))

  (apply y (concat '(:a 1 :b 2) [:b 3]))
  (x 1 :a 3 :b 4 :filter-fn :the-filter)
  (x 1 :a 3 :b 4)

  (apply str "foo" {:a 1 :b 2})
  )

(defn quick-check
  "Wraps test.check.quick-check with insights. opts are passed to test.check.quick-check so any options supported by quick-check are valid. Note that coverage failure will not fail the tests. If a :reporter-fn is provided it will be called before labels are applied."
  [num-tests {:keys [::property ::coverage ::labels ::collect]} & opts]
  (let [{:keys [reporter-filter-fn reporter-fn]
         :or   {reporter-filter-fn (fn [m] (= (:type m) :trial))}}
        opts
        ;; TODO - should classifications and label results be separated?
        labels-db   (atom (lb/init-labels labels))
        reporter-db (atom [])
        insights-reporter-fn
        (fn [m]
          (when reporter-fn
            (reporter-fn m))
          (when (reporter-filter-fn m)
            (swap! labels-db lb/update-labels (:args m))
            (swap! reporter-db conj (:args m))))
        final-opts  (concat opts [:reporter-fn insights-reporter-fn])
        quick-check-result
        (apply (partial tc/quick-check num-tests property) final-opts)
        coverage-result
        (reduce
         (fn [acc coverage-category]
           (let [coverage-result
                 (cv/apply-coverage coverage-category @reporter-db)
                 evaluated-result
                 (cv/evaluate-coverage
                  coverage-category coverage-result (count @reporter-db))
                 failed (reduce-kv
                         (fn [acc k v]
                           (if (not (::cv/sufficiently-covered? v))
                             (conj acc k)
                             acc))
                         []
                         evaluated-result)]
             (conj acc (assoc coverage-result ::failed failed))))
         []
         coverage)
        ]
    (-> quick-check-result
        (assoc ::labels (mapv ::labels @labels-db))
        (assoc ::coverage coverage-result))))

(comment
  (def property
    (for-all
     {::coverage
      [{:negative {::classify (fn [x] (< x 0))
                   ::cover    50}
        :positive {::classify (fn [x] (>= x 0))
                   ::cover    50}
        :ones     {::classify (fn [x] (= x 1))
                   ::cover    1.2}}
       {:more-neg {::classify (fn [x] (< x -100))
                   ::cover    10}
        :less-neg {::classify (fn [x] (and (> x -100) (< x 0)))
                   ::cover    10}}]
      ::labels
      [{:negative {::classify (fn [x] (< x 0))}
        :positive {::classify (fn [x] (>= x 0))}
        :ones     {::classify (fn [x] (= 1 x))}}
       {:more-neg {::classify (fn [x] (< x -100))}
        :less-neg {::classify (fn [x] (and (> x -100) (< x 0)))}}]
      ::collect [{::collector (fn [x] (count x))}]}
     [x gen/int]
     (= (inc x) (sut x))))

  (quick-check 10 property)

  (quick-check 10 property :seed 1)

  (quick-check
   10 property
   :seed 1 :reporter-fn #(println %)
   :reporter-filter-fn (fn [m] (println "I'm a FILTER") (= (:type m) :trial)))
  )

(comment
  (def property-1
    (prop/for-all [x gen/int] (= (inc x) (sut x))))

  (defn report
    []
    (let [labels-db (atom [])]
      (fn [m]
        (swap! labels-db conj (:args m)))))
  
  (tc/quick-check 10 property-1 :reporter-fn (report))

  @labels-db
  )



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
      ::labels  [{:negative {::classify (fn [x] (< x 0))}
                  :positive (fn [x] (>= x 0))
                  :ones     (fn [x] (= 1 x))}
                 {:more-neg (fn [x] (< x -100))
                  :less-neg (fn [x] (and (> x -100) (< x 0)))}]
      ::collect [{::collector (fn [x] (count x))}]}
     [x gen/int]
     (= (inc x) (sut x))))

  (coverage-check property 100)
  )
