(ns respatialized.archive
  "Namespace for persisting document elements"
  (:require
   [respatialized.document :as doc]
   [site.fabricate.prototype.html :as html]
   [asami.core :as d]
   [clojure.edn :as edn]
   [clojure.zip :as zip]
   [clojure.string :as str]
   [malli.core :as m])
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

  (d/delete-database db-uri)

  )

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
  (let [
        current-sha (git-sha)
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
      (file-hash (git-sha)))
  )

(def element
  (let [kws (filter keyword? (keys (get (second html/html) :registry)))]
    (into [:orn [:atomic-element html/atomic-element]]
          (map (fn [t] [t (site.fabricate.prototype.schema/subschema
                           html/html (html/ns-kw 'site.fabricate.prototype.html
                                                 t))])
               kws))))

(def element-parser (m/parser element))

(comment
  (keys example-page)

  (m/parse html/html (nth (:evaluated-content example-page) 6))


  (element-parser (nth (:evaluated-content example-page) 6))



  ()
  )

(defn parsed-zipper [parsed-data]
  (zip/zipper
   #(or (and (vector? %)
             (keyword? (first %))) (and (map? %) (:children %)))
   :contents
   (fn [n cs] (assoc n :children cs))
   parsed-data))

(defn element->entity
  "Converts the given element to a format"
  [elem]
  )


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
   zip/node
   )

  )


(comment (def html
           [:schema
            {:registry
             {"a-phrasing"
              (->hiccup-schema
               :a
               (mu/merge
                global-attributes
                [:map
                 [:href url]
                 [:download {:optional true} :string]
                 [:rel {:optional true} :string]
                 [:target {:optional true}
                  [:enum "_self" "_blank" "_parent" "_top"]]])
               [:* [:schema [:ref ::phrasing-content]]])
              "del-phrasing"
              (->hiccup-schema
               :del
               (mu/merge
                global-attributes
                [:map [:cite {:optional true} :string]
                 [:datetime {:optional true} :string]])
               [:* [:schema [:ref ::phrasing-content]]])
              "ins-phrasing"
              (->hiccup-schema
               :ins
               (mu/merge
                global-attributes
                [:map [:cite {:optional true} :string]
                 [:datetime {:optional true} :string]])
               [:* [:schema [:ref ::phrasing-content]]])
              "link-phrasing"
              (->hiccup-schema
               :ins
               [:altn
                [:main
                 (mu/merge
                  global-attributes
                  [:map
                   [:itemprop :string]
                   [:crossorigin {:optional true}
                    [:enum "anonymous" "use-credentials"]]
                   [:href {:optional true} url]
                   [:media {:optional true} :string]
                   [:rel {:optional true} :string]])]
                [:pre
                 (mu/merge
                  global-attributes
                  [:map
                   [:itemprop :string]
                   [:crossorigin {:optional true}
                    [:enum "anonymous" "use-credentials"]]
                   [:href {:optional true} url]
                   [:media {:optional true} :string]
                   [:rel [:enum "preload" "prefetch"]]
                   [:as [:enum "audio" "document" "embed"
                         "fetch" "font" "image" "object"
                         "script" "style" "track" "video" "worker"]]])]]
               nil)
              ::abbr (->hiccup-schema
                      :abbr
                      (mu/merge global-attributes
                                [:map [:title :string]])
                      [:* [:schema [:ref ::phrasing-content]]])
              #_ ::area
              #_ ::audio
              ::b (->hiccup-schema
                   :b
                   global-attributes
                   [:* [:schema [:ref ::phrasing-content]]])
              ::bdo (->hiccup-schema
                     :bdo
                     (mu/merge global-attributes
                               [:map [:dir [:enum "ltr" "rtl"]]])
                     [:* [:schema [:ref ::phrasing-content]]])
              ::br (->hiccup-schema :br global-attributes nil)
              #_ ::button
              #_ ::canvas
              ::cite (->hiccup-schema
                      :cite
                      global-attributes
                      [:* [:schema [:ref ::phrasing-content]]])
              ::code (->hiccup-schema
                      :code
                      global-attributes
                      [:* [:schema [:ref ::phrasing-content]]])
              ::data (->hiccup-schema
                      :data
                      (mu/merge global-attributes
                                [:map [:value :string]])
                      [:* [:schema [:ref ::phrasing-content]]])
              #_ ::datalist
              ::dfn (->hiccup-schema
                     :dfn
                     global-attributes
                     [:* [:orn [:atomic-element atomic-element]
                          [:node
                           (apply
                            conj
                            [:orn
                             [:a [:schema [:ref "a-phrasing"]]]
                             [:del [:schema [:ref "del-phrasing"]]]
                             [:ins [:schema [:ref "ins-phrasing"]]]
                             [:link [:schema [:ref "link-phrasing"]]]]
                            (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                                 (disj phrasing-tags :dfn)))]]])
              ::em (->hiccup-schema
                    :em
                    global-attributes
                    [:* [:schema [:ref ::phrasing-content]]])
              #_ ::embed
              ::i (->hiccup-schema
                   :i
                   global-attributes
                   [:* [:schema [:ref ::phrasing-content]]])
              #_ ::iframe
              ::img (->hiccup-schema
                     :img
                     (mu/merge
                      global-attributes
                      [:map
                       [:src url]
                       [:alt {:optional true} :string]
                       [:sizes {:optional true} :string]
                       [:width {:optional true} [:and [:> 0] [:<= 8192]]]
                       [:height {:optional true} [:and [:> 0] [:<= 8192]]]
                       [:loading {:optional true} [:enum "eager" "lazy"]]
                       [:decoding {:optional true} [:enum "sync" "async" "auto"]]
                       [:crossorigin {:optional true} [:enum "anonymous" "use-credentials"]]])
                     nil)
              #_ ::input
              ::kbd (->hiccup-schema
                     :kbd global-attributes
                     [:* [:schema [:ref ::phrasing-content]]])
              #_ ::label
              ::link
              [:orn
               [:meta (->hiccup-schema
                       :link
                       (mu/merge
                        global-attributes
                        [:map
                         [:itemprop {:optional true} :string]
                         [:crossorigin {:optional true}
                          [:enum "anonymous" "use-credentials"]]
                         [:href {:optional true} url]
                         [:media {:optional true} :string]
                         [:rel {:optional true} :string]])
                       nil)]
               [:phrasing [:schema [:ref "link-phrasing"]]]
               [:flow (->hiccup-schema
                       :link
                       (mu/merge
                        global-attributes
                        [:map
                         [:itemprop :string]
                         [:crossorigin {:optional true}
                          [:enum "anonymous" "use-credentials"]]
                         [:href {:optional true} url]
                         [:media {:optional true} :string]
                         [:rel {:optional true} :string]])
                       nil)]]
              #_ ::map
              ::mark (->hiccup-schema
                      :mark global-attributes
                      [:* [:schema [:ref ::phrasing-content]]])
              ::meta [:orn
                      [:flow (->hiccup-schema
                              :meta (mu/merge global-attributes
                                              [:map [:itemprop :string]])
                              nil)]
                      [:meta (->hiccup-schema :meta global-attributes nil)]]
              #_ ::meter
              #_ ::noscript
              #_ ::object
              #_ ::output
              #_ ::picture
              #_ ::progress
              ::q (->hiccup-schema
                   :q (mu/merge global-attributes
                                [:map [:cite {:optional true} :string]])
                   [:* [:schema [:ref ::phrasing-content]]])
              #_ ::ruby
              ::s (->hiccup-schema :s global-attributes
                                   [:* [:schema [:ref ::phrasing-content]]])
              ::samp (->hiccup-schema
                      :samp global-attributes
                      [:* [:schema [:ref ::phrasing-content]]])
              ::script (->hiccup-schema
                        :script
                        (mu/merge
                         global-attributes
                         [:map
                          [:async {:optional true} [:enum true "async"]]
                          [:crossorigin {:optional true} :string]
                          [:defer {:optional true} [:= true]]
                          [:integrity {:optional true} :string]
                          [:nomodule {:optional true} :string]
                          [:referrerpolicy {:optional true}
                           [:enum "no-referrer" "no-referrer-when-downgrade"
                            "origin" "origin-when-cross-origin" "same-origin"
                            "strict-origin" "strict-origin-when-cross-origin" ""]]
                          [:src {:optional true} url]
                          [:type :string]])
                        [:? :string])
              ::small (->hiccup-schema
                       :small global-attributes
                       [:* [:schema [:ref ::phrasing-content]]])
              ::span (->hiccup-schema
                      :span global-attributes
                      [:* [:schema [:ref ::phrasing-content]]])
              ::strong (->hiccup-schema
                        :strong global-attributes
                        [:* [:schema [:ref ::phrasing-content]]])
              ::sub (->hiccup-schema
                     :sub global-attributes
                     [:* [:schema [:ref ::phrasing-content]]])
              ::sup (->hiccup-schema
                     :sup global-attributes
                     [:* [:schema [:ref ::phrasing-content]]])
              #_ ::svg
              #_ ::textarea
              ::time (->hiccup-schema
                      :time (mu/merge global-attributes
                                      [:map [:datetime :string]])
                      [:* [:schema [:ref ::phrasing-content]]])
              ::var (->hiccup-schema
                     :var global-attributes
                     [:* [:schema [:ref ::phrasing-content]]])
              #_ ::video
              ::phrasing-content
              [:orn [:atomic-element atomic-element]
               [:node
                (apply
                 conj
                 [:orn
                  [:a  [:schema [:ref "a-phrasing"]]]
                  [:del [:schema [:ref "del-phrasing"]]]
                  [:ins [:schema [:ref "ins-phrasing"]]]
                  [:link [:schema [:ref "link-phrasing"]]]]
                 (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                      phrasing-tags))]]
              ::hgroup
              (->hiccup-schema
               :hgroup
               global-attributes
               [:+
                [:orn
                 [:h1 [:schema [:ref ::h1]]]
                 [:h2 [:schema [:ref ::h2]]]
                 [:h3 [:schema [:ref ::h3]]]
                 [:h4 [:schema [:ref ::h4]]]
                 [:h5 [:schema [:ref ::h5]]]
                 [:h6 [:schema [:ref ::h6]]]]])
              ::h1 (->hiccup-schema
                    :h1 global-attributes
                    [:* [:schema [:ref ::phrasing-content]]])
              ::h2 (->hiccup-schema
                    :h2 global-attributes
                    [:* [:schema [:ref ::phrasing-content]]])
              ::h3 (->hiccup-schema
                    :h3 global-attributes
                    [:* [:schema [:ref ::phrasing-content]]])
              ::h4 (->hiccup-schema
                    :h4 global-attributes
                    [:* [:schema [:ref ::phrasing-content]]])
              ::h5 (->hiccup-schema
                    :h5 global-attributes
                    [:* [:schema [:ref ::phrasing-content]]])
              ::h6 (->hiccup-schema
                    :h6 global-attributes
                    [:* [:schema [:ref ::phrasing-content]]])
              ::heading-content
              [:orn
               [:hgroup [:schema [:ref ::hgroup]]]
               [:h1 [:schema [:ref ::h1]]]
               [:h2 [:schema [:ref ::h2]]]
               [:h3 [:schema [:ref ::h3]]]
               [:h4 [:schema [:ref ::h4]]]
               [:h5 [:schema [:ref ::h5]]]
               [:h6 [:schema [:ref ::h6]]]]
              ::a
              [:orn
               [:phrasing [:schema [:ref "a-phrasing"]]]
               [:flow
                (->hiccup-schema
                 :a
                 (mu/merge
                  global-attributes
                  [:map
                   [:href [:orn [:link url]
                           [:fragment :string]]]
                   [:download {:optional true} :string]
                   [:rel {:optional true} :string]
                   [:target {:optional true} [:enum "_self" "_blank" "_parent" "_top"]]])
                 [:* [:schema [:ref ::flow-content]]])]]
              ::address (->hiccup-schema
                         :address global-attributes
                         [:* (apply
                              conj [:orn
                                    [:atomic-element atomic-element]
                                    [:phrasing-content [:schema [:ref ::phrasing-content]]]]
                              (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                                   (set/difference
                                    flow-tags
                                    heading-tags
                                    phrasing-tags
                                    sectioning-tags
                                    #{:header :footer :address})))])
              ::article (->hiccup-schema
                         :article global-attributes
                         [:* [:schema [:ref ::flow-content]]])
              ::aside (->hiccup-schema
                       :aside global-attributes
                       [:* [:schema [:ref ::flow-content]]])
              ::bdi (->hiccup-schema
                     :bdi global-attributes [:* [:schema [:ref ::phrasing-content]]])
              ::blockquote (->hiccup-schema
                            :blockquote
                            (mu/merge global-attributes
                                      [:map [:cite {:optional true} :string]])
                            [:* [:schema [:ref ::flow-content]]])
              ::del
              [:orn
               [:phrasing [:schema [:ref "del-phrasing"]]]
               [:flow (->hiccup-schema
                       :del global-attributes
                       [:* [:schema [:ref ::flow-content]]])]]
              ::details (->hiccup-schema
                         :details
                         global-attributes
                         [:catn
                          [:summary
                           [:schema
                            (->hiccup-schema
                             :summary
                             global-attributes
                             [:orn
                              [:heading [:schema [:ref ::heading-content]]]
                              [:phrasing [:or ; this seems awkward
                                          [:schema [:ref ::phrasing-content]]
                                          [:* [:schema [:ref ::phrasing-content]]]]]])]]
                          [:contents [:* [:schema [:ref ::flow-content]]]]])
              ::div (->hiccup-schema
                     :div global-attributes
                     [:* [:schema [:ref ::flow-content]]])
              ::dl
              (->hiccup-schema
               :dl
               global-attributes
               [:*
                [:catn
                 [:term
                  [:+
                   [:schema
                    (->hiccup-schema
                     :dt
                     global-attributes
                     (apply conj
                            [:orn
                             [:atomic-element atomic-element]
                             [:phrasing-content [:schema [:ref ::phrasing-content]]]
                             [:heading-content [:schema [:ref ::heading-content]]]]
                            (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                                 (set/difference flow-tags phrasing-tags heading-tags sectioning-tags))))]]]
                 [:details
                  [:+ [:schema (->hiccup-schema
                                :dd global-attributes
                                [:* [:schema [:ref ::flow-content]]])]]]]])
              ::figure (->hiccup-schema
                        :figure global-attributes
                        [:altn
                         [:caption-first
                          [:catn
                           [:figcaption
                            [:schema
                             (->hiccup-schema
                              :figcaption
                              global-attributes
                              [:* [:schema [:ref ::flow-content]]])]]
                           [:rest [:* [:schema [:ref ::flow-content]]]]]]
                         [:caption-last
                          [:catn
                           [:rest [:* [:schema [:ref ::flow-content]]]]
                           [:figcaption
                            [:schema
                             (->hiccup-schema
                              :figcaption
                              global-attributes
                              [:* [:schema [:ref ::flow-content]]])]]]]
                         [:no-caption
                          [:* [:schema [:ref ::flow-content]]]]])
              ::footer
              (->hiccup-schema
               :footer global-attributes
               [:* (apply
                    conj
                    [:orn [:atomic-element atomic-element]
                     [:phrasing-content [:schema [:ref ::phrasing-content]]]
                     [:heading-content [:schema [:ref ::heading-content]]]]
                    (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                         (set/difference flow-tags phrasing-tags heading-tags #{:header :footer})))])
              ::header
              (->hiccup-schema
               :header global-attributes
               [:* (apply
                    conj
                    [:orn [:atomic-element atomic-element]
                     [:phrasing-content [:schema [:ref ::phrasing-content]]]
                     [:heading-content [:schema [:ref ::heading-content]]]]
                    (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                         (set/difference flow-tags phrasing-tags heading-tags #{:header :footer})))])
              ::hr (->hiccup-schema :hr global-attributes nil)
              ::ins
              [:orn
               [:phrasing [:schema [:ref "ins-phrasing"]]]
               [:flow (->hiccup-schema
                       :ins global-attributes
                       [:* [:schema [:ref ::flow-content]]])]]
              ::main (->hiccup-schema
                      :main global-attributes
                      [:* [:schema [:ref ::flow-content]]])
              ::nav (->hiccup-schema :nav global-attributes
                                     [:* [:schema [:ref ::flow-content]]])
              ::ol (->hiccup-schema
                    :ol
                    (mu/merge
                     global-attributes
                     [:map
                      [:reversed {:optional true} :boolean]
                      [:start {:optional true} [:and [:> 0] [:< 65536]]]
                      [:type {:optional true} [:enum "a" "A" "i" "I" "1"]]])
                    [:*
                     [:orn
                      [:li (->hiccup-schema
                            :li
                            (mu/merge
                             global-attributes
                             [:map [:value {:optional true} :int]])
                            [:* [:schema [:ref ::flow-content]]])]
                      [:script [:schema [:ref ::script]]]]])
              ::p (->hiccup-schema
                   :p global-attributes
                   [:* [:schema [:ref ::phrasing-content]]])
              ::pre (->hiccup-schema :pre global-attributes
                                     [:* [:schema [:ref ::phrasing-content]]])
              ::section (->hiccup-schema :section global-attributes
                                         [:* [:schema [:ref ::flow-content]]])
              ::table [:schema
                       {:registry
                        {::caption (->hiccup-schema
                                    :caption
                                    global-attributes
                                    [:* [:schema [:ref ::flow-content]]])
                         ::col (->hiccup-schema
                                :col
                                (mu/merge global-attributes
                                          [:map [:span {:optional true} [:>= 1]]])
                                nil)
                         ::colgroup
                         [:orn
                          [:empty-span
                           (->hiccup-schema
                            :colgroup
                            (mu/merge global-attributes
                                      [:map [:span [:>= 1]]])
                            nil)]
                          [:cols (->hiccup-schema
                                  :colgroup
                                  (mu/merge global-attributes
                                            [:map [:span {:optional true} [:>= 1]]])
                                  [:* [:schema [:ref ::col]]])]]
                         ::th (->hiccup-schema
                               :th
                               (mu/merge global-attributes
                                         [:map
                                          [:abbr {:optional true} :string]
                                          [:colspan {:optional true} [:and [:> 0] [:< 65534]]]
                                          [:rowspan {:optional true} [:and [:> 0] [:< 65534]]]
                                          [:headers {:optional true} :string]
                                          [:scope {:optional true} [:enum "row" "col" "rowgroup"
                                                                    "colgroup" "auto"]]])
                               [:*
                                (apply conj
                                       [:orn [:atomic-element atomic-element]
                                        [:phrasing-content [:schema [:ref ::phrasing-content]]]
                                        [:heading-content [:schema [:ref ::heading-content]]]]
                                       (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                                            (set/difference flow-tags
                                                            phrasing-tags
                                                            sectioning-tags
                                                            heading-tags #{:table :footer :header})))])
                         ::td (->hiccup-schema
                               :td
                               (mu/merge
                                global-attributes
                                [:map
                                 [:colspan {:optional true} [:and [:> 0] [:< 65534]]]
                                 [:rowspan {:optional true} [:and [:> 0] [:< 65534]]]
                                 [:headers {:optional true} :string]])
                               [:* [:schema [:ref ::flow-content]]])
                         ::thead (->hiccup-schema
                                  :thead
                                  global-attributes
                                  [:* [:schema [:ref ::tr]]])
                         ::tbody (->hiccup-schema
                                  :tbody
                                  global-attributes
                                  [:* [:schema [:ref ::tr]]])
                         ::tfoot (->hiccup-schema
                                  :tfoot
                                  global-attributes
                                  [:* [:schema [:ref ::tr]]])
                         ::tr (->hiccup-schema
                               :tr
                               global-attributes
                               [:*
                                [:orn [:th [:schema [:ref ::th]]]
                                 [:td [:schema [:ref ::td]]]]])
                         ::table
                         (->hiccup-schema
                          :table
                          global-attributes
                          [:catn
                           [:caption [:? [:schema [:ref ::caption]]]]
                           [:colgroups [:* [:schema [:ref ::colgroup]]]]
                           [:header [:? [:schema [:ref ::thead]]]]
                           [:contents
                            [:altn
                             [:body
                              [:* [:schema [:ref ::tbody]]]]
                             [:rows [:+ [:schema [:ref ::tr]]]]]]
                           [:footer [:? [:schema [:ref ::tfoot]]]]])}}
                       ::table]
              ::ul (->hiccup-schema
                    :ul global-attributes
                    [:* [:orn
                         [:li (->hiccup-schema
                               :li global-attributes
                               [:* [:schema [:ref ::flow-content]]])]
                         [:script [:schema [:ref ::script]]]]])
              ::flow-content
              (apply conj
                     [:orn
                      [:atomic-element atomic-element]
                      [:phrasing [:schema [:ref ::phrasing-content]]]
                      [:heading  [:schema [:ref ::heading-content]]]]
                     (map (fn [t] [t [:schema [:ref (ns-kw t)]]])
                          (set/difference
                           flow-tags phrasing-tags heading-tags)))
              #_ ::base #_ []
              ::metadata-content
              [:orn
               [:style [:schema (->hiccup-schema
                                 :style
                                 (mu/merge global-attributes
                                           [:map [:media {:optional true} :string]])
                                 :string)]]
               [:title [:schema (->hiccup-schema :title global-attributes :string)]]
               [:script [:schema [:ref ::script]]]
               [:meta [:schema [:ref ::meta]]]
               [:link [:schema [:ref ::link]]]]
              ::head (->hiccup-schema
                      :head
                      global-attributes
                      [:* [:schema [:ref ::metadata-content]]])
              ::body (->hiccup-schema
                      :body
                      global-attributes
                      [:* [:schema [:ref ::flow-content]]])
              ::html (->hiccup-schema
                      :html
                      global-attributes
                      [:catn [:head [:schema [:ref ::head]]]
                       [:body [:schema [:ref ::body]]]])
              ::element
              [:orn
               [:flow [:schema [:ref ::flow-content]]]
               [:heading [:schema [:ref ::heading-content]]]
               [:phrasing [:schema [:ref ::phrasing-content]]]]}}
            ::html]))

(defn hiccup->asami [h] nil)
(defn asami->hiccup [n] nil)
