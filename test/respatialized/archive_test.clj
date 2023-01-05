(ns respatialized.archive-test
  (:require [clojure.test :as t]
            [asami.core :as d]
            [malli.core :as m]
            [hiccup.core :as hiccup]
            [site.fabricate.prototype.fsm :as fsm]
            [site.fabricate.prototype.html :as html]
            [site.fabricate.prototype.write :as write]
            [respatialized.archive :refer :all]
            [malli.transform :as mt]))

(defn ->db-uri []
  (str "asami:mem://respatialized-test-" (rand-int 1e7)))

(def ^:dynamic test-db-uri (->db-uri))
(declare ^:dynamic conn)

(defn db-fixture [f]
  (do
    (binding [test-db-uri
              (let [uri (->db-uri)]
                (d/create-database uri)
                uri)
              conn (d/connect test-db-uri)]
      (f)
      (d/delete-database test-db-uri))))

(t/use-fixtures :once db-fixture)


(def quotation-hiccup
  [:figure {:itemprop "quotation"
            :data-source "Red Mars"}
   [:blockquote {:itemprop "quote-text"}
    [:p "“In the classic sense of passing along calories to one's predator, ten percent was average, and twenty percent doing really well. Most predators at the tops of food chains did more like five percent.”"]
    [:p "“This is why tigers have ranges of hundreds of square kilometers,” Vlad said. “Robber barons are not really very efficient.”"]
    [:p "“So tigers don't have predators not because they're so tough, but because it's not worth the effort,” John said."]
    [:p "“Exactly!”"]
    [:p "“The problem is in calculating the values,” Marina said. “We have had to simply assign certain calorie-equivalent numerical values to all kinds of activities, and then go from there.”"]
    [:p "“But were we talking about economics?” John said."]
    [:p "“But this is economics, don't you see, this is our eco-economics! Everyone should make their living, so to speak, based on a calculation of their real contribution to the human ecology. Everyone can increase their ecological efficiency by efforts to reduce how many kilocalories they use – this is the old Southern argument against the energy consumption of the Northern industrial nations. There was a real ecologic basis to that objection, because no matter how much the industrial nations produced, in the larger equation they could not be as efficient as the South.”"]
    [:p "“They were predators on the South,” John said."]
    [:p "“Yes, and they will become predators on us too, if we let them.”"]]
   [:figcaption {:itemprop "quote-source"} "Kim Stanley Robinson, " [:em "Red Mars"] ", p. 261"]])

(def quotation-asami
  {:html/tag :figure
   :html.attribute/itemprop "quotation"
   :html.attribute.data/source "Red Mars"
   :html/contents
   [{:html/tag :blockquote
     :html.attribute/itemprop "quote-text"
     :html/contents
     [{:html/tag :p
       :html/contents
       [{:html/text "“In the classic sense of passing along calories to one's predator, ten percent was average, and twenty percent doing really well. Most predators at the tops of food chains did more like five percent.”"}]}
      {:html/tag :p
       :html/contents
       [{:html/text "“This is why tigers have ranges of hundreds of square kilometers,” Vlad said. “Robber barons are not really very efficient.”"}]}
      {:html/tag :p
       :html/contents
       [{:html/text
         "“So tigers don't have predators not because they're so tough, but because it's not worth the effort,” John said."}]}
      {:html/tag :p
       :html/contents [{:html/text "“Exactly!”"}]}
      {:html/tag :p
       :html/contents [{:html/text "“The problem is in calculating the values,” Marina said. “We have had to simply assign certain calorie-equivalent numerical values to all kinds of activities, and then go from there.”"}]}
      {:html/tag :p
       :html/contents [{:html/text "“But were we talking about economics?” John said."}]}
      {:html/tag :p
       :html/contents
       [{:html/text "“But this is economics, don't you see, this is our eco-economics! Everyone should make their living, so to speak, based on a calculation of their real contribution to the human ecology. Everyone can increase their ecological efficiency by efforts to reduce how many kilocalories they use – this is the old Southern argument against the energy consumption of the Northern industrial nations. There was a real ecologic basis to that objection, because no matter how much the industrial nations produced, in the larger equation they could not be as efficient as the South.”"}]}
      {:html/tag :p
       :html/contents [{:html/text "“They were predators on the South,” John said."}]}
      {:html/tag :p
       :html/contents [{:html/text "“Yes, and they will become predators on us too, if we let them.”"}]}]}
    {:html/tag :figcaption
     :html.attribute/itemprop "quote-source"
     :html/contents
     [{:html/text "Kim Stanley Robinson, "}
      {:html/tag :em
       :html/contents [{:html/text "Red Mars"}]}
      {:html/text ", p. 261"}]}]})

(comment
  (m/decode
   example-html-decoder
   quotation-hiccup
   (mt/transformer {:name :asami})))

(t/deftest conversion
  (let [elem
        [:p [:span {:id "038a38efd9"} "text"] "more text"]]
    (t/is (= elem (-> elem element-parser element-unparser)))))

(def example-post
  {:site.fabricate.page/evaluated-content [:html [:head] [:body]]
   :site.fabricate.page/rendered-content (hiccup/html [:html [:head] [:body]])
   :site.fabricate.page/unparsed-content "✳(def metadata {:title \"Empty Example\"})🔚"
   :site.fabricate.file/input-file "example-file.html.fab"
   :site.fabricate.page/title "Empty Example"})

(def test-post-files
  ["content/not-a-tree.html.fab" "content/ai-and-labor.html.fab"
   "./content/not-a-tree.html.fab" "./content/ai-and-labor.html.fab"
   "content/database-driven-applications.html.fab"
   "./content/database-driven-applications.html.fab"])

(def completed-posts
  (->> test-post-files
       (map #(fsm/complete (dissoc write/default-operations write/rendered-state)
                           % write/initial-state))
       (filter map?)
       (into [])))

(comment
  (file->revision
   (:site.fabricate.file/input-file (first completed-posts)))

  (count test-post-files))


;; what's the point of a test, anyway?
;;
;; - externalize your knowledge of a system into concrete properties
;; - ensure you understand and can write code that performs what you want to do in the correct order
;; - stabilize system properties

(comment

  (second (first completed-posts))

  (nth (apply conj [:html] (first completed-posts)) 2)

  (m/explain html/html
             (nth [:html
                   (first (first completed-posts))
                   (apply conj [:body] (second (first completed-posts)))]
                  2)))

(t/deftest post-database
  (t/testing "database capabilities:"

    (t/testing "parsing posts into Asami format"

      (doseq [{:keys [site.fabricate.page/evaluated-content]
               :as results}
              completed-posts]
        (let [r (-> evaluated-content
                    page-parser
                    page-unparser)]
          (t/is (= evaluated-content r)
                "Parsing and unparsing should produce identical page contents")

          (t/is (= (write/evaluated->hiccup results write/initial-state)
                   (write/evaluated->hiccup
                    (assoc results :site.fabricate.page/evaluated-content r)
                    write/initial-state))
                "Parsing and unparsing should produce identical hiccup elements")

          (t/is (=
                 (-> results
                     (write/evaluated->hiccup write/initial-state)
                     (hiccup/html))
                 (-> (assoc results :site.fabricate.page/evaluated-content r)
                     (write/evaluated->hiccup write/initial-state)
                     (hiccup/html)))
                "Parsing and unparsing should produce identical hiccup elements")))

      (let [asami-posts
            (mapv
             #(let [a (page->asami %)]
                (if (= a :malli.core/invalid)
                  (do (println
                       (:errors
                        (m/explain html/html %))) a)
                  a))
             completed-posts)]
        (t/is (not-any? #(= :malli.core/invalid %)
                        asami-posts))
        (d/transact conn {:tx-data asami-posts})))

    (t/testing "queries for post contents by html attributes"
      (let [q-res (d/q
                   '[:find ?tag ?contents
                     :where
                     [?elem :html/tag :blockquote]
                     [?elem :html/tag ?tag]
                     [?elem :html/contents+ ?contents]]
                   (d/db conn))]
        (t/is (not-empty q-res))))

    (t/testing "update semantics for existing posts"
      (let [random-post (first (shuffle completed-posts))
            changed-post
            (update random-post
                    :site.fabricate.page/evaluated-content
                    (fn [p] (let [h (pop p) t (peek p)]
                              (conj h (conj t [:div "one final updated div"])))))]

        (record-page! changed-post conn)
        (Thread/sleep 500)

        (let [q-res-1
              (d/q '[:find ?title . #_#_?d-attr ?d-val
                     :in $ ?page-title
                     :where
                     [?rev :site.fabricate.page/title ?title]
                     [?page :page/title ?page-title]
                     [?page :page/revisions ?revs]
                     [?revs :a/contains ?rev]
                     [?rev :id ?r-id]
                     [?rev :html/contents ?hc]
                     [?hc :body ?bn]
                     [?bn :html/tag :body]
                     [?bn :html/contents ?bc]
                     [?bc :a/contains ?d]
                     [?d :html/tag :div]
                     [?d :html/contents ?dc]
                     [?dc :a/contains "one final updated div"]]
                   (d/db conn) (:site.fabricate.page/title random-post))]

          (t/is (= (:site.fabricate.page/title random-post)
                   q-res-1)))

        (t/is (= 1 (d/q '[:find (count ?p) .
                          :in $ ?page-title
                          :where
                          [?p :page/title ?page-title]
                          [?p :page/revisions _]]
                        (d/db conn)
                        (:site.fabricate.page/title random-post)))
              "Uniqueness for existing pages should be enforced")))))

(comment
  (record-page! (first completed-posts) conn)

  (record-page! (first completed-posts) conn)

  )
