(ns clipure.main
  (:require
   [clipure.clipboard :as clipboard]
   [clipure.config :as config]
   [clojure.string :as string])
  (:gen-class))

(defn ^:private parse-args [args]
  (let [args (reduce #(assoc %1 %2 true) {} args)]
    {:help (or (contains? args "--help")
               (contains? args "-h"))
     :version (contains? args "--version")
     :get (contains? args "get")
     :history (contains? args "history")}))

(def ^:private help-msg
  "A command-line utility for manage clipboard.

Global options:
-h, --help       List all available commands.
-v, --verbose    Show additional command output.
    --version    Return clipure version.

Available commands:
  get        Return the current text on clipboard.
  history    List the history of the clipboard.

See https://ericdallo.github.io/clipure for detailed documentation.")

(def ^:private version-msg
  (str "clipure " config/version))

(defn -main
  "Entrypoint for clipure cli."
  [& args]
  (let [{:keys [help version get history]} (parse-args args)]
    (cond
      help
      (println help-msg)
      version
      (println version-msg)
      get
      (println (clipboard/current-content))
      history
      (println (string/join "\n" (clipboard/history)))
      :else
      (println help-msg)))
  #_(clipboard/listen (fn [value]
                      (println "--->" value)))
  #_(while true
    (Thread/sleep 250)))
