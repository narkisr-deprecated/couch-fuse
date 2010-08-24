(ns com.narkisr.file-info
  (:refer-clojure :exclude [partition])
  (:use (clojure.contrib (str-utils2 :only [partition])) pattern-match)
  (:import java.io.File))

(defn fname [path] (-> path (File.) (.getName)))

(defn parent-name [path] (-> path (File.) (.getParentFile) (.getName)))

(defn parent-path [path] (-> path (File.) (.getParent)))

(defn file-path [file] (-> file (File.) (.getPath)))

(defn hidden [folder] (str (parent-path folder) "/." (fname folder)))

(defn split-path [path] (partition path #"/"))

(defn to-hidden [path]
  (match (split-path path)
    ["/" dir "/" file] (str "/." dir "/" file)
    _ (str (hidden (parent-path path)) "/" (fname path))))

(defn combine [parent child]
  (if (= parent "/") 
    (str parent child) (str parent "/" child)))

