(ns competencies.content
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [org.httpkit.client :as http]
            [taoensso.timbre :as log]
            [clojure.spec :as s]
            [clojure.future :refer :all]))


(s/def ::content-type string?)
(s/def ::limit pos-int?)
(s/def ::total pos-int?)
(s/def ::skip pos-int?)
(s/def ::query-params (s/keys :req [(or ::content-type
                                        ::limit
                                        ::total
                                        ::skip)]))

(s/def ::space (and string? #(> (count %) 10)))
(s/def ::access-token (and string? #(> (count %) 10)))
(s/def ::content-repo (s/keys :req [::space ::access-token]))

(def repo-state (atom {}))

(defn base-url [repo]
  (str "https://cdn.contentful.com/spaces/" (:space repo)))

(defn entries-url [repo]
  (str (base-url repo) "/entries" ))

(defn entry-url [repo id]
  (str (entries-url repo) "/" id))

(defn extract-data [response]
  {:status (:status @response)
   :body (json/parse-string (:body @response) true)
   :headers (:headers @response)})

(defn add-access-token [params repo]
  (assoc params "access_token" (:access-token repo)))

(defn snakecase-params [params]
  (into {}
        (for [[k v] params] [(string/replace (name k) "-" "_") v])))

(defn prepare-query-params [params repo]
  (-> params (snakecase-params) (add-access-token repo)))

(defn do-get [url params]
  (log/debug (str "HTTP GET: " url ", params " params) )
  (http/get url params))

(defn cache-headers [response]
  {:etag (get-in [:headers :etag] response "")})

(defn fetch
  ([repo url query-params]
   (s/valid? ::query-params query-params)
   (let [prepared-params (prepare-query-params repo query-params)
         response (http/get url prepared-params)]
     (swap! repo-state assoc  url (cache-headers response))))
  ([repo url]
   (fetch repo url {})))

(defn fetch-entry [repo id]
  (log/info (str "Fetching entry with id: " id))
  (fetch (repo entry-url id)))

(defn fetch-entries
  ([repo query-params]
   (log/info (str "Fetching entries: " query-params))
   (fetch entries-url query-params))
  ([repo]
   (fetch-entries repo {})))

(def content-repo
  {:space "458l5dsk4drc"
   :access-token "1a3fb6708e9549c27e79105349fe73305310d612b7a04d3c71726f599b1472b5"})


;;;@(fetch-entry content-repo "6N2uWeWtC80q4ooQmCaOOo")
