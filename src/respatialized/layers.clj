(ns respatialized.layers
  (:require [clojure2d.core :as clj2d]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [respatialized.styles :as styles]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.circle :as c]
            [thi.ng.geom.polygon :as poly]))




(comment

  (require '[clojure.repl :as repl :refer [doc source]])


  (doc json/read)


  (def demo-colors
    {"special"
     {"background" "#18191b", "foreground" "#fefeff", "cursor" "#fefeff"},
     "colors"
     {"color1" "#ce3b32", "color6" "#7b816a", "color7" "#c5c5c6",
      "color11" "#ff8b3f", "color4" "#79353e", "color3" "#e0713f",
      "color5" "#cb8765", "color2" "#36786a", "color13" "#ffa86f",
      "color15" "#fefeff", "color12" "#a83948", "color14" "#a7b67a",
      "color8" "#2f3135", "color10" "#3aa790", "color9" "#ff3d30",
      "color0" "#18191b"}})


  (def demo-surface
    (clj2d/with-canvas->
      (clj2d/canvas 900 900)
      (clj2d/set-background (clojure2d.color/to-RGB (get-in demo-colors ["colors" "color8"])))))




  (def window (clj2d/show-window
               demo-surface
               "LAYERS"))


  (clj2d/with-canvas->
    demo-surface
    (clj2d/set-background (clojure2d.color/to-RGB (get-in demo-colors ["colors" "color8"]))))

  ;; basic intuition for layers: reduce

  (def layers1
    (reduce
     (fn [layers n] (conj layers (n (peek layers))))
     (list (c/circle 400))
     (list #(g/as-polygon % 3)
           #(g/rotate % (* 6 (/ Math/PI 12)))
           #(g/translate % [450 400])
           #(g/sample-uniform % 8 false))))

  ;; there's probably a better way to do this using protocols
  ;; but this works for now
  ;;
  ;; e.g. j
  (defn draw-geom [canvas g]
    (let [draw-op
          (cond (and (vector? g) (instance? thi.ng.geom.vector.Vec2 (peek g)))
                #(doseq [[x y] g] (clj2d/point % x y))
                (instance? thi.ng.geom.types.Polygon2 g)
                #(let [pts (conj (:points g) (first (:points g)))]
                   (doseq [[[x1 y1] [x2 y2]] (partition 2 1 pts)]
                     (clj2d/line % x1 y1 x2 y2)))
                (instance? thi.ng.geom.types.Circle2 g)
                #(let [[x y] (:p g) r (:r g)]
                   (clj2d/ellipse % x y (* r 2) (* r 2)))
                :default (constantly nil))]
      (clj2d/with-canvas->
        canvas
        (clj2d/set-color (clojure2d.color/to-color (get-in demo-colors ["colors" "color7"])))
        draw-op)))

  (doseq [[x y] (first layers1)]
    (clj2d/with-canvas->
      demo-surface
      (clj2d/set-color (clojure2d.color/to-color (get-in demo-colors ["colors" "color7"])))
      (clj2d/point x y)))

  (doseq [l layers1]
    (draw-geom demo-surface l))

  (clj2d/close-window window)

  )
