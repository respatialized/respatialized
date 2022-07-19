(ns respatialized.build-test
  (:require [respatialized.build :refer :all]
            [respatialized.render :as render]
            [site.fabricate.prototype.write :as write]
            [site.fabricate.prototype.read :as read]
            [site.fabricate.prototype.fsm :as fsm]
            [malli.core :as m]
            [clojure.java.io :as io]
            [clojure.test :as t]))

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
  (dissoc operations
          write/html-state
          write/markdown-state
          write/rendered-state))

(def allowed-failures
  {"./content/holotype-blueprint.html.fab" 1})

(t/deftest compatibility
  (let [nat-f (io/file "content/not-a-tree.html.fab")]
    (t/is (= "content/not-a-tree.html.fab"
             (read/->dir-local-path nat-f)))))

(t/deftest conformance
  (let [pages  (shuffle (write/get-template-files "./content" ".fab"))]
    (doseq [p pages]
      (t/testing (str "page " p)
        (println "reading page " p)
        (let [{:keys [site.fabricate.page/title site.fabricate.page/evaluated-content]
               :or {title p}
               :as finished} (fsm/complete eval-ops p initital-state)
              errors (->> evaluated-content
                          (tree-seq vector? identity)
                          (filter eval-error?))]
          (t/is (some? evaluated-content)
                "Post should evaluate correctly")
          (t/is (or (= 0 (count errors))
                    (= (count errors) (allowed-failures p)))
                (str "Post " title " had " (count errors) " evaluation errors")))))))
