(ns respatialized.build-test
  (:require [respatialized.build :refer :all]
            [respatialized.render :as render]
            [respatialized.archive :as archive]
            [site.fabricate.prototype.write :as write]
            [site.fabricate.prototype.read :as read]
            [site.fabricate.prototype.fsm :as fsm]
            [malli.core :as m]
            [asami.core :as d]
            [clojure.java.io :as io]
            [clojure.test :as t]))

(def test-db-uri "asami:mem://respatialized-test")
(declare conn)
(def test-state (agent initial-state))

(defn db-fixture [f]
  (do
    (defonce conn (d/connect test-db-uri))
    (d/create-database test-db-uri)
    (send test-state
          (fn [{:keys [site.fabricate.app/database]
                :as state}]
            (assoc state :site.fabricate.app/database {:db/uri test-db-uri
                                                       :db/conn conn})))

    (f)
    (d/delete-database test-db-uri)))

(t/use-fixtures :once db-fixture)

(def error-schema
  [:catn
   [:div [:enum :div]]
   [:header [:enum [:h6 "Error"]]]
   [:enumeration
    [:schema
     [:catn
      [:dl [:enum :dl]]
      [:type-header [:enum [:dt "Error type"]]]
      [:type [:schema [:cat [:enum :dd]
                       [:schema
                        [:cat [:enum :code] :string]]]]]
      [:msg-header [:enum [:dt "Error message"]]]
      [:msg [:schema [:cat [:enum :dd]
                      [:schema
                       [:cat [:enum :code] :string]]]]]
      [:phase-header [:enum [:dt "Error phase"]]]
      [:phase [:schema [:cat [:enum :dd]
                        [:schema
                         [:cat [:enum :code] :string]]]]]]]]
   [:source
    [:schema [:cat [:enum :details]
              [:enum [:summary "Source expression"]]
              [:schema [:cat [:enum :pre] [:schema [:cat [:enum :code] :string]]]]]]]])

(def eval-error? (m/validator error-schema))

(defn error-post? [path {:keys [site.fabricate/settings]
                         :as application-state-map}]
  (let [eval-op (dissoc (:site.fabricate.file/operations settings)
                        write/html-state
                        write/markdown-state
                        write/rendered-state)
        fab-error? (m/validator error-schema)]
    (->> path
         (fsm/complete eval-op application-state-map)
         :site.fabricate.page/evaluated-content
         (tree-seq vector? identity)
         (some fab-error?)
         true?)))

(def eval-ops
  (dissoc
   site.fabricate.prototype.write/default-operations
   write/html-state
   write/markdown-state
   write/rendered-state))

(def allowed-failures
  {"./content/holotype-blueprint.html.fab" 1})

(t/deftest compatibility
  (let [nat-f (io/file "content/not-a-tree.html.fab")]
    (t/is (= "content/not-a-tree.html.fab"
             (str (read/->dir-local-file nat-f))))))

(def expensive-pages
  #{"./content/holotype4.html.fab"
    "./content/sketchbook/extruder.html.fab"
    "./content/color-extraction.html.fab"})

(t/deftest conformance
  (let [pages
        (->>  (write/get-template-files "./content" ".fab")
              (filter #(and (not (expensive-pages %)) true))
              (shuffle))]
    (doseq [p pages]
      (t/testing (str "page " p)
        (println "reading page " p)
        (let [{:keys [site.fabricate.page/title site.fabricate.page/evaluated-content]
               :or {title p}
               :as finished} (fsm/complete eval-ops p initial-state)
              errors (->> evaluated-content
                          (tree-seq vector? identity)
                          (filter eval-error?))]
          (t/is (some? evaluated-content)
                "Post should evaluate correctly")
          (t/is (or (= 0 (count errors))
                    (= (count errors) (allowed-failures p)))
                (str "Post " title " had " (count errors) " evaluation errors")))))))

(t/deftest archive

  (t/testing "ability to skip unmodified pages"
    (d/delete-database test-db-uri)
    (d/create-database test-db-uri)

    (def conn (d/connect test-db-uri))
    (let [ops
          (assoc
           operations
           write/rendered-state
           (fn [page-data {:keys [site.fabricate.app/database]}]
             (println "recording page in DB")
             (let [id
                   (archive/record-page! page-data (:db/conn database))]
               (println "id of recorded page:" id))
             page-data))

          example-file "content/ai-and-labor.html.fab"]
      ;; TODO: figure out how to make this idempotent
      ;; and test that nothing is recorded even though
      ;; the results are identical - purely functional page renders

      (let [result1 (fsm/complete
                     ops
                     example-file
                     @test-state)]

        (t/is (some? (archive/record-page! result1 conn)))

        (let [r-data (archive/file->revision
                      (:site.fabricate.file/input-file result1)
                      (get-in @test-state
                              [:site.fabricate.app/database
                               :db/conn]))]
          (clojure.pprint/pprint r-data)
          (t/is (some? r-data)))
        (t/is (contains?
               result1
               :site.fabricate.page/evaluated-content))

        (let [result2 (fsm/complete
                       ops
                       example-file
                       @test-state)]
          (t/is (not (new-page? result1 @test-state)))

          (Thread/sleep 600)
          (t/is (not= result1 result2)))))))
