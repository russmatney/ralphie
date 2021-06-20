(ns ralphie.core
  (:require
   [defthing.defcom :as defcom]
   [ralphie.awesome]
   [ralphie.browser]
   [ralphie.emacs]
   [ralphie.fzf]
   [ralphie.git]
   [ralphie.install]
   [ralphie.item]
   [ralphie.monitor]
   [ralphie.notify]
   [ralphie.org]
   [ralphie.outdated]
   [ralphie.picom]
   [ralphie.rofi :as rofi]
   [ralphie.screenshot]
   [ralphie.spotify]
   [ralphie.tmux]
   [ralphie.zsh]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defcom->rofi
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn defcom->rofi [{:keys [doc name] :as cmd}]
  {:rofi/label     (str "<span>" name " </span> "
                        (when doc (str "<span color='gray'>" doc "</span> ")))
   :rofi/on-select (fn [_] (defcom/exec cmd))} )

(defcom/defcom rofi
  "Rofi for all defcoms in all required namespaces."
  {:doctor/depends-on ["rofi"]}
  (->> (defcom/list-commands)
       (map defcom->rofi)
       (rofi/rofi {:require-match? true
                   :msg            "All defthing/defcom commands"})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main [& args]
  (println "[RALPHIE] start" args)
  (let [start-time (System/currentTimeMillis)
        res        (apply defcom/run args)
        dt         (- (System/currentTimeMillis) start-time)]
    (println "[RALPHIE] complete" args "in" dt "ms")
    res))
