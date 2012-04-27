(ns com.narkisr.utils
  (:import java.lang.IllegalArgumentException))

; see http://tinyurl.com/d5kceu8  
(defmacro do-template
  "Repeatedly evaluates template expr (in a do block) using values in
  args.  args are grouped by the number of holes in the template.
  Example: (do-template (check _1 _2) :a :b :c :d)
  expands to (do (check :a :b) (check :c :d))"
  [expr & args]
  (when-not (template? expr)
    (throw (IllegalArgumentException. (str (pr-str expr) " is not a valid template."))))
  (let [expr (walk/postwalk-replace {'_ '_1} expr)
        argcount (count (find-holes expr))]
    `(do ~@(map (fn [a] (apply-template expr a))
                (partition argcount args)))))
