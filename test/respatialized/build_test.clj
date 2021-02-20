(ns respatialized.build-test
  (:require [respatialized.build :refer :all]
            [clojure.test :as t]))

(t/deftest page-parsing
  (t/testing "string parsing"
    (t/is (= :respatialized.build/parse-error
             (template-str->hiccup "<%=((+ 3 4)%>")))

    (t/is (= [:div 7]
             (template-str->hiccup "<%=(+ 3 4)%>")))))
