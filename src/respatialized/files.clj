(ns respatialized.files
  (:require [babashka.fs :as fs]))

(defn create-dir-recursive
  [target-dir]
  (let [absolute-path? (fs/absolute? target-dir)
        target-dir (if (fs/relative? target-dir)
                     (fs/relativize (fs/cwd) (fs/path (fs/cwd) target-dir))
                     target-dir)]
    (->> target-dir
         fs/components
         (reduce (fn [paths path]
                   (conj paths
                         (let [next-path (fs/path (peek paths) path)]
                           (if absolute-path?
                             (fs/absolutize (str fs/file-separator next-path))
                             next-path))))
                 [])
         (filter #(not (fs/exists? %)))
         (run! fs/create-dir))))

(defn create-dir? [d] (when-not (fs/exists? d) (create-dir-recursive d)))

(defn subpath
  ([dir p] (apply fs/path (drop 1 (fs/components (fs/relativize dir p)))))
  ([p] (subpath (fs/cwd) p)))

(comment
  (fs/parent "docs/README.md")
  (subpath "docs/path/to/some/file"))
