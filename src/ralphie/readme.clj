(ns ralphie.readme
  (:require
   [ralphie.config :refer [project-dir]]
   [clojure.string :as string]))

(def readme-path (str (project-dir) "/readme.md"))
(def readme-head-path (str (project-dir) "/readme-head.md"))
(def readme-tail-path (str (project-dir) "/readme-tail.md"))

(defn command->i3-binding-usage [{:keys [name i3-keybinding]}]
  ["```"
   (str "bindsym " i3-keybinding " exec --no-startup-id ralphie " name)
   "```"])

(defn command->sh-usage [{:keys [name]}]
  ["```sh"
   (str "ralphie " name)
   "```"])

(defn command->feature-doc
  "Builds an org bullet for the command."
  [{:keys [name one-line-desc description] :as cmd}]
  (concat
    [(str "## `" name "`" (when one-line-desc (str ": " one-line-desc)))]
    description
    (command->sh-usage cmd)
    (command->i3-binding-usage cmd)))

(defn readme-feature-lines [config]
  (->> config
       :commands
       (map command->feature-doc)
       flatten
       (string/join "\n")))

(defn build-readme-cmd [config _parsed]
  (let [feats (readme-feature-lines config)
        head  (slurp readme-head-path)
        tail  (slurp readme-tail-path)
        parts [head feats tail]]
    (spit readme-path (apply str parts))))

(def build
  {:name          "build-readme"
   :short         "-"
   :one-line-desc "build-readme"
   :handler       build-readme-cmd})
