(ns clipure.config
  (:require [clojure.string :as string]
            [clojure.java.io :as io]))

(set! *warn-on-reflection* true)

(def version
  (string/trim (slurp (io/resource "CLIPURE_VERSION"))))

(defn get-env [p] (System/getenv p))
(defn get-property [p] (System/getProperty p))

(defn global-cache-dir []
  (let [cache-home (or (get-env "XDG_CACHE_HOME")
                       (io/file (get-property "user.home") ".cache"))]
    (io/file cache-home "clipure")))
