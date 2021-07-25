(ns respatialized.build
  (:require
   [clojure.java.io :as io]
   [hiccup2.core :as hiccup]
   [hiccup.page :as hp]
   [site.fabricate.prototype.html :as html]
   [site.fabricate.prototype.fsm :as fsm]
   [site.fabricate.prototype.write :as write]
   [site.fabricate.prototype.page :refer :all]
   [juxt.dirwatch :refer [close-watcher]]
   [respatialized.css :as css]
   [respatialized.render :as render :refer :all]
   [respatialized.holotype :as holotype]
   [clojure.string :as str]))

(defn get-template-files [dir suffix]
  (->> dir
       io/file
       file-seq
       (filter #(and (.isFile %)
                     (not (.isDirectory %))
                     (.endsWith (.toString %) suffix)))
       (map #(.toString %))))

(comment
  ;; store the post data and the html and only update it if
  ;; it renders without errors

  ;; the simplest implementation: just print the error and rerender with
  ;; the last known state
  ;; from there, we can begin to think about how to store exceptions
  ;; as values representing state
  ;; Throwable->map will probably help with this
  (def example-post-map
    {"./content/holotype1.html.ct"
     {:data [#_ "post data"]
      :html "<doctype html>..."  ; not a requirement but may improve performance
      }})

  ;; after the initial implementation, other ideas are possible
  ;; 1. print exceptions in the rendered page by
  ;; 2. split up the successfully eval'd fns from the errors
  ;; 3. begin saving a succession of successful renders in a database with commits as "checkpoints"
  ;; 4. use the state map to drive a nicer terminal UI (e.g. 10/11 files rendered ...)
  ;; 5. use the state map to drive a web UI that leverages htmx to display rich information about the state of the site being built
  )

(def site-settings
  {:template-suffix ".fab"
   :input-dir "./content"
   :output-dir "./public"})



(defn -main
  ([]
   (with-redefs [site.fabricate.prototype.write/default-site-settings
                site-settings
                site.fabricate.prototype.page/doc-header
                site-page-header]
     (write/publish {:dirs ["./content/"]})
    )
   )
  ([& files]
   ))

(comment

  )

(comment

  (alter-var-root #'site.fabricate.prototype.write/default-site-settings
                  (constantly site-settings))

  (alter-var-root #'site.fabricate.prototype.page/doc-header
                  (constantly site-page-header))

  (def drafts
    (write/draft)
    )

  (close-watcher drafts)

  (def completed-posts
    (with-redefs [site.fabricate.prototype.write/default-site-settings
                  site-settings
                  site.fabricate.prototype.page/doc-header
                  site-page-header]
      (->> (get-template-files "./content" ".fab")
           (map (fn [p]
                  [p (fsm/complete write/operations p)]))
           (into {}))))

  )
