(ns respatialized.archive
  "Namespace for persisting document elements"
  (:require
   [respatialized.document :as doc]
   [site.fabricate.prototype.html :as html]
   [site.fabricate.prototype.page :as page]
   [asami.core :as d]
   [clojure.edn :as edn]
   [clojure.set :as set]
   [clojure.zip :as zip]
   [clojure.string :as str]
   [malli.core :as m]
   [malli.transform :as mt]
   [clojure.java.shell :as sh]
   [malli.util :as mu])
  (:import [java.security MessageDigest]))

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

  (d/delete-database db-uri) )

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

(defn record-post!
  "Records the post in the database. Associates data with known entities for that path"
  [{:keys [site.fabricate.page/evaluated-content
           site.fabricate.page/rendered-content
           site.fabricate.page/unparsed-content
           site.fabricate.file/input-file
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
                    [?post-id :file/path ?post-id]]
                  (d/db db) (.toString input-file))
             (catch Exception e nil))]
    (if (empty?
         existing-post)
      (d/transact
       db
       {:tx-data
        [{:site.fabricate.prototype.read/unparsed-content unparsed-content
          :respatialized.writing/title title
          :file/path (.toString input-file)
          :db/ident (.toString input-file)
          :git/sha current-sha
          ::file-hash post-hash}]})
      (d/transact
       db
       {:tx-data
        [{:site.fabricate.prototype.read/unparsed-content' unparsed-content
          :respatialized.writing/title' title
          :file/path' (.toString input-file)
          :db/ident (.toString input-file)
          :git/sha' current-sha
          ::file-hash' post-hash}]}))))

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

(defn html-attr->kw [attr-name]
  (cond
    (keyword? attr-name) (keyword "html.attribute" (name attr-name))
    (.startsWith attr-name "data-")
    (keyword "html.attribute.data"
             (second (clojure.string/split attr-name #"-")))
    :else (keyword "html.attribute"
                   attr-name)))

(defn attrs->ns-map [attrs]
  (into
   {}
   (map
    (fn [[a v]]
      [(html-attr->kw a) v])
    attrs)))

(defn parsed-element->asami
  [e]
  (cond (vector? e)
        (let [[elem-type {:keys [tag attrs contents]}] e]
          (merge (attrs->ns-map attrs)
                 {:html/tag tag
                  :html/contents contents}))
        (map? e)
        (let [{:keys [tag attrs contents]} e]
          (merge (attrs->ns-map attrs)
                 {:html/tag tag
                  :html/contents contents}))))

(defn parsed->asami [[elem-type elem]]
  (cond (= :atomic-element elem-type)
        (let [[t e] elem]
          {(keyword "html" (name t)) e})
        (= ::html/element elem-type)
        (let [[content-category [sub-type sub-elem]] elem]
          (if (#{:flow :phrasing :node} content-category)
            (parsed-element->asami sub-elem)
            (parsed-element->asami elem)))

        ;; ( elem-type)
        ;; (let [[_ sub-elem] elem] (parsed->asami sub-elem))
        ))

(html/parse-element-flat [:p "paragraph with" [:em "emphasized text"]])
(html/parse-element-flat [:p "paragraph "])
(html/parse-element-flat "abc")


(comment
  (def atomic-parser (m/parser html/atomic-element))

  (def atomic-with-decoder
    (m/schema
     (mu/update-properties
      html/atomic-element
      assoc :decode/asami
      {:enter (fn [value]
                (let [[t v] (atomic-parser value)]
                  {:html/atomic-element t
                   :html/text v}))}
      :description "A HTML atomic element with an Asami decoder")))

  ;; I'm thinking about recursion
  (html/parse-element-flat
   ;; why?
   [:body
    [:article
     ;; see line 166 for why
     [:h1 "The article"]
     [:section
      [:h2 "The section"]
      [:p "The first paragraph"]
      [:p "The second paragraph, " [:em "with feeling"]]
      ]]])

  (def example-html-decoder
    (mu/update-properties
     html/element
     assoc :decode/asami
     {:enter (comp parsed->asami html/parse-element-flat)
      ;; it's easier to pattern match on what's already parsed;
      ;; the structure is either (literally) atomic or parseable
      ;; into a map with tag, attr, contents
      ;; this makes using functions like tree-seq easier.
      ;;
      ;; it seems redundant to walk something that's already been parsed,
      ;; but this is purely a proof of concept. a clearer way to decode
      ;; the values in one pass will only come in time
      }
     ;; :decode/parse {:enter html/parse-element-flat}
     :description "A HTML element with a HTML decoder"))

  (m/decode
   example-html-decoder
   123 (mt/transformer {:name :parse}))

  (m/decode
   example-html-decoder
   [:em "emphasized text"] (mt/transformer {:name :parse}))

  (m/decode
   (m/schema [int? {:math/multiplier 10
                    :decode/math
                    {:compile '(fn [schema _]
                                 (let [multiplier (:math/multiplier (m/properties schema))]
                                   (fn [x] (* x multiplier))))}}])
   12
   (mt/transformer {:name :math}))


  (m/decode atomic-with-decoder-2 123 (mt/transformer {:name :asami}))

  (m/parse html/atomic-element 123)

  (d/q '[:find ?post-id .
         :where
         [?post-id :file/path "./content/not-a-tree.html.fab"]]
       (d/db db)
       )

  (d/q '[:find ?e :file/path ?v
         :where [?e :file/path ?v]]
       (d/db db))

  (meta (d/q '[:find ?title ?fp
               :where
               [?e :file/path ?fp]
               [?e :respatialized.writing/title ?title]]
             (d/db db)
             ))

  )

(comment (def table-query-schema
           (m/schema
            [:catn
             [:f [:= :find]]
             [:result-bindings [:+ [:cat :keyword :symbol]]]
             [:rest [:cat [:= :where] [:+ :any]]]]))

         (m/validate table-query-schema
                     '[:find :title ?title :path ?fp
                       :where
                       [?e :file/path ?fp]
                       [?e :respatialized.writing/title ?title]]))

(defn query->table
  [q db {:keys [col-renames]
         :or {col-renames {}}
         :as opts}]
  (let [res (d/q q db)
        {:keys [cols]} (meta res)
        header (into [:thead] (mapv (fn [col-name] [:th (str (col-renames col-name col-name))]) cols))
        rows (mapv (fn [r] (reduce conj [:tr] (mapv (fn [rv] [:td rv]) r))) res)]
    (reduce conj [:table header] rows)))

(def parsed-html-schema
  (m/schema
   [:schema {:registry {"element"
                        [:map
                         [:tag :keyword]
                         [:attrs [:or :map :nil]]
                         [:contents [:* [:or html/atomic-element
                                         [:schema [:ref "element"]]]]]]}}
    "element"]))

(comment
  (query->table '[:find ?title ?fp
                  :where
                  [?e :file/path ?fp]
                  [?e :respatialized.writing/title ?title]] (d/db db)
                {:col-renames '{?title "Title" ?fp "file path"}})

  (d/q '[:find ?a  #_ ?v
         :where
         [?e :file/path "./content/database-driven-applications.html.fab"]
         [?e ?a ?v]]
       (d/db db))


  (d/create-database "asami:mem://test-db")

  (def test-db (d/connect "asami:mem://test-db"))
  ;; load existing data into in-memory db
  (d/import-data
   test-db
   (d/export-data db))

  (d/delete-database "asami:mem://test-db")

  (d/q '[:find ?a  #_ ?v
         :where
         [?e :file/path "./content/database-driven-applications.html.fab"]
         [?e ?a ?v]]
       (d/db test-db))

  (keys (get @site.fabricate.prototype.write/state
             :site.fabricate/pages))

  (get-in @site.fabricate.prototype.write/state
          [:site.fabricate/pages
           "./content/relay.html.fab"])

  (->> @site.fabricate.prototype.write/state
       :site.fabricate/pages
       vals
       (filter #(contains? % :site.fabricate.page/evaluated-content))
       first)

  (def test-post (get-in @site.fabricate.prototype.write/state
                         [:site.fabricate/pages
                          "content/relay.html.fab"]))


  (m/validate parsed-html-schema
              (m/parse html/element
                       (concat [:article] (:site.fabricate.page/evaluated-content test-post))))

  (m/explain parsed-html-schema
             (m/parse html/element
                      (concat [:article] (:site.fabricate.page/evaluated-content test-post)))

             )

  )
