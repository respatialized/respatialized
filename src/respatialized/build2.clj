(ns respatialized.build2
  (:require [site.fabricate.api :as fabricate]
            [site.fabricate.prototype.document.fabricate :as fab]
            [site.fabricate.prototype.document.clojure :as clj]
            [hiccup2.core :as hiccup]
            [babashka.fs :as fs]))


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

(defmethod fabricate/collect "content/**/*.fab"
  [ptrn {:keys [site.fabricate.page/publish-dir] :as opts}]
  (mapv
   (fn [path]
     (let [src-loc (fs/canonicalize (fs/absolutize path))]
       {:site.fabricate.source/location  (fs/file src-loc)
        :site.fabricate.page/location    (output-to src-loc ptrn publish-dir)
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

(defmethod fabricate/produce! [:hiccup :html]
  [{:keys [site.fabricate.document/data] :as entry} opts]
  (assoc entry
         :site.fabricate.page/data
         (hiccup/html [:html [:head] [:body data]])))

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
