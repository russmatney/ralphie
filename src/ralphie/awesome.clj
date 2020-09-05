(ns ralphie.awesome
  (:require
   ;; [ralphie.rofi :as rofi]
   ;; [ralphie.workspace :as workspace]
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

(defn awm-cli-parse-output
  "Parses the output of awm-cli, with assumptions not worth baking into the root
  command."
  [str]
  (->>
    ;; remove leading `string` label
    (-> str string/trim (string/replace #"^string " ""))

    ;; drop quotes
    (drop 1) reverse
    (drop 1) reverse

    ;; rebuild string
    string/join

    ;; convert to clojure data structure
    load-string))


(defn awm-cli
  "Prefixes the passed lua code with common requires and local vars."
  ([cmd] (awm-cli {:pp? true :parse? true} cmd))
  ([{:keys [parse? pp?]} cmd]
   (let [full-cmd (->> ["local awful = require \"awful\";\n"
                        "local inspect = require \"inspect\";\n"
                        "local lume = require \"lume\";\n"
                        "local s = awful.screen.focused();\n"
                        "local lain = require \"lain\";\n"
                        "local view = require \"fennelview\";\n"
                        cmd]
                       (apply str))]
     (when pp?
       (clojure.pprint/pprint "<awesome-client INPUT>")
       (clojure.pprint/pprint (string/split-lines full-cmd)))
     (let [res
           (sh/sh "awesome-client" full-cmd)]
       (when pp?
         (clojure.pprint/pprint "<awesome-client OUTPUT>")
         (clojure.pprint/pprint res))
       (if parse?
         (awm-cli-parse-output (:out res))
         res)))))

(comment

  (awm-cli
    (str
      "return view(lume.map(client.get(), "
      "function (t) return {name= t.name
} end))"))

  (println "hello")
  (awm-cli "print('hello')")
  (awm-cli "return view(lume.map(s.tags, function (t) return {name= t.name} end))")
  (awm-cli "add_all_tags()")
  (set-layout "awful.layout.suit.fair"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; converts a clojure map to a lua table
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->snake-case [s]
  (string/replace s "-" "_"))

(defn ->lua-key [s]
  (-> s
      (string/replace "-" "_")
      (string/replace "?" "")))

;; TODO needs an escape/no quote signal of some kind for literals
;; eg. `awful.layout.suit.fair`
;; maybe clj metadata?
(defn ->lua-arg [arg]
  (cond
    (nil? arg)
    "nil"

    (boolean? arg)
    (str arg)

    (or (string? arg)
        (keyword? arg))
    (str "\"" arg "\"")

    (int? arg)
    arg

    (map? arg)
    (->> arg
         (map (fn [[k v]]
                (str "\n" (->lua-key (name k)) " = " (->lua-arg v))))
         (string/join ", ")
         (#(str "{" % "} \n")))

    (coll? arg)
    (->> arg
         (map (fn [x]
                (->lua-arg x)))
         (string/join ",")
         (#(str "{" % "} \n")))))

(comment
  (string? "hello")
  (->lua-arg "hello")
  (->lua-arg {:level 1 :status :status/done})
  (->lua-arg {:fix-keyword 1})
  ;; drop question marks
  (->lua-arg {:clean? nil})
  (->lua-arg {:clean? false})
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
  (awm-cli
    (str
      "return print(" (awm-fn "inspect" {:hello "world"}) ");"))

  (init-awesome)
  ;; @--init-args

  (awm-fn "awful.tag.add"
          "ralphie"
          {:screen "s"
           :layout "awful.layout.suit.floating"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Awesome Data/current-state fetchers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn screen []
  (awm-cli
    (str "return view({
tags= lume.map(s.tags, function (t) return
{name= t.name,
index= t.index,
} end),
geometry= s.geometry})")))

(defn all-tags []
  (awm-cli
    {:parse? true}
    (str "return view(lume.map(root.tags(), "
         "function (t) return {
name= t.name,
selected= t.selected,
index= t.index,
clients= lume.map(t:clients(),

function (c) return {
name= c.name,
ontop=c.ontop,
window= c.window,
} end),
} end))")))

(comment
  (->> (all-tags)
       (map #(dissoc % :clients)))
  )

(defn tag-for-name [name]
  (some->>
    (all-tags)
    (filter (comp #(= % name) :name))
    first))

(comment
  (tag-for-name "yodo-dev"))

(defn current-tag-name
  ""
  []
  (-> (awm-cli
        {:parse? true}
        (str "return view({name=s.selected_tag.name})"))
      :name))

(defn current-tag []
  (tag-for-name (current-tag-name)))

(defn visible-clients []
  (awm-cli
    {:parse? true}
    (str "return view(lume.map(awful.screen.focused().clients, "
         "function (t) return {name= t.name} end))")))

(defn all-clients []
  (awm-cli
    {:parse? true}
    (str "return view(lume.map(client.get(), "
         "function (c) return {
name= c.name,
geometry= c:geometry(),
window= c.window,
type= c.type,
class= c.class,
instance= c.instance,
pid= c.pid,
role= c.role,
tags= lume.map(c:tags(), function (t) return {name= t.name} end),
first_tag= c.first_tag.name,
} end))")))

(comment
  (->> (all-clients)
       (filter (comp #(= % "ralphie") :name))
       )
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; create new tag
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn awful-tag-add [& args]
  (apply (partial awm-fn "awful.tag.add") args))

(comment
  (->
    (str "return view({name= "
         (awful-tag-add
           "new-tag" {})
         ".name});")
    awm-cli))

(defn create-tag! [name]
  (awm-cli (awful-tag-add name {})))

;; (defcom awesome-create-tag
;;   {:name          "awesome-create-tag"
;;    :one-line-desc "Creates a new tag in your _Awesome_ Window Manager."
;;    :description   []
;;    :handler
;;    (fn [_ {:keys [arguments]}]
;;      (if-let [tag-name (some-> arguments first)]
;;        (create-tag! tag-name)

;;        ;; no tag, get from rofi
;;        (let [existing-tag-names (->> (all-tags) (map :name) set)]
;;          (rofi/rofi
;;            {:msg "New Tag Name?"}
;;            (->>
;;              ;; TODO pull in repos.org
;;              (workspace/all-workspaces)
;;              (map (comp :name :org/item))
;;              (remove #(contains? existing-tag-names %))
;;              create-tag!)))))})

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
;; Init Tags
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn initial-tags-for-awesome [items]
  (->> items
       (filter item/workspace-key)
       (sort-by item/workspace-key)))

(defn init-tags-config []
  {:tag-names (->>
                (config/workspaces-file)
                org-crud/path->nested-item
                :items
                initial-tags-for-awesome
                (map :name)
                seq)})

(defn init-tags
  ([] (init-tags nil nil))
  ([_config _parsed]
   (->> (init-tags-config)
        (awm-fn "init_tags")
        awm-cli
        )))

(comment
  (init-tags))

(defcom init-tags-cmd
  {:name          "init-tags"
   :one-line-desc "Recreates the current AwesomeWM tags"
   :description   ["Recreates the current AwesomeWM tags."
                   "Pulls the latest from your `config.org`"]
   :handler       init-tags})

(comment
  nil)
