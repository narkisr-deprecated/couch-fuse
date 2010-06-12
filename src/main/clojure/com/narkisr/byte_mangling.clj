(ns com.narkisr.byte-mangling
  (:import java.util.Arrays)
  )


(defn concat-bytes [first second]
  #^{:test (fn [] (assert (= "blacla" (java.lang.String. (concat-bytes (.  "bla" getBytes) (. "cla" getBytes))))))}
  (let [first-len (alength first ) second-len (alength second ) copy (. Arrays copyOf first (+ first-len second-len))]
    (System/arraycopy second 0 copy first-len second-len)
    copy
    ))


