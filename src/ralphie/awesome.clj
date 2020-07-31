(ns ralphie.awesome
  (:require
   [ralphie.rofi :as rofi]
   [ralphie.command :refer [defcom]]
   [clojure.string :as string]
   [clojure.java.shell :as sh]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; awesome-client helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn awm-cli
  "Prefixes the passed lua code with common requires and local vars."
  [cmd]
  (sh/sh "awesome-client"
         (->> ["local awful = require \"awful\";\n"
               "local inspect = require \"inspect\";\n"
               "local lume = require \"lume\";\n"
               "local s = awful.screen.focused();\n"
               cmd]
              (apply str))))

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
;; create new tag
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn awful-tag-add [tag-name]
  (awm-fn "awful.tag.add" tag-name))

(defn awful-return []
  (str
    "return inspect(lume.map(awful.screen.focused().tags, "
    "function (t) return t.name end))"))

(defn create-tag! [name]
  (awm-cli (str (awful-tag-add name) "\n" (awful-return))))

(comment
  (create-tag! "ralphie"))

(defcom awesome-create-tag
  {:name          "awesome-create-tag"
   :one-line-desc "Creates a new tag in your _Awesome_ Window Manager."
   :description   []
   :handler       (fn [_ {:keys [arguments]}]
                    (let [tag-name (some-> arguments first)]
                      (if tag-name
                        (create-tag! tag-name)
                        (create-tag!
                          (rofi/rofi {:msg "New Tag Name?"})))))})

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
