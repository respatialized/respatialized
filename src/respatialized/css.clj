(ns respatialized.css
  "Compositional css generated with garden"
  (:require [garden.core :as garden]
            [garden.stylesheet :refer [at-media at-import at-font-face]]
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
   [:html {:line-sizing "normal"}]
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

;(defn color-shades "Yields" [color-hex])

;; SIZES
;;



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

(def font-spec
  (list
   (at-font-face
    {:font-family "Default Sans"
     :src ["url(../fonts/DefaultSans-Regular.woff2)"
           "url(../fonts/DefaultSans-Black.woff2)"
           "url(../fonts/DefaultSans-BlackItalic.woff2)"
           "url(../fonts/DefaultSans-BoldItalic.woff2)"
           "url(../fonts/DefaultSans-ExtraBold.woff2)"
           "url(../fonts/DefaultSans-ExtraBoldItalic.woff2)"
           "url(../fonts/DefaultSans-ExtraLight.woff2)"
           "url(../fonts/DefaultSans-ExtraLightItalic.woff2)"
           "url(../fonts/DefaultSans-Italic.woff2)"
           "url(../fonts/DefaultSans-Light.woff2)"
           "url(../fonts/DefaultSans-LightItalic.woff2)"
           "url(../fonts/DefaultSans-Medium.woff2)"
           "url(../fonts/DefaultSans-MediumItalic.woff2)"
           "url(../fonts/DefaultSans-SemiBold.woff2)"
           "url(../fonts/DefaultSans-SemiBoldItalic.woff2)"
           "url(../fonts/DefaultSans-Thin.woff2)"
           "url(../fonts/DefaultSans-ThinItalic.woff2)"]} )
   (at-font-face
    {:font-family "Mainframe"
     :src ["url(../fonts/Mainframe-Regular.woff2)"
           "url(../fonts/Mainframe-Bold.woff2)"
           "url(../fonts/Mainframe-ExtraBold.woff2)"
           "url(../fonts/Mainframe-ExtraLight.woff2)"
           "url(../fonts/Mainframe-Light.woff2)"
           "url(../fonts/Mainframe-Medium.woff2)"
           "url(../fonts/Mainframe-SemiBold.woff2)"]})))

(comment
  (garden/css font-spec)

  )

(def sans-serif "'Default Sans', sans-serif")
(def monospace "'Red Hat Mono', monospace")

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



(def prism-rules
  ^{:doc "Garden translation of syntax highlighting rules from prism.js"
    :url "https://prismjs.com/download.html#themes=prism&languages=markup+css+clojure"}
  [[:code (attr "class" "*=" "\"language-\"") :pre
    (attr "class" "*=" "\"language-\"")
    {:color "black", :text-align "left", :tab-size "4", :-o-tab-size "4", :white-space "pre", :font-size "1em", :-ms-hyphens "none", :hyphens "none", :word-wrap "normal", :background "none", :word-spacing "normal", :word-break "normal", :-webkit-hyphens "none", :-moz-tab-size "4", :-moz-hyphens "none"}]
   [:pre (attr "class" "*=" "\"language-\"")
    ;; _:::-moz-selection
    :pre (attr "class" "*=" "\"language-\"") " "
    ;; :::-moz-selection
    :code (attr "class" "*=" "\"language-\"")
    ;; :::-moz-selection
    :code (attr "class" "*=" "\"language-\"") " "
    ;; :::-moz-selection
    {:text-shadow "none", :background "#b3d4fc"}]
   [:pre (attr "class" "*=" "\"language-\"")
    ;; :::selection
    :pre (attr "class" "*=" "\"language-\"") " "
    ;; :::selection
    :code (attr "class" "*=" "\"language-\"")
    ;; :::selection
    :code (attr "class" "*=" "\"language-\"") " "
    ;; :::selection
    {:text-shadow "none", :background "#b3d4fc"}]
   (at-media {:print true}
             [:code (attr "class" "*=" "\"language-\"")
              :pre (attr "class" "*=" "\"language-\"")
              {:text-shadow "none"}])
   [:pre (attr "class" "*=" "\"language-\"")
    {:padding "1em", :margin ".5em 0", :overflow "auto"}]
   [":not(pre)" ">" :code (attr "class" "*=" "\"language-\"")
    :pre (attr "class" "*=" "\"language-\"")
    {:background "#f5f2f0"}]
   [":not(pre)" ">" :code
    (attr "class" "*=" "\"language-\"")
    {:padding ".1em", :border-radius ".3em",
     :white-space "normal"}]
   [:.token :.comment :.token :.prolog :.token :.doctype :.token :.cdata {:color "slategray"}]
   [:.token :.namespace {:opacity ".7"}]
   [:.token :.property :.token :.tag :.token :.boolean :.token :.number :.token :.constant :.token :.symbol :.token :.deleted {:color "#905"}]
   [:.token :.selector :.token :.attr-name :.token :.string :.token :.char :.token :.builtin :.token :.inserted {:color "#690"}]
   [:.token :.operator :.token :.entity :.token :.url :.language-css " " :.token :.string :.style " " :.token :.string {:color "#9a6e3a", :background "hsla(0,0%,100%,.5)"}]
   [:.token :.atrule :.token :.attr-value :.token :.keyword {:color "#07a"}]
   [:.token :.function :.token :.class-name {:color "#DD4A68"}]
   [:.token :.regex :.token #_ :.important :.token :.variable {:color "#66220e" }]
   [:.operator {:color "#66220e"}]
   [:.token [:.operator {:color "#66220e"}]]
   #_[:.token :.punctuation {:color "#222"}]
   [:.token [:.punctuation {:color "#222"}]]
   [:.token :.important :.token :.bold {:font-weight "bold"}]
   #_[:.token :.italic {:font-style "italic"}]])

(def highlight-rules
  (list
   [:.token.comment
    :.token.prolog
    :.token.doctype
    {:color (get wal-colors "color5")}]
   [:.token.symbol (get wal-colors "color6")]))

(def page-style
  (list
   (at-import
    "https://fonts.googleapis.com/css2?family=Inter:wght@600;900&display=swap")
   (at-import "https://fonts.googleapis.com/css2?family=Karla:ital,wght@0,400;0,600;0,700;1,400;1,600;1,700&family=Red+Hat+Mono:wght@700&display=swap")
   [:a {:color "#C14825"
        :text-decoration "none"}
    [:img {:border-right-color "#C14825"
           :border-right-width "0.35rem"
           :border-right-style "solid"}]]

   [:body {:background-color "#FFF"
           :font-size "20px"
           :font-weight "400"
           :font-family "'Default Sans', sans-serif"
           :line-height "27px"
           :color "#444"}]
   [:header {:font-weight "600"
             :font-family "Default Sans"}]
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
   [:code {:font-size "0.95em"
           :line-height "1.2em"
           :font-weight "500"}]
   [:pre :code
    {:white-space "pre-wrap"
     :font-family "'Red Hat Mono', monospace"
     :color "#66220e"}]
   [:pre {:margin-bottom "0.4em"
          :line-height "27px"
          :font-size "0.9em"}
    [:code {:line-height "27px"}]]
   [:dt {:margin-bottom "0.2rem"}]
   [:dd {:margin-bottom "0.4rem"}]
   [:summary {:margin-bottom "0.4em"}]
   [:article {:max-width "45rem"
              :padding-left "20px"
              :color "#333"  }]
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
        prism-rules
        page-style
        font-spec
        ;; [:h1 :h2 :h3 :h4 :h5 :h6 {:color (get wal-colors "color6")}]
        ]
       garden/css
       (spit "public/css/main.css")))

(comment
  (-main)

(->> [remedy
        prism-rules
        page-style
        font-spec
        ;; [:h1 :h2 :h3 :h4 :h5 :h6 {:color (get wal-colors "color6")}]
        ]
       garden/css
       )



  )
