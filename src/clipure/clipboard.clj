(ns clipure.clipboard
  (:import
   java.awt.Toolkit
   [java.awt.datatransfer
    DataFlavor
    StringSelection
    FlavorListener])
  (:require
    [clojure.core.async :as async]))

(defonce ctx (atom {:status :idle
                    :flavor-changed? false}))

(defn ^:private system-clipboard []
  (. (Toolkit/getDefaultToolkit)
     (getSystemClipboard)))

(defn ^:private get-content [clipboard]
  (try
    (.getData clipboard DataFlavor/stringFlavor)
    (catch Exception e
      (println "Error accessing clipboard content:" e)
      nil)))

(defn ^:private listener [clipboard callback]
  (reify FlavorListener
    (flavorsChanged [this event]
      (when (= :listening (:status @ctx))
        (cond
          (:flavor-changed? @ctx)
          (swap! ctx assoc :flavor-changed? false)

          :else
          (do
            (swap! ctx assoc :flavor-changed? true)
            (if-let [content  (get-content clipboard)]
              (do
                (.setContents clipboard (StringSelection. content) nil)
                (callback content))
              (.setContents clipboard (StringSelection. "") nil))))))))

(defn listening? []
  (= :listening (:status @ctx)))

(defn start-listen [callback]
  (swap! ctx assoc :status :listening)
  (async/go
    (let [clipboard (system-clipboard)]
      (.addFlavorListener clipboard (listener clipboard callback)))))

(defn stop-listen []
  (swap! ctx assoc :status :idle))

(defn current-content []
  "")

(defn history []
  '("1" "2"))
