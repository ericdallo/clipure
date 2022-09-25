(ns clipure.main
  (:require
   [clipure.clipboard :as clipboard]
   [clipure.config :as config]
   [clojure.string :as string])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn ^:private parse-args [args]
  (let [args (reduce #(assoc %1 %2 true) {} args)]
    {:help (or (contains? args "--help")
               (contains? args "-h"))
     :version (contains? args "--version")
     :get (contains? args "get")
     :history (contains? args "history")
     :listen (contains? args "listen")}))

(def ^:private help-msg
  "A command-line utility for manage clipboard.

Global options:
-h, --help       List all available commands.
-v, --verbose    Show additional command output.
    --version    Return clipure version.

Available commands:
  get        Return the current text on clipboard.
  history    List the history of the clipboard.
  listen     Keep listening for clipboard changes.

See https://ericdallo.github.io/clipure for detailed documentation.")

(def ^:private version-msg
  (str "clipure " config/version))

(defn -main
  "Entrypoint for clipure cli."
  [& args]
  (let [{:keys [help version get history listen]} (parse-args args)]
    (cond
      help
      (println help-msg)
      version
      (println version-msg)
      get
      (println (clipboard/current-content))
      history
      (println (string/join "\n" (clipboard/history)))
      listen
      (clipboard/start-listen
        (fn [new-value]
          (println (format "Adding value to clipboard history: '%s'" new-value))))
      :else
      (println help-msg)))
  (while (clipboard/listening?)
    (Thread/sleep 250)))
