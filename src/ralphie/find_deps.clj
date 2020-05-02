(ns ralphie.find-deps
  (:require [ralphie.rofi :as rofi]))

(defn find-deps-handler [_config _parsed]
  (let [search-str
        (rofi/rofi {:msg "Finding clojars libs. Enter search..."})]
    (println "find-deps-handler")
    (println search-str)))

(def find-deps-cmd
  {:name          "find-deps"
   :one-line-desc "find-deps"
   :description   ["Looks up clojars libs for the passed str."]
   :handler       find-deps-handler})
