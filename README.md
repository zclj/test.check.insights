# test.check.insights

Get insights into your test.check generators by the use of labeling, classification and coverage. Heavily inspired by functionallity in [QuickCheck](https://hackage.haskell.org/package/QuickCheck).

**STATUS**: [*pre-alpha*](#pre-alpha)

## Usage

### Labeling

```clj

```

### Coverage

check-coverage will use a statistical test to account for randomnes in your generation. As many test that are needed to are executed to check if the required coverage can be met or not.

quick-check will show the evaluated coverage, where check-coverage will fail the test if coverage are not met.

### Collect

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
