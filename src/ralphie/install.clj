(ns ralphie.install
  (:require
   [babashka.process :refer [$ check]]
   [defthing.defcom :refer [defcom] :as defcom]
   [ralphie.notify :as notify]
   [ralphie.config :as config]
   [ralphie.util :as util]
   [clojure.string :as string]))

(defn symlink
  [source target]
  (-> ($ ln -s ~source ~target)
      check))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Install zsh completion
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn zsh-completion-path []
  (str (config/home-dir) "/.zsh/completion/_ralphie"))

(defcom install-zsh-completion
  (let [cmds-string
        (->> (defcom/list-commands)
             (map :name)
             (string/join " \\
"))
        completion-file (str "#compdef _ralphie ralphie

_arguments -C \\
  \"1: :(
" cmds-string ")\"")]
    (spit (zsh-completion-path) completion-file)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Build uberscript
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom build-uberjar
  (let [proc "rebuilding-ralphie-uberjar"]
    (notify/notify {:subject          "Ralphie Uberjar: Rebuilding"
                    :replaces-process proc})
    (let [cp (util/get-cp (config/project-dir))]
      (->
        ^{:dir (config/project-dir)}
        ($ bb -cp ~cp --uberjar ralphie.jar -m ralphie.core )
        check)
      (notify/notify {:subject          "Ralphie Uberjar: Rebuild Complete"
                      :replaces-process proc}))))
