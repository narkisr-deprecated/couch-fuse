(ns com.narkisr.attachments
  (:import java.io.File)
  (:use
    (com.narkisr common-test)
    (clojure.contrib shell-out (duck-streams :only [slurp*]) test-is str-utils)))

; these tests actually mount a live couchdb therefor they require one up
(use-fixtures :once mount-and-sleep)

(deftest add-attachment
  (spit (File. file-path) "<html>hello world</html>")
  (is (= (-> (File. file-path) slurp*) "<html>hello world</html>"))
  (sh "rm" file-path)
  (is (not (-> file-path (File.) (.exists))))
  (spit (File. file-path) "<html>hello world</html>")
  (sh "mv" file-path rename-path)
  (is (= (-> (File. rename-path) slurp*) "<html>hello world</html>")))

(deftest multiple-attachments
   (let [names (range 0 20) target "fake/multi/" hidden "fake/.multi"]
     (-> target (File.) (.mkdir))
     (doseq [name names] 
       (spit (str target name)  name))
     (doseq [name names] 
       (is (=  (-> (str target name) (File.) (.exists))  true)))
     (-> target (File.) (.delete))
     (-> hidden (File.) (.delete))))
