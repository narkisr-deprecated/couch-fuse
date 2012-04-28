(ns com.narkisr.fs-logic-test
  (:use 
    [clojure.test :only [use-fixtures deftest is]]
    com.narkisr.fs-logic)
  (:import 
    com.narkisr.protocols.Root
    com.narkisr.protocols.File))

(defn init [f]
  (let [content "some nice content"]
    (dosync 
      (ref-set root 
        (Root. "" 0755 [:description "Root directory"] 0
          {"README" (File. "README" 0644 [:description "A Readme File" :mimetype "text/plain"] 0 (. content getBytes) (. content length))})))
    (f)))

(use-fixtures :once init)

(deftest update-root
  (update-atime "/README" 123)
  (is (= (get-in @root [:files "README" :lastmod]) (/ 123 1000))))

(deftest key-looup-test
    (is (= (lookup-keys "/bla/name/bl.txt") '(:files "bla" :files "name" :files))))

(deftest node-creation
    (let [file (File. "bla.txt" 0644 [:description "" :mimetype ""] 0 nil nil)]
      (add-file "/bla.txt" file)
      (is (= file (lookup "/bla.txt")))))

(deftest node-deletion
    (remove-file "/bla.txt")
    (is (nil? (lookup "/bla.txt"))))

