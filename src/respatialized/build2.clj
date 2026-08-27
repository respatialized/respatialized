(ns respatialized.build2
  "Build namespace using Fabricate's API"
  (:require [site.fabricate.api :as fabricate]
            [site.fabricate.prototype.document.fabricate :as fab]
            [site.fabricate.prototype.document.clojure :as clj]
            [site.fabricate.prototype.page.hiccup :as hiccup]
            [site.fabricate.prototype.eval :as eval]
            [malli.core :as m]
            [dev.onionpancakes.chassis.core :as chassis]
            [respatialized.render :as render]
            [babashka.fs :as fs]
            [clojure.walk :as walk]))


;; this seems like a function that Fabricate ought to provide
(defn output-to
  ([source-path source-dir target-dir new-ext]
   (let [source-relative (fs/relativize (fs/path (fs/cwd) source-dir)
                                        source-path)]
     (-> source-relative
         ;; if no extension is provided, assume it follows the "*.ext.fab"
         ;; convention
         (#(if (nil? new-ext)
             (fs/strip-ext %)
             (str (fs/strip-ext %) "." new-ext)))
         (#(fs/path target-dir %))
         (fs/canonicalize)
         (fs/absolutize))))
  ([source-path source-dir target-dir]
   (output-to source-path source-dir target-dir nil)))


(defmethod fabricate/collect "content/*.fab"
  [ptrn {:keys [site.fabricate.page/publish-dir] :as opts}]
  (mapv
   (fn [path]
     (let [src-loc (fs/canonicalize (fs/absolutize path))]
       {:site.fabricate.source/location  (fs/file src-loc)
        :site.fabricate.page/location    (output-to src-loc
                                                    (first (fs/components ptrn))
                                                    publish-dir)
        :site.fabricate.page/format      :html
        :site.fabricate.document/format  :hiccup
        :site.fabricate.source/format    :fabricate/v0
        ::fabricate/source               ptrn
        :site.fabricate.source/directory (fs/canonicalize
                                          (fs/parent (fs/absolutize path)))}))
   (fs/glob (fs/cwd) ptrn)))



(defmethod fabricate/build [:fabricate/v0 :hiccup]
  [{:keys [site.fabricate.source/location ::fabricate/source] :as entry} opts]
  (let [article (fab/entry->hiccup-article entry opts)
        title   (or (get-in article [1 :title]) "Respatialized")]
    (assoc entry
           :site.fabricate.document/data  article
           :site.fabricate.document/title title)))

(def kindly-map? (m/validator eval/Evaluated-Form))

(defn process-kindly?
  [v]
  (if (kindly-map? v)
    (try (fabricate/render-form v)
         (catch Exception e
           (throw (ex-info "Error processing kindly map"
                           (merge (Throwable->map e) {:kindly/map v})))))
    v))

(defmethod fabricate/display-form [:fabricate/error :hiccup/html]
  [form]
  [:figure {:class "fabricate-error"}
   [:pre [:code {:class "language-clojure"} (:code form)]] [:hr]
   [:pre [:code {:class "language-clojure"} (:error form)]]])

(defmethod fabricate/produce! [:hiccup :html]
  [{:keys [site.fabricate.document/data site.fabricate.document/title
           site.fabricate.page/location]
    :as   entry} opts]
  (let [processed-page-data (walk/postwalk process-kindly? data)
        output-html         (chassis/html
                             [chassis/doctype-html5
                              (render/site-page-header {:title title}) #_[:head]
                              [:body processed-page-data]])]
    (println "writing to" (str location))
    (spit (fs/file location) output-html)
    (assoc entry :site.fabricate.page/data output-html)))

(def init-site
  {::fabricate/entries []
   ::fabricate/options {:site.fabricate.page/publish-dir "public"}})



(comment
  (-> (first (fs/glob (fs/cwd) "content/**/*.fab"))
      (fs/canonicalize)
      (fs/absolutize)
      (#(fs/relativize (fs/path (fs/cwd) "content") %)))
  (fs/strip-ext "example.html.fab")
  (fs/glob (fs/cwd) "content/**/*.clj")
  (fs/strip-ext "deps.edn" {:ext "clj"})
  (fs/glob (fs/cwd) "content/**/*.md")
  (meta #'site.fabricate.document/build-fabricate-article)
  (defmethod fabricate/collect "./content/**/*.fab"
    [ptrn]
    (fs/glob (fs/cwd) ptrn))
  (defmethod fabricate/build [:fabricate/v0 :hiccup] [entry])
  (defmethod fabricate/produce! [:hiccup :html] [entry]))
