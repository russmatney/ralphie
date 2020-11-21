(ns ralph.defcom-test
  {:clj-kondo/config
   ;; if these keys were namespace i might have been able to guess them
   '{:linters {:inline-def {:level :off}}}}
  (:require
   [ralph.defcom :as sut]
   [clojure.test :refer [deftest testing is]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defcom macro
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest defcom#macro
  (sut/defcom my-command {:name "my-command"})
  (sut/defcom my-other-command {:name "my-other-command"})
  (sut/defcom my-defcom-keyed-command {:defcom/name "defcom-keyed"})

  (testing "registered commands can be listed"
    (is (= 3 (count (sut/commands)))))

  (testing "registered commands can be found"
    (let [name  "my-command"
          found (sut/find-command (sut/commands) name)]
      (is found)
      (is (= name (:defcom/name found)))))

  (testing "defcom-keyed commands can be found"
    (let [name  "defcom-keyed"
          found (sut/find-command (sut/commands) name)]
      (is found)
      (is (= name (:defcom/name found)))))

  (testing "warns/throws on duplicate command-name"))

(deftest defcom#clear-registry
  (let [initial-count (count (sut/commands))]
    (sut/defcom com-1 {:name "com-1"})
    (sut/defcom com-2 {:name "com-2"})
    (sut/defcom com-3 {:defcom/name "com-3"})

    (testing "commands can be cleared"
      (is (= (+ initial-count 3) (count (sut/commands))))
      (sut/clear-registry)
      (is (= 0 (count (sut/commands)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; call-handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest defcom#call-handler
  (testing "defcom/handler is called with anonymous fn"
    (sut/clear-registry)
    (let [c (sut/defcom com {:defcom/name    "com"
                             :defcom/handler (fn handle [& args] args)})]
      (is (= [:config :parsed]
             (sut/call-handler c :config :parsed)))))

  (testing ":handler is called"
    (sut/clear-registry)
    (let [c (sut/defcom com {:defcom/name "com"
                             :handler     (fn [& args] args)})]
      (is (= [:config :parsed]
             (sut/call-handler c :config :parsed)))))

  (testing "defcom/handler is called for named fn"
    (sut/clear-registry)
    (defn com-handler [& args] args)
    (let [c (sut/defcom com2 {:defcom/name    "com"
                              :defcom/handler com-handler})]
      (is (= [:config :parsed]
             (sut/call-handler c :config :parsed))))))
