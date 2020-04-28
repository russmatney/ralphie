(ns ralphie.util
  (:require
   [clojure.set :as set]))


(defn ensure-set [s] (if set? s (set s)))

(defn matching-ks? [s1 s2]
  (set/subset? s2 s1))
