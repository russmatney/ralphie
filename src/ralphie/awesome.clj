(ns ralphie.awesome
  (:require
   [defthing.defcom :refer [defcom] :as defcom]
   [ralphie.notify :as notify]
   [clojure.pprint]
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

(defcom set-layout
  "Sets the awesome layout"
  (awm-cli {:parse? false
            :pp?    true}
           "awful.layout.set(awful.layout.suit.tile);"
           ;; "awful.layout.set(lain.layout.centerwork);"
           ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Set above and ontop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom set-above-and-ontop
  (do
    (notify/notify "Setting above and ontop")
    (awm-cli {:parse? false
              :pp?    false}
             "
_G.client.focus.ontop = true;
_G.client.focus.above = true;")))


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
         {:parse? true
          :pp?    false}
         (str
           "local f = client.focus;\n"
           "local fwindow = false;\n"
           "if f then
  fwindow = f.window;
end;\n"
           "local mwindow = false;\n"
           "local mclient = awful.client.getmaster();\n"
           "if mclient then
  mwindow = mclient.window;
end;\n"
           "return view(lume.map(root.tags(), "
           "function (t)
local l = t.layout;
local lname = false;
if l then
  lname = l.name
end;\n
           return {
                   name=     t.name,
                   selected= t.selected,
                   layout=   lname,
                   index=    t.index,
                   clients=  lume.map (t:clients(),
function (c) return {
   name=     c.name,
   ontop=    c.ontop,
   window=   c.window,
   master=   mwindow  == c.window,
   focused=  fwindow == c.window,
   type=     c.type,
   class=    c.class,
   instance= c.instance,
   pid=      c.pid,
   role=     c.role,
  } end),
} end))"))
       (map (fn [t]
              (-> t
                  (update :clients #(into [] %))
                  (assoc :empty (zero? (count (:clients t)))))))))

(comment
  (->> (all-tags)
       (map :clients)
       ;; (map #(dissoc % :clients))
       ))

(defn tag-for-name
  ([name] (tag-for-name name (all-tags)))
  ([name all-tags]
   (some->>
     all-tags
     (filter (comp #{name} :name))
     first)))

(comment
  (tag-for-name "yodo-dev"))

(defn workspace-for-name
  "Same as tag-for-name, but namespaces all the keys with :awesome/ prefixes.
       Intended to be merged into a workspace map."
  ([name] (workspace-for-name name (all-tags)))
  ([name all-tags]
   (some->>
     all-tags
     (filter (comp #{name} :name))
     first
     ((fn [tag]
        {:awesome/name     (:name tag)
         :awesome/clients  (:clients tag)
         :awesome/index    (:index tag)
         :awesome/selected (:selected tag)
         :awesome/empty    (:empty tag)
         ;; DEPRECATED but left for now
         :awesome/tag tag})))))

(comment
  (workspace-for-name "ralphie")
  )

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
                               name=      c.name,
                               geometry=  c:geometry (),
                               window=    c.window,
                               type=      c.type,
                               class=     c.class,
                               instance=  c.instance,
                               pid=       c.pid,
                               role=      c.role,
                               tags=      lume.map   (c:tags(), function (t) return {name= t.name} end),
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
           "                 new-tag " {})
         "                   .name});")
       awm-cli))

;; Test to ensure that these all pass tag-name through
(defn create-tag! [tag-name]
  ;; (notify/notify (str "creating new awesome tag: " tag-name))
  (awm-cli
    {:pp? false}
    (str "awful.tag.add(\"" tag-name "\"," "{layout=awful.layout.suit.tile});"))
  tag-name)

(comment
  (create-tag! "new-tag"))

(defn focus-tag! [tag-name]
  ;; (notify/notify (str "focusing awesome tag: " tag-name))
  (awm-cli
    {:pp? false}
    (str "local tag = awful.tag.find_by_name(nil, \"" tag-name "\");
tag:view_only(); "))
  tag-name)

(defn toggle-tag [tag-name]
  ;; viewtoggle tag
  (awm-cli
    {:pp? false}
    (str "awful.tag.viewtoggle(awful.tag.find_by_name(s, \"" tag-name "\"));"))
  tag-name)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Delete current tag
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-tag! [tag-name]
  (awm-cli
    {:pp? false}
    (str "local tag = awful.tag.find_by_name(nil, \"" tag-name "\");
tag:delete(); ")))

(defn delete-current-tag! []
  (awm-cli
    {:pp? false}
    "s.selected_tag:delete()"))

(defcom awesome-delete-current-tag
  "Deletes the current focused tag."
  delete-current-tag!)
