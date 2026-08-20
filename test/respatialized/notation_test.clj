(ns respatialized.notation-test
  (:require [instaparse.core :as insta]
            [respatialized.notation :refer :all]
            [clojure.test :as t]))

(defn check-example
  "Check the example and return the parsed value."
  [parser text]
  (let [parsed (insta/parse parser text :total true)]
    (t/testing text
      (t/is (not (insta/failure? parsed))
            "Parser should not fail on example text")
      (t/is (= 1 (count (insta/parses parser text)))
            "Parser should parse unambiguously for example text"))
    parsed))

(t/deftest examples
  (check-example notation-ebnf "some text")
  (let [r (check-example notation-ebnf
                         "-:test🔚 some text -inside delimiters- and more")]
    #_(t/is (some #(= % :delimiter) (flatten r)))))


(comment
  (flatten [:a [:b [:c "1"]]])
  (insta/parses notation-ebnf
                "-:test🔚 some text -inside delimiters- and more"))
