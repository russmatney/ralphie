;; bb -cp "src:test" -f scripts/tests.clj

(ns tests
  (:require [clojure.test :as t]
            [cli-test]))

(let [{:keys [fail error]} (t/run-tests 'cli-test)]
  (System/exit (+ fail error)))
