(defproject couch-fuse "0.4.1"
  :description "couchdb fuse file system"
  :dependencies [
          [org.mockito/mockito-all "1.8.1"]      
          [org.clojure/clojure "1.3.0"]
          [org.clojure/core.match "0.2.0-alpha9"]
          [org.clojure/tools.cli "0.2.1"]
          [commons-io/commons-io "1.4"]
          [fuse4j/fuse4j-core "2.4.0.0-SNAPSHOT"]
          [clj-http "0.4.0"]
          [com.narkisr/clojure-couchdb  "0.5.0"]
          [slingshot "0.10.2"]    
          [cheshire "4.0.0"]]

   :dev-dependencies [
         [circumspec/circumspec "0.0.13"]        
         [commons-logging/commons-logging "1.1.1"]        
         [log4j/log4j "1.2.15" :exclusions [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri] ]         
         [native-deps "1.0.5"]
         [org.clojure/tools.trace "0.7.3"]
    ]
   
  :native-dependencies [
        [com.narkisr/fuse4j-linux-native "2.4.0"]
   ]

  
  :jvm-opts [~(str "-Djava.library.path=native/:" (System/getenv "LD_LIBRARY_PATH"))]; http://tinyurl.com/7h6vr6s  
  :main  com.narkisr.couchfs.mounter
  :aot [com.narkisr.couchfs.mounter]
  :disable-deps-clean true ; preventing native cleanup during uberjar
)

