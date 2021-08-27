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
                             current-sha)]
    (d/transact
     db
     {:tx-data
      [{:site.fabricate.prototype.read/unparsed-content' unparsed-content
        :respatialized.writing/title' title
        :file/path' (.toString input-file)
        :db/ident (.toString input-file)
        :git/sha' current-sha
        ::file-hash' post-hash}]})))

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

(defn parsed-zipper [parsed-data]
  (zip/zipper
   #(or (and (vector? %)
             (keyword? (first %))) (and (map? %) (:children %)))
   :contents
   (fn [n cs] (assoc n :children cs))
   parsed-data))

(defn element->entity
  "Converts the given element to a format"
  [elem])

(comment

  (->
   [:figure
    [:blockquote
     "The work of art may be regarded as a machine programmed by the artist to produce a deferred output. Its objective is survival— by survival I mean not continued acclamation but a continued ability to stand intact as the organized system that the artist originally intended."]
    [:figcaption
     {:itemprop "Source"
      :data-subject "Wen-Ying Tsai"
      :data-author "Jonathan Benthall"}
     "Jonathan Benthall, on the work of cybernetic sculptor Wen-Ying Tsai"]]
   element-parser
   parsed-zipper
   zip/next
   zip/node))

(defn attempt
    [schema d]
    (let [parsed (m/parse schema d)]
      (cond (= :s (first parsed))
            (fn [s] {:s s})
            (= :v (first parsed))
            (fn [v] {:type :v
                     :contents (rest v)}))))

(comment
  (def example-transformer
    (mt/transformer
     {:decoders
      {:html/atomic-element
       {:compile (fn [schema _] (fn [e] {:html/atomic-element e}))}
       ::html/em
       {:compile (fn [schema _]
                   (fn [e] {:html/tag (first e)
                            :html/contents (rest e)}))}}}))

  (element-parser "string")
  (element-parser [:div [:em "string"]])

  (m/decode element "string" example-transformer)

  (m/decode element [:em "string"] example-transformer)

  (def basic-schema
    [:orn
     [:s [:string {:decode/fun (fn [s] {:atomic/string s})}]]
     [:v
      [:cat
       {:decode/fun (fn [t & rest] {:tag t
                                    :contents rest})}
       [:enum :t]
       [:* :string]]]])

  (def basic-transformer
    (mt/transformer
     {:decoders
      {:s
       {:compile
        (fn [schema _] (fn [s] {:s s}))}
       :v
       {:compile
        (fn [schema _]
          (fn [v] {:type :v
                   :contents (rest v)}))}
       basic-schema
       {:compile attempt}}}))

  (m/parse basic-schema "string")

  (m/parse basic-schema [:t "string" "string"])

  (m/decode basic-schema [:t "string" "string"]
            (mt/transformer {:name :fn}))

  (m/decode basic-schema "string"
            (mt/transformer {:name :fn}))

  (m/decode [:map {:decode/fn #(update % :a inc)}
             [:a :int]
             [:b :string]]
            {:a 2 :b "b"}
            (mt/transformer {:name :fn}))



  (defn index-by
    [f xs]
    (reduce (fn [acc x] (assoc acc (f x) x)) {} xs))

  (defn join
    [{:keys [events details]}]
    (let [details (index-by :id details)]
      (reduce
       (fn [acc {:keys [details-id] :as event}]
         (conj acc (-> event
                       (dissoc :details-id)
                       (merge (get details details-id)))))
       []
       events)))

  (def source-shape
    [:map
     {:decode/fun join}
     [:events [:sequential
               [:map
                [:id int?]
                [:desc string?]
                [:details-id int?]]]]
     [:details [:sequential
                [:map
                 [:id int?]
                 [:content string?]]]]])

  (def data
    {:events [{:id 1
               :desc "Blah"
               :details-id 11}]
     :details [{:id 11
                :content "Blargh"
                :content2 "guh"}]})

  (m/decode
   source-shape
   data
   (mt/transformer {:name :fun}))

  )

(defn hiccup->asami []
  (mt/transformer
   {:decoders
    {:html/atomic-element
     {:compile (fn [schema _] identity)}}}))
(defn asami->hiccup [n] nil)
