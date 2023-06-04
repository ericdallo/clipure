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
  (reify FlavorListener
    (flavorsChanged [_this _event]
      (when (listening?* ctx)
        (cond
          (:flavor-changed? @ctx)
          (swap! ctx assoc :flavor-changed? false)

          :else
          (let [clipboard ^Clipboard (system-clipboard)]
            (swap! ctx assoc :flavor-changed? true)
            (if-let [entry (get-clipboard-entry clipboard)]
              (do
                (.setContents clipboard (StringSelection. entry) nil)
                (entry-set-fn entry))
              (.setContents clipboard (StringSelection. "") nil))))))))

(defn ^:private all-entries [ctx]
  (or (some-> (seq (:entries @ctx)) vec)
      (db/get-entries)))

(defn ^:private sync-atom-with-db! [ctx]
  (swap! ctx assoc :entries (db/get-entries)))

(defn ^:private save-new-entry! [entry ctx]
  (swap! ctx (fn [{:keys [entries-limit entries] :as ctx}]
               (cond-> ctx
                 (>= (count entries) entries-limit)
                 (update :entries pop)

                 (not= entry (last entries))
                 (update :entries conj entry))))
  (db/sync-db! (:entries @ctx)))

(def listening? listening?*)

(def history all-entries)

(defn current-entry [position ctx]
  (nth (reverse (all-entries ctx)) (dec position) nil))

(defn start-listen! [ctx]
  (let [clipboard ^Clipboard (system-clipboard)
        _ (swap! ctx assoc
                 :clipboard clipboard
                 :status :listening)
        listener (listener #(save-new-entry! % ctx) ctx)]
    (async/thread
      (.addFlavorListener clipboard listener))
    (async/thread
      (loop []
        (Thread/sleep 300)
        (sync-atom-with-db! ctx)
        (recur)))
    ctx))

(defn copy [text ctx]
  (sync-atom-with-db! ctx)
  (let [clipboard ^Clipboard (system-clipboard)
        selection (StringSelection. text)]
    (.setContents clipboard selection selection)
    (save-new-entry! text ctx)))

(defn clear [ctx]
  (db/sync-db! [])
  (sync-atom-with-db! ctx))
