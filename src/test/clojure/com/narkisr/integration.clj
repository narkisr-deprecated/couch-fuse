(ns com.narkisr.integration
  (:import java.io.File)
  (:use
    (clojure.contrib.json read write)
    (com.narkisr (mounter :only [mount-with-group]))
    (clojure.contrib shell-out duck-streams test-is str-utils)))

(def file-path "/media/SSD_DRIVE/CodeProjects/couch-fuse/fake/1432286694230195736/1432286694230195736.json")

(defn mount-and-sleep [f]
  (println "mounting fake")
  (mount-with-group "http://127.0.0.1:5984/" "playground" "fake" "fuse-threads")
  (java.lang.Thread/sleep 2000)
  (f)
  (println "unmounting fake")
  (sh "fusermount" "-u" "fake")
  )

; these tests actually mount a live couchdb therefor they require one up
(use-fixtures :once mount-and-sleep)

(defn- echoable-json [value]
  (str "\"" (re-gsub #"\"" "'" (json-str value)) "\""))

(deftest in-place-edit
  "Note that using :key won't work when assoc or dissoc since couch is saving it as a string."
  (let [json #(-> (File. file-path) slurp* read-json)]
    (spit (File. file-path) (json-str (assoc (json) "key" "value")))
    (is (= ((json) "key") "value"))
    (spit (File. file-path) (json-str (dissoc (json) "key" "value")))
    (is (= (contains? (json) "key") false))
    ))


