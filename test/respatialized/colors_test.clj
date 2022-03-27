(ns respatialized.colors-test
  (:require [respatialized.colors :refer :all]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :as tc]
            [clojure.test :as t]))

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
