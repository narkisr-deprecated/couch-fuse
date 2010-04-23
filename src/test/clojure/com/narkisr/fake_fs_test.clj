(ns com.narkisr.fake-fs-test
  (:import fuse.FuseFtypeConstants fuse.Errno)
  (:use clojure.contrib.test-is com.narkisr.fake-fs com.narkisr.mocking))


(deftest getdir-error
  (is (= 0 (fs-getdir "this" "/" (mock fuse.FuseDirFiller))))
  (is (= Errno/ENOTDIR (fs-getdir "this" "/README" (mock fuse.FuseDirFiller))))
  )