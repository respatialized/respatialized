(ns respatialized.geometry
  (:require [thi.ng.geom.core :as g]
            [thi.ng.geom.types :as types]
            [thi.ng.geom.vector :as v]
            [thi.ng.geom.path :as p]
            [thi.ng.geom.triangle :as tri]
            [thi.ng.geom.svg.adapter :as adapt]
            [thi.ng.geom.svg.core :as svg]
            [thi.ng.math.core :as math]
            [thi.ng.math.macros :as mm]
            [clojure.string :as str])
  (:import [thi.ng.geom.types Path2 Bezier2 Circle2 Ellipse2
            Line2 LineStrip2 Polygon2 Rect2 Triangle2]))



(comment
  (map #(assoc-in % [1 :color] "#fff")
       (adapt/all-as-svg
        (list (tri/equilateral2 {:p [10 10] :q [20 20]})
              (tri/equilateral2 {:p [10 10] :q [20 20]}))))


  (adapt/all-as-svg
   (tri/equilateral2 {:p [10 10] :q [20 20]}))


  (for [n (map #(/ % 5.0)(range 1 5))]
    (mm/mix 0 1 n))
  )


(defn evenly-space-up-to [n max pct]
  (let [ext (* max pct)
        range-ext (map #(/ % n) (range 0 (inc n)))]
    (map (fn [r] (mm/mix 0 ext r)) range-ext)))

(comment
  (evenly-space-up-to 2 200 0.5))
