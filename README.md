# test.check.insights

Get insights into your test.check generators by the use of labeling, classification and coverage. Heavily inspired by functionallity in [QuickCheck](https://hackage.haskell.org/package/QuickCheck).

**STATUS**: [*pre-alpha*](#pre-alpha)

## quick-check with insights

test.check.insights wraps test.check's `for-all` and `quick-check` to expose its functionality with a familiar API.

`for-all` looks like the test.check version with the addition of a map as the first argument. The map contains the data for coverage, labeling and collection.

In general, all library specific keys are namespaced.

```clj
(require '[test.check.insights :as tci])

;; define a property
(def property
  (tci/for-all
   {::coverage
    [{:negative {::classify (fn [x] (< x 0))
                 ::cover    50}
      :positive {::classify (fn [x] (>= x 0))
                 ::cover    50}
      :ones     {::classify (fn [x] (= x 1))
                 ::cover    1.2}}
     {:more-neg {::classify (fn [x] (< x -100))
                 ::cover    10}
      :less-neg {::classify (fn [x] (and (> x -100)  x 0)))
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

;; check the property
(tci/quick-check 100 property)

;; test.check options are passed through
(tci/quick-check 100 property :seed 1)

```

### Labeling

Labels are defined as a vector of label categories. Each element in the vector is a map where the key is the name of the label and the value is a map containing the predicate used to classify the label.

Labeling will apply the `::classify` predicate with the arguments from the generators and the generator bindings. Hence the arity of the predicate function must match your generator bindings.

```clj
(def property-with-labels
  (tci/for-all
    {::labels
     [{:negative {::classify (fn [x] (< x 0))}
       :positive {::classify (fn [x] (>= x 0))}
       :ones     {::classify (fn [x] (= 1 x))}}
      {:more-neg {::classify (fn [x] (< x -100))}
       :less-neg {::classify (fn [x] (and (> x -100) (< x 0)))}}]}
     [x gen/int]
     (= x x)))

(tci/quick-check 10 property-with-labels :seed 1)
```

The result is merged with the test.check result:

```clj
{:result true,
 :pass? true,
 :num-tests 10,
 :time-elapsed-ms 0,
 :seed 1,
 :test.check.insights/labels
 [{:test.check.insights/labled
   [[0] [0] [-2] [-2] [3] [0] [-3] [-2] [2] [2]],
   :test.check.insights/unlabled #{},
   :negative [[-2] [-2] [-3] [-2]],
   :positive [[0] [0] [3] [0] [2] [2]],
   :ones []}
  {:test.check.insights/labled [[-2] [-2] [-3] [-2]],
   :test.check.insights/unlabled #{[3] [0] [2]},
   :more-neg [],
   :less-neg [[-2] [-2] [-3] [-2]]}]}
```

Each labeled value is included in its label. `::labeled` include all labeled values, once for each predicate match. `::unlabled` contains any values that did not match a label classification.

The result can be humanized :

```clj
(tci/humanize-report (tci/quick-check 10 property-with-labels :seed 1))
;;=>
{:result true,
 :pass? true,
 :num-tests 10,
 :time-elapsed-ms 0,
 :seed 1,
 :test.check.insights/labels
 [{:negative 40.0, :positive 60.0, :ones 0.0}
  {:more-neg 0.0, :less-neg 100.0}]}
```

The humanized report show the classified percentages for each label in relation to its category.

### Coverage

Coverage criterion is defined as a vector of maps, where each map represents a coverage category. The key in the map is the name of the coverage criterion. The value of the map should contain a predicate, `::classify` to determine if a generated value counts as covered towards this criterion and a coverage percentage, `::cover`.

```clj
(def property-with-coverage
  (tci/for-all
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
```

The result is merged with test.check result:

```clj
(tci/quick-check 10 property-with-coverage :seed 1)
;; =>
{:result true,
 :pass? true,
 :num-tests 10,
 :time-elapsed-ms 1,
 :seed 1,
 :test.check.insights/coverage
 [{:negative
   #:test.check.insights.coverage{:count 4,
                                  :target-% 50,
                                  :sufficiently-covered? false,
                                  :insufficiently-covered? false},
   :positive
   #:test.check.insights.coverage{:count 6,
                                  :target-% 50,
                                  :sufficiently-covered? false,
                                  :insufficiently-covered? false},
   :ones
   #:test.check.insights.coverage{:count 0,
                                  :target-% 1.2,
                                  :sufficiently-covered? false,
                                  :insufficiently-covered? false}}
  {:more-neg
   #:test.check.insights.coverage{:count 0,
                                  :target-% 10,
                                  :sufficiently-covered? false,
                                  :insufficiently-covered? false},
   :less-neg
   #:test.check.insights.coverage{:count 4,
                                  :target-% 10,
                                  :sufficiently-covered? false,
                                  :insufficiently-covered? false}}]}
```

`quick-check` will show the evaluated coverage (where [`check-coverage`](#check-coverage) will fail the test if coverage are not met). See [below](#check-coverage) for the meaning of `:sufficiently-covered?` and `:insufficiently-covered?`.

The result can be humanized, which show the coverage statistics as percentages and includes a list of the failed criterions :

```clj
(tci/humanize-report (tci/quick-check 10 property-with-coverage :seed 1))
;;=>
{:result true,
 :pass? true,
 :num-tests 10,
 :time-elapsed-ms 0,
 :seed 1,
 :test.check.insights/coverage
 [{:negative
   #:test.check.insights{:coverage 40.0, :target-coverage 50},
   :positive
   #:test.check.insights{:coverage 60.0, :target-coverage 50},
   :ones #:test.check.insights{:coverage 0.0, :target-coverage 1.2},
   :test.check.insights/statistically-failed
   [:negative :positive :ones]}
  {:more-neg
   #:test.check.insights{:coverage 0.0, :target-coverage 10},
   :less-neg
   #:test.check.insights{:coverage 100.0, :target-coverage 10},
   :test.check.insights/statistically-failed [:more-neg :less-neg]}]}
```

### Collect

While labeling and coverage are based on classification predicates, collect is based on value producing functions. `::collect` is defined as a vector of maps witch includes the value producing function in the `::collector` key.

```clj
(def property-with-collect
  (tci/for-all
    {::collect [{::collector (fn [x] (identity x))}]}
    [x gen/int]
    (= x x)))
```

The collector function will be applied to generated values. The results is buckets based on the value of the function with the generated values collected.

```clj
(tci/quick-check 10 property-with-collect :seed 1)
;;=>
{:result true,
 :pass? true,
 :num-tests 10,
 :time-elapsed-ms 1,
 :seed 1,
 :test.check.insights/collect
 [{0 [[0] [0] [0]],
   -2 [[-2] [-2] [-2]],
   3 [[3]],
   -3 [[-3]],
   2 [[2] [2]]}]}
```

As with the other results, it can be humanized into showing percentages:

```clj
(tci/humanize-report (tci/quick-check 10 property-with-collect :seed 1))
;; =>
{:result true,
 :pass? true,
 :num-tests 10,
 :time-elapsed-ms 0,
 :seed 1,
 :test.check.insights/collect
 [{0 30.0, -2 30.0, 3 10.0, -3 10.0, 2 20.0}]}
```

### Options
* filter-fn
* report-fn

## Statistical Hypothesis check

### check-coverage

check-coverage will use a statistical test to account for randomnes in your generation. As many test that are needed to are executed to check if the required coverage can be met or not.

## Algorithms used

The coverage check is heavilty inspired by the [QuickCheck implementation](https://github.com/nick8325/quickcheck/blob/09a569db8de0df14f8514b30d4bfe7acb41f9c41/src/Test/QuickCheck/Test.hs#L571).

The computation of the inverse normal cumulative distribution function  is based on  [this implementation](https://web.archive.org/web/20151110174102/http://home.online.no/~pjacklam/notes/invnorm/).

Statistical hypothesis checking is based on the paper [Sequential tests of statistical hypotheses](https://www.jstor.org/stable/pdf/2235829.pdf?casa_token=k36mnW8i4J4AAAAA:A_eil9LqMsi_3r86F7VvaoQsZP7yDrispRDURyyMZHx-YDTvWJ1m-NqPYRwvW4bNXjkY9woNr2FKgcWZHpJxowyVhKOX2h0fOLFJ75hXHRSzY-jgsv_N)


[Wilson score interval](https://en.wikipedia.org/wiki/Binomial_proportion_confidence_interval#Wilson_score_interval) is used in the statistical tests.

The formula as implemented in coverage.cljc can be found [here](https://www.ucl.ac.uk/english-usage/staff/sean/resources/binomialpoisson.pdf), page 5, equation (4).

## Motivation

When building more complex generators the question arises; are my generators producing the values that I expect? This library aims to help you provide the answers to that question, i.e., debug and get confidence in your generators.

Since I was exposed to the labeling and coverage features of QuickCheck I hoped that the same features would eventully get to Clojure's test.check. But they never did, so :

> If you have expectations (of others) that aren't being met, those expectations are your own responsibility. You are responsible for your own needs. If you want things, make them.

- Rich Hickey, [Open Source is Not About You](https://gist.github.com/richhickey/1563cddea1002958f96e7ba9519972d9)

## Pre-alpha

Currently collecting usage feedback. Expect breaking changes to the API.

I'm not a statistician, the current implementation is my best effort. Review of the statistical parts is very much appriciated.

## Roadmap
  * Collect feedback
  * ClojureScript implementation
  * Consider if discarded generated values can be disregarded in coverage checks. 
