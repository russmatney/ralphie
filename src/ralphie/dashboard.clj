(ns ralphie.dashboard
  (:require
   [ralph.defcom :refer [defcom]]
   [babashka.process :refer [$ check]]))

(defn rebuild-agenda-views []
  (->
    ($ emacsclient -e "(org-batch-store-agenda-views)")
    check))

(defcom rebuild-agenda-views-cmd
  {:name    "rebuild-agenda-views"
   :handler (fn [_ _] (rebuild-agenda-views))})

(comment
  (println "sup")
  )
