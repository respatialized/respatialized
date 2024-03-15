(ns respatialized.build
  (:require [clojure.java.io :as io]
            [malli.core :as m]
            [hiccup2.core :as hiccup]
            [hiccup.page :as hp]
            [hiccup.util]
            [site.fabricate.prototype.read :as read]
            [site.fabricate.prototype.page :as page]
            [site.fabricate.prototype.html :as html]
            [site.fabricate.prototype.api :as api]
            [site.fabricate.prototype.time :as time]
            [babashka.fs :as fs]
            [asami.core :as d]
            [respatialized.css :as css]
            [respatialized.files :as files]
            [juxt.dirwatch :refer [watch-dir]]
            [respatialized.archive :as archive]
            [clojure.string :as str]))


(def options {:site.fabricate.page/publish-dir "public"})
(defonce site (atom {:site.fabricate.api/options options}))

(defn get-css!
  [{:keys [site.fabricate.api/options] :as site}]
  (let
    [{:keys [site.fabricate.page/publish-dir]} options
     remedy
     {:file (fs/file (fs/path publish-dir "css" "remedy.css"))
      :url
      "https://raw.githubusercontent.com/jensimmons/cssremedy/6590d9630bdd324469620636d85b7ea3753e9a7b/css/remedy.css"}
     normalize
     {:file (fs/file (fs/path publish-dir "css" "normalize.css"))
      :url  "https://unpkg.com/@csstools/normalize.css@12.1.1/normalize.css"}]
    (doseq [{:keys [file url]} [normalize remedy]]
      (when-not (fs/exists? file) (io/copy url file))))
  site)

(defmethod api/collect "content/**.fab"
  [src options]
  (let [worktree-status (archive/git-worktree-status)]
    (mapv (fn path->entry [p]
            {:site.fabricate.source/format :site.fabricate.read/v0
             :site.fabricate.document/format :hiccup
             :site.fabricate.source/location (fs/file p)
             :site.fabricate.entry/source src
             :site.fabricate.source/created (time/file-created p)
             :site.fabricate.source/modified (time/file-modified p)
             :site.fabricate.page/outputs [{:site.fabricate.page/format :html
                                            :site.fabricate.page/location
                                            (fs/file
                                             (:site.fabricate.page/publish-dir
                                              options))}]
             :git/sha (archive/git-sha (str p))
             :git/worktree-status worktree-status
             :git/file-hash (archive/file-hash (str p))})
          (fs/glob (System/getProperty "user.dir") src))))

(comment
  (api/collect "content/**.fab" {}))

(def setup-tasks
  [get-css!
   (fn [site]
     (files/create-dir? "public/css/")
     (files/create-dir? "public/fonts/")
     (when-not (fs/exists? "public/fonts/DefSansVF.woff2")
       (fs/copy (fs/path (System/getProperty "user.font-dir")
                         "defsans-v1.01/variable/DefSansVF.woff2")
                "public/fonts/DefSansVF.woff2"))
     (css/-main)
     site)])

(def default-metadata
  {:title       "Respatialized"
   :description "Respatialized: Actual / Potential Spaces"
   "viewport"   "width=device-width, initial-scale=1.0"
   :locale      "en_US"
   :site-name   "respatialized.net"
   :site-title  "Respatialized"})

(defn doc-header
  "Returns a default header from a map with a post's metadata."
  [{:keys [title page-style scripts] :as metadata}]
  (let [page-meta (-> metadata
                      (dissoc :title :page-style :scripts)
                      (#(merge default-metadata %)))]
    (apply page/conj-non-nil
           [:head [:title (str (:site-title page-meta) " | " title)]
            [:link {:rel "stylesheet" :href "/css/normalize.css"}]
            [:link {:rel "stylesheet" :href "/css/main.css"}]]
           (concat (page/opengraph-enhance page/ogp-properties
                                           (map page/->meta page-meta))
                   (list [:meta {:charset "utf-8"}]
                         [:meta
                          {:http-equiv "X-UA-Compatible" :content "IE=edge"}])
                   (if scripts scripts)
                   (if page-style [[:style page-style]])))))

(defn fabricate-v0->hiccup
  "Generate a Hiccup representation of the page by evaluating the parsed Fabricate template of the page contents."
  [entry]
  (let [start-time     (System/currentTimeMillis)
        parsed-page    (read/parse (slurp (:site.fabricate.source/location
                                           entry)))
        evaluated-page (read/eval-all parsed-page)
        page-metadata  (page/lift-metadata
                        evaluated-page
                        (let [m (:metadata (meta evaluated-page))]
                          ;; TODO: better handling of
                          ;; unbound metadata vars
                          (if (map? m)
                            (if (contains? m :page-style)
                              (update m :page-style hiccup.util/raw-string)
                              m)
                            {})))
        hiccup-page    [:html (doc-header page-metadata)
                        [:body
                         [:main
                          (apply conj
                                 [:article {:lang "en-us"}]
                                 (page/parse-paragraphs evaluated-page))]
                         [:footer [:div [:a {:href "/"} "Home"]]]]]
        end-time       (System/currentTimeMillis)]
    (assoc entry
           :site.fabricate.document/data hiccup-page
           :site.fabricate.document/build-duration (- end-time start-time)
           :site.fabricate.page/title    (:title page-metadata))))



(defn build-fabricate-entry
  "Build the given entry if it has changed; compares with an existing site passed as an argument."
  [{:keys [git/file-hash site.fabricate.source/location] :as entry}
   {:keys [site.fabricate.api/entries] :as site}]
  (let [existing-entry (first (filter #(= location
                                          (:site.fabricate.source/location %))
                                      entries))
        existing-hash  (:git/file-hash existing-entry)]
    (if (or (nil? existing-entry) (not= file-hash existing-hash))
      (do (println "building entry from source" (str location))
          (let [{:keys [site.fabricate.document/build-duration] :as r}
                (fabricate-v0->hiccup entry)]
            (println "built entry in " (/ build-duration 1000.0) "seconds")
            r))
      (do (println "entry" (str location) "up-to-date") existing-entry))))


(defmethod api/build [:site.fabricate.read/v0 :hiccup]
  [entry _opts]
  (build-fabricate-entry entry @site))

(defn write-hiccup-html!
  "Generate HTML from Hiccup data and write it to the given file."
  [hiccup-page-data output-file]
  (let [parent-dir (fs/parent output-file)]
    (files/create-dir? parent-dir)
    (spit output-file (hiccup/html hiccup-page-data))))

(defn output-path
  [input-file output-location]
  (cond (fs/directory? output-location)
        (fs/file (fs/path output-location (files/subpath input-file)))
        (instance? java.io.File output-location) output-location))

(defn hiccup->html
  [entry _opts]
  (println "generating output for source file"
           (str (:site.fabricate.source/location entry)))
  (let [output-file (fs/file (str (output-path
                                   (fs/strip-ext
                                    (fs/strip-ext
                                     (:site.fabricate.source/location entry)))
                                   (:site.fabricate.page/location entry))
                                  ".html"))]
    (write-hiccup-html! (:site.fabricate.document/data entry) output-file)
    (assert (fs/exists? output-file))
    (-> entry
        (assoc :site.fabricate.page/output output-file
               :site.fabricate.page/format :html))))

(defmethod api/produce! [:hiccup :html] [entry opts] (hiccup->html entry opts))



(defn build!
  [site]
  (->> site
       (api/plan! setup-tasks)
       (api/assemble [])
       (api/construct! [])))

(comment
  (keys @site)
  (def task (future (do (swap! site build!) nil)))
  (do (swap! site build!) nil)
  (reset! site {:site.fabricate.api/options options})
  (->> {:site.fabricate.api/options options}
       (api/plan!)
       (api/assemble [])
       (api/construct! [])))


(defn -main
  []
  (->> {:site.fabricate.api/options options}
       (api/plan! [])
       (api/assemble [])
       (api/construct! [])))

(comment
  (require '[http.server :as server])
  (defonce srv (atom (server/start {:port 8887 :dir "public"}))))
