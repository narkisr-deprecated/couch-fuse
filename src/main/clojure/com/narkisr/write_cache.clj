(ns com.narkisr.write-cache
  (:use com.narkisr.byte-mangling))

(def write-cache (ref {}))

(defn update-cache [path bytes]
  (dosync
    (ref-set write-cache (if (contains? @write-cache path)
      (assoc @write-cache path (concat-bytes (@write-cache path) bytes))
      (assoc @write-cache path bytes)))))

(defn clear-cache [path]
  (dosync (ref-set write-cache (dissoc @write-cache path))))
