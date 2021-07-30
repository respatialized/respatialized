(ns respatialized.render
  (:require [hiccup.page :as hp]
            [hiccup2.core :as hiccup]
            [hiccup.element :as elem]
            [hiccup.util :as util]
            [clojure.string :as string]
            [site.fabricate.prototype.page :as page]
            [flatland.ordered.set :refer [ordered-set]]
            [flatland.ordered.map :refer [ordered-map]]
            [respatialized.styles :as styles]))

(defn site-page-header
  "Returns a default header from a map with a post's metadata."
  [{:keys [title page-style scripts]}]
  (let [page-header
        (apply conj
               [:head
                [:title (str "Respatialized | " title)]
                [:meta {:charset "utf-8"}]
                [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
                [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]
                [:script {:type "text/javascript" :src "js/prism.js" :async "async"}]
                [:script {:type "text/javascript" :src "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/plugins/autoloader/prism-autoloader.min.js"}]
                [:link {:type "text/css" :href "css/fonts.css" :rel "stylesheet"}]
                [:link {:type "text/css" :href "css/main.css" :rel "stylesheet"}]]
               scripts)]
    (if page-style (conj page-header [:style page-style]) page-header)))

(defn nil-or-empty? [v]
  (if (seqable? v) (empty? v)
      (nil? v)))

(defn conj-non-nil [s & args]
  (reduce conj s (filter #(not (nil-or-empty? %)) args)))

(defn header
  "Create a structured header given the option map and child elements."
  [{:keys [date level class]
    :or {level :h1}
    :as opts} & contents]
  (let [c (if (not (map? opts)) (conj contents opts) contents)
        h (apply conj [level] c)
        d (if date [:time {:datetime date} date])]
    (conj-non-nil
     [:header]
     (if class {:class class} nil) h d)))

(defn image
  ([path annotation class]
    [:img {:src path :alt annotation :class class}])
  ([path annotation] (image path annotation styles/img-default))
  ([path] (image path "")))

(defn sorted-map-vec->table
  "Converts a vector of maps to a hiccup table."
  ([sorted-map-vec header-class row-class]
   (let [ks (keys (first sorted-map-vec))
         vs (map vals sorted-map-vec)
         get-header (fn [k] [:th k])
         get-row (fn [rv] (apply conj [:tr {:class row-class}]
                                (map (fn [v] [:td v]) rv)))]
     [:table
      [:thead (apply conj [:tr {:class header-class}] (map get-header ks))]
      (into [:tbody] (map get-row vs))]))
  ([sorted-map-vec]
   (sorted-map-vec->table sorted-map-vec
                          styles/table-header
                          styles/table-row)))

(defn sorted-map->table
  "Converts a sorted map (array of structs) to a hiccup table."
  ([smap header-class row-class]
   (into
    [:table
     [:tr {:class header-class} (map (fn [k] [:th k]) (keys smap))]]
    (map (fn row [r] [:tr {:class row-class}
                      (map (fn [i] [:td i]) r)]) (vals smap))))
  ([smap]
   (sorted-map->table smap styles/table-header styles/table-row)))

(defn vec->table
  "Converts a vector of vectors to a hiccup table. Interprets the first vector as the header row."
  [[header & rows] header-class row-class]
   (into
    [:table
     [:tr {class header-class} (map (fn [i] [:th i] header))]
     (map (fn row [r] [:tr {:class row-class}
                       (map (fn [i] [:td i]) r)]) rows)]))

(defn- ->named-row [row-name vals-map col-names]
  (let [all-vals (merge (into (ordered-map)
                              (map (fn [c] [c ""]) col-names))
                        vals-map)]
    (apply conj [:tr [:th {:scope "row"} row-name]]
           (map (fn [[k v]] [:td v]) all-vals))))

(defn- map->tbody
  ([m cols]
   (apply conj [:tbody]
          (map (fn [[k v]] (->named-row k v cols)) m)))
  ([m cols group-name]
   (apply conj [:tbody [:tr [:th {:colspan (inc (count cols))} group-name]]]
          (map (fn [[k v]] (->named-row k v cols)) m))))

(defn- ->header
  ([cols]
   [:thead
    (apply conj [:tr]
           (map (fn [i] [:th (str (name i))]) cols))]))

(defn map->table
  "Converts the map to a table. Assumes keys are row headers and values
  are maps of row entries (key:column/val:val).
  Optionally breaks up the table into multiple <tbody> elements by an
  additional attribute."
  ([m subtable-attr]
   (let [body-keys (->> m
                        vals
                        (map keys)
                        flatten
                        (into (ordered-set))
                        ((fn [i] (disj i subtable-attr))))
         header
         (->header (concat ["name"] body-keys))

         grouped-entries
         (->> m
              (group-by (fn [[e vs]] (get vs subtable-attr)))
              (map (fn [[grp ms]]
                     [grp (into {} (map (fn [[entry vs]] [entry (dissoc vs subtable-attr)]) ms))])))]
     (apply conj [:table header]
            (map (fn [[sub-val vm]]
                   (map->tbody vm
                               body-keys
                               sub-val))
                 grouped-entries))))
  ([m]
   (let [all-keys (->> m
                       vals
                       (map keys)
                       flatten
                       (into (ordered-set)))
         header (->header (concat ["name"] all-keys))]
     [:table header
      (map->tbody m all-keys)])))

(def default-grid 8)
