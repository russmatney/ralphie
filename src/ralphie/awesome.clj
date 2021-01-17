(ns ralphie.awesome
  (:require
   [ralph.defcom :refer [defcom]]
   [ralphie.config :as config]
   [ralphie.notify :as notify]
   [ralphie.item :as item]
   [clojure.pprint]
   [org-crud.core :as org-crud]
   [clojure.string :as string]
   [babashka.process :refer [$ check]]))

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
    ;; TODO: use edn/read-string?
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
           (-> ($ awesome-client ~full-cmd) check :out slurp)]
       (when pp?
         (clojure.pprint/pprint "<awesome-client OUTPUT>")
         (clojure.pprint/pprint res))
       (if parse?
         (awm-cli-parse-output res)
         res)))))

(comment
  (awm-cli
    (str
      "return view(lume.map(client.get(), "
      "function (t) return {name= t.name} end))"))

  (println "hello")
  (awm-cli "print('hello')")
  (awm-cli "return view(lume.map(s.tags, function (t) return {name= t.name} end))"))

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

  (awm-fn "awful.tag.add"
          "ralphie"
          {:screen "s"
           :layout "awful.layout.suit.floating"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Set layout
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-layout-handler
  ([] (set-layout-handler nil nil))
  ([_config _parsed]
   (awm-cli {:parse? false
             :pp?    true}
            "awful.layout.set(awful.layout.suit.tile);"
            ;; "awful.layout.set(lain.layout.centerwork);"
            )))

(comment

  (awm-cli {:parse? false
            :pp?    true}
           ;; "awful.layout.set(awful.layout.suit.tile);"
           ;; "awful.layout.set(lain.layout.centerwork);"

           "awful.layout.set(awful.layout.suit.spiral.dwindle)")

  (awm-cli
    (str
      "return print(" (awm-fn "inspect" {:hello "world"}) ");")))


(defcom set-layout-cmd
  {:name          "set-layout"
   :one-line-desc "Sets the awesome layout"
   :description   [""]
   :handler       set-layout-handler})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Set above and ontop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-above-and-ontop []
  (notify/notify "Setting above and ontop")
  (awm-cli {:parse? false
            :pp?    false}
           "
_G.client.focus.ontop = true;
_G.client.focus.above = true;"))


(defcom set-above-and-ontop-cmd
  {:name    "set-above-and-ontop"
   :handler (fn [_config _parsed]
              (set-above-and-ontop))})

(comment
  (set-above-and-ontop))


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
  (->> (awm-cli
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
} end))"))
       (map (fn [t]
              (-> t
                  (update t :clients #(into [] %))
                  (assoc :empty (zero? (count (:clients t)))))))))
(comment
  (->> (all-tags)
       (map #(dissoc % :clients))))

(defn tag-for-name
  ([name] (tag-for-name name (all-tags)))
  ([name all-tags]
   (some->>
     all-tags
     (filter (comp #(= % name) :name))
     first)))

(comment
  (tag-for-name "yodo-dev"))

(defn current-tag-name
  ""
  []
  (-> (awm-cli
        {:parse? true}
        (str "return view({name=s.selected_tag.name})"))
      :name))

(defn current-tag-names
  ""
  []
  (->> (awm-cli
         {:parse? true}
         (str "return view(lume.map(s.selected_tags,
function (t) return {name= t.name} end))"))
       (map :name)))

(comment
  (current-tag-names))


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
       (filter (comp #(= % "ralphie") :name))))

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

;; Test to ensure that these all pass tag-name through
(defn create-tag! [tag-name]
  (notify/notify (str "creating new awesome tag: " tag-name))
  (awm-cli
    (str "awful.tag.add(\"" tag-name "\"," "{layout=awful.layout.suit.tile});"))
  tag-name)

(comment
  (create-tag! "new-tag"))

(defn focus-tag! [tag-name]
  (notify/notify (str "focusing awesome tag: " tag-name))
  (awm-cli
    (str "local tag = awful.tag.find_by_name(nil, \"" tag-name "\");
tag:view_only(); "))
  tag-name)

(defn toggle-tag [tag-name]
  ;; viewtoggle tag
  (awm-cli
    (str "awful.tag.viewtoggle(awful.tag.find_by_name(s, \"" tag-name "\"));"))
  tag-name)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Delete current tag
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-tag! [tag-name]
  (awm-cli
    (str "local tag = awful.tag.find_by_name(nil, \"" tag-name "\");
tag:delete(); ")))

(defn delete-current-tag! []
  (awm-cli "s.selected_tag:delete()"))

(comment
  (delete-current-tag!))

(defn delete-current-tag-handler [_ _]
  (delete-current-tag!))

(defcom awesome-delete-current-tag
  {:name          "awesome-delete-current-tag"
   :one-line-desc "Deletes the current focused tag."
   :description
   ["Deletes current tag if there are no clients exclusively attached."]
   :handler       delete-current-tag-handler})

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
       ;; TODO rename to workspace number
       (filter item/scratchpad?)
       (sort-by :org.prop/key)))

(defn init-tags-config []
  {:tag-names (->>
                (config/workspaces-file)
                org-crud/path->nested-item
                :org/items
                initial-tags-for-awesome
                (map :org/name)
                seq)})

(defn init-tags
  ([] (init-tags nil nil))
  ([_config _parsed]
   (->> (init-tags-config)
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

(comment)
