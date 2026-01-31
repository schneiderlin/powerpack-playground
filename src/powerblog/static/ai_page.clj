(ns powerblog.static.ai-page
  (:require
   [clojure.string :as str]
   [datomic.api :as d]))

(defn get-ai-posts [db]
  (->> (d/q '[:find [?e ...]
              :in $ ?tag
              :where
              [?e :blog-post/tags ?tag]]
            db :AI)
       (map #(d/entity db %))))

(defn blog-post-card [post]
  [:a {:href (:page/uri post)
       :class "card card-hoverable block post"
       :data-tags (str/join "," (:blog-post/tags post))}
   [:div {:class "p-6 space-y-4"}
    [:h3 {:class "card-title text-lg font-semibold text-gray-900 leading-tight"}
     (:page/title post)]
    [:div {:class "flex flex-wrap gap-2"}
     (for [tag (take 3 (:blog-post/tags post))]
       [:span {:class "badge badge-neutral"}
        (name tag)])]
    [:p {:class "text-sm text-gray-600 leading-relaxed line-clamp-3"}
     (:page/description post "")]
    [:div {:class "card-meta flex items-center gap-2 text-xs text-gray-600"}
     [:svg {:class "w-4 h-4" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2"}
      [:path {:d "M8 2v4"}]
      [:path {:d "M16 2v4"}]
      [:rect {:width "18" :height "18" :x "3" :y "4" :rx "2"}]
      [:path {:d "M3 10h18"}]]
     [:span (:page/date post "")]]]])

(defn page [db]
  [:div {:class "layout-container layout-section-spacing"}
   [:section {:class "section-block"}
    [:h1 {:class "text-3xl md:text-4xl font-bold text-gray-900 section-title"}
     "AI Topic"]
    [:p {:class "text-lg text-gray-600 max-w-2xl"}
     "Articles about AI, LLMs, and artificial intelligence systems."]]
   [:section {:class "section-block"}
    [:div {:class "section-label"}
     "AI Articles"]
    [:div {:class "layout-grid-3"}
     (for [post (get-ai-posts db)]
       (blog-post-card post))]]])
