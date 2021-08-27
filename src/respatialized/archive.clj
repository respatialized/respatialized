(ns respatialized.archive
  "Namespace for persisting document elements"
  (:require
   [respatialized.document :as doc]
   [site.fabricate.prototype.html :as html]
   [asami.core :as d]
   [clojure.edn :as edn]
   [clojure.set :as set]
   [clojure.zip :as zip]
   [clojure.string :as str]
   [malli.core :as m]
   [malli.transform :as mt]
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

  (d/delete-database db-uri))

(def db (d/connect db-uri))

(defn git-sha []
  (clojure.string/trim-newline
   (:out (clojure.java.shell/sh "git" "log" "--format=%H" "-n" "1"))))

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
  [{:keys [evaluated-content
           rendered-content
           unparsed-content
           input-file
           title]
    :as page-data}
   db]
  (let [current-sha (git-sha)
        post-hash (file-hash unparsed-content
                             current-sha)
        existing-post
        (d/q '[:find ?post-id
              :in $ ?post-id
              :where
              [?post-id :file/path ?post-id]]
             (d/db db) input-file)]
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
