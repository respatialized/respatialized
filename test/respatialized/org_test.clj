(ns respatialized.org-test
  (:require [respatialized.org :as org]
            [org-parser.parser :refer [parse]]
            [clojure.test :as t]))

(t/deftest parsing
  (t/is (any? (parse (slurp "test-resources/respatialized/example.org")))))
