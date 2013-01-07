(ns clj-ring-aws-tvm.core-test
  (:use clojure.test
        clj-ring-aws-tvm.core
        ring.adapter.jetty)
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]))

;;; load aws keys from a file, the file should contain something like
;; (def test-keys {:access-key-id <KEY_ID>
;;                 :secret-access-key <SECRET_KEY>})
;; where <KEY_ID> and <SECRET_KEY> are working aws keys for a user who
;; has the GetFederationToken permission
(load-file (str (System/getProperty "user.home") "/.test.aws.keys.clj"))

(def creds (make-creds test-keys))

(def bad-policy (slurp "bad_policy.json"))

(def handler (make-handler creds
                           (fn [user]
                             (if (= "Alice" user)
                               nil
                               bad-policy))
                           (fn [user]
                             (condp = user
                               "Alice" 1200      ; 15 minutes
                               "Bob" 300
                               900))
                           (fn [request]
                             (slurp (:body request)))))

(defmacro with-handler [handler & body]
  `(let [server# (run-jetty ~handler {:port 4347, :join? false})]
       (try 
         (Thread/sleep 1000)
         ~@body
         (finally (.stop server#)))))

(def url "http://localhost:4347")

(deftest test-success
  (with-handler handler
    (let [now (System/currentTimeMillis)
          response (http/post url {:body "Alice"})
          creds-map (json/read-str (:body response))
          expiration (get creds-map "expiration")]
      (is (= (:status response) 200))
      (is (get creds-map "access-key-id"))
      (is (get creds-map "secret-access-key"))
      (is (get creds-map "session-token"))
      (is (< (Math/abs (- expiration now 1200000)) 60000) "number of milliseconds between a moment taken before the request and expiration should be a around 1200000 give or take a minute (1200000 ms. is the duration specified for Alice's request)"))))

(deftest test-fail
  (with-handler handler
    (let [response (http/post url {:body "Bob" :throw-exceptions false})
          body (json/read-str (:body response))
          error-message (get body "error-message")]
      (is (= 503 (:status response)))
      (is error-message)
      (is (re-matches #"^.*Member must have value greater than or equal to 900" error-message)))))
