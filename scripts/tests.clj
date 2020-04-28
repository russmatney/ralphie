;; bb -cp "src:test" -f scripts/tests.clj

(ns tests
  (:require [clojure.test :as t]
            [ralphie.core-test]))

(let [{:keys [fail error]} (t/run-tests 'ralphie.core-test)]
  (System/exit (+ fail error)))
