(ns com.narkisr.fake-fs-test
  (:import fuse.FuseFtypeConstants fuse.Errno org.mockito.Mockito)
  (:use clojure.contrib.test-is com.narkisr.fake-fs))


(defn mock [class] (Mockito/mock class))

(deftest getdir-error
  (is (= 0 (fs-getdir "this" "/" (mock fuse.FuseDirFiller))))
  (is (= Errno/ENOTDIR (fs-getdir "this" "/README" (mock fuse.FuseDirFiller)))))