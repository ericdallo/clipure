(ns clipure.clipboard
  (:require
   [clipure.db :as db]
   [clojure.core.async :as async])
  (:import
   (java.awt Toolkit)
   (java.awt.datatransfer DataFlavor FlavorListener StringSelection)
   (java.awt.datatransfer Clipboard)))

(set! *warn-on-reflection* true)

(defn build-ctx []
  (atom {:status :idle
         :entries []
         ;; TODO get this from a setting
         :entries-limit 100
         :flavor-changed? false}))

(defn ^:private listening?* [ctx]
  (= :listening (:status @ctx)))

(defn ^:private system-clipboard []
  (. (Toolkit/getDefaultToolkit)
     (getSystemClipboard)))

(defn ^:private get-clipboard-entry [^Clipboard clipboard]
  (try
    (let [entry (.getData clipboard DataFlavor/stringFlavor)]
      (when (not= "" entry)
        entry))
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
  (swap! ctx (fn [{:keys [entries-limit entries] :as ctx}]
               (cond-> ctx
                 (>= (count entries) entries-limit)
                 (update :entries pop)

                 :always
                 (update :entries conj entry))))
  (db/save-entry! ctx))

(defn ^:private load-history! [ctx]
  (swap! ctx assoc :entries (vec (db/get-entries))))

(defn ^:private history* [ctx]
  (or (seq (:entries @ctx))
      (db/get-entries)))

(def listening? listening?*)

(def history history*)

(defn current-entry [ctx]
  (last (history* ctx)))

(defn start-listen! [ctx]
  (load-history! ctx)
  (let [clipboard ^Clipboard (system-clipboard)
        _ (swap! ctx assoc
                 :clipboard clipboard
                 :status :listening)
        listener (listener #(save-new-entry! % ctx) ctx)]
    (async/go
      (.addFlavorListener clipboard listener))
    ctx))
