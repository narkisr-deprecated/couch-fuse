(ns com.narkisr.couchfs.couch-file
  (:use 
     (com.narkisr.couchfs couch-access file-update)
     (com.narkisr fs-logic common-fs file-info)
     (couchdb (client :only [ResourceConflict]))
     (clojure.contrib.json read)
     (clojure.contrib error-kit)))

(defn join-maps [& maps]
  (reduce merge maps))

(defn- create-inner-file [file-name desc mime content-fn size-fn couch-id ]
  (create-node file file-name 0644 [:description desc :mimetype mime :couch-id couch-id] content-fn size-fn))

(defn create-file-entry [couch-id]
  (let [file-name (str couch-id ".json")]
    {file-name (create-inner-file file-name "A Couchdb json document" "application/json" (couch-content couch-id) (couch-size couch-id) couch-id )}))

(defn- create-attachment [couch-id file-name details]
  (with-xattr [:attachment true]
              (create-inner-file file-name  "A Couchdb document attachment" (details :content_type) (couch-attachment-content couch-id file-name) #(details :length) couch-id) ))

(defn- create-file-attachments [couch-id]
  (reduce (fn [res [file-name details]] (assoc res file-name (create-attachment couch-id file-name details))) {} (attachments couch-id)))

(defn- create-document-folder [couch-id]
  (let [hidden (str "." couch-id)]
    (merge {hidden (with-xattr [:meta-folder true]
                     (create-node directory hidden 0444 [:description "Couch meta folder"] (create-file-entry couch-id)))}
           {couch-id (create-node directory couch-id 0755 [:description "Couch attachments folder"]  (create-file-attachments couch-id))})))


(defn couch-files []
  (reduce merge (map #(create-document-folder %) (all-ids))))

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
  (remove-file path)
  (remove-file (to-hidden path)))

(defn create-folder [path mode]
  (let [couch-id (fname path) parent (parent-path path)]
    (create-document couch-id)
    (doseq [[k v] (create-document-folder couch-id)]
      (add-file (combine parent k) v))))

(defn create-file [path mode]
  "Adds an empty attachment to the given document path, update file will fill the missing data"
  (let [couch-id (parent-name path) attach-id (fname path)]
    (add-attachment couch-id attach-id "" "text/plain")
    (add-file (file-path path) (create-attachment couch-id attach-id {:content_type "" :length 0}) )))

(defn delete-file [path]
  "Deletes only attachments"
  (let [couch-id (parent-name path) attach-id (fname path)]
    (delete-attachment couch-id attach-id)
    (remove-file path)))

(defn fetch-content
  ([file] (-> file :content (apply [])))
  ([file f] (-> file :content (apply []) f)))

(defn fetch-size
  ([file] (-> file :size (apply []))))

(defn rename-file [from to]
  "This expensive, couch does not have a rename built in https://issues.apache.org/jira/browse/COUCHDB-715"
  (let [content (String. (fetch-content (lookup from)))]
    (delete-file from)
    (create-file to 0644)
    (update-file to (lookup to) content)))
