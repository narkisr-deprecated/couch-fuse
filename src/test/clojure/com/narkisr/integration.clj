(ns com.narkisr.integration
  (:import java.io.File)
  (:use
    (clojure.contrib.json read write)
    (com.narkisr (mounter :only [mount-with-group]) fs-logic (couch-access :only [create-non-existing-db]) )
    (clojure.contrib shell-out duck-streams test-is str-utils)))

(def file-path)

(defn mount-and-sleep [f]
  (let [uuid (java.util.UUID/randomUUID)]
    (def file-path (str "fake/" uuid "/" uuid ".json"))
    (create-non-existing-db "playground")
    (mount-with-group "http://127.0.0.1:5984/" "playground" "fake" "fuse-threads")
    (java.lang.Thread/sleep 2000)
    (sh "mkdir" (str "fake/" uuid))
    (f)
    (sh "rm" "-r" (str "fake/" uuid))
    (sh "fusermount" "-u" "fake")
    ))

; these tests actually mount a live couchdb therefor they require one up
(use-fixtures :once mount-and-sleep)

(deftest in-place-edit
  "Note that using :key won't work when assoc or dissoc since couch is saving it as a string."
  (let [json #(-> (File. file-path) slurp* read-json)]
    (spit (File. file-path) (json-str (assoc (json) "key" "value")))
    (is (= ((json) "key") "value"))
    (spit (File. file-path) (json-str (dissoc (json) "key" "value")))
    (is (= (contains? (json) "key") false))
    ))

(deftest file-creation-should-fail
  (let [temp (File. "fake/bla.txt")]
    (is (= (. temp exists) false))
    (is (thrown? java.io.FileNotFoundException (spit temp "{\"some\":value}")))))

(deftest mkdir-only-on-root
  (do-template (do
    (is (= (-> _1 (File.) (.mkdir)) _2))
    (is (= (sh "rm" "-r" _1) _3)))
    "fake/bla" true ""
    "fake/bla/nested" false "rm: cannot remove `fake/bla/nested': No such file or directory\n"
    ))

(deftest non-legal-json-with-recovery
  (let [json #(-> (File. file-path) slurp* read-json)]
    (spit (File. file-path) (json-str (assoc (json) "key" "value")))
    (spit (File. file-path) "blabla") ; non legal json
    (is (= ((json) "key") "value"))))

