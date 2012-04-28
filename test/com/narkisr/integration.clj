(ns com.narkisr.integration
  (:require [com.narkisr.protocols :as proto])
  (:import java.io.File)
  (:use
    [clojure.test :only [use-fixtures deftest is are]]
    [cheshire.core :only [parse-string generate-string]]
    [clojure.java.shell :only [sh]]
    (com.narkisr.couchfs  
      [couch-access :only [update-document get-document]])
    (com.narkisr fs-logic common-test)))

; these tests actually mount a live couchdb therefor they require one up
(use-fixtures :once mount-and-sleep)

(defn slurp-json [] (-> meta-file slurp (parse-string true)))

(deftest empty-ls
  (assert-sh "ls" "fake" :out (str uuid "\n"))
  )

(deftest file-creation-should-fail
  (let [temp (File. "fake/bla.txt")]
    (is (= (. temp exists) false))
    (is (thrown? java.io.FileNotFoundException (spit temp "{\"some\":value}")))))

(deftest in-place-edit
  "Note that using :key won't work when assoc or dissoc since couch is saving it as a string."
    (spit (File. meta-file) (generate-string (assoc (slurp-json) :key "value")))
    (is (= ((slurp-json) :key) "value"))
    (spit (File. meta-file) (generate-string (dissoc (slurp-json) :key "value")))
    (is (= (contains? (slurp-json) :key) false))
    (is (= ((slurp-json) :_rev) ((get-document uuid) :_rev))))

(deftest meta-folder-deletion
  (is (= (-> "fake/foo/" (File.) (.mkdir)) true))
  (is (= (-> "fake/.foo/" (File.) (.exists)) true))
  (is (= (-> "fake/.foo/" (File.) (.delete)) false))
  (is (= (-> "fake/foo/" (File.) (.delete)) true))
  (is (= (-> "fake/.foo/" (File.) (.delete)) true))
  (is (= (assert-sh "ls" "fake" :out (str uuid "\n")))))

(deftest folder-deletion-bash
  (assert-sh "mkdir" "fake/foo" :out "") 
  (assert-sh "rm" "-r" "fake/foo" :out "")
  (assert-sh "rm" "-r" "fake/.foo" :out ""))

(deftest mkdir-only-on-root
  (are [_1 _2 _3 _4]
    (do
     (is (= (-> _1 (File.) (.mkdir)) _2))
     (assert-sh "rm" "-r" _1 :err _3)
     (sh "rm" "-r" _4))
     "fake/bla" true "" "fake/.bla"
     "fake/bla/nested" false "rm: cannot remove `fake/bla/nested': No such file or directory\n" ""
    ))

(deftest non-legal-json-with-recovery
    (spit meta-file (generate-string (assoc (slurp-json) :key "value")))
    (spit meta-file "blabla") ; non legal json
    (is (= ((slurp-json) :_rev) ((get-document uuid) :_rev)))
    (is (= ((slurp-json) :key) "value")))

(deftest update-conflict
  "In this test we update a document value behind the scenes, still the fs value wins out in the conflict"
    (update-document uuid (assoc (slurp-json) :key "value1"))
    (spit meta-file (generate-string (assoc (slurp-json) :key "value2")))
    (is (= ((slurp-json) :key) "value2")))

