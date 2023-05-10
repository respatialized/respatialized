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
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [site.fabricate.prototype.page :as page])
  (:import [thi.ng.geom.types Path2 Bezier2 Circle2 Ellipse2
            Line2 LineStrip2 Polygon2 Rect2 Triangle2])
  (:import [thi.ng.geom.vector Vec2])
  )



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

(defn get-literal [r]
  (let [r-type (symbol (.getName (type r)))]
    (tagged-literal r-type (into {} r))))

(defmethod pprint/simple-dispatch thi.ng.geom.types.Rect2 [g]
  (pprint/pprint (get-literal g)))



(comment

  (:tag (get-literal (thi.ng.geom.rect/rect 240.298 298.29 30 30)))
  (type (:form (get-literal (thi.ng.geom.rect/rect 240.298 298.29 30 30))))

  (pprint/simple-dispatch )

  (. pprint/simple-dispatch addMethod thi.ng.geom.types.Rect2 ????)

  (print-dup )
  (pprint/pprint #"[a-z]")

  (:tag (get-geom-literal (thi.ng.geom.rect/rect 240.298 298.29 30 30)))

  (site.fabricate.prototype.page/expr->hiccup
   (get-geom-literal (thi.ng.geom.rect/rect 240.298 298.29 30 30))
   )

  (:string-value  (rewrite-clj.node/coerce (get-geom-literal (thi.ng.geom.rect/rect 240.298 298.29 30 30))))



  (binding [*])
  (pprint/pprint
   (thi.ng.geom.rect/rect 240.298 298.29 30 30)
   )

  (type (get-literal (thi.ng.geom.rect/rect 240.298 298.29 30 30)))

  (pr)
  (print-dup (thi.ng.geom.rect/rect 240.298 298.29 30 30))

  (print (thi.ng.geom.rect/rect 240.298 298.29 30 30))
  (binding [pprint/*print-right-margin* 35]
    (pprint/pprint  (thi.ng.geom.rect/rect 240.298 298.29 30 30)))

  (clojure.pprint/pprint (thi.ng.geom.rect/rect 240.298 298.29 30 30))
  (clojure.pprint/pr-with-base (thi.ng.geom.rect/rect 240.298 298.29 30 30))

  (page/expr->hiccup  {:a 2 :b 3})

  (page/expr->hiccup (vec (:arglists (meta #'thi.ng.geom.rect/rect))))

  (binding [pprint/*print-right-margin* 35]
    (with-out-str (pprint/pprint (thi.ng.geom.rect/rect 240.298 298.29 30 30))))

  )

(defn var-meta->hiccup [{:keys [arglists name ns line column file] :as var-meta}
                        src-url]
  (list
   "arguments:" [:br]
   (page/expr->hiccup (vec arglists))
   [:br]
   [:span "source: " [:a {:href (str src-url "/" file "#L" line)
                          :target "_blank"}
                      (str file " L" line)]]))

;; the centroid of a 2d vector is the vector
(extend-protocol g/ICenter
  Vec2
  #_(center ([_] _))
  (centroid ([_] _)))

(defn translate-from [g-obj dist bearing]
  (let [[x y] (g/centroid g-obj)
        Δx (* dist (Math/sin bearing))
        Δy (* dist (Math/cos bearing))]
    (g/translate g-obj (v/vec2 Δx Δy))))

(comment


  (g/rotate (v/vec2 [1 1]) Math/PI)

  )
