(ns com.narkisr.couchfs.mounter
  (:gen-class)
  (:import fuse.FuseMount org.apache.commons.logging.LogFactory java.io.File)
  (:use 
    [clojure.core.match :only [match]]
    (clojure.tools (cli :only [cli]))
    (com.narkisr.couchfs couch-fs couch-access (initialization :only [init-fs-root]))))

(defn valid? [cond message]
  (when (cond) (do (println message) (System/exit 1))))

(defn validate [host db path]
  (doseq [[cond error] [[#(some empty? [db path]) "db and path are mandatory."]
                        [#(not (db-exists? host db)) "db does not exists."]
                        [#(not (-> path (File.) (. exists))) "given mount path does not exist."]]]
    (valid? cond error)))


(defn- creat-log [] (LogFactory/getLog (class com.narkisr.couchfs.mounter)))

(defn mount [host db path]
  (alter-var-root #'*host* (fn [_] (identity host)))
  (alter-var-root #'*db* (fn [_] (identity db)))
  (init-fs-root)
  (FuseMount/mount (into-array [path "-f"]) (com.narkisr.couch-fuse.) (creat-log)))

(defn mount-with-group [host db path group]
  (alter-var-root #'*host* (fn [_] (identity host)))
  (alter-var-root #'*db* (fn [_] (identity db)))
  (init-fs-root)
  (FuseMount/mount (into-array [path "-f"]) (com.narkisr.couch-fuse.) (java.lang.ThreadGroup. group) (creat-log)))

(defn -main [version & args]
    (let [[{:keys [database host path] :as options} args banner] 
          (cli args
               ["-o" "--host" "Couchdb hostname" :default "http://127.0.0.1:5984/"]
               ["-h" "--help" "help" :default false :flag true]
               ["-d" "--database" "Couch db name"]
               ["-rv" "--run-validate" "Run input validation" :default false :flag true]
               ["-p" "--path" "Mount path on local filesystem" :default ""]
               ["-v" "--version" "Print version" :default false :flag true])]
      (cond
        (options :version) (println version)
        (options :help) (println banner)
        (options :run-validate) (validate host database path)
        :else  
        (do 
          (validate host database path)
          (mount host database path)
          )))
  )

;(-main "123" "-h" "-rv" )
;(-main "123" "-v" "-rv" )
;(-main "123" "-d" "foo" "-p" "/home/ronen/temp")
