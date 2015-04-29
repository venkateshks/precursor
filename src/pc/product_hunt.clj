(ns pc.product-hunt
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.core.memoize :as memo]
            [clojure.tools.logging :as log]
            [hiccup.core :as h]))

(def api-token "72790c16bb982b444377523ed715075101240cb3370f642def65551b39bf1270")

(defn get-post-info* [post-id]
  (-> (http/get (str "https://api.producthunt.com/v1/posts/" post-id)
                {:headers {"Authorization" (str "Bearer " api-token)
                           "Content-Type" "application/json"}
                 :throw-exceptions false
                 :socket-timeout 500
                 :conn-timeout 500})
    :body
    json/decode
    (get "post")))

(def get-post-info (memo/ttl get-post-info* :ttl/threshold (* 1000 60 10)))

(def fallback-info
  {11067 {"name" "Precursor",
          "votes_count" 341,
          "comments_count" 10,
          "tagline" "A real-time collaborative prototyping tool"
          "discussion_url" "http://www.producthunt.com/posts/precursor"
          "makers" [{"image_url" {"30px" "http://avatars-cdn.producthunt.com/106624/30?1430244521"}}]
          "votes" [{"user"
                    {"image_url" {"30px" "http://avatars-cdn.producthunt.com/7824/30?1430236722"}}}]}})

(defn post-info [post-id]
  (try
    (get-post-info post-id)
    (catch Exception e
      (log/infof "Error getting post info for %s" post-id)
      (fallback-info post-id))))

(defn product-hunt-component [post-id]
  (let [info (post-info post-id)
        make-link (fn [content]
                    [:a {:href (get info "discussion_url")}
                     content])]
    (h/html
     [:div.product-hunt-card
      [:div.votes-count (make-link (get info "votes_count"))]
      [:div.post-name (make-link (get info "name"))]
      [:div.tagline (get info "tagline")]
      [:div.comments-count (make-link (get info "comments_count"))]
      [:div.makers
       (for [maker (get info "makers")]
         [:img.avatar {:src (get-in maker ["image_url" "30px"])}])]
      (when-let [voter (some-> info (get "votes") first (get "user"))]
        [:div.hunter
         [:img.avatar {:src (get-in voter ["image_url" "30px"])}]])])))