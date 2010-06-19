(ns com.narkisr.fs-logic-test
  (:use clojure.contrib.test-is com.narkisr.fs-logic com.narkisr.mocking))

(defn init [f]
  (dosync (ref-set root (create-node directory "" 0755 [:description "Root directory"]
    {"README" (create-node file "README" 0644 [:description "A Readme File" :mimetype "text/plain"] (. "this is a nice readme contents" getBytes))})))
  (f)
  )

(use-fixtures :once init)

(deftest update-root
  (update-atime "/README" 123)
  (is (= (get-in @root [:files "README" :lastmod]) (/ 123 1000))))

(deftest key-looup-test
  (is (= (lookup-keys "/bla/name/bl.txt") '(:files "bla" :files "name" :files))))
;
(deftest node-creation
  (let [file (create-node file "bla.txt" 0644 [:description "" :mimetype ""] nil nil)]
    (add-file "/bla.txt" file)
    (is (= file (lookup "/bla.txt")))))
;
(deftest node-deletion
  (remove-file "/bla.txt")
  (is (nil? (lookup "/bla.txt"))))

;(lookup-keys (rest (partition "/1077214558877334645/ae70b718342bc0d140743709e21cdbe6.jpeg" #"/")))
;(lookup-keys (rest (partition "/1077214558877334645" #"/")))
;(lookup-keys (rest (partition "/1077214558877334645/" #"/")))
;(lookup-keys (rest (partition "/" #"/")))

