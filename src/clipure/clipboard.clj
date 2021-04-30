(ns clipure.clipboard
  (:import
   java.awt.Toolkit
   [java.awt.datatransfer
    DataFlavor
    StringSelection
    FlavorListener]))

(def ^:private flavor-changed? (atom false))

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
      (if @flavor-changed?
        (reset! flavor-changed? false)
        (do
          (reset! flavor-changed? true)
          (if-let [content  (get-content clipboard)]
            (do
              (.setContents clipboard (StringSelection. content) nil)
              (callback content))
            (.setContents clipboard (StringSelection. "") nil)))))))

(defn listen [callback]
  (let [clipboard (system-clipboard)]
    (.addFlavorListener clipboard (listener clipboard callback))))

(defn current-content []
  "")

(defn history []
  '("1" "2"))
