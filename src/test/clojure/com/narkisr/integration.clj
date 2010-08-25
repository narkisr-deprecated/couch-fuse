(ns com.narkisr.integration
  (:import java.io.File)
  (:use
    (clojure.contrib.json read write)
    (com.narkisr.couchfs (mounter :only [mount-with-group]) 
                         (couch-access :only [create-non-existing-db update-document get-document]))
    (com.narkisr fs-logic (file-info :only [to-hidden]))
    (clojure.contrib shell-out duck-streams test-is str-utils)))

(def meta-file)
(def uuid)

(defn mount-and-sleep [f]
  (def uuid (java.util.UUID/randomUUID))
  (def meta-file (-> (str "fake/." uuid "/" uuid ".json")))
  (create-non-existing-db "playground")
  (mount-with-group "http://127.0.0.1:5984/" "playground" "fake" "fuse-threads")
  (java.lang.Thread/sleep 1000)
  (sh "mkdir" (str "fake/" uuid))
  (f)
  (sh "rm" "-r" (str "fake/" uuid))
  (sh "fusermount" "-u" "fake")
  )

; these tests actually mount a live couchdb therefor they require one up
(use-fixtures :once mount-and-sleep)

(defn slurp-json [] (-> meta-file (File.) slurp* read-json))

(deftest in-place-edit
  "Note that using :key won't work when assoc or dissoc since couch is saving it as a string."
    (spit (File. meta-file) (json-str (assoc (slurp-json) "key" "value")))
    (is (= ((slurp-json) "key") "value"))
    (spit (File. meta-file) (json-str (dissoc (slurp-json) "key" "value")))
    (is (= (contains? (slurp-json) "key") false))
    (is (= ((slurp-json) "_rev") ((get-document uuid) :_rev))))

(deftest file-creation-should-fail
  (let [temp (File. "fake/bla.txt")]
    (is (= (. temp exists) false))
    (is (thrown? java.io.FileNotFoundException (spit temp "{\"some\":value}")))))

(deftest meta-folder-deletion
  (is (= (-> "fake/foo/" (File.) (.mkdir)) true))
  (is (= (-> "fake/.foo/" (File.) (.exists)) true))
  (is (= (-> "fake/foo/" (File.) (.delete)) true))
  (java.lang.Thread/sleep 1000); deletion takes a bit
  (is (= (-> "fake/.foo/" (File.) (.exists)) false )))
    

(deftest mkdir-only-on-root
  (do-template (do
    (is (= (-> _1 (File.) (.mkdir)) _2))
    (is (= (sh "rm" "-r" _1) _3)))
    "fake/bla" true ""
    "fake/bla/nested" false "rm: cannot remove `fake/bla/nested': No such file or directory\n"
    ))

(deftest non-legal-json-with-recovery
    (spit meta-file (json-str (assoc (slurp-json) "key" "value")))
    (spit meta-file "blabla") ; non legal json
    (is (= ((slurp-json) "_rev") ((get-document uuid) :_rev)))
    (is (= ((slurp-json) "key") "value")))

(deftest update-conflict
  "In this test we update a document value behind the scenes, still the fs value wins out in the conflict"
    (update-document uuid (assoc (slurp-json) "key" "value1"))
    (spit meta-file (json-str (assoc (slurp-json) "key" "value2")))
    (is (= ((slurp-json) "key") "value2")))
