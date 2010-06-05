(ns com.narkisr.fs-logic-test
  (:use clojure.contrib.test-is com.narkisr.fs-logic com.narkisr.mocking))

(alter-var-root #'root
  (fn [_] (create-node directory "" 0755 [:description "Root directory"]
    {"README" (create-node file "README" 0644 [:description "A Readme File" :mimetype "text/plain"] (. "this is a nice readme contents" getBytes))})))

(deftest update-root
  (update "/README" :lastmod 123)
  (is (= (get-in root [:files "README" :lastmod]) 123)))

(deftest key-looup-test
  (is (= (lookup-keys "/bla/name/bl.txt") '(:files "bla" :files "name" :files))))


;(lookup-keys (rest (partition "/1077214558877334645/ae70b718342bc0d140743709e21cdbe6.jpeg" #"/")))
;(lookup-keys (rest (partition "/1077214558877334645" #"/")))
;(lookup-keys (rest (partition "/1077214558877334645/" #"/")))
;(lookup-keys (rest (partition "/" #"/")))

