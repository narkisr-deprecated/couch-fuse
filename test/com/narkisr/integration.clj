(ns com.narkisr.integration
  (:require [com.narkisr.protocols :as proto])
  (:import java.io.File)
  (:use
    (com.narkisr.couchfs  (couch-access :only [update-document get-document]))
    (com.narkisr fs-logic common-test)
    (clojure.contrib shell-out test-is str-utils (duck-streams :only [slurp*]) (json :only [read-json json-str]))))

; these tests actually mount a live couchdb therefor they require one up
(use-fixtures :once mount-and-sleep)

(defn slurp-json [] (-> meta-file (File.) slurp* read-json))

(deftest empty-ls
  (is (= (sh "ls" "fake") (str uuid "\n"))))

(deftest file-creation-should-fail
  (let [temp (File. "fake/bla.txt")]
    (is (= (. temp exists) false))
    (is (thrown? java.io.FileNotFoundException (spit temp "{\"some\":value}")))))

(deftest in-place-edit
  "Note that using :key won't work when assoc or dissoc since couch is saving it as a string."
    (spit (File. meta-file) (json-str (assoc (slurp-json) :key "value")))
    (is (= ((slurp-json) :key) "value"))
    (spit (File. meta-file) (json-str (dissoc (slurp-json) :key "value")))
    (is (= (contains? (slurp-json) :key) false))
    (is (= ((slurp-json) :_rev) ((get-document uuid) :_rev))))

(deftest meta-folder-deletion
  (is (= (-> "fake/foo/" (File.) (.mkdir)) true))
  (is (= (-> "fake/.foo/" (File.) (.exists)) true))
  (is (= (-> "fake/.foo/" (File.) (.delete)) false))
  (is (= (-> "fake/foo/" (File.) (.delete)) true))
  (is (= (-> "fake/.foo/" (File.) (.delete)) true))
  (is (= (sh "ls" "fake") (str uuid "\n"))))

(deftest folder-deletion-bash
  (is (= (sh "mkdir" "fake/foo") ""))
  (is (= (sh "rm" "-r" "fake/foo") ""))
  (is (= (sh "rm" "-r" "fake/.foo") "")))

(deftest mkdir-only-on-root
  (do-template 
    (do
     (is (= (-> _1 (File.) (.mkdir)) _2))
     (is (= (sh "rm" "-r" _1) _3))
     (sh "rm" "-r" _4))
     "fake/bla" true "" "fake/.bla"
     "fake/bla/nested" false "rm: cannot remove `fake/bla/nested': No such file or directory\n" ""
    ))

(deftest non-legal-json-with-recovery
    (spit meta-file (json-str (assoc (slurp-json) :key "value")))
    (spit meta-file "blabla") ; non legal json
    (is (= ((slurp-json) :_rev) ((get-document uuid) :_rev)))
    (is (= ((slurp-json) :key) "value")))

(deftest update-conflict
  "In this test we update a document value behind the scenes, still the fs value wins out in the conflict"
    (update-document uuid (assoc (slurp-json) :key "value1"))
    (spit meta-file (json-str (assoc (slurp-json) :key "value2")))
    (is (= ((slurp-json) :key) "value2")))

