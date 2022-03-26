(ns respatialized.colors
  (:require [tech.v3.tensor :as tensor]
            [tech.v3.datatype :as dtype]
            [tech.v3.parallel.for :as pfor]
            [tech.v3.datatype.functional :as dtype-fn]
            [tech.v3.libs.buffered-image :as dtype-img]
            [clojure.data.json :as json]
            [clojure.java.io :as io]))


(defn rgb->hsv [[^Long r ^Long g ^Long b]]
  (let [r (/ r 255.0)
        g (/ g 255.0)
        b (/ b 255.0)
        v (max r g b)
        d (- v (min r g b))
        s (if (= 0.0 v) 0.0 (/ d v))
        h (if (= 0.0 s)
            0.0
            (condp == v
              r (/ (- g b) d)
              g (+ 2.0 (/ (- b r) d) )
              (+ 4.0 (/ (- r g) d))))
        h (/ h 6.0)]
    [(int (max 0 (Math/floor (* 255 h))))
     (int (max 0 (Math/floor (* 255 s)) ))
     (int (max 0 (Math/floor (* 255 v))))]))

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
         (.ndWriteLong new-img' ix d (hsv d)))))
    #_(doall
     (pmap (fn px-op [^Long ix]
             )
           ))
    (tensor/reshape new-img' [x y z])))

(comment

  (respatialized.colors.extraction/carpente-tens 0 1)
  (-> respatialized.colors.extraction/carpente-tens
      (tensor/mget 0 1)
      rgb->hsv
      tensor/ensure-tensor)


  (time (do (rgb-img->hsv respatialized.colors.extraction/carpente-tens) nil))


  )
