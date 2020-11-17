(ns ralphie.fzf
  (:require
   [ralphie.command :refer [defcom] :as command]
   [babashka.process :refer [process]]
   [clojure.string :as string]))

(defn fzf [xs]
  (let [labels         (->> xs (map :fzf/label))
        proc           (process ["fzf" "-m"]
                                {:in  (string/join "\n" labels)
                                 :err :inherit
                                 :out :string})
        selected-label (-> @proc :out string/trim)]
    (when (seq selected-label)
      (println selected-label)
      (some->> xs
               (filter (comp #{selected-label} :fzf/label))
               first))))

(defn fzf-handler
  ([] (fzf-handler nil nil))
  ([config parsed]
   (when-let [cmd (->> config
                       :commands
                       (filter (comp seq :name))
                       (map (fn [cmd] (assoc cmd :fzf/label (:name cmd))))
                       fzf)]
     ;; TODO trim parsed to remove fzf argument
     (command/call-handler cmd config parsed))))

(defcom fzf-cmd
  {:name          "fzf"
   :one-line-desc "Select a ralphie command via fzf."
   :description   "Expects to be called on the command line."
   :handler       fzf-handler})
