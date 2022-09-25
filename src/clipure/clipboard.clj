(ns clipure.clipboard
  (:require
   [clojure.core.async :as async]
   [clipure.db :as db])
  (:import
   (java.awt Toolkit)
   (java.awt.datatransfer DataFlavor FlavorListener StringSelection)
   (java.awt.datatransfer Clipboard)))

(set! *warn-on-reflection* true)

(defn build-ctx []
  (atom {:status :idle
         :entries []
         :flavor-changed? false}))

(defn ^:private listening?* [ctx]
  (= :listening (:status @ctx)))

(defn ^:private system-clipboard []
  (. (Toolkit/getDefaultToolkit)
     (getSystemClipboard)))

(defn ^:private get-clipboard-entry [^Clipboard clipboard]
  (try
    (.getData clipboard DataFlavor/stringFlavor)
    (catch Exception e
      (println "Error accessing clipboard entry:" e)
      nil)))

(defn ^:private listener [entry-set-fn ctx]
  (let [clipboard ^Clipboard (:clipboard @ctx)]
    (reify FlavorListener
      (flavorsChanged [_this _event]
        (when (listening?* ctx)
          (cond
            (:flavor-changed? @ctx)
            (swap! ctx assoc :flavor-changed? false)

            :else
            (do
              (swap! ctx assoc :flavor-changed? true)
              (if-let [entry (get-clipboard-entry clipboard)]
                (do
                  (.setContents clipboard (StringSelection. entry) nil)
                  (entry-set-fn entry))
                (.setContents clipboard (StringSelection. "") nil)))))))))

(defn ^:private save-new-entry! [entry ctx]
  (swap! ctx update :entries conj entry)
  (db/save-entry! ctx))

(def listening? listening?*)

(defn start-listen! [ctx]
  (let [clipboard ^Clipboard (system-clipboard)
        _ (swap! ctx assoc
                 :clipboard clipboard
                 :status :listening)
        listener (listener #(save-new-entry! % ctx) ctx)]
    (async/go
      (.addFlavorListener clipboard listener))
    ctx))

(defn history [ctx]
  (or (seq (:entries @ctx))
      (db/get-entries)))

(defn current-entry [ctx]
  (last (history ctx)))
