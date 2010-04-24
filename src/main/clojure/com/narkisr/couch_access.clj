(ns com.narkisr.couch-access
  (:refer-clojure :exclude [contains?])
  (:use
    (com.narkisr fs-logic)
    (couchdb client)
    (clojure.contrib stream-utils str-utils seq-utils (str-utils2 :only [blank? contains?]) error-kit)
    ))

(def *host* "http://127.0.0.1:5984/")
(def *db* "fuse")

(defmacro couch [fn & params]
  `(~fn ~*host* ~*db* ~@params))

(defn- not-design-id [id]
  (not (contains? id "design")))

(defn- all-ids []
  (lazy-seq (filter not-design-id (couch document-list))))

(defn- create-file-entry [name contents]
  {name (create-node file name 0644 [:description "A couch doc" :mimetype "text/plain"] (. (str contents) getBytes))})

(defn couch-files []
  (reduce merge (map #(create-file-entry % (couch document-get %)) (all-ids))))

