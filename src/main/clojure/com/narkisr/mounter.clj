(ns com.narkisr.mounter
  (:gen-class)
  (:import fuse.FuseMount org.apache.commons.logging.LogFactory)
  (:use com.narkisr.couch-fs com.narkisr.couch-access clojure.contrib.command-line))

(defn valid? [cond message]
  (let [log (LogFactory/getLog (class com.narkisr.mounter))]
    (if cond
      (do (. log error message) (System/exit 1)))))

(defn validate [host path db]
  (doseq [[cond error] [[(some empty? [db path]) "db and path are mandatory."] [(not (db-exists? host db)) "db does not exists."]]]
    (valid? cond error)))

(defn -main [& args]
  (with-command-line args "Couchdb fuse filesystem 0.1"
    [[host "Couchdb host name" "http://127.0.0.1:5984/"]
     [db "Couchdb db name"]
     [path "Mount path on local filesystem"] remaining]
    (validate host db path)
    (alter-var-root #'*host* (fn [_] (identity host)))
    (alter-var-root #'*db* (fn [_] (identity db)))
    (FuseMount/mount
      (into-array [path "-f"])
      (com.narkisr.couch-fuse.) (LogFactory/getLog (class com.narkisr.mounter)))))
