(ns clipure.main
  (:require
   [clipure.clipboard :as clipboard]
   [clipure.config :as config]
   [clojure.string :as string])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn ^:private parse-args [args]
  (let [args (vec args)
        vargs (reduce #(-> %1
                           (assoc %2 (or (:index %1) 0))
                           (update :index (fnil inc 0))) {} args)]
    {:help (or (contains? vargs "--help")
               (contains? vargs "-h"))
     :version (contains? vargs "--version")
     :get (when-let [pos (get vargs "get")]
            (Integer/parseInt (nth args (inc pos) "1")))
     :history (contains? vargs "history")
     :copy (when-let [pos (get vargs "copy")]
             (nth args (inc pos)))
     :listen (contains? vargs "listen")}))

(def ^:private help-msg
  "A command-line utility for manage clipboard history.

Global options:
-h, --help       List all available commands.
-v, --verbose    Show additional command output.
    --version    Return clipure version.

Available commands:
  get           Return the last saved entry to clipboard.
  history       Return the whole clipboard history.
  listen        Keep listening for clipboard changes, use as a separated process.
  copy <text>   Copy text to the clipboard.

See https://ericdallo.github.io/clipure for detailed documentation.")

(def ^:private version-msg
  (str "clipure " config/version))

(defn -main
  "Entrypoint for clipure cli."
  [& args]
  (let [{:keys [help version get history listen copy]} (parse-args args)
        ctx (clipboard/build-ctx)]
    (cond
      help
      (println help-msg)
      version
      (println version-msg)
      get
      (some-> (clipboard/current-entry get ctx) println)
      history
      (println (string/join "\n" (clipboard/history ctx)))
      copy
      (clipboard/copy copy ctx)
      listen
      (do (clipboard/start-listen! ctx)
          (println "Listening..."))
      :else
      (println help-msg))
    (while (clipboard/listening? ctx)
      (Thread/sleep 500))))
