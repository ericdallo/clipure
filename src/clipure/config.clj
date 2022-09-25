(ns clipure.config
  (:require [clojure.string :as string]
            [clojure.java.io :as io]))

(set! *warn-on-reflection* true)

(def version
  (string/trim (slurp (io/resource "CLIPURE_VERSION"))))
