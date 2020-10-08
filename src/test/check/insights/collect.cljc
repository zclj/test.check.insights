(ns test.check.insights.collect)

(defn collect
  [collectors args]
  (mapv
   (fn [{:keys [test.check.insights/collector]}]
     (group-by (fn [arg] (apply collector arg)) args))
   collectors))

(defn ->%
  [nom denom]
  (* 100 (double (/ nom denom))))

(defn humanize-report
  [collection-reports]
  (mapv
   (fn [report]
     (let [total-count (reduce + (map #(count (val %)) report))]
       (reduce-kv
        (fn [acc k collected]
          (assoc acc k (->% (count collected) total-count)))
        {}
        report)))
   collection-reports))
