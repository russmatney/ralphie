(ns ralphie.notify
  (:require
   [babashka.process :refer [$ check]]))

(defn notify
  ([notice]
   (cond (string? notice) (notify notice nil)

         (map? notice)
         (let [{:keys [subject body]} notice]
           (notify subject body))

         :else
         (notify "Malformed notify call" "Expected string or map.")
         ))

  ([subject body]
   (-> ($ notify-send ~subject ~body)
       check
       :out
       slurp)))


(ns org-crud.fs
  "Subset of me.raynes.fs (clj-commons.fs) ripped for bb compatibility."
  (:refer-clojure :exclude [name parents])
  (:require [clojure.java.io :as io])
  (:import [java.io File]))

(def ^{:doc     "Current working directory. This cannot be changed in the JVM.
             Changing this will only change the working directory for functions
             in this library."
       :dynamic true}
  *cwd* (.getCanonicalFile (io/file ".")))

(defn ^File file
  "If path is a period, replaces it with cwd and creates a new File object
   out of it and paths. Or, if the resulting File object does not constitute
   an absolute path, makes it absolutely by creating a new File object out of
   the paths and cwd."
  [path & paths]
  (when-let [path (apply
                    io/file (if (= path ".")
                              *cwd*
                              path)
                    paths)]
    (if (.isAbsolute ^File path)
      path
      (io/file *cwd* path))))

(defn absolute
  "Return absolute file."
  [path]
  (.getAbsoluteFile (file path)))

(ns org-crud.util
  (:require
   [clojure.set :as set]))

(defn ns-select-keys
  "Selects all keys from the provided `map` that match the given `ns-str`

  Pulled from `wing.core`.
  "
  [ns-str map]
  (into {} (filter (comp #{ns-str} namespace key)) map))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; get-all, get-one
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; url parsing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn re-get
  "Returns the first match for a passed regex and string.
  Shouldn't this function exist?
  "
  [pat s]
  (when s
    (let [parts (re-find pat s)]
      (when (> (count parts) 1)
        (second parts)))))

(def url-regex
  "https://stackoverflow.com/questions/3809401/what-is-a-good-regular-expression-to-match-a-url"
  #"(https?:[/][/](?:www\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\.[^\s]{2,}|www\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\.[^\s]{2,}|https?:[/][/](?:www\.|(?!www))[a-zA-Z0-9]+\.[^\s]{2,}|www\.[a-zA-Z0-9]+\.[^\s]{2,})")

(defn ->url [s]
  (re-get url-regex s))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; merge maps
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; multi-group-by
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns org-crud.headline
  (:require
   [org-crud.util :as util]
   [clojure.string :as string]
   [org-crud.fs :as fs]))

(def ^:dynamic *multi-prop-keys* #{})
(def ^:dynamic *prop-parser*
  "Contains some types with known parses.
  For now supports a few string->dates parses." {})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; headline helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->body-as-strings
  "Parses content :table-row :line-types into a list of strs"
  [{:keys [content]}]
  (->> content
       (filter #(= (:line-type %) :table-row))
       (map :text)))

(defn ->body-string [raw]
  (->> (->body-as-strings raw)
       (string/join "\n")))

(defn ->body
  "Produces a somewhat structured body,
  with support for source blocks.

  Filters drawers.
  "
  [{:keys [content]}]
  (->> content
       (remove #(= :drawer (:type %)))
       (flatten)))

(defn ->metadata
  [{:keys [content]}]
  (->> content
       (filter (comp #(= % :metadata) :line-type))
       (map :text)))

(defn ->drawer
  [{:keys [content]}]
  (->> content
       (filter (comp #(= % :drawer) :type))
       first
       :content
       (map :text)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; property drawers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->prop-key [text]
  (let [[k _val] (string/split text #" " 2)]
    (when k
      (-> k
          (string/replace ":" "")
          (string/replace "_" "-")
          (string/replace "+" "")
          (string/replace "#" "")
          string/lower-case
          keyword))))

(defn ->prop-value [text]
  (when-let [k (->prop-key text)]
    (let [[_k val] (string/split text #" " 2)]
      (when val
        (let [val (string/trim val)]
          (if-let [parser (*prop-parser* k)] (parser val) val))))))

(defn ->properties [x]
  (let [prop-lines
        (cond
          (= :section (:type x))
          (->drawer x)

          (= :root (:type x))
          ;; TODO stop after first non-comment?
          (->> x
               :content
               (filter #(= (:line-type %) :comment))
               (map :text)))]
    (if (seq prop-lines)
      (->> prop-lines
           (group-by ->prop-key)
           (map (fn [[k vals]]
                  (let [vals (map ->prop-value vals)
                        vals (if (contains? *multi-prop-keys* k)
                               ;; sorting just for testing convenience
                               (sort vals)
                               (first vals))]
                    [(keyword (str "org.prop/" (name k))) vals])))
           (into {}))
      {})))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; headline parsers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->level [{:keys [level]}]
  (or level :level/root))



(defn ->id [hl]
  (-> hl ->properties :org.prop/id))

(defn ->name [{:keys [name type]}]
  (cond
    (and name (= type :section))
    (->> name
         (re-find
           #"\*?\*?\*?\*? ?(?:TODO|DONE|CANCELLED)? ?(.*)")
         second
         ((fn [s] (string/replace s #"\[[ X-]\] " ""))))))



(defn ->raw-headline [{:keys [name level]}]
  (when level
    (str (apply str (repeat level "*")) " " name)))

(defn ->tags [{:keys [type content tags]}]
  (cond
    (= :root type)
    (let [text
          (some->>
            content
            (filter (fn [c]
                      (and (= (:line-type c) :comment)
                           (string/includes?
                             (-> c :text string/lower-case)
                             "#+roam_tags"))))
            first
            :text)]
      (when text
        (some->
          (->> text
               string/lower-case
               (re-find #".*roam_tags: (.*)"))
          second
          (string/split #" ")
          set)))

    :else (-> tags (set))))

(defn ->todo-status
  [{:keys [name]}]
  (when name
    (cond
      (re-seq #"(\[-\])" name)
      :status/in-progress

      (re-seq #"(\[ \]|TODO)" name)
      :status/not-started

      (re-seq #"(\[X\]|DONE)" name)
      :status/done

      (re-seq #"CANCELLED" name)
      :status/cancelled

      :else nil)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; url parsing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->urls
  ;; parses an org header for urls from the name and body
  [x]
  (let [strs (conj (->body-as-strings x) (->name x))]
    (->> strs
         (map util/->url)
         (remove nil?))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; word count
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn str->count [str]
  (when str
    (some->
      (string/split str #" ")
      count))
  )

(defn ->word-count [item raw]
  (+ (or (some-> item :org/name str->count) 0)
     (or (some->> raw
                  ->body-as-strings
                  (map str->count)
                  (reduce + 0)) 0)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; item - a general headline
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->item [raw source-file]
  (-> (cond
        (= :section (:type raw))
        (merge {:org/level       (->level raw)
                :org/source-file (-> source-file fs/absolute str)
                :org/id          (->id raw)
                :org/name        (->name raw)
                :org/headline    (->raw-headline raw)
                :org/tags        (->tags raw)
                :org/body        (->body raw)
                :org/body-string (->body-string raw)
                :org/status      (->todo-status raw)}
               (->properties raw))

        (= :root (:type raw))
        (let [props (->properties raw)]
          (merge {:org/level       (->level raw)
                  :org/source-file (-> source-file fs/absolute str)
                  :org/name        (:org.prop/title props)
                  :org/tags        (->tags raw)
                  :org/body        (->body raw)
                  :org/id          (:org.prop/id props)}
                 props)))
      ((fn [item]
         (when item
           (-> item
               (assoc :org/word-count (->word-count item raw))
               (assoc :org/urls (-> raw ->urls set))))))))
(ns organum.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))

;; node constructors

(defn node [type] {:type type :content []})
(defn root [] (node :root))
(defn section [level name tags kw] (merge (node :section) {:level level :name name :tags tags :kw kw}))
(defn block [type qualifier] (merge (node :block) {:block-type type :qualifier qualifier}))
(defn drawer [] (node :drawer))
(defn line [type text] {:line-type type :text text})

(defn classify-line
  "Classify a line for dispatch to handle-line multimethod."
  [ln]
  (let [headline-re #"^(\*+)\s*(.*)$"
        pdrawer-re #"^\s*:(PROPERTIES|END):"
        pdrawer (fn [x] (second (re-matches pdrawer-re x)))
        pdrawer-item-re #"^\s*:([0-9A-Za-z_\-]+):\s*(.*)$"
        block-re #"^\s*#\+(BEGIN|END)_(\w*)\s*([0-9A-Za-z_\-]*)?.*"
        block (fn [x] (rest (re-matches block-re x)))
        def-list-re #"^\s*(-|\+|\s+[*])\s*(.*?)::.*"
        ordered-list-re #"^\s*\d+(\.|\))\s+.*"
        unordered-list-re #"^\s*(-|\+|\s+[*])\s+.*"
        metadata-re #"^\s*(CLOCK|DEADLINE|START|CLOSED|SCHEDULED):.*"
        table-sep-re #"^\s*\|[-\|\+]*\s*$"
        table-row-re #"^\\s*\\|.*"
        inline-example-re #"^\s*:\s.*"
        horiz-re #"^\s*-{5,}\s*$"]
    (cond
     (re-matches headline-re ln) :headline
     (string/blank? ln) :blank
     (re-matches def-list-re ln) :definition-list
     (re-matches ordered-list-re ln) :ordered-list
     (re-matches unordered-list-re ln) :unordered-list
     (= (pdrawer ln) "PROPERTIES") :property-drawer-begin-block
     (= (pdrawer ln) "END") :property-drawer-end-block
     (re-matches pdrawer-item-re ln) :property-drawer-item
     (re-matches metadata-re ln) :metadata
     (= (first (block ln)) "BEGIN") :begin-block
     (= (first (block ln)) "END") :end-block
     (= (second (block ln)) "COMMENT") :comment
     (= (first ln) \#) :comment
     (re-matches table-sep-re ln) :table-separator
     (re-matches table-row-re ln) :table-row
     (re-matches inline-example-re ln) :inline-example
     (re-matches horiz-re ln) :horizontal-rule
     :else :paragraph)))

(defn strip-tags
  "Return the line with tags stripped out and list of tags"
  [ln]
  (if-let [[_ text tags] (re-matches #"(.*?)\s*(:[\w:]*:)\s*$" ln)]
    [text (remove string/blank? (string/split tags #":"))]
    [ln nil]))

(defn strip-keyword
  "Return the line with keyword stripped out and list of keywords"
  [ln]
  (let [keywords-re #"(TODO|DONE)?"
        words (string/split ln #"\s+")]
    (if (re-matches keywords-re (words 0))
      [(string/triml (string/replace-first ln (words 0) "")) (words 0)]
      [ln nil])))

(defn parse-headline [ln]
  (when-let [[_ prefix text] (re-matches  #"^(\*+)\s*(.*?)$" ln)]
    (let [[text tags] (strip-tags text)
          [text kw] (strip-keyword text)]
      (section (count prefix) text tags kw))))

(defn parse-block [ln]
  (let [block-re #"^\s*#\+(BEGIN|END)_(\w*)\s*([0-9A-Za-z_\-]*)?"
        [_ _ type qualifier] (re-matches block-re ln)]
    (block type qualifier)))

;; State helpers

(defn subsume
  "Updates the current node (header, block, drawer) to contain the specified
   item."
  [state item]
  (let [top (last state)
        new (update-in top [:content] conj item)]
    (conj (pop state) new)))

(defn subsume-top
  "Closes off the top node by subsuming it into its parent's content"
  [state]
  (let [top (last state)
        state (pop state)]
    (subsume state top)))

(defmulti handle-line
  "Parse line and return updated state."
  (fn [state ln] (classify-line ln)))

(defmethod handle-line :headline [state ln]
  (conj state (parse-headline ln)))

(defmethod handle-line :begin-block [state ln]
  (conj state (parse-block ln)))

(defmethod handle-line :end-block [state ln]
  (subsume-top state))

(defmethod handle-line :property-drawer-begin-block [state ln]
  (conj state (drawer)))

(defmethod handle-line :property-drawer-end-block [state ln]
  (subsume-top state))

(defmethod handle-line :default [state ln]
  (subsume state (line (classify-line ln) ln)))

(defn parse-file
  "Parse file (name / url / File) into (flat) sequence of sections. First section may be type :root,
   subsequent are type :section. Other parsed representations may be contained within the sections"
  [f]
  (with-open [rdr (io/reader f)]
    (reduce handle-line [(root)] (line-seq rdr))))(ns org-crud.core
  (:require
   [organum.core :as org]
   [org-crud.fs :as fs]
   [org-crud.headline :as headline]
   [clojure.walk :as walk]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parse helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-org-file
  [path]
  (-> path
      fs/absolute
      org/parse-file))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parsing flattened items
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parsed->flattened-items
  "Only parses :type :section (skipping :root).

  Produces flattened items, rather than nested.
  This means deeper org headlines will not be contained within parents.
  "
  [source-file parsed]
  (reduce
    (fn [items next]
      (conj items (merge
                    ;; {:org-section next}
                    (headline/->item next source-file))))
    []
    parsed))

(defn path->flattened-items
  "Returns a flattened list of org items in the passed file.

  Produces flattened items, rather than nested.
  This means deeper org headlines will not be contained within parents.
  See `path->nested-items`."
  [p]
  (->> p
       parse-org-file
       (parsed->flattened-items p)
       (remove (comp nil? :org/name))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parsing nested items
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns ralphie.sh
  "This namespace should mostly go away, especially now that bb/process provides
  a better/more direct interface for processes."
  (:require
   [babashka.process :refer [$ process check]]
   [clojure.java.shell :as clj-sh]
   [clojure.string :as string]))



(defn bash [command]
  (clj-sh/sh "bash" "-c" command))

(defn expand
  [path]
  (-> (str "echo -n " path)
      (bash)
      :out))


(ns ralphie.config
  (:require [ralphie.sh :refer [expand]]))

;; TODO support some kind of configuration

(defn org-dir [] (expand "~/todo"))
(ns ralphie.item
  (:require
   [org-crud.core :as org-crud]
   [ralphie.config :as config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parse Helper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Item data
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO namespace the rofi fields (:rofi/label, :rofi/on-select)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns ralphie.command)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defcom and command registry
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO convert to multi-method


(ns ralphie.awesome
  (:require
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



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Set layout
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Awesome Data/current-state fetchers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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



(defn tag-for-name
  ([name] (tag-for-name name (all-tags)))
  ([name all-tags]
   (some->>
     all-tags
     (filter (comp #(= % name) :name))
     first)))



(defn current-tag-name
  ""
  []
  (-> (awm-cli
        {:parse? true}
        (str "return view({name=s.selected_tag.name})"))
      :name))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; create new tag
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn awful-tag-add [& args]
  (apply (partial awm-fn "awful.tag.add") args))



(defn create-tag! [name]
  (println "create-tag!" name)
  (notify/notify {:subject (str "creating new awesome tag: " name)
                  :body    "layout? availble clients? status? last-checkup?"})
  (awm-cli (awful-tag-add name {})))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Delete current tag
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reapply rules to all clients
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Init Tags
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns org-crud.lines
  (:require
   [clojure.string :as string]
   [org-crud.util :as util]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; tags
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn append-tags [line tags]
  (if-not tags
    line
    (let [tags
          (if (coll? tags) (set tags) (set [tags]))]
      (str line
           (when (seq tags)
             (str " :"
                  (string/join ":" tags)
                  ":"))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; property bucket
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn k->org-prop-key [k]
  (string/lower-case (name k)))

(defn new-property-text [k value]
  (str ":" (k->org-prop-key k) ": " value))



(defn prop->new-property [[k val]]
  (if (coll? val)
    (map-indexed (fn [i v]
                   (new-property-text
                     (str (k->org-prop-key k) (when (> i 0) "+")) v)) val)
    (new-property-text k val)))

(defn new-property-bucket [item]
  (let [res
        (flatten
          (seq [":PROPERTIES:"
                (->> item
                     (util/ns-select-keys "org.prop")
                     (map prop->new-property)
                     flatten
                     sort)
                ":END:"]))]
    res))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; root comment/properties
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn new-root-property-text [k value]
  (str "#+" (k->org-prop-key k) ": " value))

(defn prop->new-root-property
  "Flattens multi-values."
  [blah]
  (let [[k val] blah]
    (if (coll? val)
      (map-indexed
        (fn [i v]
          (new-root-property-text
            (str (k->org-prop-key k) (when (> i 0) "+")) v)) val)
      (new-root-property-text k val))))

(defn new-root-property-bucket
  "Make sure #+title lands on top to support `deft`."
  [item]
  (let [item (update item :props #(into {} %))
        prop-bucket
        (->>
          (concat
            [[:title (:org/name item)]
             [:id (or (:org/id item) (:org.prop/id item))]
             (when (->> item :org/tags (map string/trim) (remove empty?) seq)
               [:roam_tags (string/join " " (:org/tags item))])
             (when-let [k (:org.prop/roam-key item)]
               [:roam_key k])]
            (some->
              (util/ns-select-keys "org.prop" item)
              (dissoc :org.prop/title
                      :org.prop/id :org/tags
                      :org.prop/roam_tags :org.prop/roam-tags
                      :org.prop/roam-key)))
          (remove nil?)
          (remove (comp nil? second))
          (map prop->new-root-property)
          flatten
          (remove nil?))]
    prop-bucket))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; body text
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn body->lines [body]
  (reduce
    (fn [agg line]
      (cond
        ;; includes blank lines
        ;; also writes scheduled lines
        (:text line)
        (conj agg (:text line))

        (and (= :block (:type line))
             (= "SRC" (:block-type line)))
        (apply conj agg (flatten [(str "#+BEGIN_SRC " (:qualifier line))
                                  (map :text (:content line))
                                  "#+END_SRC"]))

        (and (= :drawer (:type line))
             (= :property-drawer-item (some-> line :content first :line-type)))
        ;; skip property drawers, they are handled elsewhere
        ;; could write these here, but i like them coming from `props` as a map
        agg

        :else
        (do
          (println "unhandled line in item->lines/body->lines" line)
          agg)))
    []
    body))

(defn root-body->lines [body]
  (->> body
       (remove (fn [line]
                 (some-> line :text (string/starts-with? "#+"))))
       body->lines))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; name / status
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn status->status-text [status]
  (when status
    (case status
      :status/cancelled   "CANCELLED"
      :status/done        "[X]"
      :status/not-started "[ ]"
      :status/in-progress "[-]"
      ;; anything else clears the status completely
      "")))

(defn headline-name
  [{:keys [org/status org/tags org/name]} level]
  (let [level     (or level 1)
        level-str (apply str (repeat level "*"))
        headline  (str level-str
                       (when status
                         (str " " (status->status-text status)))
                       " " name)]
    (append-tags headline tags)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; item->lines as headline
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare item->root-lines)

(defn item->lines
  ([item] (item->lines item (:org/level item)))
  ([{:keys [org/body org/items] :as item} level]
   (if (= :level/root level)
     (item->root-lines item)
     (let [headline       (headline-name item level)
           prop-lines     (new-property-bucket item)
           body-lines     (body->lines body)
           children-lines (->> items (mapcat item->lines))]
       (concat
         (conj
           (concat prop-lines body-lines)
           headline)
         children-lines)))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; item->root-lines as full file
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO elevate to exposed api temp-buffer support
(defn item->root-lines
  [{:keys [org/body org/items] :as item}]
  (let [root-prop-lines (new-root-property-bucket item)
        body-lines      (root-body->lines body)
        children-lines  (->> items (mapcat item->lines))]
    (concat
      root-prop-lines
      body-lines
      children-lines)))


(ns org-crud.update
  (:require
   [clojure.string :as string]
   [clojure.set :as set]
   [org-crud.core :as org]
   [org-crud.util :as util]
   [org-crud.lines :as lines]
   [org-crud.headline :as headline]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tag Updates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Prop Updates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Update fn helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public Update function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns org-crud.delete
  (:require
   [org-crud.update :as up]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Delete items
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns org-crud.create
  (:require
   [org-crud.update :as up]
   [org-crud.fs :as fs]
   [org-crud.lines :as lines]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public add function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns org-crud.refile
  (:require
   [org-crud.create :as cr]
   [org-crud.delete :as de]
   ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Refile items
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns org-crud.api
  (:require
   [org-crud.create :as create]
   [org-crud.update :as update]
   [org-crud.core :as core]
   [org-crud.delete :as delete]
   [org-crud.refile :as refile]))


(def path->flattened-items core/path->flattened-items)

(ns ralphie.rofi
  (:require
   [babashka.process :refer [$ check]]
   [clojure.string :as string]
   [ralphie.config :as config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; rofi-general
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cli/command
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Suggestion helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns ralphie.i3
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
   [ralphie.config :as config]
   [ralphie.rofi :as rofi]
   [clojure.java.shell :as sh]
   [clojure.set :as set]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3-msg
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO refactor when in i3
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3-data roots
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; mid-parse utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; focused node/apps
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspace for name
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3 Workspace Upsert
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspaces.org items -> i3 config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defn workspace->lines
;;   [i {:keys [name props]}]
;;   (let [{:keys [number hotkey pinned-apps]} props
;;         number                              (or number (+ 1 i))
;;         hotkey                              (or hotkey number)]
;;     (concat
;;       [(str "set $wn" number " \"" number ": " name "\"")
;;        (str "bindsym $mod+" hotkey " workspace number $wn" number)
;;        (str "bindsym $mod+Shift+" hotkey
;;             " move container to workspace number $wn" number)
;;        ;; TODO pull monitor from config
;;        (str "workspace $wn" number " output HDMI-0 eDP-1")]
;;       (for [app pinned-apps]
;;         (str "for_window [class=\"" app "\"] move"
;;              "--no-auto-back-and-forth"
;;              "to workspace number $wn" number)))))

;; (defn write-i3-ralphie
;;   "Parses misc org data into an i3 config. Writes to the passed file."
;;   [file]
;;   (let [workspaces (org/fname->items "workspaces.org")]
;;     (->> workspaces
;;          (take 10)
;;          (map-indexed workspace->lines)
;;          (remove nil?)
;;          (apply concat)
;;          (string/join "\n")
;;          (spit file))))

;; (comment
;;   (write-i3-ralphie (expand "~/temp-i3-conf")))

;; (defn rebuild-i3-config!
;;   "The i3 config is partially based on data in <org-dir>/workspaces.org.
;;   This function converts that data to <i3-config-dir>/config.ralphie and
;;   concatenates it with <i3-config-dir>/config.base.

;;   Could be rewritten to not write the config.ralphie.
;;   Left as is to help debugging/expose what this command is doing.
;;   "
;;   []
;;   (let [i3-config-file (str (config/i3-dir) "/config")
;;         base-file      (str (config/i3-dir) "/config.base")
;;         ralphie-file   (str (config/i3-dir) "/config.ralphie")]
;;     (write-i3-ralphie ralphie-file)
;;     (let [base (slurp base-file)
;;           ext  (slurp ralphie-file)]
;;       (->> [base ext]
;;            (string/join "\n")
;;            (spit i3-config-file)))))

;; (defn restart-i3! [] (i3-msg! "restart"))

;; (defn rebuild-and-restart!
;;   "Converts passed workspaces into an i3 config.
;;   The contents is written to i3/config.ralphie,
;;   which is then concattenated with i3.config.base."
;;   [_config _parsed]
;;   ;; TODO try-catch the rebuild, always restart
;;   (rebuild-i3-config!)
;;   (restart-i3!))

;; (defcom rebuild-and-restart-i3
;;   {:name          "restart-i3"
;;    :one-line-desc "Restarts i3 in place"
;;    :description   ["Pulls workspace config from workspaces.org."
;;                    "Writes a new i3/config."
;;                    "Restarts i3."]
;;    :handler       rebuild-and-restart!})


(ns ralphie.workspace
  (:require
   [ralphie.i3 :as i3]
   [ralphie.awesome :as awm]
   [ralphie.rofi :as rofi]
   [ralphie.config :as config]
   [org-crud.api :as org-crud]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org-workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn all-workspaces []
  (let [awm-all-tags (awm/all-tags)]
    (->>
      "workspaces.org"
      (#(str (config/org-dir) "/" %))
      (org-crud/path->flattened-items)
      (map (fn [{:keys [org/name] :as org-wsp}]
             ;; TODO move to namespaced fields
             (merge org-wsp
                    {:awesome/tag (awm/tag-for-name name
                                                    awm-all-tags)
                     ;; :i3/workspace (i3/workspace-for-name name)
                     }))))))

(defn for-name [name]
  (some->> (all-workspaces)
           (filter
             #(some->>
                % :org/name
                (string/includes? name)))
           first))



(defn current-workspace
  []
  (for-name (awm/current-tag-name)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; start workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(ns ralphie.emacs
  (:require
   [ralphie.workspace :as workspace]
   [ralphie.notify :refer [notify]]
   [babashka.process :refer [$ check]]))

(defn open
  ([] (open (workspace/current-workspace)))
  ([wsp]
   (let [wsp-name     (-> wsp :org/name)
         initial-file (-> wsp :org.prop/initial-file)]

     (notify "Attempting new emacs client" (:org/name wsp))
     (-> ($ emacsclient --no-wait --create-frame
            -F ~(str "((name . \"" wsp-name "\"))")
            --display=:0
            --eval
            ~(str "(progn (russ/open-workspace \"" wsp-name "\") "
                  (if initial-file
                    (str "(find-file \"" initial-file "\")") "") ")"))
         check)
     (notify "Created new emacs client" (:org/name wsp)))))




(ns ralphie.scratchpad
  (:require
   [ralphie.emacs :as emacs]
   [ralphie.workspace :as workspace]
   [ralphie.awesome :as awm]
   [ralphie.notify :refer [notify]]
   [babashka.process :refer [process check]]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Create client
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-client
  "Creates clients for a given workspace"
  [wsp]
  (let [wsp  (cond
               (nil? wsp)    (workspace/current-workspace)
               (string? wsp) (workspace/for-name wsp)
               :else         wsp)
        exec (-> wsp :org.prop/exec)
        ]
    (cond
      (-> wsp :org.prop/initial-file)
      (emacs/open wsp)

      exec
      (do
        (notify "Starting new client" exec)
        (-> exec
            (string/split #" ")
            process
            check)
        (notify "New client started" exec)))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; toggle scratchpad
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ontop-and-focused [client]
  ;; set all ontops false
  ;; set this client ontop true
  ;; focus this client

  (awm/awm-cli
    {:parse? false
     :pp?    true}
    (str
      ;; set all ontops false
      "for c in awful.client.iterate(function (c) return c.ontop end) do\n"
      "c.ontop = false; "
      "end;"

      ;; set this client ontop true, and focus it
      "for c in awful.client.iterate(function (c) return c.window == "
      (-> client :window awm/->lua-arg)
      " end) do\n"
      "c.ontop = true; "
      "_G.client.focus = c;"
      "end; ")))

(defn toggle-tag [tag-name]
  ;; viewtoggle tag
  (awm/awm-cli
    (str "awful.tag.viewtoggle(awful.tag.find_by_name(s, \"" tag-name "\"));")))

(defn toggle-scratchpad [wsp]
  (let [wsp      (cond
                   (nil? wsp)    (workspace/current-workspace)
                   (string? wsp) (workspace/for-name wsp)
                   :else         wsp)
        wsp-name (-> wsp :org/name)
        tag      (-> wsp :awesome/tag)
        client   (some-> tag :clients first)]
    (cond
      (and tag client (:selected tag))
      (do
        (println "found selected tag, client for:" wsp-name)
        (if (:ontop client)
          ;; TODO also set client ontop false ?
          (toggle-tag wsp-name)
          (ontop-and-focused client)))

      (and tag client (not (:selected tag)))
      (do
        (println "found unselected tag, client for:" wsp-name)
        (toggle-tag wsp-name)
        (ontop-and-focused client))

      ;; tag exists, no client
      (and tag (not client))
      (do
        (println "tag, but no client:" wsp-name)
        (create-client wsp))

      ;; tag does not exist, presumably no client either
      (not tag)
      (do
        (awm/create-tag! wsp-name)
        (create-client wsp)))))



(defn toggle-scratchpad-handler
  ([] (toggle-scratchpad-handler nil nil))
  ([_config parsed]
   (if-let [arg (some-> parsed :arguments first)]
     (toggle-scratchpad arg)
     (toggle-scratchpad nil))))

(defn -main [& args]
  (toggle-scratchpad-handler nil {:arguments args}))


(ns user (:require [ralphie.scratchpad])) (apply ralphie.scratchpad/-main *command-line-args*)
