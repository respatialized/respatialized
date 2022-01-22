(ns respatialized.svg
  "Implementation of malli schema for SVG2 as specified by https://www.w3.org/TR/SVG2/struct.html"
  (:require [malli.core :as m]
            [site.fabricate.prototype.html :as html]
            [svg-clj.path :as path]
            [svg-clj.utils :as utils]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.polygon :as poly]
            [thi.ng.geom.rect :as rect]
            [thi.ng.geom.path :as g-path]
            [thi.ng.geom.svg.core :as g-svg]
            [thi.ng.geom.matrix :as matrix]
            [clojure.string :as str]
            [clojure.data.xml :as xml]))

(def glyph-svg
  (-> (str (System/getProperty "user.home") "/Pictures/Inkscape/glyph-01-tri.svg")
      slurp
      (xml/parse-str  :namespace-aware false)))

(def structural-tags
  #{:defs :g :svg :symbol :use})

(def structurally-external-tags
  #{:audio :foreignObject :iframe :image :script
    :use :video})

(def graphics-tags
  #{:audio :canvas :circle :ellipse :foreignObject
    :iframe :image :line :path :polygon :polyline :rect
    :text :textPath :tspan :video})



(def graphics-referencing-tags
  #{:audio :iframe :image :use :video} )

(def svg
  (m/schema
   [:catn
    [:tag [:= :svg]]
    [:attrs :map]
    [:desc [:? [:cat [:= :desc]]]]
    [:contents
     [:* :any]]]))

(comment
  (#'html/->hiccup-schema
 :tag [:map [:a :int]] nil)
  )

(defn path->geom-polygon [p]
  (let [pts (-> p
                path/path->elements
                last
                last
                last
                :points)]
    (->>
     (str/split pts #"\s")
     (mapv  #(into [] (map (fn [x] (Double/parseDouble x))
                           (str/split % #","))))
     (poly/polygon2)
     )
    ))



;; example input:
;; 1 2 3
;; 4 5 6
;;
;; translated into svg's affine transform to 3x3:
;; 1 2 3
;; 4 5 6
;; 0 0 1
;;
;; to 4x4 rotation matrix
;; 1 2 3 0
;; 4 5 6 0
;; 0 0 1 0
;; 0 0 0 1
;; and then transposed (there's a fn for that

(defn matrix32->matrix44 [values]
  (->> values
       (partition 3)
       (mapv #(conj (into [] %) 0))
       (#(conj % [0 0 1 0] [0 0 0 1]))
       flatten
       (apply matrix/matrix44)))

(comment
  (matrix32->matrix44 [1 2 3 4 5 6])

  )

(defn svg-mtx->mtx32 [mtx-str]
  (let [pts-str (last (re-find #"(?:\()(.*)(?:\))" mtx-str))
        [a b c d e f] (mapv #(Double/parseDouble %) (str/split pts-str #","))]
    (matrix/matrix32 a c e b d f)))

(defn parse-transform [transform]
  (cond
    (and (some? transform) (.startsWith transform "matrix"))
    (let [mtx (svg-mtx->mtx32 transform)]
      (fn [g] (g/transform g mtx)))
    :default identity))

(defn rect->geom-rect [r]
  (let [[_ {:keys [x y width height transform]}] r
        t (parse-transform transform)
        g-rect (rect/rect x y width height)]
    (t g-rect)))

(defn join-geoms [geoms]
  (poly/polygon2 (apply concat (map :points geoms))))

(defn path->geom [[t {:keys [d]}]]
  (let [parsed (g-path/parse-svg-path d)]
    (try (join-geoms parsed))))

(defn element->geom [e]
  (cond (and (vector? e) (= :path (first e)))
        #_(path->geom-polygon e)
        (path->geom e)
        (and (vector? e) (= :rect (first e)))
        (rect->geom-rect e)))

(comment
  (g-path/parse-svg-path [:path {:d "M150 0 L75 200 L225 200 Z"}])

  (->> respatialized.sketches.20220117/svg
       last
       (filter #(and (vector? %) (= :path (first %))))
       first
       #_ path/path-str->cmds
       ;; (#(#'path/path-cmd-strs %))
       ;; (map #(#'path/cmd-str->cmd %))
       ;; (concat [{:command "M"
       ;;           :coordsys :abs
       ;;           :input [0 0]}])
       ;; (partition 2 1)
       ;; (map #(#'path/merge-cursor %))
       ;; path/vh->l
       ;; #_ path/rel->abs
       ;; second
       ;; (#(update % :input (fn [i] (into [] (take 10 i)))))
       ;; #_(#(path/rel->abs [%]))

       )

  (path/path->elements [:path {:d "c 1 1 M 0 8 L 0 3 L 3 3 L 8 8 Z"}])
  (path/path->elements [:path {:d "C 1 1 0 8 L 0 3 L 3 3 L 8 8 Z"}])

  (first


   (last )
   )

  (map element->geom
       (filter
        #(and (vector? %) (= :path (first %)))
        (last respatialized.sketches.20220117/svg))
       )

  (rect->geom-rect
   [:rect {:style "fill:#eaa083;fill-rule:evenodd;stroke-width:0.56407923;fill-opacity:1",
           :id "rect4520", :width 63.496769, :height 140.30934,
           :x 7.4835467, :y -4.6250167,
           :transform "matrix(0.70710678,0.70710678,0,1,0,0)"}])

  (let
      [pts-str "0.70710678,0.70710678,0,1,0,0"
       mtx (->> (str/split pts-str #",")
                (map #(Double/parseDouble %))
                (apply matrix/matrix32))]
      (g/transform
       (poly/polygon2 [0 0 [1 0] [1 1] [0 0]])
       mtx)
      )

  (g/transform (poly/polygon2 [0 0 [1 0] [1 1] [0 0]])
               (matrix/matrix44 (range 1 16)))


  (g/transform
   (rect/rect 12 12 12 12)
   (matrix/matrix32 0.70710678,0.70710678,0,1,0,0))

  )


(comment
  (require '[clj-async-profiler.core :as prof])

  (require '[criterium.core :as crit])

  (let [f (slurp "resources/respatialized/2022-01-17.svg") ]
    (prof/profile
     (dotimes [_ 15]
       (mapv #(try
                (g/scale (element->geom %) (/ 1.0 0.27))
                (catch Exception e (do (println %) nil)))
             (drop 2 (last (utils/svg-str->hiccup f))))
       )))


  (let [f (slurp "resources/respatialized/2022-01-17.svg")
        parsed (drop 2 (last (utils/svg-str->hiccup f))) ]
    (prof/profile (dotimes [_ 5]
                    (mapv
                     #(try
                        (g/scale (element->geom %) (/ 1.0 0.27))
                        (catch Exception e (do (println %) nil)))
                     parsed))))

  (prof/serve-files 8089)

  (def geometries
    (drop 2 (last (utils/svg-str->hiccup (slurp "resources/respatialized/2022-01-17.svg")))))


   (element->geom (first (filter #(= :path (first %)) geometries)))


  )
