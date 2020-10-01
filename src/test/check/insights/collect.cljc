(ns test.check.insights.collect)

(defn collect
  [collectors args]
  (mapv
   (fn [{:keys [test.check.insights/collector]}]
     (group-by (fn [arg] (apply collector arg)) args))
   collectors))
