(ns respatialized.notation
  "Experiments with a new syntax for Clojure documents"
  (:require [instaparse.core :as insta]))

(def notation-ebnf
  (insta/parser
    "document := front-matter? token*
front-matter := (<#'\\s*'> delimiter <':'> tag)+ '🔚'
tag := #'[a-zA-Z]+'
(* this matches on any character. how would I make it match only on characters
   that actually occur in the text? *)
delimiter := #'[^a-zA-Z0-9]{1}'
<token> := !front-matter ( dt | text)*
<dt> := delimiter token* delimiter
(* this currently doesn't work because the regex matching is greedy
   making it work probably means folding inter-word whitespace into the grammar *)
text := !dt #'[\\s\\S]+'"))


(def simple-parser
  (insta/parser
    "<S> := front-matter? (<ws?> token)+
front-matter := <'['> (<ws?> special)+ <']'>
special := '*' | '-' | '`'
ws := #'\\s+'
token := special / #'\\S{1}'
"))

(def simple-parser-examples
  ["a b c d e f" "[*] a s d f * g h -" "[* -] a b c - * d e f"])

(clojure.pprint/pprint (mapv simple-parser simple-parser-examples))
