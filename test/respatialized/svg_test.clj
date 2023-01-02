(ns respatialized.svg-test
  (:require [respatialized.svg :refer :all]
            [svg-clj.jvm-utils :as utils]
            [clojure.test :as t]))

(def example-svg
  (-> "resources/respatialized/2022-01-17.svg"
      slurp
      utils/svg-str->hiccup))

(def example-geoms
  (mapv (fn [e] [e (try (element->geom e)
                        (catch Exception e nil))])
        (drop 2 (last example-svg))))

(t/deftest svg-to-geometry
  (doseq [[svg-input geom] example-geoms]
    (let [converted (some? geom)]
      (t/is converted)
      (if (not converted)
        (println "error when processing input:" svg-input)))))

(comment
(->>
 example-geoms
 (filter
  #(.startsWith (:id (second (first %))) "rect4520"))
 last
 first
 path->geom-polygon
 )


  )
