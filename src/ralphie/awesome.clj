(ns ralphie.awesome
  (:require
   [ralphie.rofi :as rofi]
   [ralphie.workspace :as workspace]
   [ralphie.command :refer [defcom]]
   [clojure.string :as string]
   [clojure.java.shell :as sh]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; awesome-client helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn awm-cli
  "Prefixes the passed lua code with common requires and local vars."
  [cmd]
  (let [cmd (->> ["local awful = require \"awful\";\n"
                  "local inspect = require \"inspect\";\n"
                  "local lume = require \"lume\";\n"
                  "local s = awful.screen.focused();\n"
                  "local lain = require \"lain\";\n"
                  cmd]
                 (apply str))]
    (println "sending to awesome" cmd)
    (sh/sh "awesome-client" cmd)))

(comment
  (awm-cli "add_all_tags()"))

(defn ->lua-arg [arg]
  (cond
    (string? arg)
    (str "'" arg "'")

    (map? arg)
    (->> arg
         (map (fn [[k v]]
                (str (name k) "= " v)))
         (string/join ",")
         (#(str "{" % "}")))))

(comment
  (string? "hello")
  (->lua-arg "hello")
  (->lua-arg {:screen "s" :tag "yodo"}))

(defn awm-fn [fn & args]
  (str fn "("
       (->> args
            (map ->lua-arg)
            (string/join ", ")
            (apply str))
       ")"))

(comment
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
;; Init tags
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn init-tags-handler [_config _parsed]
  (awm-cli "add_all_tags();"))

(defcom init-tags-cmd
  {:name          "init-tags"
   :one-line-desc "Initializes a set of tags (workspaces) for awesomeWM."
   :description   ["Initializes a set of tags (workspaces) for awesomeWM."
                   "Created to remove tag creation from awesome restarts."]
   :handler       init-tags-handler})

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
  (awm-cli (awm-fn "set_layout" layout)))


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
