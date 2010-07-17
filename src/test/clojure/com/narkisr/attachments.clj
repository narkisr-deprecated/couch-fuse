(ns com.narkisr.attachments
  (:import java.io.File)
  (:use
    (com.narkisr (mounter :only [mount-with-group]) (couch-access :only [create-non-existing-db]))
    (clojure.contrib shell-out duck-streams test-is str-utils)))



(defn mount-and-sleep [f]
  (def uuid (java.util.UUID/randomUUID))
  (def file-path (str "fake/" uuid "/" uuid ".html"))
  (create-non-existing-db "playground")
  (mount-with-group "http://127.0.0.1:5984/" "playground" "fake" "fuse-threads")
  (java.lang.Thread/sleep 2000)
  (sh "mkdir" (str "fake/" uuid))
  (f)
  (sh "rm" "-r" (str "fake/" uuid))
  (sh "fusermount" "-u" "fake")
  )

; these tests actually mount a live couchdb therefor they require one up
(use-fixtures :once mount-and-sleep)

(deftest add-attachment
  (spit (File. file-path) "<html>hello world</html>")
  (is (= (-> (File. file-path) slurp*) "<html>hello world</html>")))
