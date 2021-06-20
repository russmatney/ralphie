(ns ralphie.core
  (:require
   [defthing.defcom]
   [ralph.defcom :as defcom]

   [ralphie.awesome]
   [ralphie.browser]
   [ralphie.doctor]
   [ralphie.dashboard]
   [ralphie.emacs]
   [ralphie.fzf]
   [ralphie.git]
   [ralphie.help]
   [ralphie.install]
   [ralphie.item]
   [ralphie.monitor]
   [ralphie.notify]
   [ralphie.picom]
   [ralphie.readme]
   [ralphie.rofi :as rofi]
   [ralphie.screenshot]
   [ralphie.spotify]
   [ralphie.term]
   [ralphie.tmux]
   [ralphie.update]
   [ralphie.window]
   [ralphie.zsh]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defthing-defcom->rofi
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn defcom->rofi [parsed {:keys [defcom/name one-line-desc description] :as x}]
  (assoc x
         :rofi/label (str
                       "<span>" name " </span> "
                       (when one-line-desc
                         (str "<span color='gray'>" one-line-desc "</span> "))
                       (when description
                         (apply str (->> description
                                         (map (fn [d]
                                                (-> d
                                                    (string/replace #"\n" " ")
                                                    (#(str "<small>" % "</small>")))))))))
         :rofi/on-select
         (fn [cmd] (defcom/call-handler cmd nil parsed))))

(defn config->rofi-commands
  "VERIFY: Rofi needs commands passed in vs. relying on defcom/list-commands
  Feels like this could be rewritten to pull from defcom/list-commands,
  so we stop passing this config around.
  That's probably where the config got it, anyway.
  "
  [parsed config]
  (->> config
       :commands
       (filter (comp seq :defcom/name))
       (map (partial defcom->rofi parsed))))


;; TODO Not sure where to put this yet.
;; The rofi defcom conflicts with the rofi/rofi function

(defn defthing-defcom->rofi [{:keys [doc name] :as cmd}]
  {:rofi/label
   (str
     "<span>" name " </span> "
     (when doc (str "<span color='gray'>" doc "</span> "))
     ;; (when doc
     ;;   (apply str (->> doc
     ;;                   (map (fn [d]
     ;;                          (-> d
     ;;                              (string/replace #"\n" " ")
     ;;                              (#(str "<small>" % "</small>"))))))))
     )
   :rofi/on-select
   (fn [_] (defthing.defcom/exec cmd))} )

(defthing.defcom/defcom rofi
  "Rofi for all defcoms in all namespaces.
For Ralphie or Clawe, this means all namespaces required by ralphie/clawe.core.
Depends on `rofi`."
  {:doctor/depends-on ["rofi"]}
  (->>
    (defthing.defcom/list-commands)
    (map defthing-defcom->rofi)
    ;; add in the old commands so we can cut off old rofi
    ;; delete once ralphie defcom is deleted
    (concat (->>
              {:commands (defcom/list-commands)}
              (config->rofi-commands nil)))
    (rofi/rofi {:require-match? true
                :msg            "All defthing/defcom commands"})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main [& args]
  (println "[RALPHIE] start" args)
  (let [start-time (System/currentTimeMillis)
        res        (let [res (apply defthing.defcom/run args)]
                     ;; cut-off old defcom
                     (if-not (= :not-found res) res
                             (apply defcom/run args)))
        dt         (- (System/currentTimeMillis) start-time)]
    (println "[RALPHIE] complete" args "in" dt "ms")
    res))
