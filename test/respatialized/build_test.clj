(ns respatialized.build-test
  (:require [respatialized.build :refer :all]
            [respatialized.render :as render]
            [site.fabricate.prototype.write :as write]
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

(defn error-post? [path]
  (with-redefs [site.fabricate.prototype.write/default-site-settings
                site-settings
                site.fabricate.prototype.page/doc-header
                render/site-page-header]
    (let [eval-op (dissoc write/operations
                          write/html-state
                          write/markdown-state
                          write/rendered-state)
          fab-error? (m/validator error-schema)]
      (->> path
           (fsm/complete eval-op)
           :evaluated-content
           (tree-seq vector? identity)
           (some fab-error?)
           true?
           ))))


(def eval-ops
  (dissoc write/operations
          write/html-state
          write/markdown-state
          write/rendered-state))

(t/deftest conformance
  (let [pages (get-template-files "./content" ".fab")]
    (doseq [p pages]
      (let [{:keys [title evaluated-content]
             :or {title p}
             :as finished} (fsm/complete eval-ops p)
            errors (->> evaluated-content
                        (tree-seq vector? identity)
                        (filter eval-error?))]

        (t/testing (str title "\n" p)
          (t/is (= 0 (count errors))
                (str "Post " title " had " (count errors) " evaluation errors")))))))
