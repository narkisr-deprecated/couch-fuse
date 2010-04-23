(ns com.narkisr.mocking
  (:import org.mockito.Mockito))

(defn mock [class] (Mockito/mock class))