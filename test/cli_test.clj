(ns cli-test
  (:require [cli :as sut]
            [clojure.test :as t :refer [is testing deftest]]))

(deftest example-test
  (testing "test scripts/test script via bb"
    (is (= 1 1))
    ))
