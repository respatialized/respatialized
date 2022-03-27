(ns respatialized.colors
  (:require [tech.v3.tensor :as tensor]
            [tech.v3.datatype :as dtype]
            [tech.v3.parallel.for :as pfor]
            [tech.v3.datatype.functional :as dtype-fn]
            [tech.v3.libs.buffered-image :as dtype-img]
            [clojure.data.json :as json]
            [clojure2d.color :as clj2d-color]
            [clojure.java.io :as io]))

(comment
  (clj2d-color/format-hex (clj2d-color/from-HSV* [132 125 255]) )

  )

(defn rgb->hsv [rgb]
  (clj2d-color/to-HSV* (clj2d-color/from-RGB* rgb)))


(defn rgb-img->hsv
  "Converts the image tensor to HSV colorspace"
  [img-tens]
  (let [[x y z] (:shape (.dimensions img-tens))
        new-img (dtype-img/clone img-tens)
        img' (tensor/reshape img-tens [(* y x) z])
        new-img' (tensor/reshape new-img [(* y x) z])]
    (pfor/parallel-for
     ix (* x y)
     (let [hsv (rgb->hsv [(.ndReadLong img' ix 0)
                          (.ndReadLong img' ix 1)
                          (.ndReadLong img' ix 2)])]
       (for [d [1 2 3]]
         (.ndWriteLong new-img' ix d (int (hsv d))))))
    (tensor/reshape new-img' [x y z])))

(comment

  (respatialized.colors.extraction/carpente-tens 0 1)
  (-> respatialized.colors.extraction/carpente-tens
      (tensor/mget 0 1)
      rgb->hsv
      tensor/ensure-tensor)


  (time (do (rgb-img->hsv respatialized.colors.extraction/carpente-tens) nil))


  )
