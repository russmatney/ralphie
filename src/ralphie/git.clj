(ns ralphie.git
  (:require
   [babashka.process :refer [$ check] :as process]
   [cheshire.core :as json]
   [ralphie.notify :refer [notify]]
   [ralphie.rofi :as rofi]
   [ralphie.config :as config]
   [ralphie.clipboard :as clipboard]
   [ralphie.browser :as browser]
   [ralphie.re :as re]
   [ralph.defcom :refer [defcom]]
   [ralphie.zsh :as zsh]
   [ralphie.fs :as fs]
   [ralphie.bb :as bb]
   [clojure.string :as string]
   [ralphie.tmux :as tmux]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; local repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def local-repos-root (zsh/expand "~"))

(defn local-repos
  "Returns a list of absolute paths to local git repos"
  []
  (->> (bb/run-proc
         {:error-message (str "RALPHIE ERROR fetching local repos")}
         ^{:dir local-repos-root}
         ($ ls -a))
       ;; TODO run in parallel
       ;; or just memoize?
       (mapcat (fn [home-dir]
                 (-> (str "~/" home-dir "/*/.git")
                     zsh/expand
                     (string/split #" "))))
       ;; remove failed expansions
       (remove (fn [path] (string/includes? path "/*/")))))

(comment
  (count
    (local-repos)
    )
  )


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
  (clone {:repo-id "russmatney/ink-mode"}))

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

(defcom clone-cmd
  {:name          "clone"
   :one-line-desc "Clone from your Github Stars"
   :description
   ["When passed a repo-id, copies it into ~/repo-id."
    "Depends on `hub` on the command line."
    "Does not support private repos."
    "If no repo-id is passed, fetches stars from github."]
   :handler
   (fn [_config parsed]
     (if-let [repo-id (some-> parsed :arguments first)]
       (clone {:repo-id repo-id})
       (clone-from-stars)))})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; repo?
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn repo?
  "Returns true if the passed path is a git repo"
  [repo-path]
  (fs/exists? (str (zsh/expand repo-path) "/.git")))

(comment)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fetch
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch [repo-path]
  (notify (str "Fetching " repo-path))
  (-> {:read-key :err}
      (bb/run-proc
        ^{:dir (zsh/expand repo-path)}
        ($ git "fetch" --verbose))
      (->>
        (apply notify))))

(comment
  (fetch "~/dotfiles")

  (-> ^{:dir "/home/russ/russmatney/dotfiles"}
      ($ git "fetch" --verbose)
      check :err slurp))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; update local repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def local-repo-group-dirs
  [(zsh/expand "~/russmatney")
   (zsh/expand "~/teknql")])

(defn update-local-repos
  "Updates local repo refs using git-summary.

  git-summary repo: https://github.com/MirkoLedda/git-summary
  Expects git-summary to exist on the PATH.

  If run in directory `parent-dir`, git-summary runs a fetch in all children,
  effectively a git fetch in all `parent-dir/*`.

  See `local-repo-group-dirs` for parent-dirs that this command runs in.

  This function uses tmux-fire rather than clojure/shell or bb/process to let
  the running shell/tmux-session handle the auth.
  "
  []
  (notify "Updating local repo refs via git-summary" local-repo-group-dirs)
  (for [dir local-repo-group-dirs]
    (tmux/fire (str "cd " dir " && git-summary"))))

(defcom update-local-repos-cmd
  {:defcom/name    "update-local-repos-cmd"
   :defcom/handler
   (fn [_ _] (update-local-repos))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; gprom
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn gprom [repo-path]
  (let [path (zsh/expand repo-path)]
    ;; TODO stash/pop local changes as well
    (println "TODO gprom in path" path)))

(defcom gprom-cmd
  {:name    "gprom"
   :handler (fn [_ parsed]
              (let [path (-> parsed :arguments first)]
                (gprom path)))})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dirty/is-clean?
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare is-clean?)
(defn dirty? [x] (-> x is-clean? not))
(defn is-clean?
  "Returns true if there is no local diff in the passed path.
  Expects a .git directory at <path>/.git"
  [repo-path]
  (-> {:error-message
       (str "RALPHIE ERROR for " repo-path " in git/dirty?")}
      (bb/run-proc
        ^{:dir (zsh/expand repo-path)}
        ($ git status --porcelain))
      empty?))

(comment
  (is-clean? "~/russmatney/ralphie")
  (dirty? "~/russmatney/ralphie")

  (-> ^{:dir "/home/russ/russmatney/ralphie"}
      ($ git diff HEAD)
      check :out slurp
      empty?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; needs-push?
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn needs-push? [repo-path]
  (-> {:error-message
       (str "RALPHIE ERROR for " repo-path " in git/needs-push?")}
      (bb/run-proc
        ^{:dir (zsh/expand repo-path)}
        ($ git status))
      (->>
        (filter #(re-seq #"Your branch is ahead" %))
        seq)))

(comment
  (needs-push? "~/russmatney/ralphie"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; needs-pull?
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn needs-pull?
  "Returns true if git status reports that we are behind.
  NOTE that no fetch is made in this function, it only parses
  the current git status so the origin reference may be
  out of date. You can use `(git/fetch repo-path)` to update the repo's ref."
  [repo-path]
  (-> {
       :error-message
       (str "RALPHIE ERROR for " repo-path " in git/needs-push?")}
      (bb/run-proc
        ^{:dir (zsh/expand repo-path)}
        ($ git status))
      (->>
        (filter #(re-seq #"branch is behind" %))
        seq)
      ))

(comment
  (needs-pull? "~/russmatney/dotfiles")
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; status
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn status [repo-path]
  (let [res         (-> {:error-message
                         (str "RALPHIE ERROR for " repo-path " in git/status")}
                        (bb/run-proc
                          ^{:dir (zsh/expand repo-path)}
                          ($ git status))
                        seq)
        dirty?      (->> res (filter #(re-seq #"not staged for commit" %)) seq)
        needs-pull? (->> res (filter #(re-seq #"branch is behind" %)) seq)
        needs-push? (->> res (filter #(re-seq #"branch is ahead" %)) seq)]
    {:git/dirty?      dirty?
     :git/needs-pull? needs-pull?
     :git/needs-push? needs-push?}))

(comment
  (status "~/russmatney/dotfiles")
  (status "~/todo")

  )
