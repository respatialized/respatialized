(ns respatialized.document
  "Namespace for document processing."
  (:require
   [site.fabricate.prototype.html :as html]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.test.chuck.generators :as gen']
   [malli.core :as m]
   [malli.generator :as mg]
   [malli.registry :as mr]
   [malli.dot :as md]
   [malli.error :as me]
   [malli.util :as mu]))

(comment
  (m/validate html [:em [:a {:href "http://google.com"} "link"] "more text"])

  (m/validate (subschema html ::em)
              [:em [:a {:href "http://google.com"} "link"] "more text"])

  (def em-gen
    (mg/generator
     (subschema html ::em)
     {:size 5}))

  (m/validate (subschema phrasing-content "a-phrasing")
              [:a {:href "http://google.com"} "link"])

  (m/validate html [:h1 [:b "emphasized header"]])

  (m/parse html [:h1 [:em "emphasized header"]])

  ((m/validator html) [:h1 [:em "emphasized header"]])

  (m/validate (subschema html ::p) [:p "text"])

  (m/validate (subschema html ::section)
              [:section [:h1 "header text"] "section text"]))


(comment
                                        ; example before

  (def sample-form '("first paragraph\n\nsecond paragraph"
                     [:section
                      {:class "grid"}
                      [:div {:class "1col"} "first coll line\n\nsecond col line"]
                      [:div {:class "1col"} "another cell"]]
                     "third paragraph"))

                                        ; example after
  (def sample-parsed-form
    '([:section
       [:p "first paragraph"]
       [:p "second paragraph"]]
      [:section {:class "grid"}
       [:div {:class "1col"} [:p "first cell line"] [:p "second cell line"]]
       [:div {:class "1col"} [:p "another cell"]]
       "third paragraph"])))

(comment
  (detect-paragraphs [:section "some\n\ntext" [:em "with emphasis"]]
                     #"\n\n"))



;; seems like this may be a problem if I want to nest further
;; but the actual structure implied by the format I've chosen
;; is pretty flat - at most three levels deep:
;; [:article [:section #_ :next [:div "text"]]]

(defn process-nexts [nexts]
  (loop [[h n & rest] nexts
         res []]
    (if (empty? rest) ; base case
      (condp = [(nil? h) (nil? n)]
        [true true] res
        [false true] (conj res h)
        [false false] (conj res h n))
      (cond
        (= :next n)
        (recur (apply conj [:next] rest)
               (conj res h))
        (= :next h)
        (recur
         (drop-while #(not= % :next) rest)
         (conj res (apply conj n (take-while #(not= % :next) rest))))
        :else
        (recur
         (drop-while #(not= % :next) rest)
         (apply conj res h n (take-while #(not= % :next) rest)))))))

;; (def process-contents
;;   (comp
;;    (partition-by next?))
;;   (cond
;;     (next? (first contents))
;;     (let [[_ into-elem & rest] contents]
;;       (apply conj into-elem rest))
;;     :else contents))






(comment
  (require '[clojure.repl :refer [doc]])

  (defn ->tag-generator
    ([tag]
     (gen/fmap #(apply conj [tag] %) (gen/vector (mg/generator atomic-element))))
    ([tag num-elements]
     (gen/fmap #(apply conj [tag] %) (gen/vector (mg/generator atomic-element) num-elements))))

  ;; proof of concept: nested divs
  (-> (gen/recursive-gen
       (fn [inner]
         ;; the inner element could be programmatically
         ;; selected based on the document tree
         ;;
         ;; the tricky part: inner needs to dispatch on
         ;; the element type of its container in order to
         ;; respect the document tree
         ;;
         ;; gen/bind may be helpful here, it's like
         ;; fmap but in reverse
         (gen/one-of [(gen/fmap #(conj [:div] %) inner)
                      (gen/fmap #(apply conj [:div] %) (gen/vector inner))
                      inner]))
       (mg/generator atomic-element))
      (gen/sample 125)
      last)

  ;; a basic test of the coherence of this approach
  (every? #(or ((::div element-validators) %)
               (m/validate atomic-element %))
          (-> (gen/recursive-gen
               (fn [inner]
                 (gen/one-of [(gen/fmap #(conj [:div] %) inner)
                              (gen/fmap #(apply conj [:div] %) (gen/vector inner))
                              inner]))
               (mg/generator atomic-element))
              (gen/sample 20)))

  (defn schema->generator [schema]
    ))
