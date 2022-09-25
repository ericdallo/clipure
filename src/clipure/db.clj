(ns clipure.db
  (:require
   [clipure.config :as config]
   [clojure.java.io :as io]
   [cognitect.transit :as transit])
  (:import
   (java.io File)))

(set! *warn-on-reflection* true)

(def version 1)

(defn ^:private transit-global-db-file []
  (io/file (config/global-cache-dir) "db.transit.json"))

(defn ^:private upsert-cache! [cache cache-file]
  (try
    (with-open [;; first we write to a baos as a workaround for transit-clj #43
                bos (java.io.ByteArrayOutputStream. 1024)
                os (io/output-stream bos)]
      (let [writer (transit/writer os :json)]
        (io/make-parents cache-file)
        (transit/write writer cache)
        (io/copy (.toByteArray bos) cache-file)))
    (catch Throwable e
      (println "Error" "Could not upsert db cache" e))))

(defn ^:private read-cache [^File cache-file]
  (try
    (if (.exists cache-file)
      (let [cache (with-open [is (io/input-stream cache-file)]
                    (transit/read (transit/reader is :json)))]
        (when (= version (:version cache))
          cache))
      (println "Error" "No cache DB file found"))
    (catch Throwable e
      (println "Error" "Could not load global cache from DB" e))))

(defn save-entry! [ctx]
  (upsert-cache! {:version version
                  :entries (:entries @ctx)} (transit-global-db-file)))

(defn get-entries []
  (:entries (read-cache (transit-global-db-file))))
