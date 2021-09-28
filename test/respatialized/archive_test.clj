(ns respatialized.archive-test
  (:require [clojure.test :as t]
            [asami.core :as d]
            [malli.core :as m]
            [hiccup.core :as hiccup]
            [site.fabricate.prototype.html :as html]
            [respatialized.archive :refer :all]
            [malli.transform :as mt]))

(def test-db-uri "asami:mem://respatialized-test")
(declare conn)

(defn db-fixture [f]
  (do
    (d/create-database test-db-uri)
    (def conn (d/connect test-db-uri))
    (f)
    (d/delete-database test-db-uri)))

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
    {:html/tag :figcaptio
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
   (mt/transformer {:name :asami})
   )

  )

(t/deftest conversion
  (t/testing "Bidirectional conversion"

    (t/is (any? (parsed-element->asami
                 {:tag :p,
                  :attrs nil,
                  :contents
                  [[:atomic-element [:text "paragraph with"]]
                   [:node
                    [:em
                     {:tag :em,
                      :attrs nil,
                      :contents [[:atomic-element [:text "emphasized text"]]]}]]]})))

    (t/is (= :p
             (-> [:p "paragraph with" [:em "emphasized text"]]
                 html/parse-element-flat
                 parsed->asami
                 :html/tag)))

    (t/is (= quotation-asami
             (m/decode
              example-html-decoder
              quotation-hiccup
              (mt/transformer {:name :asami})
              )))

    (t/is (= quotation-hiccup
             #_(asami->hiccup quotation-asami)))))

(def example-post
  {:evaluated-content [:html [:head] [:body]]
   :rendered-content (hiccup/html [:html [:head] [:body]])
   :unparsed-content "✳(def metadata {:title \"Empty Example\"})🔚"
   :input-file "example-file.html.fab"
   :title "Empty Example"})

(t/deftest recording

  (let [quot-tx @(d/transact conn {:tx-data [quotation-asami]})
        figs (d/q '[:find  [?tag ?attr-prop]
                    :where [?e :html/tag :figure]
                    [?e :html/tag ?tag]
                    [?e :html.attribute/itemprop ?attr-prop]]
                  (d/db conn))]

    (t/is (any? @(record-post! example-post conn))
          "Post data should be recorded without errors.")

    (t/is (any? quot-tx)
          "Element data should be recorded without errors.")

    (t/is (>= (count figs) 1)
          "Element data should be queryable."))
  )
