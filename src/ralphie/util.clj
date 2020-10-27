(ns ralphie.util
  (:require
   [babashka.process :refer [$ check]]
   [clojure.set :as set]))


(defn ensure-set [s] (if set? s (set s)))

(defn matching-ks? [s1 s2]
  (set/subset? s2 s1))

(defn get-cp
  "Builds a classpath in a directory."
  [dir]
  (-> ^{:dir dir}
      ($ clojure -Spath)
      check :out slurp))
