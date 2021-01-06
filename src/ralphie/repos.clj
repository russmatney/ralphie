(ns ralphie.repos
  (:require
   [ralph.defcom :refer [defcom]]
   [ralphie.item :as item]
   [ralphie.config :as config]
   [ralphie.awesome :as awm]
   [ralphie.notify :refer [notify]]
   [ralphie.fs :as fs]
   [ralphie.rofi :as rofi]
   [org-crud.core :as org-crud]
   ;; [clojure.string :as string]
   [babashka.process :refer [$ check]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Repo helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ignore-dirty? [_item]
  false ;; check ALL
  ;; (some-> item item/path (string/includes? "Dropbox"))
  )

(defn is-clean? [item]
  (if (ignore-dirty? item)
    true
    (when (item/path item)
      (notify "checking if path is clean" (item/path item))
      (try
        (some-> ^{:dir (item/path item)
                  :out :string}
                ($ git diff HEAD)
                check
                :out
                (= ""))
        (catch Exception e
          (let [msg (str "ERROR in: " (:org/name item) " is-clean? exception")]
            (println msg e)
            (notify msg e)))))))

(comment
  (-> ^{:dir "/home/russ/russmatney/ralphie"}
      ($ git diff HEAD)
      check
      :out
      slurp
      (= ""))

  (-> ^{:dir "/home/russ/Dropbox/todo"
        :out :string}
      ($ git diff HEAD)
      check
      :out
      (= ""))

  (is-clean? {:org.prop/path "/home/russ/Dropbox/todo"})
  (is-clean? {:org.prop/path "/home/russ/russmatney/yodo"})
  (is-clean? {:org.prop/path "/home/russ/russmatney/ralphie"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fetch repos from org file
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch-repos []
  (->> (config/repos-file)
       (org-crud/path->flattened-items)
       (filter (comp #(= % 1) :org/level))
       (map (fn [repo]
              (let [path (str (config/home-dir) "/" (:org/name repo))]
                (-> repo
                    (assoc :org.prop/path path)))))
       (filter (comp fs/exists? item/path))
       ;; only keep if this is a .git dir
       (filter (comp fs/exists? #(-> % item/path (str "/.git"))))))

(comment
  (count (fetch-repos)))

(defn dirty-repos []
  ;; TODO expand to identify untracked files
  ;; TODO expand to fetch/identify remote updates
  (->> (fetch-repos)
       (filter item/watching?)
       (map (fn [repo]
              (-> repo
                  (assoc :clean? (is-clean? repo)))))
       (remove nil?)
       (remove :clean?)))

(comment
  (println "hi")
  (->> (fetch-repos)
       (filter item/watching?)
       (map (fn [repo]
              (-> repo
                  (assoc :clean? (is-clean? repo)))))
       (map :org/name))
  (->> (dirty-repos)
       (map :org/name))
  (count (dirty-repos)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Update repos widget
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-repos-handler
  ([] (update-repos-handler nil nil))
  ([_config _parsed]
   (->> (dirty-repos)
        (awm/awm-fn "update_repos_widget")
        awm/awm-cli)))

(comment

  (->> (dirty-repos)
       (awm/awm-fn "update_repos_widget")
       awm/awm-cli)
  )

(defcom update-repos-cmd
  {:name          "update-dirty-repos"
   :one-line-desc "Updates the dirty repos used by misc widgets."
   :handler       update-repos-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; List dirty repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-dirty-repos-handler
  ([] (list-dirty-repos-handler nil nil))
  ([_config _parsed]
   (let [reps (dirty-repos)]
     (if (> (count reps) 0)
       (when-let [selected-repo (->> reps
                                     (map (fn [x] (assoc x :label (:org/name x))))
                                     (rofi/rofi {:msg "Dirty Repos"}))]
         (rofi/rofi {:msg (str "For dirty repo" (item/label selected-repo))}
                    [{:label     "Open in workspace"
                      :on-select (fn [_]
                                   (notify "Open in workspace"
                                           ;; TODO impl
                                           "Not yet implemented"))}
                     {:label     "Remove watch/Ignore dirty"
                      :on-select (fn [_]
                                   (notify "Remove watch/Ignore dirty"
                                           ;; TODO impl
                                           "Not yet implemented"))}]))
       (notify "No dirty repos!")))))

(defcom list-dirty-repos-cmd
  {:name          "list-dirty-repos"
   :one-line-desc "Returns dirty repos in a rofi list."
   :handler       list-dirty-repos-handler})

(comment)
