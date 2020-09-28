(ns test.check.insights.coverage)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resources

;; https://github.com/nick8325/quickcheck/blob/09a569db8de0df14f8514b30d4bfe7acb41f9c41/src/Test/QuickCheck/State.hs#L63

;; https://github.com/nick8325/quickcheck/blob/09a569db8de0df14f8514b30d4bfe7acb41f9c41/src/Test/QuickCheck/Test.hs#L579

;; https://github.com/nick8325/quickcheck/blob/09a569db8de0df14f8514b30d4bfe7acb41f9c41/src/Test/QuickCheck/Property.hs#L499

;; DO NOT COUNT DISCARDED TEST AGAINST COVERAGE
;; https://hackage.haskell.org/package/QuickCheck-2.13.2/docs/Test-QuickCheck.html#v:checkCoverage
;; When you use checkCoverage, QuickCheck uses a statistical test to account for the role of luck in coverage failures. It will run as many tests as needed until it is sure about whether the coverage requirements are met. If a coverage requirement is not met, the property fails.
;; just using coverage show the stats, checkCoverage will fail it

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO - java.lang.Math -> JS

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Inverse normal cumulative distribution function (same as QuickCheck)

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
          (+ (* (+ (* (+ (* (+ (* d1 q) d2) q) d3) q) d4) q) 1)))))
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
  (>= (wilson-low k n (/ 1 certainty))
      (* tolerance p)))

(defn insufficiently-covered?
  [certainty n k p]
  (if certainty
    (< (wilson-high k n (/ 1 certainty)) p)
    (< k (* p n))))

(def default-confidence
  {:certainty 1.0E9
   :tolerance 0.9})

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
       (assoc acc k (count (filter identity classification)))))
   {}
   coverage-m))

(defn evaluate-coverage
  [coverage-m coverage number-of-tests]
  (reduce-kv
   (fn [acc k {:keys [test.check.insights/cover]}]
     (merge
      acc
      {k (check-coverage number-of-tests (get coverage k) (/ cover 100))}))
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

;; (defn filter-sufficient
;;   [eval-result]
;;   (filterv
;;    (fn [er]
;;      (::sufficiently-covered? (val er)))
;;    eval-result))

;; (defn filter-insufficient
;;   [eval-result]
;;   (filterv
;;    (fn [er]
;;      (::insufficiently-covered? (val er)))
;;    eval-result))


