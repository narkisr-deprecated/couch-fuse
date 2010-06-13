(ns com.narkisr.couch-file
  (:import java.io.File)
  (:use com.narkisr.fs-logic com.narkisr.common-fs com.narkisr.couch-access))

(defn join-maps [& maps]
  (reduce merge maps))

(defn create-file-entry [name]
  (let [file-name (str name ".json")]
    {file-name (create-node file file-name 0644 [:description "A couch document json" :mimetype "application/json" :couch-id name] (couch-content name) (couch-size name))}))

(defn- create-file-attachments [doc]
  (reduce (fn [res [file-name details]]
    (assoc res file-name
      (create-node file file-name 0644 [:description "A couch document attachment" :mimetype (details :content_type) :attachment true]
        (couch-attachment-content doc file-name) #(details :length)))) {} (attachments doc)))

(defn- create-document-folder [name]
  {name (create-node directory name 0755 [:description "A couch document folder"] (join-maps (create-file-entry name) (create-file-attachments name)))})

(defn couch-files []
  (reduce merge (map #(create-document-folder %) (all-ids))))

(defn update-file [file contents]
  (let [attrs (apply hash-map (file :xattrs))]
    (update-document (attrs :couch-id) contents)))

(defn create-folder [path mode]
  (let [name (-> path (File.) (.getName))]
    (create-document name)
    (add-file path (create-document-folder name))))

(defn fetch-content
  ([file] (-> file :content (apply [])))
  ([file f] (-> file :content (apply []) f)))

(defn fetch-size
  ([file] (-> file :size (apply []))))

(defn attachment? [node] (node :attachment))
