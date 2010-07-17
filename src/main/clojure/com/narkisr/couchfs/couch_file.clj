(ns com.narkisr.couchfs.couch-file
  (:import java.io.File)
  (:use 
    (com.narkisr.couchfs couch-access)
    (com.narkisr fs-logic common-fs file-info)
    (couchdb (client :only [ResourceConflict]))
    (clojure.contrib.json read)
    (clojure.contrib error-kit)))

(defn join-maps [& maps]
  (reduce merge maps))

(defn create-file-entry [name]
  (let [file-name (str name ".json")]
    {file-name (create-node file file-name 0644 [:description "A couch document json" :mimetype "application/json" :couch-id name] (couch-content name) (couch-size name))}))

(defn- create-attachment [doc file-name details]
  (create-node file file-name 0644 [:description "A couch document attachment" :mimetype (details :content_type) :attachment true :couch-id doc]
    (couch-attachment-content doc file-name) #(details :length)))

(defn- create-file-attachments [doc]
  (reduce (fn [res [file-name details]] (assoc res file-name (create-attachment doc file-name details))) {} (attachments doc)))

(defn- create-document-folder [name]
  {name (create-node directory name 0755 [:description "A couch document folder"] (join-maps (create-file-entry name) (create-file-attachments name)))})

(defn couch-files []
  (reduce merge (map #(create-document-folder %) (all-ids))))

(defn- update-rev-and-time [path rev-map]
  (update path :_rev (rev-map :_rev))
  (update-atime path (System/currentTimeMillis)))

(defn- use-lastest-rev [path id contents]
  (update-rev-and-time path
    (update-document id (assoc contents :_rev ((get-document id) :_rev)))))

(defn- update-attachment [path couch-id file-name contents ]
  (add-attachment couch-id file-name contents "text/plain")
  (update path :content  (couch-attachment-content couch-id file-name))
  (update path :size  #(. contents length)))

(defn update-file [path file contents-str]
  (let [{:keys [couch-id attachment]} (xattr-map file)]
    (with-handler
      (if attachment 
        (update-attachment path couch-id (file :name) contents-str)
        (update-rev-and-time path (update-document couch-id (read-json contents-str))))
      (handle ResourceConflict [msg] 
              (use-lastest-rev path couch-id (read-json contents-str))))))

(defn delete-folder [path]
  (delete-document (fname path))
  (remove-file path))

(defn create-folder [path mode]
  (let [name (fname path)]
    (create-document name)
    (add-file path ((create-document-folder name) name))))

(defn create-file [path mode]
  "Adds an attachment only in memory"
  (let [id (parent-name path) name (fname path)]
    (add-attachment id name "" "text/plain")
    (add-file (file-path path) (create-attachment id name {:content_type "" :length 0}) )))

(defn fetch-content
  ([file] (-> file :content (apply [])))
  ([file f] (-> file :content (apply []) f)))

(defn fetch-size
  ([file] (-> file :size (apply []))))


