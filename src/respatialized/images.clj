(ns respatialized.images
  "Namespace for working with image resources"
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [malli.core :as m]
            [clojure.edn :as edn]
            [byte-streams :as byte-streams])
  (:import [java.net URI]
           [java.time Instant]
           [java.util UUID HexFormat]
           [javax.imageio ImageIO ImageReader ImageWriter]
           [javax.imageio.metadata IIOMetadata]
           [java.security MessageDigest DigestInputStream]))

(def image-metadata-schema
  (m/schema
   [:map {:registry {::img-url (m/schema :string)}} [:image/hash :string]
    [:image/source-url [:schema [:ref ::img-url]]]
    [:image/relative-url {:optional true} :string]
    [:image/library-file :string] [:image/remote-url {:optional true} :string]
    [:image/download-time {:optional true} inst?]]))

(def lookup-schema
  (m/schema [:map-of {:registry {::img-url (m/schema :string)}}
             [:schema [:ref ::img-url]] image-metadata-schema]))

(def media-dir (str (fs/path (System/getProperty "user.dir") "public/media")))

(comment
  (m/validate 'uri? "https://example.com"))

(def lookup (atom {}))


(def sha-256 (MessageDigest/getInstance "SHA-256"))
(def hex-format (HexFormat/of))

(defn img-data->uuid
  "Generate a UUID from the byte array of the image."
  [img-data]
  (-> img-data
      byte-streams/to-byte-array
      (UUID/nameUUIDFromBytes)))

(defn img-data->sha256
  [img-data]
  (->> img-data
       byte-streams/to-byte-array
       (.digest sha-256)
       (.formatHex hex-format)))


;; basic logic:
;; take the URL, and see if it's been recorded in the lookup table
;; if it has, return the URL from the lookup table
;; otherwise, download and record it

(defn img-url->data
  [url lookup]
  (or (get lookup url)
      (with-open [s (io/input-stream url)]
        (let [byte-data (byte-streams/to-byte-array s)
              uuid (img-data->uuid byte-data)
              lookup-result (first (filter #(= uuid (:image/uuid %))
                                           (vals lookup)))]
          (or lookup-result
              (let [hash-256 (img-data->sha256 byte-data)
                    ext (fs/extension url)]
                {:image/source-url url,
                 :image/library-file (str (fs/path (System/getProperty
                                                    "user.img-dir")
                                                   (str uuid "." ext))),
                 :image/uuid uuid,
                 :image/sha256 hash-256,
                 :image/bytes byte-data}))))))

(defn download!
  "Idempotent image download function."
  [{:keys [image/library-file], :as image-data}]
  (let [lf (fs/file library-file)]
    (if (and (not (fs/exists? lf)) (some? (:image/bytes image-data)))
      (let [dt (Instant/now)]
        (byte-streams/transfer (:image/bytes image-data) lf)
        (-> image-data
            (assoc :image/download-time dt)
            (dissoc :image/bytes)))
      image-data)))

(defn relativize
  [file-path]
  (str (fs/relativize (fs/parent media-dir) file-path)))

(defn absolutize
  [abs-file-path]
  (if (fs/absolute? abs-file-path)
    abs-file-path
    (str (fs/path (System/getProperty "user.dir")
                  (subs (str abs-file-path) 1)))))

(comment
  (fs/absolute? (absolutize "/media/img/something.jpg")))


(defn copy!
  [image-data]
  (let [{:keys [image/library-file], :as image-data} (download! image-data)
        canonical-local-file (str (fs/path media-dir
                                           (fs/file-name library-file)))
        local-file-rel (str "/" (relativize canonical-local-file))]
    (when-not (fs/exists? canonical-local-file)
      (fs/copy library-file canonical-local-file))
    (-> image-data
        (assoc :image/relative-url local-file-rel)
        (dissoc :image/bytes))))

(comment
  (fs/relativize "public" "public/media")
  (fs/extension "https://mdl.artvee.com/sftb/101051ab.jpg")
  (img-url->data "https://mdl.artvee.com/sftb/101051ab.jpg"))

(defn sync-file!
  "Send update to file, if the state has changed."
  [file old-state new-state]
  (when-not (= old-state new-state)
    (locking file (spit file (pr-str new-state)))))

(defn open-lookup-map!
  "Return an atom that periodically writes to disk on update."
  [file]
  (let [a (atom (if (fs/exists? file) (edn/read-string (slurp file)) {}))]
    (add-watch a :update (fn [_k _u old new] (sync-file! file old new)))
    a))

(defonce lookup-state
  (open-lookup-map! (fs/file (fs/path (System/getProperty "user.img-dir")
                                      "metadata.edn"))))

(defn update!
  "Add the image at URL to the lookup map, if it hasn't been added already"
  [lookup-map url]
  (let [data (-> url
                 (img-url->data lookup-map)
                 copy!)]
    (assoc lookup-map url data)))

(defn get!
  "Return the image data for the given URL"
  [url]
  (let [updated (swap! lookup-state update! url)] (get updated url)))

(comment
  (fs/relativize)
  (get! "https://mdl.artvee.com/sftb/101051ab.jpg"))
