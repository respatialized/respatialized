(ns respatialized.build2-test
  (:require [respatialized.build2 :as build2]
            #_[site.fabricate.build-test :as build-test]
            [site.fabricate.api :as fabricate]
            [site.fabricate.prototype.schema :as schema]
            [site.fabricate.prototype.properties :as props]
            [malli.core :as m]
            [malli.error :as me]
            [clojure.test :as t]))

(defn- check-schema
  ([schema value msg]
   (t/is (m/validate schema value)
         (or (not-empty (str (when msg (str msg "\n"))
                             (me/humanize (m/explain schema value))))
             "value conforms to schema")))
  ([schema value] (check-schema schema value nil)))

(defn check-init-site
  [site]
  (check-schema fabricate/site-schema
                site
                "Init site should have required components")
  site)

(defn check-collected-entries
  [{:keys [::fabricate/entries] :as site}]
  (t/is (not-empty entries) "Entries should be collected")
  (doseq [e entries]
    (check-schema props/CollectedEntry
                  e
                  "Post-collect entry should have required components"))
  site)

(defn check-built-entries
  [{:keys [::fabricate/entries] :as site}]
  (doseq [e entries]
    (check-schema props/BuiltEntry
                  e
                  "Post-build entry should have required components"))
  site)

(defn check-produced-entries
  [{:keys [::fabricate/entries] :as site}]
  (doseq [e entries]
    (check-schema props/ProducedEntry
                  e
                  "Post-produce entry should have required components"))
  site)

(t/deftest build
  (t/testing "site building conforms to default Fabricate properties"
    (->> build2/init-site
         (fabricate/plan! [check-init-site])
         check-collected-entries
         (fabricate/assemble [check-built-entries])
         (fabricate/construct! [check-produced-entries]))))


(comment
  (m/form props/CollectedEntry))
