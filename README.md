# test.check.insights

Get insights into your test.check generators by the use of labeling, classification and coverage. Heavily inspired by functionality in [QuickCheck](https://hackage.haskell.org/package/QuickCheck).

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
   (= x x)))

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
 [{:test.check.insights/labeled
   [[0] [0] [-2] [-2] [3] [0] [-3] [-2] [2] [2]],
   :test.check.insights/unlabeled #{},
   :negative [[-2] [-2] [-3] [-2]],
   :positive [[0] [0] [3] [0] [2] [2]],
   :ones []}
  {:test.check.insights/labeled [[-2] [-2] [-3] [-2]],
   :test.check.insights/unlabeled #{[3] [0] [2]},
   :more-neg [],
   :less-neg [[-2] [-2] [-3] [-2]]}]}
```

Each labeled value is included in its label. `::labeled` include all labeled values, once for each predicate match. `::unlabeled` contains any values that did not match a label classification.

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

The result can be humanized, which show the coverage statistics as percentages and includes a list of the failed criterion :

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
   #{:negative :positive :ones}
  {:more-neg
   #:test.check.insights{:coverage 0.0, :target-coverage 10},
   :less-neg
   #:test.check.insights{:coverage 100.0, :target-coverage 10},
   :test.check.insights/statistically-failed #{:more-neg :less-neg}}]}
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

As mentioned options will be passed through to test.check so any options supported by test.check will work. 

Internally test.check.insights rely on the `:reporter-fn` of test.check to collect values, but will also call any provided `:reporter-fn` before the collection.

The values that will be collected and serve as the basis for the evaluation of insights must pass the `:reporter-filter-fn`. If no function is provided test.check.insights will default to collect generated values with the type of `:trial`.

```clj
(tci/quick-check
  100
  property
  :seed 1
  :reporter-fn #(println %)
  :reporter-filter-fn (fn [m] (= (:type m) :trial)))
```

## Statistical Hypothesis Checking

When building more complex generators it can be critical to verify that the values you think are produced actually will be produced. If not, you might not be testing what you think you are and due to the randomness, a few samples might look good.

`check-coverage` aim to solve this problem. It will use a statistical test to account for randomness in your generation. As many test that are needed are executed to check if the required coverage can be met or not.

### check-coverage

Given a property with coverage criterion:

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

We get a result similar to:

```clj
(tci/check-coverage 100 property-with-coverage)
;;=>
[{:result true,
  :pass? true,
  :num-tests 102400,
  :time-elapsed-ms 215,
  :seed 1602573210230,
  :test.check.insights/coverage
  {:negative
   #:test.check.insights.coverage{:count 50259,
                                  :target-% 50,
                                  :sufficiently-covered? true,
                                  :insufficiently-covered? false},
   :positive
   #:test.check.insights.coverage{:count 52141,
                                  :target-% 50,
                                  :sufficiently-covered? true,
                                  :insufficiently-covered? false},
   :ones
   #:test.check.insights.coverage{:count 1336,
                                  :target-% 1.2,
                                  :sufficiently-covered? true,
                                  :insufficiently-covered? false}}}
 {:result true,
  :pass? false,
  :num-tests 6400,
  :time-elapsed-ms 12,
  :seed 1602573210493,
  :test.check.insights/coverage
  {:more-neg
   #:test.check.insights.coverage{:count 490,
                                  :target-% 10,
                                  :sufficiently-covered? false,
                                  :insufficiently-covered? true},
   :less-neg
   #:test.check.insights.coverage{:count 2643,
                                  :target-% 10,
                                  :sufficiently-covered? true,
                                  :insufficiently-covered? false}}}]
```

The result of `check-coverage` is merged with the test.check result. Note the `check-coverage` will set `:pass?` to `false` if coverage can not be satisfied for all criterion.

As we can see the `:num-tests` are different for the two categories of coverage criterion. `check-coverage` will start with the number of tests given as input (100 in the case above) and will for each iteration increase the number of tests with a multiple of a power of 2.

The result include `count`, the number of generated values classified with the specified criterion classification predicate and the `target-%` as it was defined for this criterion. In addition, there are two values related to the statistical check, `:sufficiently-covered?` and `:insufficiently-covered?`. `:sufficiently-covered?` will be set to `true` if the target coverage is shown to be statistically achievable and `:insufficiently-covered?` will be set to `true` if the opposite can be shown. This means that as long as both values are `false` we can not determined anything about the coverage with any significance. If both values are false, `check-coverage` will increase the number of tests and run new tests.

If we look at the result above again, we can see that the first category passed (`:pass? true`) by running 102400 tests (`:num-tests 102400`) but the second category only needed 6400 tests to reach a conclusion. The close the required coverage is to the actual distribution of the generator the "harder" it will be to verify coverage, resulting in more tests. This means that with a required coverage that is far of from the actual distribution, `check-coverage` can be fast in its conclusion:

```clj
;; Require 50% of values to be ones
(def property-with-far-off-coverage
  (tci/for-all
   {::coverage
    [{:negative {::classify (fn [x] (< x 0))
                 ::cover    50}
      :positive {::classify (fn [x] (>= x 0))
                 ::cover    50}
      :ones     {::classify (fn [x] (= x 1))
                 ::cover    50}}]}
   [x gen/int]
   (= x x)))

(tci/check-coverage 100 property-with-far-off-coverage)
;;=>
[{:result true,
  :pass? false,
  :num-tests 100,
  :time-elapsed-ms 0,
  :seed 1602574843079,
  :test.check.insights/coverage
  {:negative
   #:test.check.insights.coverage{:count 53,
                                  :target-% 50,
                                  :sufficiently-covered? false,
                                  :insufficiently-covered? false},
   :positive
   #:test.check.insights.coverage{:count 47,
                                  :target-% 50,
                                  :sufficiently-covered? false,
                                  :insufficiently-covered? false},
   :ones
   #:test.check.insights.coverage{:count 4,
                                  :target-% 50,
                                  :sufficiently-covered? false,
                                  :insufficiently-covered? true}}}]
```

Only the initial 100 test are required to reach `:insufficiently-covered? true` for the `:ones` criterion.

Since the output from `check-coverage` is the same as from `quick-check` with regards to coverage, `humanize-report` can be used:

```clj
(mapv
 tci/humanize-report
 (tci/check-coverage 100 property-with-far-off-coverage))
;;=>
[{:result true,
  :pass? true,
  :num-tests 102400,
  :time-elapsed-ms 209,
  :seed 1602583299465,
  :test.check.insights/coverage
  {:negative
   #:test.check.insights{:coverage 48.4031378293468,
                         :target-coverage 50},
   :positive
   #:test.check.insights{:coverage 50.2804386793362,
                         :target-coverage 50},
   :ones
   #:test.check.insights{:coverage 1.316423491317002,
                         :target-coverage 1.2}}}
 {:result true,
  :pass? false,
  :num-tests 6400,
  :time-elapsed-ms 13,
  :seed 1602583299726,
  :test.check.insights/coverage
  {:more-neg
   #:test.check.insights{:coverage 15.86319218241042,
                         :target-coverage 10},
   :less-neg
   #:test.check.insights{:coverage 84.13680781758957,
                         :target-coverage 10},
   :test.check.insights/statistically-failed #{:more-neg}}}]
```

This will display the percentages instead of counts. Note that the percentages of `:negative` is on target but the `:sufficiently-covered?` is still `false`. This is due to that significance have not been reached with this number of tests.

Finally, there is one more way the coverage check can fail and that is if the max number of allowed tests are reached before reaching a conclusion.  

```clj
(tci/check-coverage 100 property-with-coverage :max-number-of-tests 1)
;;=>
[{:result true,
  :pass? false,
  :num-tests 100,
  :time-elapsed-ms 1,
  :seed 1602583744482,
  :test.check.insights/coverage
  {:negative
   #:test.check.insights.coverage{:count 42,
                                  :target-% 50,
                                  :sufficiently-covered? false,
                                  :insufficiently-covered? false},
   :positive
   #:test.check.insights.coverage{:count 58,
                                  :target-% 50,
                                  :sufficiently-covered? false,
                                  :insufficiently-covered? false},
   :ones
   #:test.check.insights.coverage{:count 4,
                                  :target-% 1.2,
                                  :sufficiently-covered? false,
                                  :insufficiently-covered? false}},
  :test.check.insights.coverage/status :gave-up}
 {:result true,
  :pass? false,
  :num-tests 100,
  :time-elapsed-ms 0,
  :seed 1602583744483,
  :test.check.insights/coverage
  {:more-neg
   #:test.check.insights.coverage{:count 0,
                                  :target-% 10,
                                  :sufficiently-covered? false,
                                  :insufficiently-covered? false},
   :less-neg
   #:test.check.insights.coverage{:count 58,
                                  :target-% 10,
                                  :sufficiently-covered? true,
                                  :insufficiently-covered? false}},
  :test.check.insights.coverage/status :gave-up}]
```

Reaching the limit will set the `:pass?` to `false` and include the status of `:gave-up`. The default value of `:max-number-of-tests` is set to 10000000, witch I pulled out of my hat, so let me know if you have another suggestion.

### Algorithms used

The coverage check is heavily inspired by the [QuickCheck implementation](https://github.com/nick8325/quickcheck/blob/09a569db8de0df14f8514b30d4bfe7acb41f9c41/src/Test/QuickCheck/Test.hs#L571).

The computation of the inverse normal cumulative distribution function is based on  [this implementation](https://web.archive.org/web/20151110174102/http://home.online.no/~pjacklam/notes/invnorm/).

Statistical hypothesis checking is based on the paper [Sequential tests of statistical hypotheses](https://www.jstor.org/stable/pdf/2235829.pdf?casa_token=k36mnW8i4J4AAAAA:A_eil9LqMsi_3r86F7VvaoQsZP7yDrispRDURyyMZHx-YDTvWJ1m-NqPYRwvW4bNXjkY9woNr2FKgcWZHpJxowyVhKOX2h0fOLFJ75hXHRSzY-jgsv_N)

[Wilson score interval](https://en.wikipedia.org/wiki/Binomial_proportion_confidence_interval#Wilson_score_interval) is used in the statistical tests.

The formula as implemented in coverage.cljc can be found [here](https://www.ucl.ac.uk/english-usage/staff/sean/resources/binomialpoisson.pdf), page 5, equation (4).

## Motivation

When building more complex generators, as in the [QuickREST paper](https://arxiv.org/pdf/1912.09686.pdf), the question arises; are my generators producing the values that I expect? This library aims to help you provide the answers to that question, i.e., debug and get confidence in your generators.

Since I was exposed to the labeling and coverage features of QuickCheck I hoped that the same features would eventually get to Clojure's test.check. But they never did, so :

> If you have expectations (of others) that aren't being met, those expectations are your own responsibility. You are responsible for your own needs. If you want things, make them.

- Rich Hickey, [Open Source is Not About You](https://gist.github.com/richhickey/1563cddea1002958f96e7ba9519972d9)

I hope that fellow property-based testing users in the Clojure community will find this library useful.

## Pre-alpha

Currently collecting usage feedback. Expect breaking changes to the API.

I'm not a statistician, the current implementation is my best effort. Review of the statistical parts is very much appreciated.

## Roadmap

- Collect feedback
- ClojureScript implementation
- Consider if discarded generated values can be disregarded in coverage checks.

## License

Copyright © 2020 Stefan Karlsson.

Available under the terms of the Eclipse Public License 2.0, see `LICENSE`.
