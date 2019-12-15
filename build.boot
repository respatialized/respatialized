#!/usr/bin/env boot

(set-env!
 :source-paths #{"src"}
 :resource-paths #{"content" "resources"}
 ;; :asset-paths #{"output"}
 :dependencies '[[org.clojure/clojure "1.10.1"]
                 [perun "0.4.3-SNAPSHOT" :scope "test"]
                 [hiccup "2.0.0-alpha2" :exclusions [org.clojure/clojure]]
                 [pandeiro/boot-http "0.8.3" :exclusions [org.clojure/clojure]]
                 [clojure2d "1.2.0-SNAPSHOT"]
                 [generateme/fastmath "1.4.0-SNAPSHOT"]
                 [net.mikera/clisk "0.11.0"]
                 ])

(def project 'respatialized)

(require '[io.perun :as perun]
         '[pandeiro.boot-http :refer [serve]])

(deftask build
  "Build the blog from its constituent pieces."
  []
  (comp
   (perun/global-metadata :filename "metadata.edn")
   (perun/draft)
   (perun/markdown :md-exts {:tables true})
   (perun/print-meta)
   (perun/build-date)
   (perun/render :renderer 'respatialized.core/render-markdown)
   (perun/static :renderer 'respatialized.holotype/one
                 :page "holotype1.html")
   ;; (perun/static :renderer 'respatialized.holotype/two
   ;;               :page "holotype2.html")
   (perun/collection :renderer 'respatialized.core/render-index
                     :page "index.html")
   (perun/tags :renderer 'respatialized.core/render-tags)
   (perun/assortment :renderer 'respatialized.core/render-assortment
                     :grouper 'respatialized.core/assort)
   ;; (perun/sitemap)
   (sift :to-asset #{#"output/."})
   (perun/rss :description "respatialized")
   (perun/atom-feed :filterer :original)
   (perun/print-meta)
   (target)
   (notify)))

(deftask dev
  "Build and serve the blog locally."
  []
  (comp (watch :exclude #{#"resources\\/output\\/*"
                          #"output\\/*" #"target\\/*"})
        (build)
        (serve :resource-root "public")))