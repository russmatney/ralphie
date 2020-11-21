(ns ralphie.doctor
  (:require
   [ralph.defcom :refer [defcom]]
   [clojure.string :as string]))

(defn missing-for-key
  "Returns commands that are missing a value for the key."
  [{:keys [commands]} key]
  (seq (remove #(get % key) commands)))

(def checks
  {:missing-names        {:check-fn #(missing-for-key % :name)}
   :missing-descriptions {:check-fn #(missing-for-key % :description)}
   :missing-handlers     {:check-fn #(missing-for-key % :handler)}})

(defn checkup-handler [config _parsed]
  (let [failed-checks
        (->> checks
             (map (fn [[k {:keys [check-fn] :as chk}]]
                    [k (merge chk {:result (check-fn config)})]))
             (filter (fn [[_k {:keys [result]}]] (seq result)))
             )
        check->string
        (fn [{:keys [result]}]
          (string/join ", " (doall
                              (map #(str "(:name " (:name %) ")")
                                   result))))]
    (if (seq failed-checks)
      (let [failed
            (map (fn [[k check]] [k (check->string check)])
                 failed-checks)]
        (doall
          (map (fn [[k s]]
                 (println "Failed check:" k "for command(s):" s))
               failed)))
      (println "No failed checks :)"))))

(comment
  (checkup-handler {:commands [{:name "hi"}]} nil)
  (missing-for-key {:commands [{:name "hi"}]} :hi)
  )

(defcom checkup-cmd
  {:name          "doctor-checkup"
   :one-line-desc "Debug helper for sanity-checking"
   :description   ["Runs a sanity check on your built config, and logs a report."]
   :handler       checkup-handler})
