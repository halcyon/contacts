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

(defn authorize-uri
  []
  (str (:authorize-uri oauth2-params)
       "?response_type=code"
       "&client_id=" (url-encode (:client-id oauth2-params))
       "&redirect_uri=" (url-encode (:redirect-uri oauth2-params))
       "&scope=" (url-encode (:scope oauth2-params))
       ;; "&state=" (url-encode csrf-token)
       ))

(defn update-access-token!
  [authorization-code]
  (let [authorization-resp (try
                             (http/post (:access-token-uri oauth2-params)
                                        {:form-params {:code         authorization-code
                                                       :grant_type   "authorization_code"
                                                       :client_id    (:client-id oauth2-params)
                                                       :redirect_uri (:redirect-uri oauth2-params)}
                                         :basic-auth  [(:client-id oauth2-params) (:client-secret oauth2-params)]})
                             (catch Exception _ nil))
        grant-resp         (json/parse-string (:body authorization-resp)
                                              true)]
    (swap! auth-db assoc :grant grant-resp)))
