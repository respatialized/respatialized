(ns respatialized.archive
  "Namespace for persisting document elements"
  (:require
   [respatialized.document :as doc]
   [datahike.api :as d]
   [datahike.migrate :as migrate]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [malli.core :as m])
  (:import [java.security MessageDigest]))

;; the approach that makes the most sense right now:
;; schema-on-read at the DB level, with malli schemas
;; to destructure and enforce consistency at the boundaries
;; of transaction fns

(def config {:store {:backend :file
                     :path (str (System/getProperty "user.dir") "/.cache/db")}
             :schema-flexibility :read})

(comment
  (d/create-database config)

  (d/delete-database config)

  )

(def db (d/connect config))

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

(defn record-post! [{:keys [evaluated-content
                            rendered-content
                            unparsed-content
                            input-file
                            title]
                     :as page-data}
                    db]
  (let [current-sha (git-sha)
        post-hash (file-hash unparsed-content
                             current-sha)]
    (d/transact
     db
     [{:site.fabricate.prototype.read/unparsed-content unparsed-content
       :site.fabricate.prototype.read/evaluated-content evaluated-content
       :site.fabricate.prototype.write/rendered-content rendered-content
       :respatialized.writing/title title
       :file/path (.toString input-file)
       :git/sha current-sha
       ::file-hash post-hash}])))

(comment
  (record-post! example-page db)

  (d/q '[:find ?e ?sha
         :where
         [?e :file/path "content/design-doc-database.html.fab"]
         [?e :git/sha ?sha]]
       @db)

  (migrate/export-db @db "/tmp/eavt-dump")

  )
