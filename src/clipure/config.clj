(ns clipure.config
  (:require [clojure.string :as string]
            [clojure.java.io :as io]))

(def version
  (string/trim (slurp (io/resource "CLIPURE_VERSION"))))
