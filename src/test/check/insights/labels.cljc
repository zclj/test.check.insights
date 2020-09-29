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
        :test.check.insights/unlabled []}
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
               [:test.check.insights/labels k]
               conj args)
              (update-in
               [:test.check.insights/labels :test.check.insights/labled]
               conj args)
              (update-in
               [:test.check.insights/labels :test.check.insights/unlabled]
               (fn [x] (vec (drop-last x)))))
          acc))
      (update-in
       label-category
       [:test.check.insights/labels :test.check.insights/unlabled]
       conj args)
      (:test.check.insights/label-classifications label-category)))
   label-categories))

