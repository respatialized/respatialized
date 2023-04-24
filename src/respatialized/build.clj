(ns respatialized.build
  (:require
   [clojure.java.io :as io]
   [malli.core :as m]
   [hiccup2.core :as hiccup]
   [hiccup.page :as hp]
   [site.fabricate.prototype.html :as html]
   [site.fabricate.prototype.fsm :as fsm]
   [site.fabricate.prototype.write :as write]
   [site.fabricate.prototype.page :refer :all]
   [asami.core :as d]
   [respatialized.css :as css]
   [juxt.dirwatch :refer [watch-dir]]
   [respatialized.render :as render :refer :all]
   [respatialized.holotype :as holotype]
   [respatialized.archive :as archive]
   [clojure.string :as str]))

(defn new-page? [{:keys [site.fabricate.file/input-file] :as page-data}
                 {:keys [site.fabricate.app/database]
                  :as application-state-map}]
  (let [{:keys [::archive/revision-new?] :as rev-entity}
        (archive/file->revision input-file
                                (:db/conn database))]
    revision-new?))

(def operations
  (assoc write/default-operations
         write/file-state
         (fn [{:keys [site.fabricate.file/input-file] :as page-data}
              application-state-map]
           (if (new-page? page-data application-state-map)
             (assoc page-data :site.fabricate.page/unparsed-content
                    (slurp input-file))
             (do (println "Page at" (.toString input-file)
                          "up to date, skipping") page-data)))
         write/rendered-state
         (fn [{:keys [site.fabricate.page/rendered-content
                      site.fabricate.file/output-file] :as page-data}
              {:keys [site.fabricate.app/database]
               :as application-state-map}]
           (do
             (println "writing page content to" output-file)
             (spit output-file rendered-content)
             (println "Recording page data in database")
             (archive/record-page! page-data (:db/conn database))
             page-data))))

(defn deep-merge
  "Recursively merges maps."
  [& maps]
  (letfn [(m [& xs]
            (if (some #(and (map? %) (not (record? %))) xs)
              (apply merge-with m xs)
              (last xs)))]
    (reduce m maps)))

(def initial-state
  (deep-merge write/initial-state
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
                 :no-cache true}}
               :site.fabricate.app/database
               {:db/uri archive/db-uri
                :db/conn nil}}))

(assert (m/validate write/state-schema initial-state))

(defn -main
  ([]
   (send write/state (constantly initial-state))

   (write/publish! {:dirs ["./content/"]})
   (send-off write/state write/stop!)
   (shutdown-agents)
   )

  ([& files]
   ))



(comment

  (keys write/operations)

  (do (fsm/complete write/default-operations
                    "content/design-doc-database.html.fab"
                    write/initial-state)
      (println "done"))



  (do (fsm/complete operations
                    "content/working-definition.html.fab"
                    initial-state)
      (println "done"))

  (do (fsm/complete operations
                    "content/boxed_types_libpython_clj.html.fab"
                    @write/state)
      (println "done"))

  (do (fsm/complete operations
                    "content/not-a-tree.html.fab"
                    initial-state)
      (println "done"))

  (do (fsm/complete operations
                    "content/holotype4.html.fab"
                    initial-state)
      (println "done"))

  )

(defn up! []
  (-> write/state
      (send (constantly initial-state))
      (#(do (set-error-handler!
             %
             (fn [ag ^Throwable ex]
               (.printStackTrace ex)
               ag)) %))
      (send-off (fn [{:keys [site.fabricate.app/database] :as s}]
                  (assoc-in s [:site.fabricate.app/database :db/conn]
                            (d/connect (:db/uri database)))))
      (send-off write/draft!)))

(comment

  (up!)

  (-> write/state
      (send (constantly initial-state))
      (#(do (set-error-handler!
             %
             (fn [ag ^Throwable ex]
               (.printStackTrace ex)
               ag)) %))
      (send-off (fn [{:keys [site.fabricate.app/database] :as s}]
                  (assoc-in s [:site.fabricate.app/database :db/conn]
                            (d/connect (:db/uri database)))))
      (send-off write/draft!)

      #_(send-off
         (fn [{:keys [site.fabricate/settings]
               :as application-state-map}]
           application-state-map
           #_(println "watching output dir for changes")
           #_(let [output-dir (:site.fabricate.file/output-dir settings)
                   out-dir-trailing (if (not (.endsWith output-dir "/"))
                                      (str output-dir "/") output-dir)]
               (assoc application-state-map
                      :site.fabricate.file.output/watcher
                      (watch-dir
                       (fn [{:keys [file count action]}]
                         (if (#{:create :modify} action)
                           (do
                             (println "syncing")
                             (let [r (clojure.java.shell/sh "netlify" "deploy" "--dir=public/")]
                               (println (or (:out r) (:err r)))))))
                       (io/file output-dir)))))))

  (->  write/state
       (send-off write/stop!)
       #_(send-off (fn [{:keys [site.fabricate.file.output/watcher]
                         :as application-state-map}]
                     (juxt.dirwatch/close-watcher watcher))))

  (restart-agent write/state initial-state)

  (agent-error write/state)


  (get-in write/default-site-settings )
  )


(comment

  (get-in @write/state [:site.fabricate.app/database :db/conn])

  )
