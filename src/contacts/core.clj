(ns contacts.core
  (:require
   [cemerick.url :refer [url-encode]]
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.data.xml :as xml]))

(def auth-db (atom {}))

(def oauth2-params
  {:client-id (System/getenv "GOOGLE_CLIENT_ID")
   :client-secret (System/getenv "GOOGLE_CLIENT_SECRET")
   :authorize-uri "https://accounts.google.com/o/oauth2/auth"
   :redirect-uri "urn:ietf:wg:oauth:2.0:oob"
   :access-token-uri "https://accounts.google.com/o/oauth2/token"
   :scope "profile https://www.googleapis.com/auth/contacts"})

(defn authorize-uri [client-params csrf-token]
  (str
   (:authorize-uri client-params)
   "?response_type=code"
   "&client_id="
   (url-encode (:client-id client-params))
   "&redirect_uri="
   (url-encode (:redirect-uri client-params))
   "&scope="
   (url-encode (:scope client-params))
   "&state="
   (url-encode csrf-token)))

(defn get-authentication-response [csrf-token response-params]
  (when (= csrf-token (:state response-params))
    (try
      (:body (http/post (:access-token-uri oauth2-params)
                        {:form-params {:code         (:code response-params)
                                       :grant_type   "authorization_code"
                                       :client_id    (:client-id oauth2-params)
                                       :redirect_uri (:redirect-uri oauth2-params)}
                         :basic-auth [(:client-id oauth2-params) (:client-secret oauth2-params)]
                         :as          :json}))
      (catch Exception _ nil))))


(authorize-uri oauth2-params "bob")
(defn auth
  [code]
  (let [response (json/parse-string (get-authentication-response "bob" {:state "bob"
                                                                        :code code})
                                    true)]
    (swap! auth-db assoc :response response)))
