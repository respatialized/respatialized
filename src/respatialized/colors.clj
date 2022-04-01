(ns respatialized.colors
  (:require [tech.v3.tensor :as tensor]
            [tech.v3.datatype :as dtype]
            [tech.v3.parallel.for :as pfor]
            [tech.v3.datatype.functional :as dtype-fn]
            [tech.v3.libs.buffered-image :as dtype-img]
            [clojure.data.json :as json]
            [clojure2d.color :as clj2d-color]
            [clojure.java.io :as io]))




(defn rgb->hsv [rgb]
  (clj2d-color/to-HSV* (clj2d-color/from-RGB* rgb)))

(defn bgr->hsv [bgr]
  (clj2d-color/to-HSV* (clj2d-color/from-RGB* (into [] (reverse bgr)))))

(defn hsv->bgr [hsv]
  (into [] (reverse (clj2d-color/to-RGB* (clj2d-color/from-HSV* hsv)))))

(comment
  (clj2d-color/to-HSV* [255 255 255])

  )



(defn rgb-img->hsv
  "Converts the image tensor to HSV colorspace"
  [img-tens]
  (let [[y x z] (:shape (.dimensions img-tens))
        img' (tensor/reshape img-tens [(* y x) z])
        new-img (tensor/clone img')]
    (pfor/parallel-for
     ix (* y x)
     (let [hsv (rgb->hsv [(.ndReadLong img' ix 0)
                          (.ndReadLong img' ix 1)
                          (.ndReadLong img' ix 2)])]
       (for [d [0 1 2]]
         (.ndWriteLong new-img ix d (int (hsv d))))))
    (tensor/reshape new-img [y x z])))

(defn bgr-img->hsv
  "Converts the image tensor to HSV colorspace"
  [img-tens]
  (let [[y x z] (:shape (.dimensions img-tens))
        img' (tensor/reshape img-tens [(* y x) z])
        new-img (tensor/clone img')]
    (pfor/parallel-for
     ix (* x y)
     (let [hsv (bgr->hsv [(.ndReadLong img' ix 0)
                          (.ndReadLong img' ix 1)
                          (.ndReadLong img' ix 2)])]
       (for [d [0 1 2]]
         (.ndWriteLong new-img ix d (int (hsv d))))))
    (tensor/reshape new-img [y x z])))

(defn pixelwise-convert [img-tens convert-fn]
  (let [[y x z] (:shape (.dimensions img-tens))
        img' (tensor/reshape img-tens [(* y x) z])
        new-img (tensor/clone img')]
    (pfor/parallel-for
     ix (* x y)
     (let [c (convert-fn [(.ndReadLong img' ix 0)
                          (.ndReadLong img' ix 1)
                          (.ndReadLong img' ix 2)])]
       (for [d [0 1 2]]
         (.ndWriteLong new-img ix d (int (c d))))))
    (tensor/reshape new-img [y x z])))

(comment

  (= test-img)

  (.getRGB
   respatialized.colors.extraction/carpente-img
   918
   918 )

  [914 890]


  (respatialized.colors.extraction/carpente-tens 0 1)
  (-> respatialized.colors.extraction/carpente-tens
      (tensor/mget 2592 1403)
      bgr->hsv
      )


  (time (do (rgb-img->hsv respatialized.colors.extraction/carpente-tens) nil))


  )
