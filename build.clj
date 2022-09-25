(ns build
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.tools.build.api :as b]))

(def lib 'com.github.ericdallo/clipure)
(def current-version (string/trim (slurp (io/resource "CLIPURE_VERSION"))))
(def class-dir "target/classes")
(def basis {:project "deps.edn"})
(def uber-file (format "target/%s-standalone.jar" (name lib)))

(defn clean [_]
  (b/delete {:path "target"}))

(defn pom [opts]
  (b/write-pom {:target ""
                :lib lib
                :version current-version
                :basis (b/create-basis (update basis :aliases concat (:extra-aliases opts)))
                :src-dirs ["src"]
                :resource-dirs ["resources"]
                :scm {:tag current-version}}))

(defn ^:private uber [opts]
  (clean opts)
  (pom opts)
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :main 'clipure.main
           :basis (b/create-basis (update basis :aliases concat (:extra-aliases opts)))}))

(defn ^:private uber-aot [opts]
  (clean opts)
  (println "Building uberjar...")
  (let [basis (b/create-basis (update basis :aliases concat (:extra-aliases opts)))
        src-dirs (into ["src" "resources"] (:extra-dirs opts))]
    (b/copy-dir {:src-dirs src-dirs
                 :target-dir class-dir})
    (b/compile-clj {:basis basis
                    :src-dirs src-dirs
                    :java-opts ["-server"]
                    :class-dir class-dir})
    (b/uber {:class-dir class-dir
             :uber-file uber-file
             :main 'clipure.main
             :basis basis})))

(def uberjar uber-aot)

(defn uberjar-for-native [opts]
  (uber-aot (merge opts {:extra-aliases [:native]})))

(defn native-cli [opts]
  (println "Building native image...")
  (if-let [graal-home (System/getenv "GRAALVM_HOME")]
    (let [jar (or (System/getenv "CLIPURE_JAR")
                  (do (uberjar-for-native opts)
                      uber-file))
          native-image (if (fs/windows?) "native-image.cmd" "native-image")
          command (->> [(str (io/file graal-home "bin" native-image))
                        "-jar" jar
                        "clipure"
                        "-H:+ReportExceptionStackTraces"
                        "--verbose"
                        "--no-fallback"
                        "--native-image-info"
                        (or (System/getenv "CLIPURE_XMX")
                            "-J-Xmx8g")
                        (when (= "true" (System/getenv "CLIPURE_STATIC"))
                          ["--static"
                           (if (= "true" (System/getenv "CLIPURE_MUSL"))
                             ["--libc=musl" "-H:CCompilerOption=-Wl,-z,stack-size=2097152"]
                             ["-H:+StaticExecutableWithDynamicLibC"])])]
                       (flatten)
                       (remove nil?))
          {:keys [exit]} (b/process {:command-args command})]
      (when-not (= 0 exit)
        (System/exit exit)))
    (println "Set GRAALVM_HOME env")))
