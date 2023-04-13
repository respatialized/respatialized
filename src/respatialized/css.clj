(ns respatialized.css
  "Compositional css generated with garden"
  (:require [garden.core :as garden]
            [garden.stylesheet
             :as style
             :refer [at-media at-import at-font-face]]
            [garden.selectors :as select :refer [attr-starts-with attr before after]]
            [garden.units :as u]
            [garden.color]))


;; reset
(def remedy
  ^{:doc "Garden translation of CSS remedy"
    :source "https://github.com/jensimmons/cssremedy"
    :license {:name "Mozilla Public License"
              :version 2.0
              :url "https://www.mozilla.org/en-US/MPL/2.0/"}}
  [#_[:*
      (before) (after)
      {:box-sizing "border-box"}]    ; unclear if I got the selectors right here
   #_ [:html {:line-sizing "normal"}] ; the line-sizing property isn't implemented yet
   [:body {:margin "0"}]
   [(attr "hidden") {:display "none"}]
   [:h1 {:font-size "2rem"}]
   [:h2 {:font-size "1.5rem"}]
   [:h3 {:font-size "1.17rem"}]
   [:h4 {:font-size "1.00rem"}]
   [:h5 {:font-size "0.83rem"}]
   [:h6 {:font-size "0.67rem"}]
   [:h1 {:margin "0.67em 0"}]
   [:pre {:white-space "pre-wrap"}]
   [:hr {:border-style "solid", :border-width "1px 0 0",
         :color "inherit", :height "0", :overflow "visible"}]
   [:img :svg :video :canvas :audio :iframe :embed :object
    {:display "block", :vertical-align "middle", :max-width "100%"}]
   #_ [:audio ":not([controls])" {:display "none"}]
   [:picture {:display "contents"}]
   [:source {:display "none"}]
   [:img :svg :video :canvas {:height "auto"}]
   [:audio {:width "100%"}]
   [:img {:border-style "none"}]
   [:svg {:overflow "hidden"}]
   [:article :aside :figcaption :figure :footer :header
    :hgroup :main :nav :section {:display "block"}]
   [(attr "type" "=" "'checkbox'")
    (attr "type" "=" "'radio'")
    {:box-sizing "border-box", :padding "0"}]])



(def wal-colors
  {"color1" "#33484E",
   "color6" "#F6971B",
   "color7" "#e6cca4",
   "color11" "#9E5B2B",
   "color4" "#E66A13",
   "color3" "#9E5B2B",
   "color5" "#768176",
   "color2" "#57585A",
   "color13" "#768176",
   "color15" "#e6cca4",
   "color12" "#E66A13",
   "color14" "#F6971B",
   "color8" "#a18e72",
   "color10" "#57585A",
   "color9" "#33484E",
   "color0" "#011117",
   "background" "#011117",
   "foreground" "#e6cca4",
   "cursor" "#e6cca4"})

(def oklch-hex-conversions
  {"red" "#fc4354"
   "sand"  "#f9f2ec"
   "off-white" "#fcfaf7"
   "aqua" "#34b7d7"
   "cobalt" "#0378b4"
   "leaf" "#309457"
   "steel" "#e1e8f6"
   "dark-steel" "#3f4454"
   "golden" "#fbdd6f"
   "ochre" "#9e4305"
   "dark-umber""#31080e"
   "ink"  "#061627"
   "violet" "#b05fe1"
   "marine" "#2464b4"
   })

;;(defn color-shades "Yields" [color-hex])

;; SIZES
;;


(def grid-ratios
  {:columns {:desktop "6"
             :mobile "4"}
   :main {:desktop "1 / 4"
          :mobile "1 / 4"}
   :side {:desktop "5 / 6"
          :mobile "2 / 4"}})

(def grid-areas
  {:desktop "main main main main side side"
   :mobile
   "main main main main main main
    _    side side side side side"})


;; SPACING
;; utilities for dealing with spacing of elements: floats, line heights


(defn create-scale [])

(def base-font-size (u/em 1.5))
(def line-height (u/em 1.25))
(def baseline (u/em-div line-height 2))
(def column-gap (u/em* baseline 4))
(def row-gap (u/em* baseline 2))

(defn max-widths [n]
  (conj
   (map
    (fn [i] [(keyword (str ".mw" i)) {:max-width (u/em* i line-height)}])
    (range 1 (+ n 1)))
   [:.mw-full {:max-wdith "100%"}]))

(defn max-heights [n]
  (map
   (fn [i] [(keyword (str ".mh" i)) {:max-height (u/em* i line-height)}])
   (range 1 (+ n 1))))

;; COLOR
;; BORDERS
;; DISPLAY
;; FONTS

(def google-fonts-url
  "https://fonts.googleapis.com/css2?family=Chivo+Mono:wght@100..900&display=swap")

(def font-spec
  (list
   (at-font-face
    {:font-family "'Def Sans'"
     :src ["url('../fonts/DefSansVF.woff2')"]})))

(comment
  (garden/css font-spec)

  )

(def sans-serif "'Def Sans', sans-serif")
(def monospace "'Mainframe', monospace")

(def html-rules
  [:html {:-webkit-font-smoothing "auto"
          :-moz-osx-font-smoothing "auto"
          :text-rendering "optimizeLegibility"
          :font-family sans-serif
          :background (get wal-colors "background")
          :color (garden.color/lighten (get wal-colors "foreground") 10)
          :line-height line-height
          :font-size base-font-size}])

;; SYNTAX HIGHLIGHTING



;; ASSEMBLING IT

(select/defpseudoelement selection)

(def page-style
  (list

   #_(at-import google-fonts-url )
   [:a {:color (get oklch-hex-conversions "ochre")
        :text-decoration "none"}
    [:img {:border-right-color (get oklch-hex-conversions "ochre")
           :border-right-width "0.35rem"
           :border-right-style "solid"}]]

   [:body {:background-color (oklch-hex-conversions "off-white")
           :font-size "min(5vmin, 20px)"
           :font-weight "400"
           :font-family "'Def Sans', sans-serif"
           :line-height "1.35em"
           :color (oklch-hex-conversions "ink")}]
   [(selection) {:background-color (oklch-hex-conversions "aqua")}]
   [:header {:font-weight "600"
             :font-family "Def Sans"}]
   [:h1 {:font-size "2rem"}]
   [:h2 {:font-size "1.7rem"}]
   [:h3 {:font-size "1.5rem"}]
   [:h4 {:font-size "1.3rem"}]
   [:h5 {:font-size "1.3rem"}]
   [:h6 {:font-size "1.3rem"}]
   #_[:h1 :h2 :h3 {:font-size "3rem"
                   :line-height "3rem"
                   :margin-bottom "0.6em"
                   :margin-top "0.2em"}]
   [:h4 :h5 :h6 {:font-size "1.5rem"}]
   [:h1 :h2 :h3 :h4 :h5 :h6 {:font-weight "800"
                             :line-height "1.25em"}]
   [:strong {:font-weight "900"}]
   [:blockquote :aside
    {:font-size "0.9em"
     :margin-left "1em"}]
   [:pre :code
    {:white-space "pre-wrap"
     :font-family "'Chivo Mono', monospace"
     :font-weight 375
     ;;:line-height "27px"
     :font-size "0.925em"
     :color (oklch-hex-conversions "dark-steel")}]
   [:pre {:margin-bottom "0.4em"}]
   [:dt {:margin-bottom "0.2rem"}]
   [:dd {:margin-bottom "0.4rem"}]
   [:summary {:margin-bottom "0.4em"}]
   [:article {:max-width "105ch"
              :color (oklch-hex-conversions "ink")  }]
   (at-media {:screen true} [:article {:padding-left "1.5em"
                                       :padding-right "2.5em"}])
   (at-media {:max-width "700px"} [:article { :padding-left "0.25em"
                                             :padding-right "0.05em"}])
   [:p {:margin-bottom "0.4em"
        :margin-top "0.2em"}
    [(select/a select/after)
     {:position "relative"
      :vertical-align "super"
      :margin-left "0.15em"
      :margin-bottom "-0.15em"
      :line-height "0.5em"
      :margin-top "-0.15em"
      :font-weight "Bold"
      :font-size "0.7em"
      :content "\"→\""}]]
   [:table {:font-size "0.95rem"
            :letter-spacing "0.01rem"
            :line-height "1.45em"
            :max-width "90em"}]
   [:td :th {#_#_:border "0.5px solid"
             :padding "0.25rem"
             :text-align "left"}]
   [(select/attr "colspan") {:text-align "center"
                             :border-bottom "0.5px solid"
                             :border-top "0.5px solid"
                             }]
   [:table {:border-collapse "collapse"
            :max-width "1400px"}]))

(defn -main
  "main fn"
  []
  (->> [remedy
        page-style
        font-spec
        ;; [:h1 :h2 :h3 :h4 :h5 :h6 {:color (get wal-colors "color6")}]
        ]
       garden/css
       (spit "public/css/main.css")))

(comment
  (-main)

  (->> [remedy
        page-style
        font-spec
        ;; [:h1 :h2 :h3 :h4 :h5 :h6 {:color (get wal-colors "color6")}]
        ]
       garden/css
       )



  )
