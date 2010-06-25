(ns com.narkisr.mounter
  (:gen-class)
  (:import fuse.FuseMount org.apache.commons.logging.LogFactory java.io.File)
  (:use com.narkisr.couch-fs com.narkisr.couch-access clojure.contrib.command-line))

(defn valid? [cond message]
  (if (cond) (do (println message) (System/exit 1))))

(defn validate [host db path]
  (doseq [[cond error] [[#(some empty? [db path]) "db and path are mandatory."]
                        [#(not (db-exists? host db)) "db does not exists."]
                        [#(not (-> path (File.) (. exists))) "given mount path does not exist."]]]
    (valid? cond error)))


(defn- creat-log [] (LogFactory/getLog (class com.narkisr.mounter)))

(defn mount [host db path]
  (alter-var-root #'*host* (fn [_] (identity host)))
  (alter-var-root #'*db* (fn [_] (identity db)))
  (bind-root)
  (FuseMount/mount (into-array [path "-f"]) (com.narkisr.couch-fuse.) (creat-log)))

(defn mount-with-group [host db path group]
  (alter-var-root #'*host* (fn [_] (identity host)))
  (alter-var-root #'*db* (fn [_] (identity db)))
  (bind-root)
  (FuseMount/mount (into-array [path "-f"]) (com.narkisr.couch-fuse.) (java.lang.ThreadGroup. group) (creat-log)))

(defn -main [version & args]
  (with-command-line args version
    [[run-valid? "runs validation only" false] [host "Couchdb host name" "http://127.0.0.1:5984/"]
     [db "Couchdb db name"] [path "Mount path on local filesystem"] remaining]
    (if run-valid? (validate host db path) (mount host db path))))
