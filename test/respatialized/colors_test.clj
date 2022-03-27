(ns respatialized.colors-test
  (:require [respatialized.colors :refer :all]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :as tc]
            [clojure.test :as t]))

;; general idea: use manual coordinates to verify color
;; extraction and conversion from tensor representations
(def test-img-url "https://live.staticflickr.com/65535/51913654989_7490d04a5a_4k.jpg")
(def test-coords [1000 1000])

(comment
  (require '[clojure2d.color :as clj2d-color])

  (rgb->hsv [200 100 50])

  )



(tc/defspec valid-return-values 500
  (prop/for-all
   [r (gen/fmap int (gen/double* {:min 0 :max 255}))
    g (gen/fmap int (gen/double* {:min 0 :max 255}))
    b (gen/fmap int (gen/double* {:min 0 :max 255}))]
   (let [hsv (rgb->hsv [r g b])]
     (not-any? #(or (< 255 %) (< % 0)) hsv))))
