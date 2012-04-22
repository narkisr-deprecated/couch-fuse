(defproject couch-fuse "0.4.1"
  :description "couchdb fuse file system"
  :dependencies [
          [org.mockito/mockito-all "1.8.1"]      
          [org.clojure/clojure "1.2.0"]
          [org.clojure/clojure-contrib "1.2.0"]
          [commons-io/commons-io "1.4"]
          [fuse4j/fuse4j-core "2.4.0.0-SNAPSHOT"]
          [org.clojars.narkisr/clojure-couchdb "0.2.3"]
          [pattern-match/pattern-match "1.0.0"] ]

   :dev-dependencies [
         [circumspec/circumspec "0.0.13"]        
         [commons-logging/commons-logging "1.1.1"]        
         [log4j/log4j "1.2.15"]        
         [native-deps "1.0.5"]
    ]
   
  :native-dependencies [
        [com.narkisr/fuse4j-linux-native "2.4.0"]
   ]

  :exclusions [ javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri ] 
  :jvm-opts [~(str "-Djava.library.path=native/:" (System/getenv "LD_LIBRARY_PATH"))]; http://tinyurl.com/7h6vr6s  
  :main  com.narkisr.couchfs.mounter
  :aot [com.narkisr.couchfs.mounter]
  :disable-deps-clean true ; preventing native cleanup during uberjar
)

