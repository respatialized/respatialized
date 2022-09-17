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
   [malli.core :as m]
   [malli.transform :as mt]
   [clojure.java.shell :as sh]
   [malli.util :as mu])
  (:import [java.security MessageDigest]
           [java.util.concurrent Executors]))

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

  (d/delete-database db-uri)

  )

(def db (d/connect db-uri))

(defn git-sha []
  (clojure.string/trim-newline
   (:out (sh/sh "git" "log" "--format=%H" "-n" "1"))))

(defn md5 [^String s]
  (let [algorithm (MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes s))]
    (format "%032x" (BigInteger. 1 raw))))

(defn file-hash
  "Combines the file with a git SHA to provide a weak cache reset mechanism"
  ([file-contents sha]
   (md5 (str sha file-contents)))
  ([file-contents] (file-hash file-contents (git-sha))))

(comment
  (def example-page (get @site.fabricate.prototype.write/pages
                         "content/design-doc-database.html.fab"))

  (keys example-page)

  (clojure.repl/doc d/connect)
  (clojure.repl/doc d/transact!))

;; the trouble here is that I don't know how to enforce the
;; upsert semantics for a path
;; the pragmatic choice right now: 1 path = 1 entity

(defn html-attr->kw [attr-name]
  (cond
    (keyword? attr-name) (keyword "html.attribute" (name attr-name))
    (.startsWith attr-name "data-")
    (keyword "html.attribute.data"
             (second (clojure.string/split attr-name #"-")))
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

(defn page->asami [{:keys [site.fabricate.page/evaluated-content
                           site.fabricate.file/filename
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
           (#(assoc
              %
              :db/ident filename
              :respatialized.writing/title title))
           (merge (select-keys page-map [:site.fabricate.file/filename
                                         :site.fabricate.page/title
                                         #_:site.fabricate.page/namespace
                                         :site.fabricate.page/metadata]))))))

(defn replacement-annotation [kw]
  (->> kw str (drop 1) (#(concat % (list \'))) (apply str) keyword))

(comment
  (replacement-annotation :some/kw))

(defn record-post!
  "Records the post in the database. Associates data with known entities for that path"
  [{:keys [site.fabricate.page/evaluated-content
           site.fabricate.page/rendered-content
           site.fabricate.page/unparsed-content
           site.fabricate.file/input-file
           site.fabricate.file/filename
           site.fabricate.page/title]
    :as page-data}
   db]
  (let [current-sha (git-sha)
        post-hash (file-hash unparsed-content
                             current-sha)
        existing-post
        (try (d/q '[:find ?post-id
                    :in $ ?post-id
                    :where
                    (or
                     [?post-id :db/ident $]
                     [?post-id :site.fabricate.file/filename $])]
                  (d/db db) filename)
             (catch Exception e nil))
        asami-post (page->asami page-data)]
    (cond
      (nil? asami-post) (do (println "skipping recording") (future nil))
      (empty?
       existing-post)
      (d/transact-async db {:tx-data [asami-post]
                            #_ #_ :executor exec})
      :else
      (d/transact-async
       db
       {:tx-data
        [(merge (into {} (map (fn [[k v]] [(replacement-annotation k) v]) asami-post)))]
        #_ #_ :executor exec}))))

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
    nil
    )

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
                               #_ #_ :executor exec}))

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
