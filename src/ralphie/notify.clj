(ns ralphie.notify
  (:require
   [babashka.process :refer [$ check]]))

(defn notify
  ([notice]
   (cond (string? notice) (notify notice nil)

         (map? notice)
         (let [{:keys [subject body]} notice]
           (notify subject body))

         :else
         (notify "Malformed notify call" "Expected string or map.")
         ))

  ([subject body]
   (-> ($ notify-send ~subject ~body)
       check
       :out
       slurp)))

(comment
  (notify "subj" "body\nbody\nbody")
  (notify {:subject "subj" :body {:value "v" :label "laaaa"}})
  (notify {:subject "subj" :body "BODY"})
  (-> ($ notify-send subj body)
      check))
