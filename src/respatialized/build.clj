(ns respatialized.build
  (:require
   [clojure.java.io :as io]
   [hiccup2.core :as hiccup]
   [hiccup.page :as hp]
   [site.fabricate.prototype.html :as html]
   [site.fabricate.prototype.fsm :as fsm]
   [site.fabricate.prototype.write :as write]
   [site.fabricate.prototype.page :refer :all]
   [asami.core :as d]
   [respatialized.css :as css]
   [respatialized.render :as render :refer :all]
   [respatialized.holotype :as holotype]
   [respatialized.archive :as archive]
   [clojure.string :as str]))

(defn post-hashes [path db]
  (let [res
        (d/q
         '[:find ?post-id ?hash
           :in $ ?path
           :where
           [?post-id :file/path ?path]
           [?post-id ::archive/file-hash ?hash]]
         (d/db db) path)
        [_ fh] (first res)
        current-hash (archive/file-hash (slurp path))]
    {:recorded-hash fh
     :current-hash current-hash}))

(def operations
  (assoc write/default-operations
         write/file-state
         (fn [{:keys [site.fabricate.file/input-file] :as page-data}
              application-state-map]
           (let [hashes (post-hashes (.toString input-file) archive/db)]
             println hashes
             (if (= (:recorded-hash hashes) (:current-hash hashes))
               (do
                 (println "Page at" (.toString input-file)
                          "up to date, skipping")
                 page-data)
               (assoc page-data :site.fabricate.page/unparsed-content
                      (slurp input-file)))))
         write/rendered-state
         (fn [{:keys [site.fabricate.page/rendered-content
                      site.fabricate.file/output-file] :as page-data}
              application-state-map]
           (do
             (println "writing page content to" output-file)
             (spit output-file rendered-content)
             (println "Recording page data in database")
             @(archive/record-post! page-data archive/db)
             page-data))))

(def initital-state
  (merge write/initial-state
         {:site.fabricate/settings
          {:site.fabricate.file/template-suffix ".fab"
           :site.fabricate.file/input-dir "./content"
           :site.fabricate.file/output-dir "./public"
           :site.fabricate.file/operations operations
           :site.fabricate.page/doc-header site-page-header
           :site.fabricate.server/config
           {:cors-allow-headers nil,
            :dir (str (System/getProperty "user.dir") "/public"),
            :port 8000,
            :no-cache true}}}))

(defn -main
  ([]
   (send write/state (constantly initital-state))

   (write/publish! {:dirs ["./content/"]})

   )

  ([& files]
   ))



(comment

  (keys write/operations)

  (do (fsm/complete operations
                    "content/design-doc-database.html.fab"
                    initital-state)
      (println "done"))

  (do (fsm/complete operations
                    "content/working-definition.html.fab"
                    initital-state)
      (println "done"))

  )

(comment

  (-> write/state
      (send (constantly initital-state))
      (send-off write/draft!))

  (send-off write/state write/stop!)

  (get-in @write/state
          [:site.fabricate/pages ]
          )

  (restart-agent write/state initital-state)


  (def completed-posts
    (with-redefs [site.fabricate.prototype.write/default-site-settings
                  site-settings
                  site.fabricate.prototype.page/doc-header
                  site-page-header]
      (->> (get-template-files "./content" ".fab")
           (map (fn [p]
                  (println "rendering" p)
                  [p (fsm/complete write/operations p)]))
           (into {}))))

  (def current-post
    (with-redefs [site.fabricate.prototype.write/default-site-settings
                  site-settings
                  site.fabricate.prototype.page/doc-header
                  site-page-header]


      (fsm/complete write/operations "./content/database-driven-applications.html.fab")

      ))

  )
