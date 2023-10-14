(ns respatialized.markdown
  (:require [commonmark-hiccup.core :as md :refer [markdown->hiccup]]
            [hiccup2.core :as hiccup]
            [clojure.edn :as edn])
  (:import [org.commonmark.parser Parser]
           [org.commonmark.node FencedCodeBlock]))

(defn extract-meta! [doc]
  (let [node (.getFirstChild doc)]
    (when (and (instance? FencedCodeBlock node)
               (= (.getInfo node) "edn"))
      (.unlink node)
      (edn/read-string (.getLiteral node)))))

(def parser (.build (Parser/builder)))
(defn parse [md-str]
  (let [doc (.parse parser md-str)]
    (assoc (extract-meta! doc)
           :hiccup (#'md/render-node md/default-config doc))))

(comment
  (parse (slurp "test-resources/respatialized/example.md"))

  )
