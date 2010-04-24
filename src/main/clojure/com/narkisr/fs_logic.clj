(ns com.narkisr.fs-logic)

(defn with-type [type x]
  (with-meta x {:type type}))

(defmacro def-fstype [name & keys]
  `(defstruct ~name :name :mode :xattrs ~@keys))

(def-fstype directory :files)
(def-fstype file :content)
(def-fstype link :link)

(defmacro create-node [type & values]
  `(with-type ~(clojure.lang.Keyword/intern type)
    (struct ~type ~@values)))

(def root
  (create-node directory "" 0755 [:description "Root directory"]
    {"README" (create-node file "README" 0644 [:description "A Readme File" :mimetype "text/plain"] (. "this is a nice readme contents" getBytes))}))

(defn lookup [path]
  (if (= path "/") root
    (let [f (java.io.File. path) parent (lookup (. f getParent))]
      (if (= (type parent) :directory) (get-in parent [:files (. f getName)])))))

(defn directory? [node] (= (type node) :directory))

(defn create-handle [metadata]
  (let [type-data {:type :filehandle}]
    (proxy [clojure.lang.IObj] []
      (withMeta [meta] (merge type-data metadata))
      (meta [] (merge type-data metadata))
      (toString [] (str "handle for " (:node metadata)))
      (finalize [] (println "finalizing")))))


(type (create-handle {:node "bla"}))