(ns respatialized.markdown
  (:require [commonmark-hiccup.core :as md :refer [markdown->hiccup]]
            [hiccup2.core :as hiccup]))

(comment
  (markdown->hiccup (slurp "test-resources/respatialized/example.md"))

  )
