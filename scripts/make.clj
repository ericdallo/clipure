(ns make
  (:refer-clojure :exclude [test])
  (:require
   [babashka.deps :as deps]
   [babashka.fs :as fs]
   [babashka.process :as p]))

(def clipure-bin (if (#'fs/windows?)
                   "clipure.exe"
                   "clipure"))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn clean []
  (let [files ["target"
               clipure-bin
               "clipure.jar"]]
    (doseq [f files]
      (fs/delete-tree f))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn pom []
  (-> (deps/clojure ["-T:build" "pom"] {})
      (p/check)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn uberjar []
  (-> (deps/clojure ["-T:build" "uberjar"] {})
      (p/check))
  (fs/move "target/clipure-standalone.jar" "." {:replace-existing true}))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn native-cli []
  (-> (deps/clojure ["-T:build" "native-cli"] {})
      (p/check)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn test []
  (-> (deps/clojure ["-M:test"] {})
      (p/check)))
