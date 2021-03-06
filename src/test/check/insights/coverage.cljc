(ns test.check.insights.coverage)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Inverse normal cumulative distribution function (same as QuickCheck)

;; https://github.com/nick8325/quickcheck/blob/09a569db8de0df14f8514b30d4bfe7acb41f9c41/src/Test/QuickCheck/Test.hs#L606

;; Algorithm taken from
;; https://web.archive.org/web/20151110174102/http://home.online.no/~pjacklam/notes/invnorm/
;; Accurate to about one part in 10^9.

;; The 'erf' package uses the same algorithm, but with an extra step
;; to get a fully accurate result, which we skip because it requires
;; the 'erfc' function.

(def p-low 0.02425)
(def p-high (- 1 p-low))

(def a1 -3.969683028665376e+01)
(def a2  2.209460984245205e+02)
(def a3 -2.759285104469687e+02)
(def a4  1.383577518672690e+02)
(def a5 -3.066479806614716e+01)
(def a6  2.506628277459239e+00)

(def b1 -5.447609879822406e+01)
(def b2  1.615858368580409e+02)
(def b3 -1.556989798598866e+02)
(def b4  6.680131188771972e+01)
(def b5 -1.328068155288572e+01)

(def c1 -7.784894002430293e-03)
(def c2 -3.223964580411365e-01)
(def c3 -2.400758277161838e+00)
(def c4 -2.549732539343734e+00)
(def c5  4.374664141464968e+00)
(def c6  2.938163982698783e+00)

(def d1  7.784695709041462e-03)
(def d2  3.224671290700398e-01)
(def d3  2.445134137142996e+00)
(def d4  3.754408661907416e+00)

(defn invnormcdf
  [p]
  (cond
    (< p p-low)
    (let [q (Math/sqrt (* -2 (Math/log p)))]
      (/ (+ (* (+ (* (+ (* (+ (* (+ (* c1 q) c2) q) c3) q) c4) q) c5) q) c6)
         (+ (* (+ (* (+ (* (+ (* d1 q) d2) q) d3) q) d4) q) 1)))

    (<= p p-high)
    (let [q (- p 0.5)
          r (* q q)]
      (/ (* (+ (* (+ (* (+ (* (+ (* (+ (* a1 r) a2) r) a3) r) a4) r) a5) r) a6) q)
         (+ (* (+ (* (+ (* (+ (* (+ (* b1 r) b2) r) b3) r) b4) r) b5) r) 1)))

    :else
    (let [q (Math/sqrt (* -2 (Math/log (- 1 p))))]
      (-
       (/ (+ (* (+ (* (+ (* (+ (* (+ (* c1 q) c2) q) c3) q) c4) q) c5) q) c6)
          (+ (* (+ (* (+ (* (+ (* d1 q) d2) q) d3) q) d4) q) 1))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wilson scoring

(defn wilson
  [k n z]
  (let [nf n
        p  (/ k n)]
    (/
     (+ p
        (/ (* z z) (* 2 nf))
        (* z
           (Math/sqrt
            (+ (/ (* p (- 1 p)) nf)
               (/ (* z z) (* 4 nf nf))))))
     (+ 1 (/ (* z z) nf)))))

(defn wilson-low
  [k n a]
  (wilson k n (invnormcdf (/ a 2))))

(defn wilson-high
  [k n a]
  (wilson k n (invnormcdf (- 1 (/ a 2)))))

(defn sufficiently-covered?
  [{:keys [certainty tolerance]} n k p]
  (if-not (zero? n)
    (>= (wilson-low k n (/ 1 certainty))
        (* tolerance p))
    false))

(defn insufficiently-covered?
  [certainty n k p]
  (if-not (zero? n)
    (if certainty
      (< (wilson-high k n (/ 1 certainty)) p)
      (< k (* p n)))
    false))

(def default-confidence
  {:certainty 1.0E9
   :tolerance 0.9})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Coverage checks

(defn check-coverage
  ([tests-n class-n p]
   (check-coverage tests-n class-n p default-confidence))
  ([tests-n class-n p confidence]
   {::sufficiently-covered?
    (sufficiently-covered? confidence tests-n class-n p)
    ::insufficiently-covered?
    (insufficiently-covered? (:certainty confidence) tests-n class-n p)}))

(defn apply-coverage
  [coverage-m args]
  (reduce-kv
   (fn [acc k {:keys [test.check.insights/classify]}]
     (let [classification
           (mapv
            (fn [arg]
              (apply classify arg))
            args)]
       (assoc acc k {::count (count (filter identity classification))})))
   {}
   coverage-m))

(defn evaluate-coverage
  [coverage-m coverage number-of-tests]
  (reduce-kv
   (fn [acc k {:keys [test.check.insights/cover]}]
     (let [coverage-result
           (check-coverage
            number-of-tests (get-in coverage [k ::count]) (/ cover 100))]
       (merge
        acc
        {k (merge (get coverage k) {::target-% cover} coverage-result)})))
   {}
   coverage-m))

(defn filter-k
  [k eval-result]
  (filterv
   (fn [result]
     (get (val result) k))
   eval-result))

(def filter-sufficient (partial filter-k ::sufficiently-covered?))
(def filter-insufficient (partial filter-k ::insufficiently-covered?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reporting

(defn report-coverage
  [coverage-categories args]
  (reduce
   (fn [acc coverage-category]
     (let [coverage-result (apply-coverage coverage-category args)
           evaluated-result
           (evaluate-coverage coverage-category coverage-result (count args))]
       (conj acc (merge coverage-result evaluated-result))))
   []
   coverage-categories))

(defn ->%
  [nom denom]
  (* 100 (double (/ nom denom))))

(defn humanize-coverage
  [coverage total-count]
  {:test.check.insights/coverage (->% (::count coverage) total-count)
   :test.check.insights/target-coverage (::target-% coverage)})

(defn summarize-report
  [report]
  (let [total-count  (reduce + (map ::count (vals report)))
        failed       (reduce-kv
                      (fn [acc k v]
                        (if (not (::sufficiently-covered? v))
                          (conj acc k)
                          acc))
                      #{}
                      report)
        human-report (reduce-kv
                      (fn [acc k coverage]
                        (assoc acc k (humanize-coverage coverage total-count)))
                      {}
                      report)]
    (if (seq failed)
      (assoc human-report :test.check.insights/statistically-failed failed)
      human-report)))

(defn humanize-report
  [coverage-reports]
  (if (map? coverage-reports)
    (summarize-report coverage-reports)
    (mapv summarize-report coverage-reports)))

(comment

  (def coverage
    {::sufficiently-covered?   false
     ::insufficiently-covered? false
     ::coverage-count          2
     ::target-coverage-%       50})

  (humanize-coverage coverage 10)

  (def coverage-reports
    [{:one coverage
      :two coverage}
     {:three coverage}])

  (humanize-report coverage-reports)
  )

