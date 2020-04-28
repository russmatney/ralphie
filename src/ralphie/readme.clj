(ns ralphie.readme
  (:require [clojure.string :as string]))

(defn command->i3-binding-usage [{:keys [name i3-keybinding]}]
  ["#+BEGIN_SRC i3-config"
   (str "bindsym " i3-keybinding " exec --no-startup-id ralphie " name)
   "#+END_SRC"])

(defn command->sh-usage [{:keys [name]}]
  ["#+BEGIN_SRC sh"
   (str "ralphie " name)
   "#+END_SRC"])

(defn command->feature-doc
  "Builds an org bullet for the command."
  [{:keys [name one-line-desc description] :as cmd}]
  (concat
    [(str "** ~" name "~" (when one-line-desc (str ": " one-line-desc)))]
    description
    (command->sh-usage cmd)
    (command->i3-binding-usage cmd)))

(comment
  (command->feature-doc {:name          "name"
                         :one-line-desc "just the name for the thing"
                         :description   ["Names the thing."]}))

(defn readme-feature-lines [config]
  (->> config
       :commands
       (map command->feature-doc)
       flatten
       (string/join "\n")))

(comment
  (readme-feature-lines
    {:commands [{:name          "name"
                 :one-line-desc "just the name for the thing"
                 :description   ["Names the thing."]}]}))

(defn build-readme-cmd [config _parsed]
  (let [feats (readme-feature-lines config)]
    feats))

(def build
  {:name          "build-readme"
   :short         "-"
   :one-line-desc "build-readme"
   :handler       build-readme-cmd})
