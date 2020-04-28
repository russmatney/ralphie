(ns ralphie.core-test
  (:require [ralphie.core :as sut]
            [clojure.test :as t :refer [is testing deftest]]))

(deftest example-test
  (testing "test scripts/test script via bb"
    (is (= 1 1))
    ))
