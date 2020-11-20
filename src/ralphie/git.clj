(ns ralphie.git
  (:require
   [babashka.process :refer [$ check]]
   [cheshire.core :as json]
   [ralphie.notify :refer [notify]]
   [ralphie.rofi :as rofi]
   [ralphie.config :as config]
   [ralphie.clipboard :as clipboard]
   [ralphie.browser :as browser]
   [ralphie.re :as re]
   [ralphie.command :refer [defcom]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; transforms
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn repo->rofi-x [{:keys [repo-id language description] :as repo}]
  (assoc repo :rofi/label (str repo-id " | " language " | " description)))

(defn star->repo [star]
  (let [owner     (get-in star ["owner" "login"])
        repo-name (get star "name")]
    {:owner       owner
     :repo-id     (str owner "/" repo-name)
     :description (get star "description")
     :language    (get star "language")
     :fork?       (get star "fork")
     :star-count  (get star "stargazers_count")
     :watch-count (get star "watchers_count")}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fetch github stars
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch-stars
  "Returns the 30 most recent github stars for the user.
  TODO: support sorting by :updated-at (repo last updated)
  might be better to pull both
  consider caching as well, and a seperate command to cache-burst."
  []
  (->> (config/github-username)
       (#(str "https://api.github.com/users/" % "/starred"))
       slurp
       json/parse-string))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clone
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clone [{:keys [repo-id]}]
  (notify (str "Clone attempt: " repo-id))
  (->> ($ hub clone ~repo-id ~(str (config/home-dir) "/" repo-id))
       check
       :out
       slurp
       (notify (str "Successful clone: " repo-id))))

(comment
  (clone {:repo-id "metosin/eines"})
  (clone {:repo-id "russmatney/ink-mode"})
  )

(defn clone-from-stars []
  (->> (fetch-stars)
       (map star->repo)
       (map repo->rofi-x)
       (concat (->> (clipboard/values)
                    (map (fn [v]
                           (when-let [repo-id (re/url->repo-id v)]
                             {:repo-id    repo-id
                              ;; TODO pango markup describing source (clipboard)
                              :rofi/label repo-id})))
                    (filter :repo-id))
               (->> (browser/tabs)
                    (map (fn [t]
                           (when-let [repo-id (re/url->repo-id (:tab/url t))]
                             {:repo-id    repo-id
                              ;; TODO pango markup describing source (open tab)
                              :rofi/label repo-id})))
                    (filter :repo-id)))
       (rofi/rofi {:message "Select repo to clone" :on-select clone})))

(comment
  (dorun (map println (clone-from-stars))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clone cmd, handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clone-handler
  [_config parsed]
  (if-let [repo-id (some-> parsed :arguments first)]
    (clone {:repo-id repo-id})
    (clone-from-stars)))

(defcom clone-cmd
  {:name          "clone"
   :one-line-desc "Clone from your Github Stars"
   :description
   ["When passed a repo-id, copies it into ~/repo-id."
    "Depends on `hub` on the command line."
    "Does not support private repos."
    "If no repo-id is passed, fetches stars from github."]
   :handler       clone-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; gprom
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn gprom-handler [_config _parsed]
  (println "ask which repo to gprom")
  )

(defcom gprom-cmd
  {:name          "gprom"
   :one-line-desc "gprom"
   :description   [""]
   :handler       gprom-handler})

(comment)
