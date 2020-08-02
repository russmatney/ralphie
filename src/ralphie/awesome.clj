(ns ralphie.awesome
  (:require
   [ralphie.rofi :as rofi]
   [ralphie.workspace :as workspace]
   [ralphie.command :refer [defcom]]
   [ralphie.config :as config]
   [ralphie.item :as item]
   [clojure.pprint]
   [org-crud.core :as org-crud]
   [clojure.string :as string]
   [clojure.java.shell :as sh]))

(declare init-awesome set-layout)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; awesome-client helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn awm-cli
  "Prefixes the passed lua code with common requires and local vars."
  [cmd]
  (let [full-cmd (->> ["local awful = require \"awful\";\n"
                       "local inspect = require \"inspect\";\n"
                       "local lume = require \"lume\";\n"
                       "local s = awful.screen.focused();\n"
                       "local lain = require \"lain\";\n"
                       cmd]
                      (apply str))]
    (clojure.pprint/pprint "<awesome-client INPUT>")
    (clojure.pprint/pprint (string/split-lines full-cmd))
    (sh/sh "awesome-client" full-cmd)))

(comment
  (awm-cli "add_all_tags()")
  (set-layout "awful.layout.suit.fair"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; converts a clojure map to a lua table
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO needs an escape/no quote signal of some kind
;; metadata?
(defn ->lua-arg [arg]
  (cond
    (string? arg)
    (str "\"" arg "\"")

    (map? arg)
    (->> arg
         (map (fn [[k v]]
                (str (name k) " = " (->lua-arg v))))
         (string/join ",")
         (#(str "{" % "}")))

    (coll? arg)
    (->> arg
         (map (fn [x]
                (->lua-arg x)))
         (string/join ",")
         (#(str "{" % "}")))))

(comment
  (string? "hello")
  (->lua-arg "hello")
  (->lua-arg {:screen "s" :tag "yodo"}))

;; (defonce --init-args (atom nil))

(defn awm-fn [fn & args]
  ;; (cond
  ;;   (= fn "init")
  ;;   (reset! --init-args args))

  (str fn "("
       (->> args
            (map ->lua-arg)
            (string/join ", ")
            (apply str))
       ")"))

(comment
  (init-awesome)
  ;; @--init-args

  (awm-fn "awful.tag.add"
          "ralphie"
          {:screen "s"
           :layout "awful.layout.suit.floating"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; list awesome tags
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn awesome-tag-names []
  (some->>
    (awm-cli (str "return inspect(lume.map(awful.screen.focused().tags, "
                  "function (t) return t.name end))"))
    :out
    (re-find #"\{ (.+) \}")
    first
    (drop 2)
    reverse
    (drop 2)
    reverse
    (apply str)
    (#(string/replace % "\"" ""))
    (#(string/split % #","))
    (map string/trim)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; create new tag
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn awful-tag-add [& args]
  (apply (partial awm-fn "awful.tag.add") args))


(comment
  (awful-tag-add
    "ralphie"
    {:screen "s"
     :layout "awful.layout.suit.floating"}))

(defn create-tag! [name]
  (awm-cli (awful-tag-add name
                          {:screen "s"
                           :layout "lain.layout.centerwork"})))

(defcom awesome-create-tag
  {:name          "awesome-create-tag"
   :one-line-desc "Creates a new tag in your _Awesome_ Window Manager."
   :description   []
   :handler
   (fn [_ {:keys [arguments]}]
     (if-let [tag-name (some-> arguments first)]
       (create-tag! tag-name)

       ;; no tag, get from rofi
       (let [existing-tag-names (set (awesome-tag-names))]
         (rofi/rofi
           {:msg "New Tag Name?"}
           (->>
             ;; TODO pull in repos.org
             (workspace/all-workspaces)
             (map (comp :name :org/item))
             (remove #(contains? existing-tag-names %))
             create-tag!)))))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Delete current tag
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-current-tag! []
  (awm-cli "s.selected_tag:delete()"))

(comment
  (delete-current-tag!))

(defcom awesome-delete-current-tag
  {:name          "awesome-delete-current-tag"
   :one-line-desc "Deletes the current focused tag."
   :description
   ["Deletes current tag if there are no clients exclusively attached."]
   :handler       (fn [_ _] (delete-current-tag!))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reapply rules to all clients
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn reapply-rules-handler [_config _parsed]
  (awm-cli "reapply_rules();"))

(defcom reapply-rules-cmd
  {:name          "reapply-rules"
   :one-line-desc "Reapplies rules to all clients"
   :description   ["Reapplies rules to clients."
                   "When tags are re-created without metadata, clients get lost."
                   "This should re-run the rules, so they get reattached."]
   :handler       reapply-rules-handler})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Set tag laytou
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-layout
  [layout]
  (awm-cli (str "set_layout(" layout ");")))

(comment
  (println "hi")
  (set-layout "awful.layout.suit.fair"))

(defn set-tag-layout-handler [_config parsed]
  (let [layout (or (some-> parsed :arguments first)
                   ;; TODO current tag fn to set name in this str?
                   (rofi/rofi {:msg "Set current tag layout to:"}
                              ["awful.layout.suit.tile"
                               "awful.layout.suit.floating"
                               "awful.layout.suit.fair"
                               "awful.layout.suit.magnifier"
                               "awful.layout.suit.spiral"
                               "awful.layout.suit.spiral.dwindle"
                               "lain.layouts.centerwork"
                               "lain.layouts.centerwork.horizontal"]))]
    (set-layout layout)))

(defcom set-tag-layout-cmd
  {:name          "set-tag-layout"
   :one-line-desc "Sets the current tag's layout."
   :description   ["Sets the current tag's layout."]
   :handler       set-tag-layout-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Awesome Global Init
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-config [conf-org]
  (let [awesome-tags
        (item/->level-1-list conf-org item/awesome-tag-parent?)]
    {:tag-names (seq (map :name awesome-tags))}))

(defn init-awesome
  "Initializes awesome's configuration process.

  The parsed config is handed into all init_helpers."
  ([] (init-awesome nil nil))
  ([_config _parsed]
   (->>
     (config/awesome-config-org-path)
     org-crud/path->nested-item
     build-config
     (awm-fn "init")
     awm-cli)

   ;; pause? wait for all-clear?
   (awm-cli "reapply_rules();")
   ))

(comment
  (init-awesome)
  )

(defcom init-cmd
  {:name          "awesome-init"
   :one-line-desc "Initializes your awesome config"
   :description   ["Initializes your awesome config."
                   "Reads awesome config from a `config.org` file"]
   :handler       init-awesome})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Init Tags
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn init-tags
  ([] (init-tags nil nil))
  ([_config _parsed]
   (->>
     (config/awesome-config-org-path)
     org-crud/path->nested-item
     build-config
     (awm-fn "init_tags")
     awm-cli)))

(comment
  (init-tags))

(defcom init-tags-cmd
  {:name          "init-tags"
   :one-line-desc "Recreates the current AwesomeWM tags"
   :description   ["Recreates the current AwesomeWM tags."
                   "Pulls the latest from your `config.org`"]
   :handler       init-tags})
