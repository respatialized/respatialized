(ns respatialized.markdown-test
  (:require [respatialized.markdown :as md]
            [commonmark-hiccup.core :as cmh :refer [markdown->hiccup]]
            [hiccup2.core :as hiccup]
            [clojure.test :as t]))

(t/deftest rendering
  (t/is (string? (-> "test-resources/respatialized/example.md"
                     slurp
                     markdown->hiccup
                     hiccup/html
                     str)))

  (t/is (=
         "example"
         (:title (md/parse (slurp "test-resources/respatialized/example.md") )) )
        "EDN front matter should parse properly"
        )
  )
