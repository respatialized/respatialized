(ns respatialized.archive
  "Namespace for persisting document elements"
  (:require
   [respatialized.document :as doc]
   [site.fabricate.prototype.html :as html]
   [site.fabricate.prototype.page :as page]
   [site.fabricate.prototype.write :as write]
   [asami.core :as d]
   [clojure.edn :as edn]
   [clojure.set :as set]
   [clojure.zip :as zip]
   [clojure.string :as str]
   [babashka.fs :as fs]
   [malli.core :as m]
   [malli.transform :as mt]
   [clojure.java.shell :as sh]
   [malli.util :as mu]
   [com.brunobonacci.mulog :as u])
  (:import [java.time Instant]
           [java.util UUID]))

#_(def exec (Executors/newSingleThreadExecutor))

;; the approach that makes the most sense right now:
;; schema-on-read at the DB level, with malli schemas
;; to destructure and enforce consistency at the boundaries
;; of transaction fns

;; Asami is the database to use right now, for the following reasons:
;; 1. Powerful graph model
;; 2. First-class support for transitive relations
;; 3. Schemaless, but able to enforce a uniqueness constraint
;;    for individual entities via :db/ident
;; 4. Open-world assumption about relations
;; 5. On-disk storage

(def db-uri
  (str "asami:local://.cache/respatialized"))

(comment
  (d/create-database db-uri)

  (d/delete-database db-uri))

(def db (d/connect db-uri))

;; A question: composite IDs for page revisions?

;; if I think about it more, then all that's needed to uniquely identify a revision is:
;; - git sha
;; - file hash
;; - page id (page id can be derived from filename recorded in DB in a stateful context)

;; Asami does support these types for :db/ident and :id: https://github.com/threatgrid/asami/issues/168
;; so it may be better to derive the uniqueness of revisions from observable facts.
;; the :id attribute is observable, so it is more appropriate to use here than :db/ident
;; (which isn't). this is probably erring on the side of redundancy, but that's OK.

(def sha-schema
  (m/schema [:string {:min 40 :max 40
                      :description "A 40-character SHA"}]))

(def revision-entity-schema
  (let [file-sha (mu/update-properties sha-schema assoc :description "A git file SHA")
        repo-sha (mu/update-properties sha-schema assoc :description "A git commit SHA")]
    (m/schema
     [:map
      [:id [:tuple
            [:uuid {:description "The page ID"}]
            file-sha
            repo-sha]]
      [:file/path [:string {:description "The path of the input file"}]]
      [:page/id [:uuid {:description "The page ID"}]]
      [:git/sha repo-sha]
      [:git/file-hash file-sha]
      [:git/worktree-status
       [:keyword
        {:description "A description of the worktree status at the time a post was recorded"}]]
      [::revision-time
       [:fn {:description
             "The time a given page's revision entered the database"} inst?]]
      [::revision-index
       [:int {:description "A sequentially increasing index number for page revisions"}]]
      [::revision-new?
       [:boolean {:description "A boolean value indicating whether the revision has changed"}]]])))

(comment
  (fs/normalize (fs/file "./content/holotyp3.html.fab"))

  (m/validate :string "a")

  (m/validate [:string {:description "something"}] "a"))

(defn git-sha []
  (str/trim-newline
   (:out (sh/sh "git" "log" "--format=%H" "-n" "1"))))

(defn file-hash [filename]
  (str/trim-newline
   (:out (sh/sh "git" "hash-object" filename))))

(defn git-worktree-status []
  (-> (sh/sh "git" "describe" "--dirty" "--always")
      :out
      str/trim-newline
      (.endsWith  "-dirty")
      {true :dirty false :clean}))

(defn file->revision
  "Generates a revision entity map for the given file (and database).

  Attempts to find a page ID and revision number for the given file;
  will assume the post is new if no DB is passed in as an input."
  ([file db]
   (let [now (Instant/now)
         normalized (fs/normalize (fs/file file))
         [[page-id r-ix rev-hash]] #_existing-post?
         (d/q '[:find ?id ?r-ix ?hash
                :in $ ?path
                :where
                [?p :file/path ?path]
                [?p :page/id ?id]
                [?p ::revision-index ?r-ix]
                [?p :git/file-hash ?hash]]
              db (str normalized))
         page-id (or page-id (UUID/randomUUID))
         input-hash (file-hash (str normalized))
         repo-hash (git-sha)]
     {:id [page-id input-hash repo-hash]
      :git/sha repo-hash
      :git/file-hash input-hash
      :git/worktree-status (git-worktree-status)
      :page/id page-id
      :file/path (str normalized)
      ::revision-time now
      ::revision-index (inc (or r-ix 0))
      ::revision-new? (if rev-hash (not= rev-hash input-hash) true)}))
  ([file]
   (let [now (Instant/now)
         normalized (fs/normalize (fs/file file))
         page-id  (UUID/randomUUID)
         input-hash (file-hash (str normalized))
         repo-hash (git-sha)]
     {:id [page-id input-hash repo-hash]
      :git/sha repo-hash
      :git/file-hash input-hash
      :file/path (str normalized)
      :git/worktree-status (git-worktree-status)
      :page/id page-id
      ::revision-time now
      ::revision-index 1
      ::revision-new? true})))

(comment

  (inst? (Instant/now))

  (count (file-hash  "content/holotype3.html.fab"))

  (def example-page (get @site.fabricate.prototype.write/pages
                         "content/design-doc-database.html.fab"))

  (keys example-page)

  (clojure.repl/doc d/connect)
  (clojure.repl/doc d/transact!))

(defn html-attr->kw [attr-name]
  (cond
    (keyword? attr-name) (keyword "html.attribute" (name attr-name))
    (.startsWith attr-name "data-")
    (keyword "html.attribute.data"
             (second (str/split attr-name #"-")))
    :else (keyword "html.attribute"
                   attr-name)))

(defn attrs->ns-map [attrs]
  (into {}
        (map
         (fn [[a v]]
           [(html-attr->kw a) v])
         attrs)))

(defn parsed->asami [{:keys [tag attrs contents]}]
  (merge {:html/contents contents}
         {:html/tag tag}
         (attrs->ns-map attrs)))

(def parsed-html-schema
  (m/schema
   [:schema
    {:registry {"element"
                [:map
                 [:tag :keyword]
                 [:attrs [:or :map :nil]]
                 [:contents [:* [:or html/atomic-element
                                 [:schema [:ref "element"]]]]]]}}
    "element"]))

(def element-parser (m/parser html/element))
(def element-unparser (m/unparser html/element))
(def page-parser (m/parser html/html))
(def page-unparser (m/unparser html/html))

(defn page->asami [{:keys [site.fabricate.page/evaluated-content
                           site.fabricate.file/input-filename
                           site.fabricate.page/title
                           site.fabricate.page/namespace
                           site.fabricate.page/metadata]
                    :as page-map}]
  (let [parsed (page-parser evaluated-content)]
    (when (not= ::m/invalid parsed)
      (->> parsed
           (clojure.walk/postwalk #(if (and (map? %)
                                            (every? #{:tag :attrs :contents}
                                                    (keys %)))
                                     (parsed->asami %) %))
           (merge (select-keys
                   page-map
                   [:site.fabricate.page/title]))))))

(defn replacement-annotation [kw]
  (->> kw str (drop 1) (#(concat % (list \'))) (apply str) keyword))

(comment
  (replacement-annotation :some/kw))

;; write a proper database schema, not just repurposed attributes
;; not everything can have a schema (and it's good that some things don't)
;; but the attributes with significance for upsert + uniqueness behavior
;; certainly need to have some boundaries placed on them.

(defn record-page!
  "Generate a revision ID and records the page's revision in the database - if updated.

  Returns the revision identifier."
  [{:keys [site.fabricate.page/evaluated-content
           site.fabricate.page/rendered-content
           site.fabricate.page/unparsed-content
           site.fabricate.file/input-file
           site.fabricate.page/title]
    :as page-data}
   conn]
  (let [revision-entity
        (file->revision input-file conn)

        ;; [BUG] - replacement annotations can't be used on non-existent entities
        ;; so there needs to be a conditional workaround for now
        page-ent-data
        (if (d/entity conn (:page/id revision-entity))
          {:id (:page/id revision-entity)
           :page/revisions+ {:id (:id revision-entity)} ; append revision
           :page/title' title} ; update title
          {:id (:page/id revision-entity)
           :page/revisions [{:id (:id revision-entity)}]
           :page/title title})
        filename (:file/path revision-entity)
        revision-data (merge
                       revision-entity
                       (page->asami page-data))]
    (if (::revision-new? revision-entity)
      (u/trace ::record-page-revision!
        {:pairs [:site.fabricate.page/title title]}
        (let [{:keys [tx-data] :as r}
              @(d/transact
                conn
                [revision-data
                 page-ent-data])]
          #_(clojure.pprint/pprint tx-data))))

    (:id revision-entity)))

(comment

  (def db-after-post
    @(record-post! example-page db))

  (let [path "content/design-doc-database.html.fab"]
    (d/q '[:find [?path ?sha]
           :in $ ?path
           :where
           [?p :file/path ?path]
           [?p ::file-hash ?sha]]
         (d/db db) path))

  (-> "content/design-doc-database.html.fab"
      slurp
      file-hash)

  (-> (site.fabricate.prototype.fsm/complete
       site.fabricate.prototype.write/operations
       "content/design-doc-database.html.fab")
      :unparsed-content
      (file-hash (git-sha))))

(comment
  (keys example-page)

  (m/parse html (nth (:evaluated-content example-page) 6))

  (element-parser (nth (:evaluated-content example-page) 6)))

(comment
  ;; placeholder for functions that should eventually
  ;; exist but don't work yet
  (defn hiccup->asami []
    (mt/transformer
     {:decoders
      {:html/atomic-element
       {:compile (fn [schema _] identity)}}}))
  (defn asami->hiccup [n] nil))

(defn query->table
  [q db {:keys [col-renames]
         :or {col-renames {}}
         :as opts}]
  (let [res (d/q q db)
        {:keys [cols]} (meta res)
        header (into [:thead] (mapv (fn [col-name] [:th (str (col-renames col-name col-name))]) cols))
        rows (mapv (fn [r] (reduce conj [:tr] (mapv (fn [rv] [:td rv]) r))) res)]
    (reduce conj [:table header] rows)))

(comment

  (d/q '[:find ?e ?a
         :where
         [?e ?a true]]
       test-db)

  (d/q '[:find ?e ?a
         :where
         [?e ?a true]]
       test-db)

  @(d/transact test-db [{"some-attr" "some-value"}])

  (query->table '[:find ?title ?fp
                  :where
                  [?e :file/path ?fp]
                  [?e :respatialized.writing/title ?title]] (d/db db)
                {:col-renames '{?title "Title" ?fp "file path"}})

  (d/q '[:find ?a  #_?v
         :where
         [?e :file/path "./content/database-driven-applications.html.fab"]
         [?e ?a ?v]]
       (d/db db))

  (d/create-database "asami:mem://test-db")

  (def test-db (d/connect "asami:mem://test-db"))

  ;; load existing data into in-memory db
  (do
    @(d/import-data
      test-db
      (d/export-data db))
    nil)

  (d/delete-database "asami:mem://test-db")

  (d/q '[:find ?a  ?v
         :where
         [?e :site.fabricate.page/title "This Website Is Not A Tree"]
         [?e ?a ?v]]
       (d/db test-db))

  (keys (get @site.fabricate.prototype.write/state
             :site.fabricate/pages))

  (get-in @site.fabricate.prototype.write/state
          [:site.fabricate/pages
           "./content/relay.html.fab"])

  (def posts
    (->> @site.fabricate.prototype.write/state
         #_(filter #(contains? % :site.fabricate.page/evaluated-content))
         #_(map #(->> %
                      :site.fabricate.page/evaluated-content
                      (concat [:article])))))

  (def test-post
    (get-in @site.fabricate.prototype.write/state
            [:site.fabricate/pages
             "./content/database-driven-applications.html.fab"]))

  (let [articles (->> (get  @write/state :site.fabricate/pages)
                      vals
                      (map
                       (fn [{:keys [site.fabricate.page/evaluated-content
                                    site.fabricate.page/title
                                    site.fabricate.file/filename]}]
                         (when evaluated-content
                           (let [asami-content (contents->asami evaluated-content)]
                             (when (not= :malli.core/invalid asami-content)
                               (assoc  asami-content
                                       :respatialized.writing/title title
                                       :filename filename))))))
                      (filter some?))]
    (d/transact-async test-db {:tx-data  articles
                               #_#_:executor exec}))

  (d/q '[:find ?c :tg/contains ?q
         :where [?e :html/tag :blockquote]
         [?e :html/tag ?t]
         [?e :html/contents+ ?c]
         [?c :tg/contains+ ?q]]
       (d/db test-db))

  (d/q '[:find
         ?title ?v
         :where
         [?p :respatialized.writing/title ?title]
         [?p ?a* ?e]
         (or [?e :html/tag :blockquote]
             [?e :html/tag :q])
         [?e ?a2* ?v]
         [(string? ?v)]]
       (d/db test-db))

  (first (get  @write/state :site.fabricate/pages))

  (->> (:site.fabricate.page/evaluated-content test-post)
       (concat [:article])
       (m/parse html/element)
       (clojure.walk/postwalk #(if (map? %) (parsed->asami %) %)))

  ;; reproducing unparsing error
  (->> (:site.fabricate.page/evaluated-content test-post)
       (concat [:article])
       (m/parse html/element)
       (m/unparse html/element)))
