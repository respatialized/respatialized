(ns respatialized.styles
  (:require [hiccup.page :as hp]
            [garden.core :as garden]
            [garden.selectors :as select]
            [garden.stylesheet :as stylesheet]
            [clojure.string :as str]
            [clojure.pprint :as pprint :refer [pprint]]))

(def page "bg-moon-gray ml3 basier")
(def copy "")

(def code "ws-pre navy bg-silver")
(def in-code "ws-normal navy")

(def header-default "b")
(def page-header "f1 b")
(def entry-header "f2 b")
(def entry-date "f4")

(def blockquote-outer "bl bw2 b--green w-60")
(def blockquote-content "f4 pl3 bold")
(def blockquote-author "pl3")

(def table-header "")
(def table-row "")

(def img-default "w-50")

(def stat-rethinking-bg "#483737")
(def stat-rethinking-fg "#EEE")
(def stat-rethinking-neutral "#BBB")
(def stat-rethinking-dash [4 2])

;; const markColor = '#30a2da';
;; const axisColor = '#cbcbcb';
;; const guideLabelColor = '#999';
;; const guideTitleColor = '#333';
;; const backgroundColor = '#f0f0f0';
;; const blackTitle = '#333';

(def stat-rethinking-vl
  "EDN definition for vega-lite style for stat rethinking"
  {:arc {:fill stat-rethinking-fg}
   :area {:fill stat-rethinking-fg}
   :line {:stroke stat-rethinking-fg :strokeWidth 3}
   :axisXQuantitative {:tickCount 12 :domainDash stat-rethinking-dash}
   :axisYQuantitative {:tickCount 6 :titleAngle 0 :titleAlign "left"
                       :titleBaseline "bottom"
                       :domainDash stat-rethinking-dash
                       :orient "right"}
   :axis {:domainColor stat-rethinking-fg
          :domainDash stat-rethinking-dash
          :grid true :gridColor stat-rethinking-fg
          :gridDash stat-rethinking-dash
          :tickBand "extent" :tickDash stat-rethinking-dash
          :gridWidth 1 :labelColor stat-rethinking-fg
          :tickColor stat-rethinking-fg
          :labelFont "'Red Hat Mono', monospace" :labelFontSize "10"
          :titleFont #_"'Gelasio', serif" "'Red Hat Mono', monospace"
          :labelFontStyle "bold" :titleColor stat-rethinking-fg
          :titleFontSize 18}
   :background stat-rethinking-bg
   :view {:strokeWidth 0}})

(def stat-rethinking-garden
  "Garden CSS style for statistical rethinking"
  (list
   (stylesheet/at-import
    "https://fonts.googleapis.com/css2?family=Gelasio:ital,wght@0,400;0,700;1,400&family=Syne:wght@400;500;600;700;800&display=swap")
   [:body :article
    {:background-color stat-rethinking-bg
     :font-family "'Gelasio', serif"
     :color stat-rethinking-fg}]
   [:h1 :h2 :h3 :h4 :h5 :h6 {:font-family "'Syne', sans-serif" :text-transform "uppercase"}]
   [:h1 {:font-weight 800 :font-size "4em"
         :line-height "1.1"}]
   [:pre :code {:color stat-rethinking-fg
                :font-size "1em" :font-family "'League Mono', monospace"}]
   [:a {:color "#FF4444" :text-decoration "none"}]
   [(select/a select/after)  {:content "\"▸\"" :position "relative" :vertical-align "super"
                              :margin-left "0.1em"}]
   [(select/attr-starts-with :class "language-") {:color stat-rethinking-fg}]
   [:figcaption {:font-weight 500 :font-family "'Syne', sans-serif" :text-transform "uppercase"
                 :font-size "1.15em" :border-bottom "1px"}]
   [:h2 {:font-weight 700}]
   [:h3 {:font-weight 700}]
   [:h4 {:font-weight 600}]
   [:h5 {:font-weight 400}]
   [:h6 {:font-weight 300}]))

(def stat-rethinking-css (garden/css stat-rethinking-garden))
(comment
  stat-rethinking-css)

(select/defclass annotated)
(select/defclass annotation)

(select/defpseudoelement selection)

(def geom-style

  (list
   (stylesheet/at-import "https://fonts.googleapis.com/css2?family=Overpass+Mono:wght@300..700&display=swap")
   [:.wide {:font-family "Anybody"
            :font-weight 900
            :font-stretch "150%"
            :text-transform "uppercase"}]
   [:.big {:font-size "5em"
           :margin-top "0em"
           :margin-bottom "0em"}]
   [:article {:background-color "#1a1a1aff"
              :grid-auto-columns "minmax(20px, 25ch)"
              :max-width "200ch"
              :text-align "left"
              :color "#e6e6e6ff"
              :font-family "'Overpass Mono', monospace"}]
   [:body {:background-color "#1a1a1aff"
           :color "#e6e6e6ff"}]
   [:pre :code {:color "#e2e2e2"
                :font-family "'Overpass Mono', monospace"
                :text-align "left"
                :hyphens "none"}]
   [:.keyword {:color "#5abeb1"
               :font-weight 550}]
   [:.symbol {:color "#ff5549"
              :font-weight 550}]
   [:.number {:font-weight 550}]
   [:a {:color "#ff5549"}]
   [(select/a select/after)
    {:position "relative"
     :font-weight "900"
     :vertical-align "super"
     :margin-left "0.15em"
     :font-size "0.95em"
     :content "\"›\""}]
   [:dd :dt {:margin-bottom "0.25em"
             :text-justify "none"
             :margin-left "0em"
             :text-align "left"
             :hyphens "none"}
    [:pre {:margin-bottom "0em"
           :margin-top "0em"}]]
   [(selection) {:background-color "#5abeb1"}]
   [:.annotation {:display "none"}]
   [(select/+ (annotated select/hover) annotation)
    {:display "inherit"}]))
