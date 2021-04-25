(ns ralphie.browser
  (:require [babashka.process :as p]
            [ralph.defcom :refer [defcom]]
            [clojure.string :as string]
            [ralphie.rofi :as rofi]
            [ralphie.notify :as notify]
            [ralphie.clipboard :as clipboard]
            [babashka.process :as process]))

(defn line->tab [s]
  (->>
    s
    (#(string/split % #"\t"))
    ((fn [[tab-id title url]]
       ;; TODO parse browser from tab-id + another (cached) bt command
       {:tab/id    tab-id
        :tab/title title
        :tab/url   url}))))

(defn tabs
  "List browser tabs - depends on https://github.com/balta2ar/brotab."
  []
  (->> (p/process '[bt list])
       p/check
       :out
       slurp
       (#(string/split % #"\n"))
       (map line->tab)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; List open tabs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-open-tabs-handler
  ([] (list-open-tabs-handler nil nil))
  ([_config _parsed]
   (->> (tabs)
        (map #(assoc % :rofi/label
                     ;; TODO pango markup
                     (str (:tab/url %) ": " (:tab/title %))))
        (rofi/rofi {:msg "Open tabs"})
        ((fn [t]
           (rofi/rofi {:msg (str "Selected: " (:tab/url t))}
                      [;; TODO filter options based on url (is this a git url?)
                       {:rofi/label     "Clone repo"
                        :rofi/on-select (fn [_] (notify/notify
                                                  "Clone repo not implemented" t))}
                       {:rofi/label     "Copy to clipboard"
                        :rofi/on-select (fn [_] (notify/notify
                                                  "Copy not implemented" t))}]))))))

(defcom list-open-tabs-cmd
  {:name    "list-open-tabs"
   :one-line-desc
   "Lists currently open tabs via Rofi. Select to clone, copy, or other options."
   :handler list-open-tabs-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copy all open tabs to clipboard
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn copy-all-tabs-handler
  ([] (copy-all-tabs-handler nil nil))
  ([_config _parsed]
   (let [ts (tabs)]
     (->> ts
          (map :tab/url)
          (string/join "\n")
          clipboard/set-clip)
     (notify/notify (str "Copied " (count ts) " tabs to the clipboard")))))

(defcom copy-all-tabs-cmd
  {:name          "copy-all-tabs"
   :one-line-desc "Copies all open urls to the clipboard, seperated by newlines."
   :handler       copy-all-tabs-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Open
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn open
  "Opens the passed url"
  [opts]
  (let [url (some opts [:url :browser.open/url])]
    (->
      (process/$ xdg-open ~url)
      process/check :out slurp)))

(comment
  (open {:url "https://github.com"})
  )
