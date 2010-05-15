(ns com.narkisr.couch-file
  (:use com.narkisr.fs-logic com.narkisr.common-fs com.narkisr.couch-access))

(defn create-file-entry [name]
  (let [file-name (str name ".json")]
    {file-name (create-node file file-name 0644 [:description "A couch document json" :mimetype "application/json" :couch-path name] (couch-content name) (couch-size name))}))

(defn- create-document-folder [name]
  {name (create-node directory name 0755 [:description "A couch document folder"] (create-file-entry name))})

(defn couch-files []
  (reduce merge (map #(create-document-folder %) (all-ids))))

(defn fetch-content
  ([file] (-> file :content (apply [])))
  ([file f] (-> file :content (apply []) f)))

(defn fetch-size
  ([file] (-> file :size (apply []))))
