(ns com.narkisr.file-info
  (:import java.io.File))

(defn fname [path] (-> path (File.) (.getName)))

(defn parent-name [path] (-> path (File.) (.getParentFile) (.getName)))

(defn file-path [file] (-> file (File.) (.getPath)))

