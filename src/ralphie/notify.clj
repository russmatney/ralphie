(ns ralphie.notify
  (:require
   [babashka.process :as process :refer [$ check]]))

(defn notify
  ([notice]
   (cond (string? notice) (notify notice nil)

         (map? notice)
         (let [{:keys [subject body] :as opts} notice]
           (notify subject body opts))

         :else
         (notify "Malformed notify call" "Expected string or map.")
         ))

  ([subject body & args]
   (let [{:keys [replaces-process]}
         (some-> args first)
         exec-strs (cond-> ["notify-send.py" subject]
                     body (conj body)
                     replaces-process
                     (conj "--replaces-process" replaces-process))
         proc      (process/process (conj exec-strs) {:out :string})]
     (println exec-strs)
     ;; we only check when --replaces-process is not passed
     ;; ... skips error messages if bad data is passed
     ;; ... also not sure when these get dealt with. is this a memory leak?
     (when-not replaces-process
       (-> proc check :out))
     nil)))

(comment
  (notify "subj" "body\nbody\nbody")
  (notify {:subject "subj"})
  (notify "subj" "body\nbodybodddd" {:replaces-process "blah"})

  (notify {:subject "subj" :body {:value "v" :label "laaaa"}})
  (notify {:subject "subj" :body "BODY"})
  (-> ($ notify-send subj body)
      check)
  )
