(ns respatialized.colors-test
  (:require [respatialized.colors :refer :all]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :as tc]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as dtype-fn]
            [tech.v3.tensor :as tensor]
            [tech.v3.libs.buffered-image :as dtype-img]
            [clojure2d.core :as clj2d]
            [clojure2d.color :as clj2d-color]
            [clojure2d.pixels :as clj2d-pixels]
            [clojure.test :as t]))

;; general idea: use manual coordinates to verify color
;; extraction and conversion from tensor representations
(def test-img-url "https://live.staticflickr.com/65535/51913654989_7490d04a5a_4k.jpg")
(def test-img-path "./public/media/carpente-reijer-stolk-sm.jpg")
(def test-coords [100 100])

(def off-white (clj2d-color/color "#f4f0e9") )

(def test-img-clj2d (clj2d/load-image test-img-path))
(def test-img-dtype (dtype-img/load test-img-path))
(def test-img-tens (tensor/ensure-tensor test-img-dtype))

(t/deftest bgr-hsv-bgr
  (t/testing "rgb to hsv roundtrip"
    (t/is (dtype-fn/eq
           test-img-tens
           (-> test-img-tens
               (pixelwise-convert bgr->hsv)
               (pixelwise-convert hsv->bgr))))))

(comment

  (= test-img-tens test-img-tens)

  (-> "https://github.com/cnuernber/dtype-next/blob/master/test/data/test.jpg?raw=true"
      (dtype-img/load )
      (tensor/ensure-tensor)
      (tensor/mget 10 10))

  (-> "https://github.com/cnuernber/dtype-next/blob/master/test/data/test.jpg?raw=true"
      (dtype-img/load )
      (dtype-img/image-channel-format))

  (-> test-img-url
      (dtype-img/load )
      (dtype-img/image-channel-format))

  (-> test-img-url
      (dtype-img/load )
      (tensor/ensure-tensor)
      (tensor/mget 10 10))

  (clj2d-pixels/get-color
   (clj2d-pixels/to-pixels test-img-clj2d)
   10 10)

  (def test-cnvs
    (clj2d/canvas 900 900))


  (clj2d/show-window test-cnvs)


  (clj2d/with-canvas [c test-cnvs]
    (clj2d/image c test-img-clj2d)
    (clj2d/set-color c 0 0 0)
    (clj2d/rect c 40 40 200 200))

  )

#_(t/deftest image-processing
  (t/is
   (= (.getRGB test-img-clj2d (first test-coords) (second test-coords))
      (.getRGB test-img-dtype (first test-coords) (second test-coords))))


  (let [[x y] test-coords
        [r g b a]
        (clj2d-color/color
         (.getRGB test-img-clj2d x y))
        [b' g' r'] (test-img-tens x y) ]
    (t/is
     (= (mapv int [r g b]) [r' g' b']))))

(comment
  (clj2d-color/color
   (.getRGB test-img-clj2d 1000 1000))

  (test-img-tens 1000 1000)

  (dtype-img/image-type test-img-dtype)

  (into [] (clj2d-color/color (.getRGB test-img-dtype (first test-coords) (second test-coords))))

  (unchecked-byte (.getRGB test-img-dtype (first test-coords) (second test-coords)) )

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
