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
   [ralph.defcom :refer [defcom]]
   [ralphie.zsh :as zsh]))

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
  (try
    (->> ($ hub clone ~repo-id ~(str (config/home-dir) "/" repo-id))
         check
         :out
         slurp
         (notify (str "Successful clone: " repo-id)))
    (catch Exception e
      (notify "Error while cloning" e)
      (println e))))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; run command helper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn run-proc
  "Runs the passed babashka process in the dir, catching errors.
  "
  ([proc] (run-proc proc nil))
  ([proc {:keys [error-message
                 dir]}]
   (let [dir           (-> (or dir
                               ;; TODO config for fallback dir?
                               "~/russmatney/clawe")
                           zsh/expand)
         error-message (or error-message (str "Ralphie Error: "
                                              (str proc) " " dir))]
     (try
       (some-> ^{:dir dir :out :string}
               proc
               check
               :out
               slurp)
       (catch Exception e
         (println error-message e)
         (notify error-message e))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dirty/is-clean?
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare is-clean?)
(defn dirty? [x] (-> x is-clean? not))
(defn is-clean?
  "Returns true if there is no local diff in the passed path.
  Expects a .git directory at <path>/.git"
  [path]
  (try
    (some-> ^{:dir (zsh/expand path)
              :out :string}
            ($ git diff HEAD)
            check
            :out
            empty?)
    (catch Exception e
      (let [msg (str "ERROR for " path " in git/is-clean?")]
        (println msg e)
        (notify msg e)))))

(comment
  (is-clean? "~/russmatney/ralphie")
  (dirty? "~/russmatney/ralphie")
  (-> ^{:dir "/home/russ/russmatney/ralphie"}
      ($ git diff HEAD)
      check
      :out
      slurp
      empty?)

  (-> ^{:dir "/home/russ/Dropbox/todo"
        :out :string}
      ($ git diff HEAD)
      check
      :out
      empty?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; needs-push?
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn needs-push? [repo-path]
  (let [opts {:dir repo-path
              :error-message
              (str "RALPHIE ERROR for " repo-path " in git/needs-push?")}]
    (-> (run-proc ($ git status) opts)
        )))

(comment
  (needs-push? "~/russmatney/ralphie")
  )
