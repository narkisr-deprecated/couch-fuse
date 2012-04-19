(ns com.narkisr.couchfs.file-update
  (:use 
     (com.narkisr.couchfs (couch-access :only [couch-attachment-content add-attachment update-document get-document]))
     (com.narkisr (fs-logic :only [update update-atime]))))

(defn update-rev-and-time [path rev-map]
  (update path :_rev (rev-map :_rev))
  (update-atime path (System/currentTimeMillis)))

(defn use-lastest-rev [path id contents]
  (update-rev-and-time path 
    (update-document id (assoc contents :_rev ((get-document id) :_rev)))))

(defn update-attachment [path couch-id file-name contents ]
  (add-attachment couch-id file-name contents "text/plain")
  (update path :content  (couch-attachment-content couch-id file-name))
  (update path :size  #(. contents length)))


