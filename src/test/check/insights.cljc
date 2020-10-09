(ns test.check.insights
  (:require [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen]
            [clojure.test.check :as tc]
            [test.check.insights.coverage :as cv]
            [test.check.insights.labels :as lb]
            [test.check.insights.collect :as cl]))

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

(defn coverage-check
  [n {:keys [::property ::coverage]}]
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
          cv-result          (cv/apply-coverage coverage @report-db)
          eval-result        (cv/evaluate-coverage coverage cv-result test-count)
          ;; TODOiiiii
          sufficent          (cv/filter-sufficient eval-result)
          all-sufficient?    (= (count sufficent) (count coverage))
          insufficient       (cv/filter-insufficient eval-result)
          ;; TODO -> bool?
          some-insufficient? (boolean (seq insufficient))
          report             eval-result
          ;; (-> {}
          ;;     ;;(assoc :report @report-db)
          ;;     ;;(assoc :counts cv-result)
          ;;     (assoc ::cv/evaluated eval-result)
          ;;     ;;(assoc :sufficient sufficent)
          ;;     ;;(assoc :all-sufficient? all-sufficient?)
          ;;     ;;(assoc :insufficient insufficient)
          ;;     ;;(assoc :some-insufficient? some-insufficient?)
          ;;     ;;(assoc :coverage-report {:test-count test-count})
          ;;     )
          ]
      ;;(println test-count)
      ;; TODOL all-sufficeient complement some-insiffdf?
      (cond
        all-sufficient?    (merge qc-result
                                  {::coverage report
                                   ;;[(assoc report :status :success)]
                                   })
        some-insufficient? (merge qc-result
                                  {:pass? false}
                                  {::coverage report
                                   ;; [(-> report (assoc ::cv/status :failed)
                                   ;;       ;; (assoc ::cv/statistically-failed
                                   ;;       ;;        (mapv
                                   ;;       ;;         (fn [[k coverage]]
                                   ;;       ;;           [k
                                   ;;       ;;            (::cv/target-% coverage)])
                                   ;;       ;;         insufficient))
                                   ;;       )]
                                   })
        (> test-count 10000000)
        (assoc qc-result ::coverage (assoc report ::cv/status :gave-up))
        :else              (recur (inc i))))))

;; TODO: Is it ok to put check coverage result in vec to better fit reporting or not?

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API

(defn check-coverage
  "Run statistical hypothesis check to establish if the given coverage of values produced by a property can be achieved"
  [n {:keys [::property ::coverage]}]
  (if (= (count coverage) 1)
    (coverage-check n {::property property ::coverage (first coverage)})
    (mapv #(coverage-check n {::property property ::coverage %}) coverage)))


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
                   ::cover    10}}]}
     [x gen/int]
     (= x x)))

  (check-coverage 100 property)

  (map humanize-report (check-coverage 100 property))

  (def property-with-one-category
    (for-all
     {::coverage
      [{:negative {::classify (fn [x] (< x 0))
                   ::cover    50}
        :positive {::classify (fn [x] (>= x 0))
                   ::cover    50}
        :ones     {::classify (fn [x] (= x 1))
                   ::cover    1.2}}]}
     [x gen/int]
     (= x x)))

  (check-coverage 100 property-with-one-category)

  (humanize-report (check-coverage 100 property-with-one-category))
  )



(defn quick-check
  "Wraps test.check.quick-check with insights. opts are passed to test.check.quick-check so any options supported by quick-check are valid. Note that coverage failure will not fail the tests. If a :reporter-fn is provided it will be called before labels are applied."
  [num-tests {:keys [::property ::coverage ::labels ::collect]} & opts]
  (let [{:keys [reporter-filter-fn reporter-fn]
         :or   {reporter-filter-fn (fn [m] (= (:type m) :trial))}}
        opts
        labels-db            (atom (lb/init-labels labels))
        reporter-db          (atom [])
        insights-reporter-fn (fn [m]
                               (when reporter-fn
                                 (reporter-fn m))
                               (when (reporter-filter-fn m)
                                 (swap! labels-db lb/update-labels (:args m))
                                 (swap! reporter-db conj (:args m))))
        final-opts           (concat opts [:reporter-fn insights-reporter-fn])
        quick-check-result   (apply tc/quick-check num-tests property final-opts)
        coverage-result      (cv/report-coverage coverage @reporter-db)
        collector-result     (if collect
                               (cl/collect collect @reporter-db)
                               {})]
    (-> quick-check-result
        (assoc ::labels (mapv ::labels @labels-db))
        (assoc ::coverage coverage-result)
        (assoc ::collect collector-result))))

;; TODO: make statistically covered #{}

(defn humanize-report
  [{:keys [::labels ::coverage ::collect] :as report}]
  (cond-> report
    labels   (update ::labels lb/humanize-report)
    coverage (update ::coverage cv/humanize-report)
    collect  (update ::collect cl/humanize-report)))
  

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
      ::collect [{::collector (fn [x] (identity x))}]}
     [x gen/int]
     (= (inc x) (sut x))))

  (quick-check 10 property)

  (quick-check 10 property :seed 1)

  (quick-check
   10 property
   :seed 1 :reporter-fn #(println %)
   :reporter-filter-fn (fn [m] (println "I'm a FILTER") (= (:type m) :trial)))

  (humanize-report (quick-check 10 property))
  )
