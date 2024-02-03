(ns respatialized.build
  (:require [clojure.java.io :as io]
            [malli.core :as m]
            [hiccup2.core :as hiccup]
            [hiccup.page :as hp]
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

(defmethod api/collect "content/**.fab"
  [src options]
  (let [worktree-status (archive/git-worktree-status)]
    (mapv (fn path->entry [p]
            {:site.fabricate.source/format :site.fabricate.read/v0,
             :site.fabricate.document/format :hiccup,
             :site.fabricate.source/location (fs/file p),
             :site.fabricate.entry/source src,
             :site.fabricate.source/created (time/file-created p),
             :site.fabricate.source/modified (time/file-modified p),
             :site.fabricate.page/outputs
               [{:site.fabricate.page/format :html,
                 :site.fabricate.page/location
                   (fs/file (:site.fabricate.page/publish-dir options))}],
             :git/sha (archive/git-sha (str p)),
             :git/worktree-status worktree-status,
             :git/file-hash (archive/file-hash (str p))})
      (fs/glob (System/getProperty "user.dir") src))))

(comment
  (api/collect "content/**.fab" {}))

(def setup-tasks
  [(fn [site]
     (files/create-dir? "public/css/")
     (files/create-dir? "public/fonts/")
     (when-not (fs/exists? "public/fonts/DefSansVF.woff2")
       (fs/copy (fs/path (System/getProperty "user.font-dir")
                         "defsans-v1.01/variable/DefSansVF.woff2")
                "public/fonts/DefSansVF.woff2"))
     (css/-main)
     site)])

(def default-metadata
  {:title "Respatialized",
   :description "Respatialized: Actual / Potential Spaces",
   "viewport" "width=device-width, initial-scale=1.0",
   :locale "en_US",
   :site-name "respatialized.net",
   :site-title "Respatialized"})

(defn fabricate-v0->hiccup
  "Generate a Hiccup representation of the page by evaluating the parsed Fabricate template of the page contents."
  [entry]
  (let [start-time (System/currentTimeMillis)
        parsed-page (read/parse (slurp (:site.fabricate.source/location entry)))
        evaluated-page (read/eval-all parsed-page)
        page-metadata (page/lift-metadata evaluated-page
                                          (let [m (:metadata (meta
                                                              evaluated-page))]
                                            ;; TODO: better handling of
                                            ;; unbound metadata vars
                                            (if (map? m) m {})))
        hiccup-page [:html
                     (conj (page/doc-header (merge default-metadata
                                                   page-metadata))
                           [:link {:href "/css/main.css", :rel "stylesheet"}])
                     [:body
                      [:main
                       (apply conj
                              [:article {:lang "en-us"}]
                              (page/parse-paragraphs evaluated-page))]
                      [:footer [:div [:a {:href "/"} "Home"]]]]]
        end-time (System/currentTimeMillis)]
    (assoc entry
           :site.fabricate.document/data hiccup-page
           :site.fabricate.document/build-duration (- end-time start-time)
           :site.fabricate.page/title (:title page-metadata))))



(defn build-fabricate-entry
  "Build the given entry if it has changed; compares with an existing site passed as an argument."
  [{:keys [git/file-hash site.fabricate.source/location], :as entry}
   {:keys [site.fabricate.api/entries], :as site}]
  (let [existing-entry (first (filter #(= location
                                          (:site.fabricate.source/location %))
                                      entries))
        existing-hash (:git/file-hash existing-entry)]
    (if (or (nil? existing-entry) (not= file-hash existing-hash))
      (do (println "building entry from source" (str location))
          (let [{:keys [site.fabricate.document/build-duration], :as r}
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
                                        (:site.fabricate.source/location
                                          entry)))
                                    (:site.fabricate.page/location entry))
                                  ".html"))]
    (write-hiccup-html! (:site.fabricate.document/data entry) output-file)
    (assert (fs/exists? output-file))
    (-> entry
        (assoc :site.fabricate.page/output output-file
               :site.fabricate.page/format :html))))

(defmethod api/produce! [:hiccup :html] [entry opts] (hiccup->html entry opts))

(def options {:site.fabricate.page/publish-dir "public"})

(defonce site (atom {:site.fabricate.api/options options}))

(defn build!
  [site]
  (->> site
       (api/plan! setup-tasks)
       (api/assemble [])
       (api/construct! [])))

(comment
  (def task (future (do (swap! site build!) nil)))
  (swap! site build!)
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
  (defonce srv (atom (server/start {:port 8887, :dir "public"}))))
