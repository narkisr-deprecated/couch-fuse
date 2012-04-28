(ns com.narkisr.file-info
  (:refer-clojure :exclude [partition])
  (:use 
    [clojure.core.match :only [match]]
    )
  (:import java.io.File)
  )

(defn fname [path] (-> path (File.) (.getName)))

(defn parent-name [path] (-> path (File.) (.getParentFile) (.getName)))

(defn parent-path [path] (-> path (File.) (.getParent)))

(defn file-path [file] (-> file (File.) (.getPath)))

(defn hide [name] (str "." name))

(defn hidden [folder] (str (parent-path folder) "/." (fname folder)))

(defn un-hide [folder]
  (let [parent (parent-path folder) name (. (fname folder) replaceFirst "\\." "")]
    (if (= parent "/") 
      (str "/" name)
      (str  parent "/" name  ))))

(defn partition
  "See http://tinyurl.com/cv5sfr4" 
  [s re]
  (let [m (re-matcher re s)]
    ((fn step [prevend]
       (lazy-seq
        (if (.find m)
          (cons (.subSequence s prevend (.start m))
                (cons (re-groups m)
                      (step (+ (.start m) (count (.group m))))))
          (when (< prevend (.length s))
            (list (.subSequence s prevend (.length s)))))))
     0)))

(defn split-path [path] (rest (partition path #"/")))

(defn to-hidden [path]
  (match [(split-path path)] 
         [(["/" dir] :seq)] (str "/." dir)
         [(["/" dir "/" file] :seq)] (str "/." dir "/" file)
         :else (str (hidden (parent-path path)) "/" (fname path))))

(defn combine [parent child]
  (if (= parent "/") 
    (str parent child) (str parent "/" child)))

