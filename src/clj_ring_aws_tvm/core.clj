(ns clj-ring-aws-tvm.core
  (:import com.amazonaws.auth.BasicAWSCredentials
           com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient
           com.amazonaws.services.securitytoken.model.GetFederationTokenRequest)
  (:require [clojure.data.json :as json]))

(defn make-creds [{:keys [access-key-id secret-access-key]}]
  (BasicAWSCredentials. access-key-id secret-access-key))

(defn make-sts [creds]
  (AWSSecurityTokenServiceClient. creds))

(defn make-request [user duration policy]
  (-> (GetFederationTokenRequest.)
      (.withName user)
      (.withDurationSeconds (int duration))
      (.withPolicy policy)))

(defn extract-credentials [token-result]
  (let [creds (.getCredentials token-result)]
    {:access-key-id (.getAccessKeyId creds)
     :secret-access-key (.getSecretAccessKey creds)
     :session-token (.getSessionToken creds)
     :expiration (-> creds .getExpiration .getTime)}))

(defn make-handler
  "Creates a Ring handler.
  creds an instance of AWSCredentials, you can use the make-creds function to create an instance of BasicAWSCredentials.
  policy-fn a function which takes a user and returns an AWS policy string.
  duration-fn a function which takes a user and returns duration in milliseconds.
  user-extractor-fn a function which takes a Ring request map and returns a user."
  [creds policy-fn duration-fn user-extractor-fn]
  (let [sts (make-sts creds)]
    (fn [request]
      (let [user (user-extractor-fn request)
            policy (policy-fn user)
            duration (duration-fn user)
            token-request (make-request user duration policy)]
        (try 
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body (json/write-str
                  (extract-credentials
                   (.getFederationToken sts token-request)))}
          (catch Throwable e
            {:status 503
             :headers {"Content-Type" "application/json"}
             :body (json/write-str
                    {:error-message (.getMessage e)})}))))))
