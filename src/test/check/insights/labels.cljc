(ns test.check.insights.labels)

(defn init-labels
  [labels]
  (mapv
   (fn [label-category]
     (reduce
      (fn [acc k]
        (assoc-in acc [:test.check.insights/labels k] []))
      {:test.check.insights/labels
       {:test.check.insights/labled   []
        :test.check.insights/unlabled #{}}
       :test.check.insights/label-classifications
       label-category}
      (keys label-category)))
   labels))

(defn update-labels
  [label-categories args]
  (mapv
   (fn [label-category]
     (reduce-kv
      (fn [acc k v]
        (if (and args (apply (:test.check.insights/classify v) args))
          (-> acc
              (update-in
               [:test.check.insights/labels k] conj args)
              (update-in
               [:test.check.insights/labels :test.check.insights/labled] conj args)
              ;; remove from set
              (update-in
               [:test.check.insights/labels :test.check.insights/unlabled]
               disj args))
          acc))
      (update-in
       label-category
       [:test.check.insights/labels :test.check.insights/unlabled] conj args)
      (:test.check.insights/label-classifications label-category)))
   label-categories))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reporting

(defn ->%
  [nom denom]
  (* 100 (double (/ nom denom))))

(defn humanize-report
  [label-reports]
  (mapv
   (fn [report]
     (let [clean-report
           (dissoc report :test.check.insights/labled :test.check.insights/unlabled)
           total-count (reduce + (map #(count (val %)) clean-report))]
       (reduce-kv
        (fn [acc k collected]
          (assoc acc k (->% (count collected) total-count)))
        {}
        clean-report)))
   label-reports))
