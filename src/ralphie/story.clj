(ns ralphie.story
  (:require
   [ralphie.rofi :as rofi]
   [ralph.defcom :refer [defcom]]))

(defn person [name]
  {:name name})

(defn marry [p1 p2]
  (println (str (:name p1) " and " (:name p2) " were married")))

(defn angry [p]
  (println (:name p) "was angry"))

(defn dead-by [victim murderer]
  (println "It was the dead of night")
  (println murderer " murdered " victim)
  )

(comment
  (let [tom  (person "Tom")
        russ (person "Russ")
        duaa (person "Duaa")
        ]
    (println "a story, for you:")
    (marry tom duaa)
    (angry russ)
    (dead-by tom russ)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Command
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn story-handler [_config _parsed]
  (rofi/rofi
    {:xs [{:label "How to make a story tho?"}
          {:label "Does it start with a kiss?"}
          {:label "Who even knows?"}]}))

(defcom story-cmd
  {:name          "story"
   :one-line-desc "story"
   :description   ["Starts a story"]
   :handler       story-handler})
