(ns respatialized.org
  "Namespace for parsing org-mode files in Fabricate's evaluation context."
  (:require [org-parser.parser :refer [parse]]
            [org-parser.core :as org-parser]
            [clj-org.org :refer [parse-org]]
            ))


(comment

  ;; this demonstrates the tradeoff

  ;; parsing more of the structure of the file, but basically
  ;; directly to the format specified by the EBNF grammar,
  ;; which requires rewriting into Hiccup
  (parse (slurp "test-resources/respatialized/example.org"))

  ;; directly to hiccup, but without features
  (parse-org (slurp "test-resources/respatialized/example.org"))

  )
