(ns clipure.main
  (:require
   [clipure.clipboard :as clipboard]
   [clipure.config :as config]
   [clojure.string :as string])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn ^:private parse-args [args]
  (let [vargs (reduce #(assoc %1 %2 true) {} args)]
    {:help (or (contains? vargs "--help")
               (contains? vargs "-h"))
     :version (contains? vargs "--version")
     :get (contains? vargs "get")
     :history (contains? vargs "history")
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
  paste <text>  Paste text to current cursor position.

See https://ericdallo.github.io/clipure for detailed documentation.")

(def ^:private version-msg
  (str "clipure " config/version))

(defn -main
  "Entrypoint for clipure cli."
  [& args]
  (let [{:keys [help version get history listen]} (parse-args args)
        ctx (clipboard/build-ctx)]
    (cond
      help
      (println help-msg)
      version
      (println version-msg)
      get
      (println (clipboard/current-entry ctx))
      history
      (println (string/join "\n" (clipboard/history ctx)))
      listen
      (do (clipboard/start-listen! ctx)
          (println "Listening..."))
      :else
      (println help-msg))
    (while (clipboard/listening? ctx)
      (Thread/sleep 200))))
